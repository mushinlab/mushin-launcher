# D. Техническая спецификация

> Отклонения от этого документа фиксируются в `docs/adr/` (ADR-0002 и далее) и перечислены в `docs/adr/README.md`.

## D.1 Зафиксированные решения (ADR-0001…; менять только новым ADR)

1. Одна `MainActivity` (Home, Search, Popup, Settings) — один `ComponentActivity` с `setContent`.
2. Kotlin 2.4.20, AGP 9.3.1, Gradle 9.7.x, Compose BOM 2026.09.00, compileSdk 37, targetSdk 37, minSdk 33. JVM toolchain 17.
3. DI ручной: `AppContainer` создаётся в `Application.onCreate`, зависимости через конструкторы, `by lazy`.
4. Room (последняя стабильная на момент E0, фиксируется в `libs.versions.toml`, через KSP) для доменных данных; DataStore Preferences для настроек.
5. Без Navigation, Hilt, Coil, Accompanist и сетевых библиотек. Иконки загружаются своим загрузчиком.
6. В поиске нет `TextField`, ввод — только через `KeyboardRouter`.
7. Нет `INTERNET`, нет `QUERY_ALL_PACKAGES`.
8. Ноль анимаций, кроме системных.
9. Код GPL-проектов не читается и не копируется.

## D.2 Модули

```
:app                — MainActivity, Application, AppContainer, манифест, тема
:core:model         — чистые data-классы (AppKey, AppEntry, Environment, Palette, GestureBinding…) [JVM]
:core:search        — движок поиска, раскладки, транслит [чистый JVM, без Android]
:core:data          — Room (entities, DAO, migrations), DataStore, репозитории
:core:apps          — AppCatalog (LauncherApps, профили, Callback), IconLoader/IconCache, AppLauncher, NotificationShade
:core:ui            — токены палитры, LocalPalette, базовые composable (ZebraRow, AnchoredPopup, Divider)
:feature:home       — HomeScreen, GestureLayer, Clock
:feature:search     — SearchScreen, Keyboard, KeyboardRouter, RecentsPane
:feature:settings   — Settings*, EnvEditor, AppPicker, GesturePicker
:benchmark          — Macrobenchmark + генератор Baseline Profile (com.android.test)
:core:search:bench  — microbenchmark поиска (androidx.benchmark)
build-logic         — convention plugins (android-library, compose, jvm-library)
```

Правила зависимостей: `feature:*` → `core:*`; `core:search` и `core:model` ни от чего Android не зависят; `feature:*` не зависят друг от друга. Проверка — Gradle-плагин `dependency-analysis` или ручной тест в CI.

## D.3 Слои и state management

- **Data.** Репозитории выдают `StateFlow` с `SharingStarted.Eagerly` в `applicationScope` (`SupervisorJob() + Dispatchers.Default`).
- **Domain.** Use-case-функции без классов-обёрток (`switchEnvironment`, `launchFromSearch`…).
- **UI.**
  - `LauncherViewModel` (один на Activity) собирает `HomeUiState` через `combine(envFlow, itemsFlow, catalogFlow, settingsFlow)`.
  - Горячий путь поиска идёт мимо Flow: `SearchController` — обычный класс с `mutableStateOf(query)`. Поиск вызывается синхронно на main thread (≤ 4 мс), результат кладётся в `mutableStateOf(results)`. Так убирается задержка диспетчеризации корутин.
  - `OverlayState` (`None | Popup(anchor, target) | Search(mode) | Settings(stack)`) — один `mutableStateOf` в Activity-scope.
- **Против лишних рекомпозиций.**
  - Все UI-модели — `@Immutable` data-классы с `ImmutableList` (kotlinx.collections.immutable) или собственной обёрткой `@Immutable`.
  - Лямбды стабилизированы через `remember`.
  - Часы — отдельный composable, читающий `State<Long>` минуты.
  - Палитра передаётся через `CompositionLocal` с `staticCompositionLocalOf` (смена палитры перерисовывает всё — так и задумано).
  - Включены Compose Compiler reports (`reportsDestination`) и strong skipping (по умолчанию в Kotlin 2.x).

