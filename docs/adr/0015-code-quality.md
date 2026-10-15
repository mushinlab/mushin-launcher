# ADR-0015. Качество кода перед публикацией: архитектура, имена, правила

Статус: принято (указания владельца после ревью кода Claude и Muse). 2026-10-16. Отчёт ревью — [code-review-2026-10-16.md](../code-review-2026-10-16.md).

## Решения
1. **Имена в коде.**
   - Пакеты `com.bluecubelabs.env` → `com.mushinlab.launcher`, Gradle-плагины `env.*` → `mushin.*`, проект `mushin-launcher`, свойство сборки `-Pmushin.debugSignedRelease`.
   - Понятие «энв» в коде — `Space` (`SpaceType`, `SpaceRepository`, `spaceId`…). Общие для приложения имена с префиксом `Env` стали `Mushin*`: `MushinApp`, `MushinDatabase`, `MushinTheme`, `MushinColors`, `MushinType`, `MushinFont`, `MushinStroke`.
   - Хранилище не меняется: таблицы и колонки Room (`environment`, `environment_item`, `env_id`), файл БД `env.db`, ключи DataStore (`current_env_id`), строковые test tag. Экспортированные схемы Room совпадают байт в байт, миграции не нужны.
2. **Архитектура `app`.**
   - Состояние интерфейса перенесено из `MainActivity` во ViewModel и держатели состояния: `LauncherNavigator` (оверлеи, режим поиска, переименование, уведомления, стек настроек, «Назад»), `HomeInteractions` (свайпы, рисунки, подсказка обучения), `SearchInteractions` (меню, скрытие, спейс, аппаратная клавиатура), `AppLaunches` (запуск и уведомления о сбое).
   - Данные для держателей — через интерфейс `LauncherModel` (реализация `RepositoryLauncherModel`), поэтому они проверяются unit-тестами с фейком.
   - Операции с данными разнесены по редакторам: `SpaceEditor`, `AppEditor`, `PreferencesEditor`, `MemoryEditor`, `SuggestionEditor`.
   - Системные действия (открыть часы, запрос роли, выбор фото, плитка, тактильный отказ) ViewModel отправляет событиями `LauncherEffect`; их выполняет `MainActivity` через `SystemScreens`. ViewModel не держит Activity.
   - Тексты уведомлений хранятся как id строковых ресурсов и разрешаются в интерфейсе.
   - Преобразования данных в состояния экранов — чистые функции (`LauncherData.kt`, `SettingsMapping.kt`) с unit-тестами.
   - Стек настроек сохраняется в `SavedStateHandle` (`SettingsRouteCodec`) и переживает выгрузку процесса. Поиск и всплывающие меню после выгрузки процесса не восстанавливаются: это то же поведение, что при нажатии «Домой».
3. **Комментарии.** Запрет на комментарии остаётся. Исключение — KDoc на публичном API модулей `core:*`: там алгоритмы (распознавание рисунка, ранжирование поиска), смысл которых не виден из имён.
4. **Тесты.** Правило «каждый публичный класс в core:* — unit-тест» уточнено: прямой тест нужен каждому классу с поведением; data-классы, enum и DAO проверяются через код, который их использует.
5. **detekt.** Стандартные пороги detekt. Два общепринятых отступления: ранние `return` (guard clauses) не считаются в `ReturnCount`; функции Room (`@Query`, `@Insert` и т. п.) не считаются в `TooManyFunctions`. Для Compose `MagicNumber` и `LongParameterList` не применяются к `@Composable` (и `MagicNumber` — к `@Preview`), а `UnusedPrivateMember` — к `@Preview`. Подавлять правила через `@Suppress` нельзя.
6. **Язык документов.** README, CONTRIBUTING и CHANGELOG начиная с 0.1.1 — на английском. Spec, ADR и вопросы остаются на русском как внутренняя история решений.
7. **Превью.** Для `@Preview` в `core:ui` добавлены `androidx.compose.ui:ui-tooling-preview` (только аннотации) и `androidx.compose.ui:ui-tooling` в `debugImplementation`: инструменты отрисовки превью в release не попадают. `ui-tooling` транзитивно приносит `material3` и `material-ripple`, но только в debug-сборку: код проекта их не использует, а в `releaseRuntimeClasspath` их нет (проверено). Это не нарушает запрет на Material из AGENTS.md. Превью есть у базовых компонентов `core:ui`.
8. **`supportsRtl="false"`** остаётся: интерфейс есть только на русском и английском, оба пишутся слева направо, а жесты и раскладки рассчитаны на LTR.
