#!/usr/bin/env bash
# Verify that a given commit builds reproducibly.
#
# Builds the target commit inside the pinned `wekit-build` container image and
# reports the SHA-256 of every release APK. With --twice, it builds the same
# commit two times in independent output trees and diffs the results, proving
# determinism end-to-end.
#
# By default the APK is signed with the committed fixed debug key
# (reproducible/debug.keystore) and AGP's dependency block is disabled, so the
# ENTIRE .apk — signature included — is byte-for-byte reproducible.
#
# Use --strip-sig when building with a real release key (WEKIT_KEYSTORE_* set):
# that key's signature cannot be reproduced without the private key, so the
# check compares a signature-independent digest instead. See reproducible/README.md.
#
# Usage:
#   reproducible/verify-reproducible.sh [--twice] [--keep] [--strip-sig] [<commit-ish>]
#
#   <commit-ish>  commit to build (default: HEAD)
#   --twice       build twice and compare hashes (determinism check)
#   --keep        keep the temporary checkout/output dirs on exit
#   --strip-sig   compare signature-independent content (for real-key builds)
#
# Requires: podman, git, and the `wekit-build` image
# (podman build -t wekit-build -f reproducible/Containerfile .).

set -euo pipefail

IMAGE="${WEKIT_BUILD_IMAGE:-wekit-build}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Persistent, shared dependency caches (Gradle + Cargo registry). These hold
# only downloaded artifacts, never build outputs, so sharing them across passes
# does not affect reproducibility — it just avoids re-downloading (and makes the
# build resilient to proxy/network flakiness). Override with WEKIT_CACHE_DIR.
CACHE_DIR="${WEKIT_CACHE_DIR:-${TMPDIR:-/tmp}/wekit-repro-cache}"
mkdir -p "$CACHE_DIR/gradle" "$CACHE_DIR/cargo-registry"

TWICE=0
KEEP=0
STRIP_SIG=0
COMMIT="HEAD"
for arg in "$@"; do
    case "$arg" in
        --twice)      TWICE=1 ;;
        --keep)       KEEP=1 ;;
        --strip-sig)  STRIP_SIG=1 ;;
        -*)           echo "unknown flag: $arg" >&2; exit 2 ;;
        *)            COMMIT="$arg" ;;
    esac
done

COMMIT_SHA="$(git -C "$REPO_ROOT" rev-parse "$COMMIT")"
COMMIT_SHORT="$(git -C "$REPO_ROOT" rev-parse --short "$COMMIT")"

WORK="$(mktemp -d "${TMPDIR:-/tmp}/wekit-repro.XXXXXX")"
cleanup() {
    if [[ "$KEEP" == "1" ]]; then
        echo "kept work dir: $WORK" >&2
    else
        rm -rf "$WORK"
    fi
}
trap cleanup EXIT

echo ">> commit:  $COMMIT_SHA ($COMMIT_SHORT)"
echo ">> image:   $IMAGE"
echo ">> workdir: $WORK"

# Produce a clean, deterministic source tree for the commit, including
# submodules pinned to their recorded SHAs. A fresh clone (rather than the live
# working tree) guarantees no untracked/dirty files leak into the build.
prepare_src() {
    local dest="$1"
    # A full local clone (not --shared): the build container does not mount the
    # host repo, so object alternates pointing back at it would be unresolvable.
    git clone --quiet --no-checkout "$REPO_ROOT" "$dest"
    git -C "$dest" checkout --quiet "$COMMIT_SHA"
    # Submodule URLs point at public remotes; fetch them at the pinned commits.
    git -C "$dest" submodule update --init --recursive --quiet
}

