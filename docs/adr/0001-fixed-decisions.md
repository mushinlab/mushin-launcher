# ADR-0001. Зафиксированные решения

Статус: принято, 2026-10-03.

Решения из `docs/spec/tech.md` §D.1 считаются неизменяемыми; менять только новым ADR.

1. Одна `MainActivity` (Home, Search, Popup, Settings) — один `ComponentActivity` с `setContent`.
2. Kotlin 2.4.20, AGP 9.3.1, Gradle 9.7.1, Compose BOM 2026.09.00, compileSdk 37, targetSdk 37, minSdk 33. JVM target 17.
3. DI ручной: `AppContainer` создаётся в `Application.onCreate`, зависимости через конструкторы, `by lazy`.
4. Room 2.8.5 (через KSP 2.3.12) для доменных данных; DataStore Preferences 1.2.1 для настроек.
5. Без Navigation, Hilt, Coil, Accompanist, Material и сетевых библиотек. Иконки загружаются своим загрузчиком.
6. В поиске нет `TextField`, ввод — только через `KeyboardRouter`.
7. Нет `INTERNET`, нет `QUERY_ALL_PACKAGES`.
8. Ноль анимаций, кроме системных.
9. Код GPL-проектов не читается и не копируется.

Версии остальных библиотек зафиксированы в `gradle/libs.versions.toml` на дату E0.
