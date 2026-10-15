# Contributing to Mushin Launcher

Thanks for your interest. This page explains how the project is built, how the code is organised and which rules every change has to follow.

## Getting started

You need JDK 17 or newer and the Android SDK (compile and target SDK 37, min SDK 33). Put `sdk.dir` in `local.properties`.

```
./gradlew assembleDebug                 # build
./gradlew testDebugUnitTest test        # unit tests (Android modules and JVM modules)
./gradlew connectedDebugAndroidTest     # UI tests on an emulator
```

Before every commit run the same checks as CI:

```
./gradlew ktlintFormat detekt lintDebug testDebugUnitTest test checkModuleGraph :app:checkReleasePermissions
```

## Modules

| Module | Purpose |
|---|---|
| `:core:model`, `:core:search` | Pure Kotlin/JVM: models, search, swipe-typing and drawing recognition |
| `:core:data` | Room database and DataStore settings |
| `:core:apps` | Installed apps, icons, launching apps |
| `:core:ui` | Theme, palettes and base composables |
| `:feature:home`, `:feature:search`, `:feature:settings` | Screens. They depend only on `:core:*`, never on each other (`checkModuleGraph` enforces this) |
| `:app` | `MainActivity`, `LauncherViewModel`, dependency container, wiring of the features |
| `:benchmark`, `:core:search:bench` | Macro and micro benchmarks, Baseline Profile |

## Architecture

- `LauncherViewModel` combines repository flows into screen states. The mapping functions are pure and unit-tested (`LauncherData.kt`, `SettingsMapping.kt`).
- UI state lives in plain state holders owned by the ViewModel: `LauncherNavigator` (overlays, settings stack, back handling), `HomeInteractions`, `SearchInteractions`. They read data through the `LauncherModel` interface, so tests use a fake.
- Data changes go through small editors: `SpaceEditor`, `AppEditor`, `PreferencesEditor`, `MemoryEditor`, `SuggestionEditor`.
- `MainActivity` handles only the lifecycle and platform calls. The ViewModel asks for them with `LauncherEffect` events.
- Dependencies are wired by hand in `AppContainer`. There is no DI framework and no Navigation library.

## Names

The user-facing concept is a **space**. In code it is `Space` (`SpaceRepository`, `spaceId`). App-wide names use the `Mushin` prefix (`MushinTheme`, `MushinDatabase`). The project's working name `env` survives only in storage: Room table and column names (`environment`, `env_id`), the database file `env.db` and DataStore keys (`current_env_id`). Do not rename them without a migration.

## Rules

- No new dependencies without a decision record in `docs/adr`. Hilt, Navigation, Coil, Material and any network library are not allowed.
- The app has no `INTERNET` permission and must never get one. `:app:checkReleasePermissions` fails the build if it appears.
- Do not open or copy code from GPL-licensed launchers.
- Compose foundation only. No `TextField` on the search screen: it has its own keyboard. No ripple; transitions are at most 200 ms (`Motion.TRANSITION_MS`).
- Gesture timeouts come from `ViewConfiguration`, never hard-coded.
- No `!!` outside tests. UI models are `@Immutable` and use immutable collections.
- No comments in code: names and structure explain it. The exception is KDoc on the public API of `:core:*` modules.
- detekt runs with standard thresholds. Fix the code instead of relaxing the config or adding `@Suppress`.

## Tests

- Every public class with behaviour in `:core:*` has a direct unit test. Data classes, enums and DAOs are covered by the code that uses them.
- Tests that check an acceptance criterion from the spec are named `AC_xx_y_...`.
- UI tests run on an emulator. Never run them on a personal phone: they uninstall the app and wipe its data.

## Documentation

- Requirements live in `docs/spec`, decisions in `docs/adr`, open questions in `docs/questions.md`. These are written in Russian.
- `README.md`, this file and `docs/CHANGELOG.md` (from 0.1.1 on) are in English. Add a changelog entry for every user-visible change.

## Security

Please report vulnerabilities privately through [GitHub private vulnerability reporting](https://github.com/mushinlab/mushin-launcher/security/advisories/new), not in a public issue.

## License

By contributing you agree that your contributions are licensed under the [Apache License 2.0](LICENSE).
