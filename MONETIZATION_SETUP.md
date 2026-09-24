# Monetization setup — Unlimited Sheep

This document covers everything that must be configured **outside** the codebase to ship the
35-sheep allowance and the one-time **Unlimited Sheep** purchase. The app-side implementation is
already in place behind `FeatureFlags.LIMITED_DAILY_SHEEP_ENABLED`.

## Identifiers

| Thing | Value |
| --- | --- |
| Product ID (both stores) | `unlimited_sheep` |
| RevenueCat entitlement | `unlimited_sheep` |
| RevenueCat offering | `default` (contains the `unlimited_sheep` package) |
| Android package | `com.skooldev.shweep` |
| iOS bundle id | `com.skooldev.shweep` |
| Price | one-time non-consumable, base price $0.99 (auto-localized by each store) |

## Release boundary

| Version | Behavior |
| --- | --- |
| 1.0.0 / 1.0.2 | No allowance, no purchase. Settings → About shows the installed version. |
| 2.0.0 | Introduces the 35-sheep allowance, the optional purchase, and the one-time "what's changed" notice. |

Only advertise the allowance and the purchase in listings for 2.0.0 and later. The legal pages are
version-aware and link to an archive for 1.0.0/1.0.2; publishing them does not change what an older
installed version does.

## Keys: what goes where

| Key | Created in | Stored | Secret? |
| --- | --- | --- | --- |
| RevenueCat **Android** public SDK key (`goog_…`) | RevenueCat → Project → Android app | `composeApp/src/commonMain/kotlin/com/skooldev/shweep/purchase/RevenueCatConfig.kt` (**committed**) | No |
| RevenueCat **iOS** public SDK key (`appl_…`) | RevenueCat → Project → iOS app | `RevenueCatConfig.kt` (**committed**) | No |
| RevenueCat **secret** API key (`sk_…`/`rc_…`) | RevenueCat → Project settings | Not needed by the app. If ever created: RevenueCat dashboard only | **Yes** |
| Google service-account JSON (RevenueCat → Play) | Google Cloud + Play Console | Uploaded to RevenueCat dashboard only | **Yes** |
| App Store Connect API key (Issuer ID, Key ID, `.p8`) | App Store Connect → Users and Access → Integrations | Uploaded to RevenueCat dashboard only | **Yes** |
| Android upload keystore (existing) | — | env vars / `local.properties` / GitHub environment `google-play-internal` (unchanged) | **Yes** |

Public SDK keys are meant to be embedded in the shipped app; every installed copy contains them, so
committing them is safe. **Never** commit the secret key, the service-account JSON, or the `.p8`.

## Manual setup

### 1. RevenueCat

1. Create a project named **Shweep**.
2. Add an **Android app** with package `com.skooldev.shweep`; copy its `goog_…` public SDK key.
3. Add an **iOS app** with bundle id `com.skooldev.shweep`; copy its `appl_…` public SDK key.
4. Create an entitlement with identifier `unlimited_sheep`.
5. Create a `default` offering containing a package attached to the `unlimited_sheep` product.
6. Connect the stores:
   - **Google Play**: create a Google Cloud service account, grant it "View app information and
     download bulk reports" in Play Console, download the JSON, and upload it in RevenueCat under
     the Android app's Play Store credentials.
   - **App Store**: create an App Store Connect API key with the App Manager role and upload the
     Issuer ID, Key ID, and `.p8` in RevenueCat under the iOS app's App Store Connect API key.
7. Paste the two public SDK keys into `RevenueCatConfig.kt`, replacing the placeholders.

### 2. Google Play Console

1. Monetize → Products → In-app products → create `unlimited_sheep` as a **one-time** product,
   base price $0.99 (let Play auto-localize), and activate it.
2. Update the **Data safety** form: declare "Device or other IDs" collected by RevenueCat for
   App functionality and Analytics, encrypted in transit, not used for advertising, not sold.
   Follow RevenueCat's data-safety guidance. Treat these answers as provisional and verify them
   against the shipped SDK configuration and the current store guidance before submitting.
3. Update the store listing to mention the optional one-time purchase.

### 3. App Store Connect

1. Confirm the app record uses bundle id `com.skooldev.shweep`.
2. Features → In-App Purchases → create a **non-consumable** with product id `unlimited_sheep`,
   ~$0.99 price tier, display name/description/review screenshot. Leave it "Ready to Submit"; it is
   reviewed with the next binary.
