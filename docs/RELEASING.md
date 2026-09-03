# Releasing

Every version is published by pushing a tag. [`.github/workflows/release.yml`](../.github/workflows/release.yml)
then runs the unit tests, builds a signed release APK, names it after the version and attaches it
to a GitHub Release along with its SHA-256 sum.

## One-time setup

Release builds are signed with a key that never enters the repository. Generate one, keep the
`.jks` file and its passwords somewhere safe, and note that **losing it means no future build can
upgrade an already-installed app**: Android refuses an update signed by a different key.

```bash
keytool -genkeypair -v \
  -keystore rastreado-release.jks \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias rastreado
```

Add four repository secrets under Settings > Secrets and variables > Actions:

| Secret | Value |
| --- | --- |
| `RASTREADO_KEYSTORE_BASE64` | `base64 -w0 rastreado-release.jks` |
| `RASTREADO_KEYSTORE_PASSWORD` | The keystore password |
| `RASTREADO_KEY_ALIAS` | `rastreado`, or whichever alias was used |
| `RASTREADO_KEY_PASSWORD` | The key password |

The workflow checks all four are present before it builds, so a missing secret fails the run
immediately rather than publishing an APK nobody can install.

## Cutting a release

1. Bump `versionCode` and `versionName` in [`app/build.gradle.kts`](../app/build.gradle.kts).
   `versionCode` must increase on every published build; `versionName` is what users see.
2. Move the `CHANGELOG.md` entries from `Unreleased` into a section for the new version.
3. Commit, then tag with `v` and the exact `versionName`:

   ```bash
   git tag v0.2.0
   git push origin master --tags
   ```

The tag has to match `versionName`, or the workflow stops: an APK reporting a version other than
the one on its release is worse than no release. To rebuild an existing tag without moving it, run
the workflow manually from the Actions tab and give it the tag name.

## Building a release locally

Gradle only defines a signing config when the keystore is supplied through the environment, so a
plain checkout builds an unsigned release and needs no key:

```bash
./gradlew assembleRelease                      # unsigned
```

To produce a signed one, point the same four variables the workflow uses at your keystore:

```bash
RASTREADO_KEYSTORE=/path/to/rastreado-release.jks \
RASTREADO_KEYSTORE_PASSWORD=... \
RASTREADO_KEY_ALIAS=rastreado \
RASTREADO_KEY_PASSWORD=... \
  ./gradlew assembleRelease
```
