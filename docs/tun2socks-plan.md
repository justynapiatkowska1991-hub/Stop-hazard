# STOP HAZARD — tun2socks migration plan

The current VPN service must not forward raw TUN packets itself. Android VpnService provides the TUN file descriptor; a tun2socks engine must consume that descriptor and forward TCP/UDP traffic.

Reference implementation: hev-socks5-tunnel Android JNI wrapper.

Required architecture:
Android VpnService -> TUN fd -> hev-socks5-tunnel JNI -> protected upstream/direct transport -> internet.

Do not ship another APK until the native tunnel is actually linked and started from BlockVpnService.