## D.4 Хранение: схема Room (version 1)

```sql
CREATE TABLE environment (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT    NOT NULL COLLATE NOCASE UNIQUE,
  type          TEXT    NOT NULL CHECK(type IN ('NORMAL','WORK','CUSTOM')),
  sort_order    INTEGER NOT NULL,
  palette_preset TEXT   NOT NULL,           -- 'GRAPHITE'|'NAVY'|'PAPER'|'MOSS'|'CUSTOM'
  c_background INTEGER NOT NULL, c_zebra_a INTEGER NOT NULL, c_zebra_b INTEGER NOT NULL,
  c_text_primary INTEGER NOT NULL, c_text_secondary INTEGER NOT NULL, c_accent INTEGER NOT NULL,
  c_icon_tint INTEGER NOT NULL, c_divider INTEGER NOT NULL, c_surface INTEGER NOT NULL,
  c_key_background INTEGER NOT NULL, c_key_pressed INTEGER NOT NULL,
  is_dark       INTEGER NOT NULL,           -- 0/1
  created_at    INTEGER NOT NULL            -- epoch ms
);
-- Инварианты в репозитории (транзакция): ровно 1 NORMAL; ≤1 WORK; ≤20 CUSTOM.

CREATE TABLE environment_item (
  env_id        INTEGER NOT NULL REFERENCES environment(id) ON DELETE CASCADE,
  package_name  TEXT    NOT NULL,
  activity_name TEXT    NOT NULL,
  user_serial   INTEGER NOT NULL,
  position      INTEGER NOT NULL,           -- 0..N-1, плотная нумерация
  PRIMARY KEY (env_id, package_name, activity_name, user_serial)
);
CREATE INDEX idx_env_item_order ON environment_item(env_id, position);

CREATE TABLE app_override (
  package_name  TEXT    NOT NULL,
  activity_name TEXT    NOT NULL,
  user_serial   INTEGER NOT NULL,
  custom_label  TEXT,                       -- NULL = без переименования
  hidden        INTEGER NOT NULL DEFAULT 0,
  orphaned_at   INTEGER,                    -- момент удаления пакета (Q12), NULL если установлен
  updated_at    INTEGER NOT NULL,
  PRIMARY KEY (package_name, activity_name, user_serial)
);

CREATE TABLE search_recent (
  env_id        INTEGER NOT NULL REFERENCES environment(id) ON DELETE CASCADE,
  package_name  TEXT    NOT NULL,
  activity_name TEXT    NOT NULL,
  user_serial   INTEGER NOT NULL,
  last_launched_at INTEGER NOT NULL,
  launch_count  INTEGER NOT NULL DEFAULT 1,
  PRIMARY KEY (env_id, package_name, activity_name, user_serial)
);
CREATE INDEX idx_recent_env_time ON search_recent(env_id, last_launched_at DESC);

CREATE TABLE app_usage (                    -- глобально, для ранжирования
  package_name  TEXT NOT NULL, activity_name TEXT NOT NULL, user_serial INTEGER NOT NULL,
  launch_count  INTEGER NOT NULL, last_launched_at INTEGER NOT NULL,
  decayed_score REAL NOT NULL,              -- см. D.11
  PRIMARY KEY (package_name, activity_name, user_serial)
);

CREATE TABLE gesture_binding (
  direction     TEXT PRIMARY KEY CHECK(direction IN ('UP','DOWN','LEFT','RIGHT')),
  action        TEXT NOT NULL CHECK(action IN ('NONE','LAUNCH_APP','NOTIFICATIONS','SEARCH','ALL_APPS')),
  package_name  TEXT, activity_name TEXT, user_serial INTEGER   -- только для LAUNCH_APP
);
```

- `PRAGMA foreign_keys=ON` (Room включает для `@ForeignKey`).
- Переупорядочивание: `DELETE FROM environment_item WHERE env_id=?` и вставка всех строк с новыми `position` в одной `@Transaction`.
- Seed при `onCreate` БД: 2 энва и 4 биндинга (AC-12.1).

