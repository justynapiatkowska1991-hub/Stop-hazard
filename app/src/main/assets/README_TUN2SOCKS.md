# STOP HAZARD — tun2socks integration

Selected engine: universal-android-tun2socks (badvpn-based), BSD-3-Clause.
Upstream: https://github.com/mokhtarabadi/universal-android-tun2socks

Integration note:
- The native AAR/JNI binaries must be built with Android NDK before packaging.
- BlockVpnService will pass the established VpnService TUN file descriptor to the native engine.
- Traffic policy is allow-by-default; only BlockedDomains is denied.
- IPv6 must not be allowed until the filtering path handles IPv6, to avoid bypass.

This file records the selected dependency and integration contract; it is not the native binary itself.
