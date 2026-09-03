# Architecture

A single Gradle module, `:app`, with layers separated by package. Nothing here is elaborate: the app
polls one endpoint, stores the result, and tells the user about it. The layering exists so the
tracking source and the notification behaviour can each be changed without touching the other.

```
domain/       pure Kotlin: statuses, code recognition, OCR field extraction
data/
  remote/     TrackingProvider interface + the Cainiao implementation
  local/      Room entities, DAO, database
  settings/   DataStore-backed preferences
  ParcelRepository.kt   the one place local and remote meet
notify/       channels, notifications, the out-for-delivery alarm
work/         WorkManager periodic sync and its scheduler
scan/         CameraX analysers wrapping ML Kit barcode and text recognition
ui/           Compose screens, each with a ViewModel; theme; navigation
AppContainer.kt   manual dependency wiring
```

Dependencies point inwards: `ui` and `work` depend on `data`, `data` depends on `domain`, and
`domain` depends on nothing. That is what makes `domain` unit-testable without an emulator, and it is
where the logic worth testing lives.

## Data flow

A lookup, whether periodic or user-triggered, always takes the same path:

```
SyncWorker / ViewModel
  └─ ParcelRepository.sync(code)
       ├─ TrackingProvider.fetch(code, languageTag)   → ProviderResult
       ├─ CainiaoMapper.toSnapshot(...)               → TrackingSnapshot
       └─ ParcelDao.applySnapshot(...)                → one Room transaction
              └─ Flow<ParcelEntity> emits, UI recomposes
```

The UI never calls a provider. It observes Room `Flow`s and requests a sync; whatever arrives comes
back through the database, so a screen shows the same data whether it was opened after a background
sync or a manual pull-to-refresh.

`ParcelDao.applySnapshot` is a single `@Transaction`, so a parcel is never left with events from one
lookup and a status from another. New events are recognised by a deterministic `eventId` built from
the parcel code, timestamp, action code and description, which is what makes a re-sync idempotent
and lets `SyncOutcome` report exactly which events are new rather than re-notifying the whole
history. The description is part of that key because Cainiao emits distinct events sharing a
timestamp and an action code, differing only in the city they name.

## Background sync

`SyncScheduler` registers one `PeriodicWorkRequest` and replaces it whenever the interval or the
network constraint changes in settings. The worker syncs every non-archived, non-delivered parcel,
collects the `SyncOutcome`s, and hands them to `Notifications`.

Three details are deliberate:

- **Delivered parcels are skipped**, since a delivered parcel produces no further events. It is the
  only status treated as terminal.
- **A parcel added mid-journey does not announce its own history.** The repository suppresses new
  events when `lastSyncAt` is null, so adding a code that is already halfway to Brazil files its
  twenty past events silently.
- **Only a network failure retries.** A malformed or rejected response will look the same in a
  minute's time, so it waits for the next period instead of burning a retry.

Which channel an update lands on is `Notifications`' call, not the worker's: an event whose status
`needsAttention` (awaiting pickup, failed attempt, returned) goes to the problems channel, and
everything else to the quiet updates channel.

## The out-for-delivery alarm

An ordinary notification is easy to miss, and missing this one means missing the parcel. So
`OUT_FOR_DELIVERY` on a parcel with the alarm enabled goes through a dedicated channel with
`IMPORTANCE_HIGH`, alarm audio attributes (which sound through the silent profile) and a
full-screen intent onto `AlarmActivity`, which shows over the lock screen with dismiss and snooze.
`AlarmPlayer` loops the tone and vibration for as long as that screen is up.

Full-screen intents are not guaranteed: the OS may withhold them, and on Android 14+ the permission
is granted per-app. When the intent does not fire, the notification still arrives on the same
channel with the same sound, so the failure mode is a loud notification rather than silence.

## Scanning

Both scanners share `CameraPane`, a Compose wrapper around a CameraX `PreviewView`, and differ only
in what they bind:

- **Barcode**: a continuous `ImageAnalysis` running `BarcodeAnalyzer`, which reports the first
  result whose payload `TrackingCode` finds plausible. Continuous, because framing a barcode is a
  matter of moving the phone until it locks.
- **OCR**: a single `ImageCapture` frame passed to `ScreenTextReader`, then to
  `OcrFieldExtractor`. Single-shot, because reading a whole screen of text once and reviewing the
  result beats a preview that changes its mind every frame.

Both models are ML Kit's bundled, on-device ones. No network, no key, and the tradeoff is APK size.

`OcrFieldExtractor` has one non-obvious rule worth knowing before changing it: glyph repair (`O`
read as `0`, `S` as `5`) is attempted **only** when the unrepaired text yields no code at all, and
only for glyphs sitting between two digits. Applying it eagerly corrupts codes that were read
correctly and mangles ordinary words.

## UI

Compose with Material 3. Each screen has a `ViewModel` exposing a single immutable state class,
built from repository `Flow`s. `AppContainer` is reached through `rememberContainer()` rather than a
DI framework.

Theme resolution has two independent inputs: the theme mode (system, light or dark) and whether to
use wallpaper colours, both from settings. `LocalStatusPalette` carries the per-status accent
colours, so a status has one colour definition rather than one per screen that displays it.

## Localisation

Brazilian Portuguese lives in `values/` as the default and English in `values-en/`, which is the
right way round for the app's audience: an unlocalised string falls back to pt-BR rather than to
English. Both locales are declared in `res/xml/locales_config.xml`, so Android 13+ shows Rastreado in
the system's per-app language picker, and the in-app switch goes through
`AppCompatDelegate.setApplicationLocales` to cover Android 12 and below.

The provider is told which language to answer in, derived from the effective locale, so event text
in the timeline matches the rest of the interface.

## Testing

`./gradlew test` runs JVM unit tests over the layers that carry logic: status normalisation, code
recognition, OCR field extraction, and the Cainiao mapper against a response captured verbatim from
the live endpoint. No emulator, no Robolectric.

The mapper test doubles as a canary. If Cainiao changes its payload shape, refreshing the fixture
and re-running the suite says precisely what broke.
