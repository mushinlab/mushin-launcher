# Env Launcher — правила для агента

## Источник истины
- `docs/spec/*.md` — требования (AC Given/When/Then). `docs/plan.md` — эпики и DoD. `docs/adr/*.md` — неизменяемые решения.
- Если spec противоречит коду — прав spec. Если spec неоднозначен — СТОП, предложи ADR, не додумывай.
- Отклонения от spec уже приняты в ADR-0002 и далее (`docs/adr/README.md`). Открытые вопросы владельца — в журнале решений (ссылка в `docs/adr/README.md`).

## Команды
- Сборка: `./gradlew assembleDebug` | Тесты: `./gradlew testDebugUnitTest test` | UI: `./gradlew connectedDebugAndroidTest`
- Перед коммитом: `./gradlew ktlintFormat detekt lintDebug testDebugUnitTest test checkModuleGraph`
- JVM-модули (`:core:model`, `:core:search`) тестируются задачей `test`, Android-модули — `testDebugUnitTest`.

## Устройство и замеры
- Instrumented/UI: `./gradlew :app:connectedDebugAndroidTest :core:apps:connectedDebugAndroidTest` (эмулятор API 36 подходит).
- Baseline Profile: `./gradlew :app:generateBaselineProfile` (на эмуляторе добавить `-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR`).
- Macrobenchmark: `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`; microbenchmark поиска: `./gradlew :core:search:bench:connectedAndroidTest`.
- Результаты замеров — в `perf/results/`.

## Модули (D.2)
- `:core:model`, `:core:search` — чистый Kotlin/JVM, без Android.
- `:core:data` (Room + DataStore), `:core:apps` (LauncherApps, иконки, запуск), `:core:ui` (палитра, базовые composable).
- `:feature:home`, `:feature:search`, `:feature:settings` — зависят только от `:core:*`, не друг от друга.
- `:app` — `MainActivity`, `EnvApp`, `AppContainer`, связывание фич. `:benchmark`, `:core:search:bench` — бенчмарки.

## Жёсткие запреты
- НЕ использовать TextField/BasicTextField/фокус на экране поиска. НЕ анимации (animate*, AnimatedVisibility, Crossfade, ripple).
- НЕ добавлять зависимости без ADR (запрещены Hilt, Navigation, Coil, Material, сеть). НЕ INTERNET, НЕ QUERY_ALL_PACKAGES.
- НЕ открывать и НЕ копировать код GPL-проектов (focus_launcher, Kvaesitso, Olauncher, KISS и др.).
- НЕ рефлексия по полям фреймворка (Android 17 static final). Единственное исключение — D.13.
- НЕ хардкодить таймауты жестов — только viewConfiguration.

## Стиль
- Kotlin, Compose (только foundation), @Immutable UI-модели, ImmutableList, без !! (кроме тестов).
- Без комментариев в коде: имена и структура объясняют сами себя.
- Каждый публичный класс в core:* — unit-тест. Тесты на AC называются `AC_xx_y_...`.

## Definition of Done (общий)
- DoD задачи выполнен, все тесты зелёные, нет новых lint/detekt-ошибок, обновлён `docs/CHANGELOG.md`.