# Build one source tree in the container. Writes APKs into <src>/app/build/...
# PODMAN_EXTRA_ARGS lets the caller add e.g. proxy/network flags:
#   PODMAN_EXTRA_ARGS="--network=host -e http_proxy=http://127.0.0.1:7890 -e https_proxy=http://127.0.0.1:7890"
build_one() {
    local src="$1"
    # shellcheck disable=SC2086
    podman run --rm \
        --userns=keep-id \
        ${PODMAN_EXTRA_ARGS:-} \
        -v "$src:/workspace:Z" \
        -v "$CACHE_DIR/gradle:/opt/gradle-cache:Z" \
        -v "$CACHE_DIR/cargo-registry:/opt/cargo-registry:Z" \
        -e ANDROID_HOME=/opt/android-sdk \
        -e GRADLE_USER_HOME=/opt/gradle-cache \
        -e CARGO_HOME=/opt/cargo-registry \
        -e SOURCE_DATE_EPOCH="$(git -C "$REPO_ROOT" log -1 --format=%ct "$COMMIT_SHA")" \
        -w /workspace \
        "$IMAGE" \
        bash -c '
            set -e
            # cargo needs its bin on PATH even when CARGO_HOME is remapped.
            export PATH="/opt/cargo/bin:$PATH"
            # git in the container sees a bind-mounted tree owned by another uid;
            # allow it so the build-time version-derivation git calls succeed.
            git config --global --add safe.directory /workspace
            git config --global --add safe.directory "*"
            # The Gradle wrapper downloader uses the JVM HTTP stack, which reads
            # proxy settings from system properties, not $http_proxy. Derive them
            # from $http_proxy (host:port) when present so downloads work behind a
            # proxy without passing space-containing args through podman.
            if [ -n "${http_proxy:-}" ]; then
                hostport="${http_proxy#*://}"; hostport="${hostport%/}"
                phost="${hostport%%:*}"; pport="${hostport##*:}"
                export GRADLE_OPTS="-Dhttp.proxyHost=$phost -Dhttp.proxyPort=$pport -Dhttps.proxyHost=$phost -Dhttps.proxyPort=$pport"
            fi
            chmod +x gradlew
            # Retry once: dependency downloads through a proxy can hit transient
            # TLS errors. Build outputs are unaffected by the retry.
            ./gradlew :app:assembleRelease \
                --no-daemon --no-build-cache --no-configuration-cache \
            || ./gradlew :app:assembleRelease \
                --no-daemon --no-build-cache --no-configuration-cache
        '
}

# Hash every release APK in a build tree, sorted by filename.
#
# By default this hashes the RAW .apk bytes: with the committed fixed debug key
# (reproducible/debug.keystore) and AGP's dependency block disabled, the entire
# APK — signature included — is byte-for-byte reproducible.
#
# With --strip-sig it hashes a signature-INDEPENDENT digest instead (the APK
# with its signing block removed). Use that when building with a real release
# key, whose signature you cannot reproduce without the private key.
hash_apks() {
    local src="$1"
    local dir="$src/app/build/outputs/apk/release"
    for f in $(cd "$dir" && find . -name '*.apk' | sort); do
        if [[ "$STRIP_SIG" == "1" ]]; then
            printf '%s  %s\n' "$(python3 "$SCRIPT_DIR/strip-sigblock-hash.py" "$dir/$f")" "$f"
        else
            printf '%s  %s\n' "$(sha256sum "$dir/$f" | cut -d' ' -f1)" "$f"
        fi
    done
}

if [[ "$STRIP_SIG" == "1" ]]; then
    HASH_LABEL="signature-independent APK SHA-256"
else
    HASH_LABEL="APK SHA-256 (raw, signature included)"
fi

SRC1="$WORK/build1"
prepare_src "$SRC1"
echo ">> building (pass 1)..."
build_one "$SRC1"

echo ""
echo "=== $HASH_LABEL (pass 1) ==="
HASH1="$(hash_apks "$SRC1")"
echo "$HASH1"

if [[ "$TWICE" == "1" ]]; then
    SRC2="$WORK/build2"
    prepare_src "$SRC2"
    echo ""
    echo ">> building (pass 2)..."
    build_one "$SRC2"

    echo ""
    echo "=== $HASH_LABEL (pass 2) ==="
    HASH2="$(hash_apks "$SRC2")"
    echo "$HASH2"

    echo ""
    if diff <(echo "$HASH1") <(echo "$HASH2") >/dev/null; then
        if [[ "$STRIP_SIG" == "1" ]]; then
            echo "RESULT: REPRODUCIBLE — both passes produced identical APK content"
            echo "        (ignoring the private-key-dependent signing block)."
        else
            echo "RESULT: REPRODUCIBLE — both passes produced byte-identical APKs."
        fi
    else
        echo "RESULT: NOT REPRODUCIBLE — hashes differ between passes:"
        diff <(echo "$HASH1") <(echo "$HASH2") || true
        echo ""
        echo "To locate the differing bytes, install diffoscope and run it on the two APKs:"
        echo "  diffoscope $SRC1/app/build/outputs/apk/release/<name>.apk \\"
        echo "             $SRC2/app/build/outputs/apk/release/<name>.apk"
        echo "(re-run with --keep to retain the build trees)"
        exit 1
    fi
fi
