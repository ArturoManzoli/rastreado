# Contributing to Rastreado

Thanks for looking. This is a small, focused app, and the most valuable contributions are usually
the smallest: an unrecognised tracking event, a bad translation, a status that reads wrong.

Issues and pull requests are welcome in **English or Brazilian Portuguese**. Use whichever you
write better; nobody will ask you to switch.

## The most useful things to send

### An unrecognised event

If a parcel shows "No information" for an event, or a status that does not match what Correios or
AliExpress says, that is a missing entry in the normaliser and a two-line fix. Open an issue with
the event text and its action code exactly as the app displayed them. If you want to fix it
yourself, add the mapping to `StatusResolver` and a case to `StatusResolverTest`.

### A translation fix

The strings live in `app/src/main/res/values/strings.xml` (pt-BR, the default) and
`app/src/main/res/values-en/strings.xml` (English). Both files must define the same keys. Sentence
case, please, and keep the tone plain: this is an app someone opens while waiting for a parcel, not
a courier's internal console.

New languages are welcome, in `values-<code>/`, plus an entry in `res/xml/locales_config.xml`.

### A second tracking provider

`TrackingProvider` is a one-function interface, and [docs/TRACKING.md](docs/TRACKING.md) explains
what a new implementation needs. A provider that requires the user to supply their own API key is
fine; one that routes parcel codes through a server the project runs is not.

## Setting up

```bash
git clone https://github.com/ArturoManzoli/rastreado.git
cd rastreado
./gradlew assembleDebug
./gradlew test
```

You need JDK 21 and the Android SDK with platform 37. Android Studio brings both; on the command
line, point Gradle at your SDK with `ANDROID_HOME` or a `local.properties` holding
`sdk.dir=/path/to/Android/Sdk`. Everything else Gradle fetches.

## Code style

The `.editorconfig` covers the mechanical part, and Android Studio honours it. Beyond that, the
conventions in this codebase:

- **Comments explain why, not what.** Assume the reader knows Kotlin and can see what the line does.
  A comment earns its place by recording a constraint the code cannot show: a quirk of Cainiao's
  payload, an OS restriction, a decision that looks wrong until you know the reason. Most functions
  need none.
- **Keep logic out of composables.** Pure logic goes in `domain/`, where it can be tested without an
  emulator. If a new rule is worth reasoning about, it probably belongs there.
- **Fix the cause, not the call site.** A guard in one screen that the next screen will also need
  belongs in the shared function.

## Tests

`./gradlew test` runs the JVM unit tests, and they must pass. Logic added under `domain/` or a
provider mapper wants a test case beside it; UI work generally does not, since there is no
instrumentation suite to hang it from.

The Cainiao mapper is tested against a real response captured at
`app/src/test/resources/cainiao_lp_last_mile.json`. If your change needs a payload shape that
fixture does not cover, add another fixture rather than editing that one, so the captured response
stays a faithful record of what the endpoint actually sent.

## Commits and pull requests

- One logical change per commit, with a subject in the imperative (`domain: map GTMS_DO_DEPART to
  out for delivery`) and a body explaining the resulting behaviour.
- Say in the pull request what you observed and what changed. For a tracking fix, the event text
  that misbehaved is worth more than a description of the diff.
- CI runs the unit tests and a debug build on every pull request. Green before review, please.

## Licence

Rastreado is GPL-3.0-or-later, and contributions are accepted under that licence. There is no CLA:
you keep your copyright, and your commits stay attributed to you.