**DataStore Preferences (`settings.preferences_pb`)** — таблицы Settings в Room сознательно нет: нужны атомарное чтение на старте и простота.

| Ключ | Тип | По умолчанию |
|---|---|---|
| `current_env_id` | Long | id «Обычный» |
| `icon_mode` | String NONE/LEFT/RIGHT | NONE |
| `clock_format` | String SYSTEM/H24/H12 | SYSTEM |
| `show_date` | Boolean | true |
| `home_text_size` | String S/M/L | M |
| `kb_layouts` | String "en,ru" (порядок) | "en,ru" |
| `kb_active_layout` | String | "en" |
| `kb_haptics` | Boolean | true |
| `kb_digit_hints` | Boolean | true |
| `search_fuzzy` / `search_layout_swap` / `search_translit` | Boolean | true |
| `onboarding_done` | Boolean | false |

**Миграции.** `exportSchema = true`, схемы лежат в `core/data/schemas/` под VCS. Каждое изменение схемы — явная `Migration(n, n+1)` и тест `MigrationTestHelper`. `fallbackToDestructiveMigration` запрещён.

## D.5 AndroidManifest

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.EXPAND_STATUS_BAR"/>
  <uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES"/>
  <uses-permission android:name="android.permission.ACCESS_HIDDEN_PROFILES"/> <!-- v1.1, Private Space -->
  <queries>
    <intent>
      <action android:name="android.intent.action.MAIN"/>
      <category android:name="android.intent.category.LAUNCHER"/>
    </intent>
  </queries>
  <application android:name=".EnvApp" android:label="@string/app_name"
      android:theme="@style/Theme.Env.Starting" android:allowBackup="false"
      android:supportsRtl="false">
    <activity android:name=".MainActivity"
        android:exported="true"
        android:launchMode="singleTask"
        android:clearTaskOnLaunch="true"
        android:stateNotNeeded="true"
        android:excludeFromRecents="true"
        android:resumeWhilePausing="true"
        android:taskAffinity=""
        android:screenOrientation="portrait"
        android:windowSoftInputMode="stateAlwaysHidden|adjustNothing"
        android:configChanges="keyboard|keyboardHidden|navigation|touchscreen|orientation|screenSize|screenLayout|smallestScreenSize|uiMode|density|fontScale|locale|layoutDirection|colorMode">
      <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.HOME"/>
        <category android:name="android.intent.category.DEFAULT"/>
      </intent-filter>
    </activity>
  </application>
</manifest>
```

- Категорию `LAUNCHER` у Activity не ставим: лаунчер не должен появляться в собственном списке. При необходимости его можно открыть из системных настроек.
- `Theme.Env.Starting`: `android:windowBackground = #0E0E0E` (фон Graphite, без белой вспышки), `android:windowShowWallpaper = false`, `android:windowSplashScreenBackground = #0E0E0E`, `windowSplashScreenAnimatedIcon` — прозрачный drawable.
- Изменения шрифта и локали обрабатываются без пересоздания: `onConfigurationChanged` → `LocalConfiguration` перекомпонуется сам.

## D.6 Работа с LauncherApps и профилями

- `AppCatalog`:
  - при старте — `launcherApps.profiles` → для каждого `getActivityList(null, user)` → `AppEntry(key, label, firstInstallTime, isSystem, userType)`;
  - `registerCallback(callback, Handler(mainLooper))`;
  - `registerReceiver` для `ACTION_MANAGED_PROFILE_AVAILABLE/UNAVAILABLE`, `ACTION_PROFILE_AVAILABLE/UNAVAILABLE` (API 35);
  - метки грузятся на `Dispatchers.Default`, запись — атомарная замена `MutableStateFlow`.
