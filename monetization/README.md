# :monetization

Optional module for the future **RevenueCat + Firebase** monetization feature
(Unlimited Sheep purchase and cross-device daily sheep quota).

## Status

Scaffolding only. This module is **not part of the default build** and
`composeApp` does not depend on it.

- Included in Gradle only when `shweep.monetization.enabled=true`.
- With the default `false` value, Gradle never configures it and never resolves
  `com.revenuecat.purchases:purchases-kmp`.

## Why a separate module

Keeping RevenueCat/Firebase in an isolated module makes it impossible for that
code, configuration, keys, or network activity to reach the shipping app unless
a build explicitly opts in.

## Planned contents

- `RevenueCatStorePurchaseGateway` implementing the app's `StorePurchaseGateway`
  seam, unlocking from `customerInfo.entitlements["unlimited_sheep"]?.isActive`.
- RevenueCat mappers converting SDK models into app-owned models.
- Platform `PurchaseConfiguration` supplying the public SDK key.
- A Firebase quota client (`getQuotaStatus`, `consumeSheep`) with an
  installation-id based idempotency key.

## Entitlement rule

Always unlock from the entitlement, never from product ownership or a locally
persisted boolean:

```kotlin
customerInfo.entitlements[UNLIMITED_SHEEP_ENTITLEMENT_ID]?.isActive == true
```

## Activation

See `MONETIZATION.md` in the repository root.
