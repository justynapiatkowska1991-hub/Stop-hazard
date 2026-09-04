# STOP HAZARD

STOP HAZARD contains a web preview and an Android native protection prototype.

## Native architecture
- VpnService lifecycle
- DNS question parsing
- built-in and custom domain policy
- local blocked-event history
- local protection statistics
- persistent settings
- Android runtime VPN permission

## Production requirement
A VPN interface by itself does not provide Internet forwarding. A production implementation must include a complete user-space IP/TUN stack or a properly integrated upstream tunnel/DNS proxy, plus IPv6 handling, TCP/UDP forwarding, encrypted-DNS policy, lifecycle recovery, battery considerations, and extensive device testing.

The project therefore keeps the filtering policy separate from transport so the networking layer can be completed and tested without pretending that a partial prototype is a finished system-wide blocker.