- Тип профиля (API 35+): `launcherApps.getLauncherUserInfo(user)?.userType` (`"android.os.usertype.profile.MANAGED"` / `".PRIVATE"`). Вызов в try/catch: без роли HOME метод может бросить исключение.
- Запуск — `launcherApps.startMainActivity(component, user, sourceBounds = null, opts = null)`. «Об приложении» — `startAppDetailsActivity`. Удаление — `Intent(Intent.ACTION_DELETE, "package:$pkg".toUri()).putExtra(Intent.EXTRA_USER, user)`.
- Работа на паузе — `userManager.requestQuietModeEnabled(false, user)`. Вызывающий должен быть foreground default launcher.
- Private Space (v1.1): секция «Личное пространство» внизу результатов поиска. В popup — пункт «Заблокировать» / «Разблокировать» (`requestQuietModeEnabled(true/false, privateUser)`). Если пространство заблокировано, его элементы в избранном не рендерятся.

## D.7 Конвейер иконок

```
IconKey = (component, userSerial, lastUpdateTime, sizePx, mode)   // tint НЕ входит в ключ
load(key):
  mem = LruCache<IconKey, ImageBitmap>(maxSize = 8 MB, sizeOf = byteCount)
  if mem.hit → return
  disk = cacheDir/icons/<sha1(key)>.webp (WEBP_LOSSLESS)
  if disk.hit → decode (inPreferredConfig=ARGB_8888) → mem.put → return
  d = launcherActivityInfo.getIcon(densityDpi)            // Dispatchers.IO
  bmp = if (d is AdaptiveIconDrawable && d.monochrome != null && mode == AUTO)
          renderLayer(d.monochrome, sizePx, viewport = 72/108)   // берём альфу, RGB → белый
        else
          renderFull(d, sizePx) → desaturate(ColorMatrix.setSaturation(0))
                                → normalizeLuminance (линейно растянуть L в [0.15, 1.0])
                                → сохранить как «яркость в альфе»: A' = A × L, RGB = белый
  if coverage(bmp) > 0.92 → bmp = null (→ буквенный fallback, AC-04 Edge)
  disk.write(bmp); mem.put(bmp)
draw: Image(bitmap, colorFilter = ColorFilter.tint(palette.iconTint, BlendMode.SrcIn))
```

- Размер: `sizePx = round(28dp × density)`, для recents — 20dp. Для Pixel 6 это 74 px и 53 px; кэшируется под ближайший больший кратный 8 размер.
- Viewport adaptive: слой 108dp, видимая часть — центральные 72dp. Слой рисуется в bounds, расширенные на 25 % с каждой стороны.
- Прогрев:
  - после загрузки каталога иконки избранного **текущего** энва грузятся с приоритетом (до первого кадра TTFD);
  - затем иконки остальных энвов и recents — в фоне, с `Dispatchers.IO.limitedParallelism(2)`;
  - остальные приложения — лениво при показе в поиске.
- Инвалидация: `onPackageChanged`/`onPackageAdded` → удалить ключи пакета (в памяти и на диске). Смена палитры кэш не трогает.
- `onTrimMemory(level ≥ TRIM_MEMORY_BACKGROUND)` → `mem.evictAll()`.

## D.8 Встроенная клавиатура

- Композиция: одна `Canvas`-подобная раскладка. Клавиши — `Box` без собственных `pointerInput`; **один** `pointerInput` на весь контейнер клавиатуры. Hit-test по прямоугольникам, рассчитанным в `onSizeChanged`. Так на клавишу нет отдельной обработки, работает rollover и ничего не теряется на границах.
- Обработка указателя (`awaitEachGesture` с поддержкой нескольких указателей):
  - DOWN → `pressedKey[id] = hit(pos)`, haptic `KEYBOARD_TAP` (`LocalView.current.performHapticFeedback`, без разрешения VIBRATE), запуск таймера 300 мс для цифры (верхний ряд) или 400 мс для backspace-repeat;
  - MOVE: dy ≤ −24dp на клавише верхнего ряда → режим «цифра»; dx ≤ −48dp на backspace → «очистить»;
  - UP → commit (буква, цифра или действие);
  - новый DOWN другого пальца при активном первом → commit первого (rollover).
