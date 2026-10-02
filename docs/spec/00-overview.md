# Пакет требований и спецификаций: персональный минималистичный Android-лаунчер «Энв» (Pixel 6 / Android 17)

Делать проект стоит как собственный clean-room-лаунчер с нуля, а не как форк Focus Launcher. Стек: Kotlin 2.4.20, AGP 9.3.1, Compose BOM 2026.09.00. compileSdk и targetSdk — 37, minSdk — 33. Поиск со своей клавиатурой и popup рендерятся в той же Activity, что и главный экран, без навигации и без системного IME. Это единственный способ уложить открытие поиска в один кадр. Код Focus Launcher и большинства референсов распространяется под GPL-3.0: копировать его можно только при переходе всего проекта на GPL. Поэтому из референсов берём только идеи и UX.

Файлы спецификации:

- `00-overview.md` — TL;DR и ключевые выводы исследования (этот файл)
- `business.md` — A. Бизнес-анализ
- `functional.md` — B. Функциональная спецификация (AC)
- `ux.md` — C. UX-спецификация
- `tech.md` — D. Техническая спецификация
- `references.md` — F. Сравнение референсных лаунчеров
- `glossary.md` — G. Глоссарий и Caveats
- `../plan.md` — E. План реализации

## TL;DR

- **Что строим и на чём.** Ниже — однозначная спецификация лаунчера для одной Activity с одним слоем жестов:
  - главный экран: часы, дата и «зебра»-список избранного;
  - long-press на любом месте открывает anchored-popup с энвами и шестерёнкой; на строке приложения в нём есть ещё и секция действий над приложением;
  - двойной тап открывает поиск с собственной клавиатурой в первом же кадре после второго касания;
  - справа поиска — recents текущего энва (1/3 ширины), слева — результаты (2/3).

  Поиск с пустым запросом показывает все приложения по алфавиту, поэтому отдельный экран «Все приложения» не нужен.
- **Платформа.** Android 17 (API 37) вышел 16 июня 2026. Для лаунчера на нём важны:
  - принудительный edge-to-edge и predictive back, унаследованные от Android 16;
  - MemoryLimiter — лимиты памяти для всех приложений;
  - ужесточение BAL для IntentSender;
  - запрет менять static final через рефлексию;
  - Private Space: требует ACCESS_HIDDEN_PROFILES и роли HOME.

  Pixel 6/6 Pro теряют гарантированные обновления в октябре 2026, то есть прямо сейчас. Android 17 QPR1 — их последнее крупное обновление: Droid-Life 15.09.2026 написал «this will be their final major update». Pixel 6a поддерживается до июля 2027. Платформа устройства фактически заморожена, и это скорее плюс для стабильности целевой среды.
- **Как вести разработку с AI-агентом.** 13 последовательных эпиков (E0–E12) с Definition of Done. Решения, перечисленные в разделе «Зафиксированные решения», считаются неизменяемыми. Короткий CLAUDE.md (до 200 строк) ссылается на spec-файлы в репозитории. Каждая задача заканчивается проверяемым шагом: тестом, бенчмарком или скриншот-тестом.

---

## Ключевые выводы исследования

### Платформа и устройство

