# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[semantic versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Photographing an order screen now asks you to mark the tracking code and the product name,
  rather than guessing both from the whole frame
- Archiving a parcel now reads as an inbox the parcel leaves, rather than a filing box

### Fixed

- Tracking codes read by OCR no longer keep a letter the recogniser mistook for a digit, which
  left the parcel showing no information because the carrier had no such code

## [0.1.0] - 2026-09-03

First release.

### Added

- Parcel tracking through Cainiao's public feed, covering the Chinese leg, customs and the
  Brazilian last mile in one timeline
- The Brazilian tracking code surfaced alongside the AliExpress one, once the carrier assigns it
- Barcode scanning for tracking codes, on-device via ML Kit
- On-device OCR of an order screen photo, extracting the tracking code and the product name
- Background lookups on a configurable interval, optionally Wi-Fi only
- Notifications per status change, on a separate channel for delivery problems
- A full-screen alarm when a parcel goes out for delivery, at alarm volume with dismiss and snooze
- Brazilian Portuguese and English, switchable in-app
- Light and dark themes following the system or pinned, with optional wallpaper colours on
  Android 12+

[Unreleased]: https://github.com/ArturoManzoli/rastreado/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/ArturoManzoli/rastreado/releases/tag/v0.1.0
