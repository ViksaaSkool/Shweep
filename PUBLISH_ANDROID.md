# Publishing to Google Play (internal testing)

This document covers the one-time manual setup and the automated flow that publishes a signed Android App Bundle (`.aab`) to the Google Play **internal testing** track.

The automation lives in [`.github/workflows/publish-android.yml`](.github/workflows/publish-android.yml).

> **Currently disabled.** The workflow job carries `if: false`, so pushes do not publish. Remove or change that condition only when Play publishing is intentionally resumed.

> For a 2.0 release, update the version scheme first: `ANDROID_VERSION_NAME_PREFIX` should be `2.0`, and iOS `MARKETING_VERSION` in `iosApp/Configuration/Config.xcconfig` must be bumped separately. The examples below use 1.x and are illustrative only.

> Authentication uses **Google Workload Identity Federation**, so no long-lived service-account JSON key is stored in GitHub.

## What is automated

After the one-time setup below, this happens automatically on app-related pushes to `main`:

```text
app change pushed to main
-> unique versionCode computed
-> keystore restored and certificate verified
-> Google authenticated via WIF
-> signed release AAB built
-> signature and contents verified
-> AAB uploaded to the Play internal track as a completed release
```

Documentation, website, art, Markdown and iOS-only changes do **not** publish, because the workflow filters on app-related paths.

## What cannot be automated

Google requires these one-time actions outside the repository. They cannot be performed by GitHub Actions:

- Create the app in Play Console.
- Accept the developer and publishing agreements.
- Enable Play App Signing.
- Register the upload certificate.
- Upload the first AAB manually.
- Invite the service account in Play Console.
- Configure internal testers.

## 1. Play Console: create the app

1. **All apps -> Create app** with package name `com.skooldev.shweep`.
2. Complete the required declarations and account verification.
3. Configure **Internal testing** and add a tester list; save the opt-in URL.
4. **Setup -> App integrity**: enable **Play App Signing** (Google-managed app-signing key, your own upload key).

The package name is fixed after the first artifact upload.

## 2. Create the upload keystore

Generate once, on a trusted machine:

```bash
keytool -genkeypair \
  -keystore shweep-upload.jks \
  -alias shweep-upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Keep an encrypted offline backup of the keystore and its passwords. Never commit it (it is git-ignored).

Record the upload certificate SHA-256 fingerprint (used as `ANDROID_UPLOAD_CERT_SHA256`):

```bash
keytool -list -v -keystore shweep-upload.jks -alias shweep-upload | awk -F': ' '/SHA256:/{print $2; exit}'
```

Compare this with the **Upload key certificate** in Play Console, not the app-signing certificate.

## 3. Google Cloud: service account and WIF

1. Create a Google Cloud project (or reuse one).
2. Enable **Google Play Android Developer API** (`androidpublisher.googleapis.com`).
3. Create a service account, e.g. `shweep-github-play-publisher`.
4. Create a Workload Identity Pool `github-actions` with a `github` provider.

Attribute mappings:

```text
google.subject      = assertion.sub
attribute.repository = assertion.repository
attribute.repository_owner = assertion.repository_owner
attribute.ref        = assertion.ref
```

Provider condition, restricted to this repo and branch:

```text
assertion.repository == "ViksaaSkool/Shweep" &&
assertion.repository_owner == "ViksaaSkool" &&
assertion.ref == "refs/heads/main"
```

Grant the GitHub principal permission to impersonate the service account:

```text
roles/iam.workloadIdentityUser
```

The provider identifier and the service-account email are not secrets.

## 4. Play Console: invite the service account

**Users and permissions -> Invite new users**, using the service-account email:

- App access: **Shweep** only
- Permission: **Release apps to testing tracks**

Do not grant production release, financial, order, or global-admin permissions.

If **Setup -> API access** appears, link the Google Cloud project that hosts the service account.

## 5. First manual internal release

1. Build the first signed AAB locally (see below) and upload it via **Testing -> Internal testing**.
2. Complete the first rollout.
3. Install it as a tester to confirm the flow works.
4. Note its `versionCode`; pick `PLAY_VERSION_CODE_BASE` so that `base + 1` is higher than every existing version code.

## 6. GitHub environment and configuration

The environment `google-play-internal` is already created and restricted to `main`.

Add **environment secrets** under `Settings -> Environments -> google-play-internal`:

| Secret | Value |
|---|---|
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | `base64 < shweep-upload.jks` |
| `ANDROID_UPLOAD_STORE_PASSWORD` | Keystore password |
| `ANDROID_UPLOAD_KEY_ALIAS` | e.g. `shweep-upload` |
| `ANDROID_UPLOAD_KEY_PASSWORD` | Key password |

Add **environment variables**:

| Variable | Example |
|---|---|
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/github-actions/providers/github` |
| `GCP_PLAY_SERVICE_ACCOUNT` | `shweep-github-play-publisher@PROJECT_ID.iam.gserviceaccount.com` |
| `PLAY_VERSION_CODE_BASE` | `10000` |
| `ANDROID_PACKAGE_NAME` | `com.skooldev.shweep` |
| `ANDROID_VERSION_NAME_PREFIX` | `1.0` |
| `ANDROID_UPLOAD_CERT_SHA256` | Upload certificate SHA-256 |

