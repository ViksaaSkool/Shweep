---
name: kmp_module_boundary
description: Enforce KMP module boundaries to prevent Android/iOS-specific APIs from leaking into shared code.
---

## Purpose

Prevent Android- or iOS-specific APIs from leaking into shared code.

## Rules

- Code in commonMain may only use:
  - Kotlin standard library
  - kotlinx.coroutines, Flow, StateFlow
  - kotlinx.serialization
  - Compose Multiplatform UI
  
- commonMain MUST NOT reference:
  - android.*
  - androidx.*
  - Context, Resources, Looper, Handler
  - AndroidX DataStore, Room, SharedPreferences
  
- Platform-specific behavior must be expressed via:
  - interfaces defined in commonMain, or
  - expect/actual declarations.
  
- Actual implementations must live in:
  - androidMain for Android
  - iosMain for iOS.
  
- If a feature requires platform APIs (storage, haptics, blur, filesystem),
  define an abstraction in commonMain and implement it per platform.

## Outcome

Shared code remains portable and builds on both Android and iOS without hacks.
