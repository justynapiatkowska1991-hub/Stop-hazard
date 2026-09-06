#!/usr/bin/env bash
set -euo pipefail

# Build tun2socks for Android ABIs with the Android NDK.
# This script intentionally fails if the upstream source or NDK is unavailable;
# it must never create placeholder .a files.
#
# Expected environment:
#   ANDROID_NDK_HOME=/path/to/android-ndk
#   TUN2SOCKS_SOURCE=/path/to/tun2socks-source
#
# The CI workflow should invoke the upstream project's documented Android build
# procedure and copy the resulting real static libraries into:
#   app/src/main/cpp/prebuilt/lib/<ABI>/libtun2socks.a

: "${ANDROID_NDK_HOME:?ANDROID_NDK_HOME is required}"
: "${TUN2SOCKS_SOURCE:?TUN2SOCKS_SOURCE is required}"

test -d "$ANDROID_NDK_HOME" || { echo "NDK not found: $ANDROID_NDK_HOME"; exit 1; }
test -d "$TUN2SOCKS_SOURCE" || { echo "tun2socks source not found: $TUN2SOCKS_SOURCE"; exit 1; }

echo "NDK: $ANDROID_NDK_HOME"
echo "Source: $TUN2SOCKS_SOURCE"
echo "Next: invoke the upstream Android/NDK build and copy real ABI libraries."