With Workload Identity Federation, `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` is **not** needed.

## 7. Versioning

The workflow computes:

```text
versionCode = PLAY_VERSION_CODE_BASE + github.run_number
versionName = ANDROID_VERSION_NAME_PREFIX + "." + github.run_number
```

`composeApp/build.gradle.kts` reads these through `-PandroidVersionCode` / `-PandroidVersionName`. Local builds keep the defaults (`versionCode = 1`, `versionName = "1.0"`).

## 8. Building a signed AAB locally

For day-to-day local builds use [BUILD_LOCAL_ANDROID.md](BUILD_LOCAL_ANDROID.md):

```bash
./tools/build-local-aab.sh v-1.0.7
```

It reads signing values as `environment variable → local.properties → secure prompt`, auto-increments `versionCode`, and copies the verified bundle to `build-local/Shweep_<versionName>.aab`.

The equivalent manual flow is:

```bash
export ANDROID_KEYSTORE_PATH="$PWD/shweep-upload.jks"
export ANDROID_UPLOAD_STORE_PASSWORD='...'
export ANDROID_UPLOAD_KEY_ALIAS='shweep-upload'
export ANDROID_UPLOAD_KEY_PASSWORD='...'

./gradlew :composeApp:bundleRelease \
  -PandroidVersionCode=1 \
  -PandroidVersionName=1.0

jarsigner -verify composeApp/build/outputs/bundle/release/composeApp-release.aab
```

Without those environment variables the release build is produced unsigned, which is fine for local inspection but cannot be uploaded to Play.

## 9. Verification checklist

Before the first automated run:

- [ ] App exists in Play Console with package `com.skooldev.shweep`
- [ ] At least one signed AAB uploaded manually
- [ ] Play App Signing enabled
- [ ] CI upload certificate matches Play Console's upload certificate
- [ ] Service account invited with "Release apps to testing tracks"
- [ ] Android Publisher API enabled
- [ ] WIF provider restricted to `ViksaaSkool/Shweep` and `refs/heads/main`
- [ ] All secrets and variables set on `google-play-internal`
- [ ] `PLAY_VERSION_CODE_BASE + 1` exceeds every existing version code
- [ ] Internal tester list configured

After the run, confirm in **Testing -> Internal testing -> Releases** that the release shows the expected version code/name and `completed` status, then install via the tester opt-in link.

## Notes

- `versionCode` must strictly increase on every upload; gaps are fine.
- Do not edit the app in Play Console while an automated upload is running (overlapping edits invalidate each other).
- The workflow pins the third-party `r0adkll/upload-google-play` action to an immutable commit SHA because it receives Play publish authority.
- Losing the upload keystore is recoverable through Play's upload-key reset, but it causes delay. Keep the offline backup.