- Повтор backspace: `while(pressed) { delay(50); deleteLast() }` после стартовой задержки 400 мс, с haptic на каждом повторе. Корутина привязана к жесту.
- Состояние: `KeyboardState(layoutId, pressedKeys, popupKey)` — `@Stable`-класс с `mutableStateOf`. Перерисовывается только нажатая клавиша: у каждой клавиши свой `derivedStateOf { pressed == key }`.
- Ряды (ширина ряда = 100 %, клавиши равной ширины внутри ряда):
  - EN: `q w e r t y u i o p` / `a s d f g h j k l` (отступ 5 % по краям) / `[⇄EN→RU] z x c v b n m [⌫]` / `[⏎ Открыть]` (на всю ширину).
  - RU: `й ц у к е н г ш щ з х ъ` / `ф ы в а п р о л д ж э` / `[⇄] я ч с м и т ь б ю [⌫]` / `[⏎ Открыть]`.
  - Ширина клавиши EN верхнего ряда ≈ 41dp, RU ≈ 34dp. Высота 52dp обеспечивает ≥ 48dp по вертикали; горизонтальная ширина меньше 48dp — стандартный компромисс клавиатур, это принимается.
- Аппаратная клавиатура: `override fun dispatchKeyEvent(e)` в Activity → `KeyboardRouter.onKey(e)`. Если событие поглощено — возвращается true.

## D.9 Распознавание жестов на Home (без конфликтов)

Один корневой `Modifier.pointerInput(Unit)` на весь Home с собственным детектором `homeGestures(...)`. У строк своих `pointerInput` **нет**: hit-test строки делается по `LayoutCoordinates`, которые строки регистрируют через `onGloballyPositioned` в `RowRegistry`.

```kotlin
awaitEachGesture {
  val down = awaitFirstDown(requireUnconsumed = false)
  val row = rowRegistry.hit(down.position)              // null = фон
  val now = down.uptimeMillis
  // 1) Двойной тап — только на фоне, распознаётся на ВТОРОМ DOWN
  if (row == null && lastBgTapUp != null &&
      now - lastBgTapUp.time <= vc.doubleTapTimeoutMillis &&
      (down.position - lastBgTapUp.pos).getDistance() <= 100.dp.toPx()) {
      lastBgTapUp = null; onDoubleTap(); consumeUntilUp(); return@awaitEachGesture
  }
  // 2) Ожидание: long-press / slop / up
  val result = withTimeoutOrNull(vc.longPressTimeoutMillis) { awaitSlopOrUp(down) }
  when {
    result == null -> { haptic(LONG_PRESS); onLongPress(down.position, row); consumeUntilUp() }
    result is Up && row != null -> onRowTap(row)                // мгновенно
    result is Up && row == null -> lastBgTapUp = TapInfo(result.time, result.pos)
    result is SlopExceeded -> trackSwipe(down, startZoneOk(down.position))  // C.4
  }
}
```

- `vc = viewConfiguration` из `PointerInputScope`. Таймауты не хардкодятся.
- Мультитач: появление второго указателя до решения → жест отменяется.
- Тестируемость: детектор — чистая функция над потоком событий. Логика решения вынесена в `GestureArbiter` (JVM-класс), который покрыт unit-тестами с синтетическими событиями.

## D.10 Позиционирование popup

`Popup(popupPositionProvider = AnchoredBelowPoint(anchor, safeInsets), properties = PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true))`.

Алгоритм в `calculatePosition(anchorBounds, windowSize, layoutDirection, popupContentSize)`:

```
m = 8.dp; gap = 8.dp
safe = Rect(insets.left+m, insets.top+m, W-insets.right-m, H-insets.bottom-m)
x = clamp(anchor.x - popupW/2, safe.left, safe.right - popupW)
yBelow = anchor.y + gap
y = if (yBelow + popupH <= safe.bottom) yBelow
    else if (anchor.y - gap - popupH >= safe.top) anchor.y - gap - popupH   // над пальцем
    else safe.bottom - popupH                                               // прижать к низу
```

