# Hev SOCKS5 tunnel integration

Reference: https://github.com/zak20090/hev-socks5-tunnel-android

The library provides an Android JNI wrapper around hev-socks5-tunnel and accepts the Android VpnService TUN FileDescriptor. It requires a real SOCKS5 upstream; it is not a standalone internet forwarder.

Integration requirements:
- add the library module/dependency
- configure NDK/CMake requirements
- establish TUN with 0.0.0.0/0 only after the tunnel is ready
- protect the upstream sockets from the VPN
- provide a local SOCKS5/direct upstream for the tunnel
- only then add domain filtering

Do not ship an APK until these pieces are wired and the tunnel has a real upstream.
