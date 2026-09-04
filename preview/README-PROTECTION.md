# STOP HAZARD — Protection module

This folder contains the presentation-layer protection modules and a native Android foundation.

## Current capabilities
- normalized hostname matching
- built-in gambling/casino domain rules
- custom domain rules
- subdomain matching
- local counters and last-blocked state
- protection enable/disable state
- strict-mode state
- visual protection dashboard

## Important technical boundary
A browser-hosted page cannot enforce system-wide blocking for arbitrary apps or browser tabs. The Android VPN service is the correct integration point for device-wide enforcement.

A production VPN must implement complete packet forwarding/upstream transport, IPv4 and IPv6 handling, DNS routing, encrypted DNS policy, lifecycle recovery and extensive testing. The repository deliberately separates policy from transport so those pieces can be implemented and tested independently.

Never store payment credentials, API secrets or private access tokens in client-side code or Git.