- Ширина popup фиксирована, 240dp. Максимальная высота — 60 % высоты safe-области; дальше внутренний скролл.
- Фон `surface`, рамка 1dp `divider`, скругление 12dp, без тени и без анимации.
- Пункты по 48dp. Divider 1dp с отступами 0dp по краям.
- Тест: unit-тест провайдера для 9 якорных точек (углы, края, центр).

## D.11 Алгоритм поиска (модуль `:core:search`, чистый Kotlin)

**Индекс** (строится при изменении каталога или overrides; ≤ 30 мс на 500 приложений, в фоне). Для каждого компонента:

```
names = [customLabel?, originalLabel]                    // оба индексируются
for name in names:
  n   = normalize(name)              // lowercase(Locale.ROOT), NFKD, удалить диакритику, ё→е,
                                     // удалить всё кроме букв/цифр; слова разделены по пробелам/дефисам/CamelCase
  words = splitWords(name)           // "Google Maps"→[google,maps]; "WhatsApp"→[whats,app]; "СберБанк"→[сбер,банк]
  lat = translitToLatin(n)           // кириллица→латиница, латиница без изменений
  key = phonetic(lat)                // см. ниже
  initials = words.map{ it.first() } joined (+ латинизированные инициалы)
  store IndexEntry(component, nameKind, n, lat, key, words(lat), initials)
```

**Варианты запроса.** Из запроса `q` строятся:

1. `q0 = normalize(q)`
2. `q1 = layoutSwap(q0)` — RU↔EN по физическим клавишам (таблица ниже). Применяется, если `q0` целиком из одной раскладки.
3. `qLat = translitToLatin(q0)`, `qKey = phonetic(qLat)`
4. `q1Lat`, `q1Key` — то же для `q1`.

**Таблица раскладки** (только буквы; позиции символов EN, которых нет на нашей EN-клавиатуре, используются только в направлении RU→EN и дают символы, которые не совпадут ни с чем):

| EN | q | w | e | r | t | y | u | i | o | p | [ | ] | a | s | d | f | g | h | j | k | l | ; | ' | z | x | c | v | b | n | m | , | . |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| RU | й | ц | у | к | е | н | г | ш | щ | з | х | ъ | ф | ы | в | а | п | р | о | л | д | ж | э | я | ч | с | м | и | т | ь | б | ю |

Проверка: «ыдфсл» → s,l,a,c,k → «slack» ✓; «ckfr» → с,л,а,к → «слак» ✓.

**Транслит RU→LAT** (основной вариант; для неоднозначных букв генерируются альтернативы, максимум 4 варианта на запрос):

а→a, б→b, в→v, г→g, д→d, е→e, ж→zh, з→z, и→i, й→y|i|j, к→k, л→l, м→m, н→n, о→o, п→p, р→r, с→s, т→t, у→u, ф→f, х→h|kh, ц→ts|c, ч→ch, ш→sh, щ→sch|shch, ъ→∅, ы→y, ь→∅, э→e, ю→yu|ju, я→ya|ja.

**Фонетический ключ** `phonetic(lat)` применяется к обеим сторонам по порядку:

- `ck→k`, `c→k`, `q→k`, `ph→f`, `w→v`, `x→ks`, `y→i`, `j→i`, `kh→h`;
- `ts→c` (после предыдущих замен — `ts→s`);
- схлопывание повторов (`ll→l`);
- гласные остаются.

Примеры: «slack» → `slak`, «слак» → `slak` → полное совпадение ключей ✓; «Telegram» → `telegram`, «телеграм» → `telegram` ✓; «sber» vs «СберБанк» → `sberbank` → префикс ✓.

**Оценка** (максимум по всем вариантам запроса × все имена компонента):

