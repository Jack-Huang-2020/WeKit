#!/usr/bin/env python3
"""Print the SHA-256 of an APK with its APK Signing Block removed.

v2/v3/v4 APK signatures depend on the signing private key and are therefore not
reproducible by third parties. This digest covers everything else — all ZIP
entries, the central directory and the EOCD — which IS reproducible from source.
Two builds of the same commit produce the same digest here even though the
signed .apk files differ.

Usage: strip-sigblock-hash.py <apk> [<apk> ...]
"""
import hashlib
import struct
import sys

APK_SIG_BLOCK_MAGIC = b"APK Sig Block 42"


def content_digest(path: str) -> str:
    data = open(path, "rb").read()
    magic = data.rfind(APK_SIG_BLOCK_MAGIC)
    if magic != -1:
        # Block layout: uint64 size | id-value pairs | uint64 size | magic(16)
        block_size = struct.unpack("<Q", data[magic - 8:magic])[0]
        start = (magic + 16) - (block_size + 8)
        data = data[:start] + data[magic + 16:]
    return hashlib.sha256(data).hexdigest()


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    for path in argv[1:]:
        digest = content_digest(path)
        if len(argv) == 2:
            print(digest)
        else:
            print(f"{digest}  {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
