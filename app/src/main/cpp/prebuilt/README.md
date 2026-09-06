# tun2socks native binaries

This directory is reserved for ABI-specific tun2socks static libraries:
- arm64-v8a/libtun2socks.a
- armeabi-v7a/libtun2socks.a
- x86_64/libtun2socks.a
- x86/libtun2socks.a

The Android build must not claim VPN forwarding is complete until these binaries are produced and linked successfully.