| Уровень | Условие | Базовый балл |
|---|---|---|
| EXACT | `n == q` или `key == qKey` | 1000 |
| PREFIX | `n.startsWith(q)` или `lat.startsWith(qLat)` | 900 |
| WORD_PREFIX | какое-либо слово начинается с q (в n или lat) | 800 |
| INITIALS | `initials.startsWith(q)` при \|q\| ≥ 2 | 700 |
| SUBSTRING | `n.contains(q)` или `lat.contains(qLat)` при \|q\| ≥ 2 | 500 |
| PHONETIC_PREFIX | `key.startsWith(qKey)` | 450 |
| SUBSEQUENCE | буквы q идут по порядку в lat при \|q\| ≥ 3, первая совпадает с первой буквой имени («tlgrm») | 350 |
| FUZZY | OSA-Дамерау–Левенштейн между qKey и префиксом key длины \|qKey\| (±1): допустимо d ≤ 1 при \|q\| 3–5, d ≤ 2 при \|q\| ≥ 6 | 300 − 60·d |

Модификаторы:

- −40, если совпадение найдено через `layoutSwap`; −20 — через транслит;
- −10, если совпало `originalLabel`, хотя есть `customLabel`;
- +min(120, 40·ln(1 + decayed_score)), где `decayed_score = Σ exp(−Δt/14 суток)` по запускам; хранится инкрементально: `score = score·exp(−Δt/τ) + 1`;
- +60, если последний запуск был не более 1 часа назад;
- скрытое приложение допускается только при уровне ≥ PREFIX (AC-09.4).

Тай-брейк: балл ↓, длина имени ↑, алфавит. Результат — не более 50 элементов.

**Пустой запрос** → алфавитный список видимых приложений (сортировка `Collator.getInstance(ru)`, сначала кириллица, затем латиница). **Enter** → `results.firstOrNull()`.

**Обязательные тест-кейсы (`SearchEngineTest`)**:

| Запрос | Набор | Ожидание |
|---|---|---|
| sl | Slack, Slides, Signal | Slack или Slides в топ-2, Slack с usage — №1 |
| ыдфсл | Slack | №1 |
| ckfr | Slack | топ-3 |
| слак | Slack | топ-3 |
| телеграм | Telegram | №1 |
| tlgrm | Telegram | топ-3 |
| gm | Google Maps, Gmail | оба в топ-3 |
| sber | СберБанк | №1 |
| поч | Gmail (rename «Почта») | №1 |
| slakc | Slack | топ-3 (fuzzy d=1) |
| "" | 10 приложений | алфавит |
| xyzq | 10 приложений | пусто |

## D.12 Раскладки — данные

`KeyboardLayout(id: String, rows: List<List<KeySpec>>)`, `KeySpec(char: Char?, digit: Char?, action: KeyAction?, weight: Float)`. Раскладки определены в `:core:search` как константы, чтобы та же таблица использовалась для `layoutSwap`. Добавление новой раскладки = новая константа + строка в настройках.

## D.13 Шторка уведомлений

```kotlin
class ReflectionNotificationShade(private val ctx: Context) : NotificationShade {
  override fun expand(): Boolean = runCatching {
    val sbm = ctx.getSystemService("statusbar")
    sbm.javaClass.getMethod("expandNotificationsPanel").invoke(sbm); true
  }.getOrElse { Log.w(TAG, "shade", it); false }
}
```

Вызывается только когда Activity в RESUMED.

## D.14 Производительность: цели и измерение

| Метрика | Цель (Pixel 6, release + Baseline Profile) | Инструмент |
|---|---|---|
| Cold start TTID | медиана ≤ 350 мс, P95 ≤ 500 мс | Macrobenchmark `StartupTimingMetric`, COLD, 10 итераций |
| TTFD (`reportFullyDrawn` после избранного + иконок текущего энва) | медиана ≤ 600 мс | то же |
| Открытие поиска после второго DOWN | ≤ 1 кадр | Perfetto: `Trace.beginSection("search_open")`; Macrobenchmark `TraceSectionMetric("search_open")` |
| Возврат на Home из приложения | 0 кадров с `frameOverrunMs > 0` у лаунчера в P95 | `FrameTimingMetric`, сценарий: запустить приложение → `pressHome()` |
| Ввод символа → новые результаты | ≤ 1 кадр; поиск ≤ 4 мс P95 | microbenchmark `:core:search:bench` + `FrameTimingMetric` при наборе 10 символов |
| Jank в поиске при скролле «все приложения» | P95 `frameDurationCpuMs` ≤ 8 мс | `FrameTimingMetric` + JankStats в debug |
| Память | PSS ≤ 60 МБ после 10 минут | `dumpsys meminfo`, `am memory-limiter status` |
| APK | ≤ 4 МБ (R8, shrinkResources) | CI |

