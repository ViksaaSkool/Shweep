# Shweep monetization backend

Optional backend for the **future** monetization feature: authenticated,
cross-device daily sheep quota plus RevenueCat entitlement mirroring.

This directory does not affect the mobile app build. The app only talks to it
when built with `-Pshweep.monetization.enabled=true`.

## Responsibilities

- Firebase Authentication (Google on Android, Apple on iOS).
- Firestore as the authoritative quota store.
- `consumeSheep` / `getQuotaStatus` callable functions with server-side time.
- RevenueCat webhook that mirrors the `unlimited_sheep` entitlement.

RevenueCat remains the source of truth for the **purchase**. Firestore is the
source of truth for the **daily quota**.

## Collections

| Collection | Purpose |
|---|---|
| `users/{uid}` | Minimal profile; `platform`, timestamps. |
| `installations/{installationId}` | Random per-install id; `uid`, platform, app version. |
| `entitlements/{uid}` | Entitlement cache written only by the webhook/server. |
| `quotaPeriods/{uid}_{periodStart}` | `usedCount`, `exhaustedAt`, server timestamps. |
| `quotaRequests/{uid}_{requestId}` | Idempotency records for `consumeSheep`. |
| `revenueCatEvents/{eventId}` | Webhook dedupe. |

The client must never write to `entitlements`, `quotaPeriods`, or
`quotaRequests`; Firestore rules deny all client writes to them.

## Secrets (server only)

Set these in Firebase Secret Manager — never in the repository or the app:

- `REVENUECAT_SECRET_API_KEY`
- `REVENUECAT_WEBHOOK_AUTH` (webhook authorization header value)

## Setup

```sh
cd backend/functions
npm install
firebase deploy --only functions,firestore:rules
```

See `MONETIZATION.md` in the repository root for the full runbook.
