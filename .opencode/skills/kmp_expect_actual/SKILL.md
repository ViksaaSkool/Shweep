---
name: kmp_expect_actual
description: Correctly separate platform-specific APIs using expect/actual declarations in KMP.
---

## Purpose

Correctly separate platform-specific APIs using expect/actual.

## Rules

- expect declarations must live in commonMain.
- actual implementations must live in androidMain or iosMain.
- Use expect/actual for:
  - haptics
  - filesystem paths
  - platform time quirks
  - blur or visual effects requiring platform APIs
  - platform feature detection
- expect declarations should be minimal and capability-focused,
  not mirror full platform APIs.
- commonMain must never reference platform classes directly.

## Example pattern

```kotlin
// commonMain
expect fun performHaptic()

// androidMain
actual fun performHaptic() { 
    // Android implementation
}

// iosMain
actual fun performHaptic() { 
    // iOS implementation
}
```

## Outcome

Shared logic stays clean while platform behavior remains native and correct.