Техники:

- Baseline Profile (`androidx.baselineprofile`), сценарии генерации: cold start → double tap → набор «sl» → Back → long-press → закрыть;
- `profileinstaller`;
- R8 full mode;
- слой поиска всегда скомпонован (R4);
- `reportFullyDrawn()` через `ReportDrawnWhen { iconsReady && itemsReady }`;
- нет работы в `Application.onCreate` кроме создания `AppContainer` и запуска загрузки каталога на `Default`;
- первый кадр Home рендерится из последнего снимка `HomeSnapshot` (имена и порядок избранного текущего энва + палитра), который хранится в отдельном маленьком файле DataStore. Room и LauncherApps догружают данные следом.

## D.15 Тестирование и CI

- **Unit (JVM).** Тесты `:core:search` (D.11), `GestureArbiter` (двойной тап, long-press, пороги свайпов, стартовые зоны), `AnchoredBelowPoint` (9 точек), контраст палитр, инварианты энвов.
- **Robolectric.** DAO и миграции (`MigrationTestHelper`), DataStore, `AppCatalog` с фейковым `LauncherApps` (обёртка-интерфейс `LauncherAppsGateway`).
- **Compose UI (emulator API 37).**
  - Home: зебра, высоты строк ≥ 48dp (`assertHeightIsAtLeast(48.dp)`), отсутствие колокольчика и Work-области;
  - double tap → `onNodeWithTag("keyboard").assertIsDisplayed()` и **отсутствие** IME: `WindowInsets.isImeVisible == false`;
  - long-press → popup внутри safe-area;
  - Back и Home.
- **Screenshot-тесты** (Compose Preview Screenshot Testing или Roborazzi) для 4 пресетов палитр.
- **Macrobenchmark** — на физическом Pixel 6, локально перед релизом. Результаты коммитятся в `perf/results/*.json`.
- **CI (GitHub Actions, ubuntu-latest, JDK 21).** На каждый PR: `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleRelease`. Instrumented-тесты — nightly на эмуляторе API 37 (`reactivecircus/android-emulator-runner`). Гейт: размер APK и отсутствие новых lint-ошибок.

## D.16 Что изменится при публикации в Google Play

- Целевой API. Справка Google Play Console «Target API level requirements»: «Starting August 31, 2026: New apps and app updates must target Android 16 (API level 36) or higher... Developers will be able to request an extension to November 1, 2026». Требование API 37, по вторичным источникам, ожидается с августа 2027. С targetSdk 37 мы уже в зоне соответствия.
- `QUERY_ALL_PACKAGES` не нужен: хватает `<queries>` с MAIN/LAUNCHER и `LauncherApps`. Если бы он понадобился, потребовалась бы декларация в Play Console. Политика разрешает его при «core purpose to search for all apps» (device search).
- Скрытый API шторки — риск отказа и поломки. Accessibility-fallback (F25) подпадает под строгую политику Accessibility API, поэтому не планируется.
- `REQUEST_DELETE_PACKAGES` — допустим для лаунчера, но декларируется.
- Нужны Data safety form («данные не собираются») и privacy policy (простая страница).
- Лицензия: при любом заимствовании GPL-кода публикуемый APK обязан сопровождаться исходниками под GPL-3.0. При clean-room подходе лицензия выбирается свободно.
- Нужны `minSdk` ниже 33 (например, 29) и fallback для `getMonochrome` / `getLauncherUserInfo` с проверками `Build.VERSION.SDK_INT`. Код с самого начала пишется с `@RequiresApi`-гардами.
