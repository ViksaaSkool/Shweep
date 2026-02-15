---
name: kmp_compose_multiplatform
description: Ensure Compose Multiplatform UI works consistently on Android and iOS without platform-specific hacks.
---

## Purpose

Ensure Compose UI works consistently on Android and iOS.

## Rules

- Shared UI must use Compose Multiplatform–supported APIs only.
- Avoid Android-only Compose APIs and modifiers.
- Use Material 3 components compatible with Compose Multiplatform.
- Do not rely on Android-specific assumptions:
  - window insets
  - system UI controllers
  - density or font scaling behavior
- Any platform-specific UI behavior must be gated behind
  expect/actual or explicit platform checks.
- Animations must be frame-safe and not rely on Android-only clocks.

## Outcome

UI code renders correctly on both platforms without conditional hacks.