| Факт | Следствие для лаунчера |
|---|---|
| Android 17 = API 37, стабильный релиз 16.06.2026. Сборка CP3A.260905.009 — это сентябрьский Pixel Drop 2026 (Android 17 QPR1). | compileSdk = 37. targetSdk = 37 (для личного использования допустимо). |
| Pixel 6/6 Pro: конец поддержки — октябрь 2026. По данным Yahoo Tech, оставшиеся обновления — «the August security patch, the September Pixel Drop with Android 17 QPR1, and the October security patch». QPR2 (декабрь 2026) на них не выйдет. Pixel 6a: июль 2027. Официальная политика Google: «5 years starting from when the device first became available on the Google Store in the US»; Pixel 6/6 Pro поступили в продажу в октябре 2021, 6a — в июле 2022. | Целевая ОС больше не изменится. Нельзя рассчитывать на API из QPR2 и Android 18. minSdk 33 оставляет запас на переезд на новое устройство. |
| Android 16 (targetSdk 36): `windowOptOutEdgeToEdgeEnforcement` отключён, отказаться от edge-to-edge нельзя. Predictive back включён по умолчанию, `onBackPressed()` не вызывается. | Весь back — только через `OnBackPressedDispatcher` / `BackHandler`. Все отступы — через `WindowInsets`. |
| Android 17 (все приложения): лимиты памяти по объёму RAM устройства. При убийстве процесса exit reason = `REASON_OTHER`, в описании строка `"MemoryLimiter:AnonSwap"`. | Лаунчер живёт постоянно: кэш иконок нужно жёстко ограничить по байтам, а состояние лаунчера — сделать восстанавливаемым. |
| Android 17 (targetSdk 37): static final нельзя менять через рефлексию (`IllegalAccessException`). MessageQueue стал lock-free и ломает рефлексию по его приватным полям. | Рефлексия по приватным полям фреймворка запрещена. Исключение одно — вызов скрытого метода `expandNotificationsPanel`, это метод, а не поле. |
| Android 17 (targetSdk 37): ограничения BAL распространены на `IntentSender`. `MODE_BACKGROUND_ACTIVITY_START_ALLOWED` следует заменить на гранулярные режимы, например `MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE`. | Запуск приложений — только когда лаунчер на переднем плане, через `LauncherApps.startMainActivity`. Shortcuts и PendingIntent — из видимого состояния. |
| Android 17 (targetSdk 37): на дисплеях sw ≥ 600dp ориентация, resizeability и aspect ratio игнорируются, opt-out убран. | На Pixel 6 (телефон, sw ≈ 411dp) блокировка портрета продолжает работать. На планшете или складном устройстве перестанет. |
| Android 17: по данным анонса Beta 1, Activity больше не пересоздаётся при CONFIG_KEYBOARD / KEYBOARD_HIDDEN / NAVIGATION / TOUCHSCREEN / COLOR_MODE — вместо этого вызывается `onConfigurationChanged`. В текущих страницах behavior changes этого пункта нет. | Подключение аппаратной клавиатуры не сбрасывает состояние поиска. На всякий случай объявляем `configChanges` явно. |
| Android 17 (все приложения): после поворота экрана видимость IME больше не восстанавливается. | Нас это не касается: системный IME в поиске не используется вообще. |
| Private Space (Android 15+). Лаунчеру нужны `android.permission.ACCESS_HIDDEN_PROFILES` и роль `ROLE_HOME`. Тип профиля определяется через `LauncherApps.getLauncherUserInfo()` (API 35). Требования: отдельный контейнер; пользователь может скрыть или показать пространство; нужно слушать `ACTION_PROFILE_AVAILABLE` / `ACTION_PROFILE_UNAVAILABLE`; проверять `isQuietModeEnabled()`. | Отдельная секция в поиске (v1.1). Если пространство заблокировано, избранное из него скрывается. |
| `setSystemGestureExclusionRects`: лимит — 200dp по вертикали на край. Лимит **не действует** для IME и для Activity с `CATEGORY_HOME`. Нижняя зона (mandatory system gestures, жест «домой») не исключается никогда. | Лаунчер технически может забрать боковые края под свои жесты. Мы сознательно не делаем этого (см. C.4). Свайп вверх засчитывается только если стартовал выше mandatory-инсета. |
| `EXPAND_STATUS_BAR` имеет protectionLevel normal. Скрытый `StatusBarManager.expandNotificationsPanel()` по данным открытых лаунчеров (launcher-plusplus, Minlauncher) работает через рефлексию на API 37 (greylist). Официально это не документировано. | Свайп вниз → шторка через рефлексию в try/catch. Если вызов не сработал — уведомляем пользователя один раз. |
| `detectTapGestures`: если задан `onDoubleTap`, одиночный тап распознаётся только по истечении `doubleTapTimeoutMillis`. В AOSP это ровно 300 мс: в ViewConfiguration.java задано `DOUBLE_TAP_TIMEOUT = 300`. | Распознавать двойной тап на строках нельзя, иначе запуск приложения задержится на 300 мс. Нужен собственный детектор (см. D.9). |
| `AdaptiveIconDrawable.getMonochrome()` (API 33) может вернуть null. У монохромного слоя используется только альфа-канал. | Двухпутевой конвейер: монохромный слой, если он есть, иначе обесцвечивание через ColorMatrix (см. D.7). |

### Тулчейн на 1 октября 2026

| Компонент | Версия | Обоснование |
|---|---|---|
| Kotlin | 2.4.20 | Текущий стабильный релиз. Матрица совместимости Kotlin полностью поддерживает AGP до 9.3.1 и Gradle до 9.7.0. |
| AGP | 9.3.1 | Compose 1.12 требует compileSdk 37 и AGP ≥ 9.2.0. AGP 9.4.x уже доступен, но выходит за полностью поддерживаемый диапазон Kotlin 2.4.20. |
| Gradle | 9.7.x | AGP 9.x требует Gradle ≥ 9.6.0. |
| Compose BOM | 2026.09.00 | Актуальный стабильный BOM (Compose 1.12.x, runtime 1.12.1). |
| JDK | toolchain 17, CI на 21 | |
| Особенность AGP 9 | встроенный Kotlin | Плагин `org.jetbrains.kotlin.android` не применяется. Применяется только `org.jetbrains.kotlin.plugin.compose`, его версия совпадает с версией Kotlin. |
| DI | ручной (AppContainer) | Hilt поддерживает AGP 9 только с версии 2.59; лишняя KSP-обработка замедляет сборку. Текстовый Focus Launcher (F-Droid) тоже обходится «hand-rolled service locator». |

### Референсы и лицензии

- **mslalith/focus_launcher.** Лицензия GPL-3.0, 264 звезды и 27 форков на GitHub (на 01.10.2026). Стек: Compose, Circuit, Hilt, Room, DataStore, Ktor. Модули core/feature, build-logic, отдельный модуль baselineprofile. Функции: тёмная тема, icon packs, фаза луны, анимированные часы, избранное, скрытие приложений.
- **com.focus.launcher (F-Droid).** Автор — Chaitany Patel, лицензия GPL-3.0-or-later, версия 1.1.52 от 25.09.2026.
  - Возможности: только текст, до пяти приложений; поиск «understands names, initials and typos»; приложения, установленные за последние 24 часа, показываются сверху; для рабочего профиля — отдельные вкладки.
  - Разрешения: EXPAND_STATUS_BAR, QUERY_ALL_PACKAGES, PACKAGE_USAGE_STATS, READ_CALENDAR, REQUEST_DELETE_PACKAGES.
  - APK весит 1,3 MiB, minSdk 26.
- **Остальные референсы.** Kvaesitso, Olauncher, mLauncher и KISS — GPL-3.0. По Lawnchair источники **расходятся**: официальная документация и IzzyOnDroid указывают Apache-2.0, README одного из форков — GPLv3. До заимствования кода нужно проверить файл LICENSE в репозитории `LawnchairLauncher/lawnchair`. AOSP Launcher3 распространяется под Apache-2.0: заимствовать из него можно, сохранив NOTICE.