3. Update **App Privacy**: declare "Identifiers → Device ID" (not linked to identity) and
   "Usage Data → Product Interaction" collected by RevenueCat. Follow RevenueCat's App Privacy
   guidance. Treat these answers as provisional and verify them against the shipped SDK
   configuration before submitting. Do not assume that "no Shweep login" means "not linked to the
   user" for Apple's classification.
4. Update the listing to mention the optional one-time purchase.

### 4. Enable the feature

Set `LIMITED_DAILY_SHEEP_ENABLED = true` in
`composeApp/src/commonMain/kotlin/com/skooldev/shweep/FeatureFlags.kt` only after steps 1–3 are
done. The flag remains the kill switch.

For the 2.0.0 release, also:

- Build and publish the app as version `2.0.0` (Android `versionName`, iOS `MARKETING_VERSION`).
- Confirm `FeatureFlags.UPDATE_NOTICE_VERSION` is `"2.0.0"` so returning users see the one-time
  "what's changed" notice. Bump this constant only when a release adds user-visible changes that
  returning users should be told about.
- Publish the updated `docs/privacy` and `docs/terms` (with the archive pages) before or with the
  2.0.0 binary.

## Testing

### Local test mode (no accounts, about a minute)

Flip one flag in `composeApp/src/commonMain/kotlin/com/skooldev/shweep/FeatureFlags.kt`:

```kotlin
const val LOCAL_TEST_MODE = true
```

That switches the app to:

- 3 free sheep instead of 50, with a 3-minute reset window instead of 24 hours
- an in-memory mock purchase gateway: "Buy Unlimited Sheep · $0.99" succeeds instantly with no
  RevenueCat keys, and "Restore purchases" reflects the mock state
- a "Local test mode" caption in the paywall dialog and the Settings card

Run the app (`./gradlew :composeApp:installDebug`, or Xcode for iOS) and check:

1. Swipe three sheep: the paywall dialog appears with a 3-minute countdown.
2. Dismiss it and swipe again: the dialog returns and the countdown does not restart.
3. Kill and reopen the app: the dialog shows immediately on entering the counting screen.
4. Wait out the 3 minutes: the dialog closes and counting resumes with 3 fresh sheep.
5. Tap Buy: the dialog closes, sheep become unlimited, and Settings shows "Unlimited sheep:
   Purchased" with the thank-you line.
6. Reset between runs: `adb shell pm clear com.skooldev.shweep` (Android) or delete the app (iOS).

Set `LOCAL_TEST_MODE = false` again before committing. If you switch modes with a lock already
recorded, clear the app data so the stored allowance does not look stale.

### Store sandbox (real purchase flow)

Keep `LOCAL_TEST_MODE = false` and use the real store products.

- **Android**: publish a build to an internal track, add a license tester, and purchase with a test
  card. Verify buy, restore, and that the allowance disappears after purchase.
- **iOS**: use a Sandbox Apple ID in App Store Connect. Verify buy, restore, and that reinstalling
  plus "Restore purchases" restores unlimited sheep.
- **Offline**: counting and the allowance must keep working with no connection; only buying and the
  entitlement check need the network.
- **24-hour window**: exhaust the allowance, confirm the window start is recorded in the local
  `paywall_shown_at` DataStore key, confirm the dialog reappears within 24 hours, and confirm the
  allowance resets after 24 hours. This window is local only; the app does not send it to
  RevenueCat.

## Policy notes

- Digital goods are sold through Google Play Billing / Apple IAP via RevenueCat (Play Payments
  policy, App Review Guideline 3.1.1).
- A "Restore purchases" action is available in both the paywall dialog and Settings (required for
  non-consumables on iOS).
- Keep the app positioned as general-audience wellness; do not enroll it as a children's app, which
  would add Families-policy purchase and data restrictions.
- Keep the privacy policy (`docs/privacy`) and terms (`docs/terms`) accurate. They are
  version-aware: they describe the allowance and purchase for 2.0.0 and later, and link to the
  archived 1.0.0/1.0.2 documents at `/privacy/1.0/` and `/terms/1.0/`.
- Do not describe the purchase as removing the allowance "permanently"; the allowance is removed
  only while the purchase entitlement remains valid (a refund or revocation ends it).
- Do not claim the allowance is device- or account-linked, tamper-proof, or impossible to reset.
  Describe it plainly: "your allowance resets 24 hours after you use it". The allowance is
  calculated and stored locally, so clearing app data or reinstalling can start a fresh allowance;
  never promise otherwise in copy.
- The App shows the localized price before purchase and the installed version in Settings → About.
