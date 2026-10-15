# Mushin Launcher — правила для агента

Продукт называется Mushin Launcher, понятие «энв» в интерфейсе — «спейс» (ADR-0007). В коде пакет `com.mushinlab.launcher`, понятие — `Space`, общие для приложения имена — `Mushin*` (ADR-0015). Рабочее имя `env` осталось только в хранилище: таблицы и колонки Room (`environment`, `env_id`), файл БД `env.db`, ключи DataStore (`current_env_id`). Их не переименовывать без миграции.

## Источник истины
- `docs/spec/*.md` — требования (AC Given/When/Then). `docs/plan.md` — эпики и DoD. `docs/adr/*.md` — неизменяемые решения.
- Если spec противоречит коду — прав spec. Если spec неоднозначен — СТОП, предложи ADR, не додумывай.
- Отклонения от spec уже приняты в ADR-0002 и далее (`docs/adr/README.md`). Открытые вопросы владельца — в `docs/questions.md` и в таблицах ADR.

## Команды
- Сборка: `./gradlew assembleDebug` | Тесты: `./gradlew testDebugUnitTest test` | UI: `./gradlew connectedDebugAndroidTest`
- Перед коммитом: `./gradlew ktlintFormat detekt lintDebug testDebugUnitTest test checkModuleGraph :app:checkReleasePermissions`
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
- `:app` — `MainActivity`, `MushinApp`, `AppContainer`, `LauncherViewModel`, связывание фич.
- Состояние интерфейса — во ViewModel и держателях состояния (`LauncherNavigator`, `HomeInteractions`, `SearchInteractions`), данные — через `LauncherModel` и редакторы. `MainActivity` — только жизненный цикл и системные вызовы по `LauncherEffect` (ADR-0015).
- `:benchmark`, `:core:search:bench` — бенчмарки.

## Жёсткие запреты
- НЕ использовать TextField/BasicTextField/фокус на экране поиска. Анимации — только переходы ≤ 200 мс из ADR-0003 (`Motion.TRANSITION_MS`) и пять стилей нажатия из ADR-0004 (`PressFeedback`); ripple запрещён.
- НЕ добавлять зависимости без ADR (запрещены Hilt, Navigation, Coil, Material, сеть). НЕ INTERNET, НЕ QUERY_ALL_PACKAGES.
- НЕ открывать и НЕ копировать код GPL-проектов (focus_launcher, Kvaesitso, Olauncher, KISS и др.).
- НЕ рефлексия по полям фреймворка (Android 17 static final). Единственное исключение — D.13.
- НЕ хардкодить таймауты жестов — только viewConfiguration.
- НЕ отправлять данные с компьютера в сеть: только запросы без полезной нагрузки (скачивание зависимостей). Никаких публикаций, загрузок и push без явного разрешения владельца.

## Стиль
- Kotlin, Compose (только foundation), @Immutable UI-модели, ImmutableList, без !! (кроме тестов).
- Без комментариев в коде: имена и структура объясняют сами себя. Исключение — KDoc на публичном API модулей `core:*` (ADR-0015).
- detekt — стандартные пороги (`config/detekt/detekt.yml`); не ослаблять конфиг и не подавлять правила detekt через `@Suppress`.
- Каждый публичный класс с поведением в core:* — прямой unit-тест; data-классы, enum и DAO проверяются через использующий их код. Тесты на AC называются `AC_xx_y_...`.
- Внешние документы (README, CONTRIBUTING, CHANGELOG с версии 0.1.1) — на английском; spec, ADR и вопросы — на русском.

## Definition of Done (общий)
- DoD задачи выполнен, все тесты зелёные, нет новых lint/detekt-ошибок, обновлён `docs/CHANGELOG.md`.
