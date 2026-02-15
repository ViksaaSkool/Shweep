---
name: kmp_storage_datastore
description: Ensure correct use of DataStore-style persistence in a KMP project with platform-specific implementations.
---

## Purpose

Ensure correct use of DataStore-style persistence in a KMP project.

## Rules

- commonMain must define a storage abstraction (e.g. SessionsStore).
- commonMain must NOT import or reference AndroidX DataStore directly.
- Android implementation:
  - Use AndroidX DataStore.
  - Provide the actual implementation in androidMain.
- iOS implementation:
  - Use a file-backed store (JSON or Proto via kotlinx.serialization).
  - Perform atomic writes (write temp file, then replace).
  - Guard updates with a Mutex to avoid race conditions.
- Stored data should be modeled as a single payload object
  (e.g. SessionsPayload containing a list).
- Repository logic (append, delete, trim, retention rules)
  must live in commonMain.
- Storage must expose:
  - a Flow of the stored payload
  - a suspend update(transform) function.

## Outcome

Identical persistence behavior on Android and iOS with a DataStore-style API.
