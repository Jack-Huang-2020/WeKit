# Reproducible builds

WeKit builds are reproducible **per commit**: anyone can rebuild a given commit
and obtain a **byte-for-byte identical APK**, signature included, using the
committed fixed debug key.

## What "reproducible" means here

An Android release APK is a ZIP whose bytes are:

1. the ZIP entries (`classes.dex`, native `.so` libraries, resources, assets…),
2. the central directory + end-of-central-directory record,
3. an **APK Signing Block** (v2/v3/v4 signatures) inserted before the central
   directory.

Parts 1 and 2 are fully determined by the source at a commit. Part 3 is
deterministic too — APK v2/v3 RSA signatures use RSASSA-PKCS#1-v1.5, which
produces identical bytes for identical (key, content) — **as long as the same
signing key is used**. The build therefore ships a fixed debug key
(`reproducible/debug.keystore`, with a pinned certificate validity date) as the
default signing key, so unofficial rebuilds match byte-for-byte.

Two things had to be neutralised to reach full byte-identity:

- `BuildConfig.BUILD_TIMESTAMP` — now derived from `SOURCE_DATE_EPOCH` (or the
  commit time), not `System.currentTimeMillis()`.
- AGP's **SDK dependency block** (signing-block pair id `0x504b4453`) — an
  encrypted, non-deterministic blob AGP injects for Google Play. Disabled via
  `dependenciesInfo { includeInApk = false }`.

Official releases are signed with a **private release key** whose signature you
cannot reproduce without the key. For those, reproducibility is verified on the
signature-independent content (the APK with its signing block stripped); see
`--strip-sig` below and the `contentSha256` published in `update.json`.

## The build environment

`reproducible/Containerfile` pins every input that can affect output:

| Component      | Pinned to                     |
|----------------|-------------------------------|
| JDK            | Temurin 21.0.11_10            |
| Android platform | `android-37.0`              |
| Android build-tools | `37.0.0` + `36.0.0` (AGP default) |
| Android NDK    | `30.0.14904198`               |
| Rust           | stable `1.96.1`               |

Rust is additionally pinned by `app/src/main/rust/wekit-native/rust-toolchain.toml`,
and the `silk-v3-sys` git dependency is pinned by `rev` in `Cargo.toml`.

Build the image (once):

```bash
podman build -t wekit-build -f reproducible/Containerfile .
```

Behind a proxy, add `--network=host --build-arg http_proxy=… --build-arg https_proxy=…`.

## Verifying a commit

```bash
# Build HEAD twice and confirm the content hashes match:
reproducible/verify-reproducible.sh --twice

# Or just print the content hashes for a specific commit:
reproducible/verify-reproducible.sh <commit-ish>
```

The script clones the commit into a clean tree (so no dirty/untracked files
leak in), builds it inside the `wekit-build` image, and prints the SHA-256 of
each APK. By default these are raw `.apk` hashes (fully byte-identical with the
committed debug key). With `--twice` it builds the commit two times
independently and diffs the results.

For a build with a real release key (`WEKIT_KEYSTORE_*` set), add `--strip-sig`
so the comparison ignores the private-key-dependent signing block.

Behind a proxy, pass podman flags through `PODMAN_EXTRA_ARGS`:

```bash
export PODMAN_EXTRA_ARGS="--network=host \
  -e http_proxy=http://127.0.0.1:7890 -e https_proxy=http://127.0.0.1:7890"
reproducible/verify-reproducible.sh --twice
```

If a build hits memory pressure (the release Rust profile uses `lto=true` +
`codegen-units=1`), constrain it with `--memory=…` in `PODMAN_EXTRA_ARGS` and
lower Gradle/Cargo parallelism.

## How CI publishes the proof

On every push to `master`, CI:

1. sets `SOURCE_DATE_EPOCH` to the commit time so `BuildConfig.BUILD_TIMESTAMP`
   is deterministic,
2. builds the signed release APKs (real release key),
3. runs `strip-sigblock-hash.py` over them and records the content hashes,
4. embeds them in `update.json` under `contentSha256`, published on the rolling
   `CI` release.

Because official releases use a private key, CI publishes the
signature-independent digest. To verify the official build of a commit:

```bash
# your rebuild, ignoring the (differing) release signature:
reproducible/verify-reproducible.sh --strip-sig <commit>
# compare each hash against the "contentSha256" map in the release's update.json
```

Identical hashes prove the published APK was built from that exact source; the
differing signature is expected. (If you instead build with the default debug
key, the raw hashes will differ from the official ones only in the signature —
that is the same fact viewed from the other side.)

## Determinism notes

Historical sources of non-determinism, now all neutralised:

- `BuildConfig.BUILD_TIMESTAMP` — was `System.currentTimeMillis()`, now derived
  from `SOURCE_DATE_EPOCH` (or the commit time when the worktree is clean).
- AGP SDK dependency block — disabled via `dependenciesInfo`.
- Signing key — a fixed debug key is committed so unofficial rebuilds are fully
  byte-identical; RSA PKCS#1-v1.5 signing is itself deterministic.

Everything else — the Rust native libraries, KSP-generated `FeaturesProvider`,
method-hash tables, embedded AboutLibraries and Eruda sources — was already
byte-identical build to build. Eruda is vendored at
`app/src/main/vendor/eruda/eruda.min.js` (checksum-guarded) rather than fetched
from a CDN, so the build is fully offline/hermetic.
