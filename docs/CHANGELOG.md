# Changelog

## Unreleased

### E0 Bootstrap
- Gradle 9.7.1, AGP 9.3.1, Kotlin 2.4.20, Compose BOM 2026.09.00; version catalog, build-logic (convention plugins), модули D.2.
- Спецификация разложена по `docs/spec/*`, план — `docs/plan.md`, ADR-0001, ADR-0002.
- ktlint + detekt (`env.quality`), проверка графа модулей `checkModuleGraph`, CI workflow для GitHub Actions.

### E1 Data
- Room v1 (схема в `core/data/schemas`), репозитории энвов, переопределений, истории, жестов; очистка по Q12.
- DataStore-настройки, снимок Home для первого кадра (`HomeSnapshotStore`).

### E2–E3 Каталог и иконки
- `AppCatalog` поверх `LauncherApps` (профили, callback, quiet mode, suspended), `AppLauncher`, шторка через рефлексию.
- Конвейер иконок: монохромный слой или обесцвечивание, ALPHA_8 в памяти (LRU 8 МБ) и на диске, буквенный fallback.

### E4–E6 Home, жесты, popup
- Home: часы, дата, зебра, лимит строк, иконки, карточка роли.
- `GestureArbiter` + детектор жестов без задержки тапа; popup-слой в той же композиции.

### E7–E9 Поиск
- Движок поиска (нормализация, транслит, фонетика, раскладочный своп, fuzzy, ранжирование по использованию).
- Своя клавиатура EN/RU с цифрами, повтором backspace и rollover; экран поиска с recents; аппаратная клавиатура.

### E10 Настройки
- Стек маршрутов в Activity; энвы (CRUD, палитры), редактор с drag & drop, пикеры, главный экран, поиск, клавиатура, жесты, приложения, система.

### E11 Perf
- Baseline Profile (сгенерирован на эмуляторе API 36), Macrobenchmark: холодный старт, открытие поиска, набор; microbenchmark поиска.
- Результаты эмулятора: `perf/results/*.json`. Замер на Pixel 6 не проводился.

### E12 Hardening
- Локализация ru (по умолчанию) и en; лог причин завершения процесса (`files/exit_log.tsv`); ручной чек-лист `docs/checklist.md`.
