# Security policy

## Supported versions

Only the latest release is supported. Fixes land on `master` and go out in the next release.

## Reporting a vulnerability

Please report privately through GitHub's
[private vulnerability reporting](https://github.com/ArturoManzoli/rastreado/security/advisories/new)
rather than in a public issue. If that is not available to you, email arturomanzoli@gmail.com.

Include what you did, what happened, and the Android version and app version you saw it on. Expect
an acknowledgement within a week; this is a spare-time project, so a fix may take longer than that.

## Scope

Rastreado has no backend and no account, so the attack surface is what lives on the device and the
one request that leaves it. Things in scope:

- Leaking tracking codes, parcel history or product names to another app on the device
- Exported components (activities, receivers) reachable by another app that should not be
- The alarm path bypassing lock-screen protections in a way it should not
- Anything making the app send data anywhere other than Cainiao's tracking endpoint
- Backup and device-transfer rules exposing more than intended

Out of scope:

- Cainiao's endpoint itself, its availability, its data or its own security. Report those to
  Alibaba, not here.
- An attacker who already has physical access to an unlocked phone
- The fact that a tracking code is sent unencrypted to nobody: lookups go over HTTPS, but the code
  is by nature known to the carrier
- Missing hardening that does not lead to a concrete impact (no root detection, no certificate
  pinning against a public endpoint, and so on), unless you can show what it lets an attacker do

## What Rastreado does with data

Everything is local: a Room database and a preferences file in the app's private storage. No
account, no server, no analytics. Barcode scanning and OCR run on-device through ML Kit, so images
never leave the phone. The only outbound request carries a tracking code to Cainiao.
