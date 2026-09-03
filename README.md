<div align="center">

<img src="docs/images/icon.png" alt="" width="112" height="112">

# Rastreado

**Track AliExpress parcels all the way to your door in Brazil.**

[![CI](https://github.com/ArturoManzoli/rastreado/actions/workflows/ci.yml/badge.svg)](https://github.com/ArturoManzoli/rastreado/actions/workflows/ci.yml)
[![Release](https://github.com/ArturoManzoli/rastreado/actions/workflows/release.yml/badge.svg)](https://github.com/ArturoManzoli/rastreado/actions/workflows/release.yml)
[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/android-8.0%2B-3ddc84.svg)](#requirements)

[Português (Brasil)](README.pt-BR.md)

</div>

Rastreado is an Android app for the specific problem of buying from AliExpress and waiting in
Brazil: the parcel leaves China under a Cainiao code, gets a Correios code on arrival, and the two
halves of the journey are usually shown in two different places. Rastreado follows both in one
timeline, and rings a real alarm when the parcel goes out for delivery so you are home to receive
it.

It is offline-first, has no account, no analytics and no ads, and talks to exactly one network host.

## Features

- **Both halves of the journey** in one timeline, from the Chinese warehouse to the Brazilian
  courier, including customs clearance and the local tracking code
- **Barcode scanning** to read a tracking code straight off a parcel label
- **Photograph the order screen** and let on-device OCR pull out the tracking code and the product
  name
- **Background lookups** on a schedule you choose, with a Wi-Fi-only option
- **Notifications** for status changes, with a separate channel for delivery problems
- **A full-screen alarm** when the parcel goes out for delivery, at alarm volume even on silent,
  with dismiss and snooze
- **Brazilian Portuguese and English**, switchable in-app independently of the system language
- **Light and dark themes** following the system, or pinned either way, with optional wallpaper
  colours on Android 12+

## Screenshots

Not yet captured. Contributions welcome, see [CONTRIBUTING.md](CONTRIBUTING.md).

## Where the tracking data comes from

Rastreado reads Cainiao's public tracking feed, the same one AliExpress itself shows buyers. For a
parcel bound for Brazil that feed relays the Correios events too, which is what makes the last-mile
leg visible without a second source.

Correios' own API is not an option for an app like this: since 2023 it is contract-only, and it
authorises the **sender** of a parcel rather than its recipient. The details, including the event
vocabulary and how it is normalised, are in [docs/TRACKING.md](docs/TRACKING.md).

Rastreado is not affiliated with AliExpress, Alibaba, Cainiao or Correios.

## Privacy

Everything the app knows lives on the phone, in a local database and a preferences file. There is no
backend, no account and no telemetry. The only outbound request is the tracking lookup itself, which
sends a tracking code to Cainiao and nothing else. Barcode scanning and OCR both run on-device
through ML Kit, so no photo ever leaves the phone.

## Requirements

- Android 8.0 (API 26) or newer
- A camera, for barcode scanning and OCR; the app works without one, by typing the code

## Building

```bash
git clone https://github.com/ArturoManzoli/rastreado.git
cd rastreado
./gradlew assembleDebug          # APK in app/build/outputs/apk/debug/
./gradlew test                   # unit tests
```

JDK 21 and the Android SDK with platform 37 are required; Gradle downloads everything else. Point
Gradle at your SDK either through `ANDROID_HOME` or a `local.properties` holding
`sdk.dir=/path/to/Android/Sdk`.

Every release is built from a `vX.Y.Z` tag and published with its APK and SHA-256 sum; see
[docs/RELEASING.md](docs/RELEASING.md).

## Architecture

Single-module Kotlin app: Jetpack Compose and Material 3 for the UI, Room for storage, WorkManager
for background lookups, ML Kit for scanning, and a pluggable provider layer so a second tracking
source can be added without touching the rest. [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) has the
walkthrough.

## Contributing

Bug reports, translations and provider implementations are all welcome. Start with
[CONTRIBUTING.md](CONTRIBUTING.md), and note that the project follows a
[Code of Conduct](CODE_OF_CONDUCT.md).

## Licence

[GPL-3.0-or-later](LICENSE). Copyright (C) 2026 Arturo Manzoli and contributors.

Bundled icons come from [Material Design Icons](https://pictogrammers.com/library/mdi/) (Apache-2.0).
