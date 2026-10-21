# Changelog

## 0.4.0 — 2026-10-21

### Added
- Home edit mode (ADR-0028). The pencil at the bottom of the home screen now switches edit mode on and off (it turns into a check mark).
  - In edit mode the clock shows its three positions and can be dragged right away, and the line above the list appears and moves the list.
  - Three buttons appear at the bottom left:
    - **Background** opens the photo picker or, if the space already has an image, the background editor;
    - **Text** opens a sheet with font, weight, text size and the new text alignment (left, centre, right);
    - **Theme** opens a sheet with the palette, “Icons” and “Color icons”.
  - The sheets look like the drawing sheet in Memory and dim the home screen only slightly, so changes are visible behind them.
  - In edit mode apps do not start from the list and home gestures are off. The check mark, Back or Home leave edit mode.
- Search: the top of the result list fades out when more results are above it.
- Clock settings on the home screen (ADR-0029): in edit mode a pencil button next to the clock opens a “Clock” sheet with the clock format and “Show date”.

### Changed
- Outside edit mode the clock and the list stay in place. Holding the clock opens the home menu, and the line above the list is hidden; its space is kept.
- App icons on the home screen and in search always sit at the right end of the row; the left/right choice is gone, and icons saved on the left move to the right. Labels follow the new text alignment within the rest of the row.
- Background editor (ADR-0028):
  - “Cancel” and “Other image” are in a row above the preview.
  - The three sliders take one line each, with the name next to the slider.
  - “Remove background” (left) and “Save” (right) are at the bottom. Removing closes the editor.
  - The mock home screen follows the text alignment.
- The background of a space is no longer in the space settings; it is set from the home edit mode for the current space.
- Settings → Style is gone (ADR-0029). The clock format and date are in the clock sheet, “Notification count” is in the Theme sheet, and the drawing line (thickness, colour, dots) is in Settings → Gestures.
- Every icon button (pencil, “?”, the edit buttons, Back in settings, “+” next to the spaces, search options) and the clock now show the press feedback chosen in Settings → System → Advanced → Press animation.
- The gap between the list handle and the first home row is three times larger (12 dp instead of 4 dp); the handle stays in place and the list starts 8 dp lower.
- The tips “Set up your first space” and “Customize the launcher” (also onboarding slides 1 and 2) show the new home edit mode: the pencil turns into a check, the palette and text buttons open their sheets, “Add apps” opens the app picker, and the clock and the list handle are dragged directly. Their texts are updated in all seven languages.

## 0.3.1 — 2026-10-18

### Added
- Background editor: a third slider, “Row background” (ADR-0026), after “Brightness” and “Blur”, sets how opaque the home rows (including “Add apps”) are over the space's image, from 20 % to 100 % in 5 % steps; the default 90 % keeps the previous look. The mock home screen shows it live, “Save” stores it with the other values, “Cancel” discards it, and “Remove background” resets it to 90 %. Spaces without an image keep opaque rows. Database v10 adds `environment.bg_row_alpha` (default 0.9) by an automatic migration; existing spaces keep 90 %.

### Fixed
- Memory → “Erase launch history” (was “Clear usage”) also erases the recent apps of every space; since 0.3.0 removed “Clear space history” from search, recents could only be removed by clearing the app's storage. The confirmation lists what is erased.
- If the system has neither the per-app nor the general notification access screen, the notification-count switch shows a notice instead of crashing.
- The home snapshot ignores a file with an implausible row count instead of allocating for it.
- Settings dropdowns remove the dim and blur flags from their window when they close.

### Security
- Incremental security and code-quality audit of v0.1.0..0.3.0 with an independent review by Muse: `docs/release/security-audit-2026-10-18.md`. No critical or high findings.
- The privacy policy (effective 2026-10-18) and the in-app privacy text list everything stored on the device: launch history now covers launch days per space, recents and the last hint time and drives search, recents and hints; background images, the home snapshot and the process exit log are named.

## 0.3.0 — 2026-10-18

### Added
- Interface languages (ADR-0023): English is now the base language and Russian a translation; the app declares English, Russian, Spanish, Portuguese (Brazil), German, French and Indonesian through the Android 13 per-app language setting (`locales_config.xml`). Settings → System → Language (second item) offers “System default” and the seven languages, each named in itself; the choice applies at once, without a restart, and is also available in the system app-language settings. All texts are translated into Spanish, Portuguese (Brazil), German, French and Indonesian with one terminology per language (machine translation by the agent; review by native speakers is pending, Q-135 in ADR-0023).
- Keyboard layouts ES (QWERTY with ñ), PT (QWERTY with ç), DE (QWERTZ with ü, ö, ä), FR (AZERTY) and ID (QWERTY) next to EN and RU (ADR-0023). EN and RU keys keep their exact places, so learned drawings are unaffected. Swipe typing on a Latin layout follows that layout's own keys; on RU it still uses the QWERTY positions, and drawings on the home screen keep the QWERTY geometry.
- Search ignores diacritics: plain letters find “España”, “Ação”, “Über” or “Café”, letters such as ñ, ç and ü typed on the new layouts find names with or without them, and ß, æ, œ, ø and ł are spelled out (ss, ae, oe, o, l). Wrong-layout correction and transliteration still work between EN and RU only.
- Each space has its own background (ADR-0020). Space settings → Background → “Change background” opens the system photo picker when the space has no image yet, then a full-screen editor: the image in a frame with the proportions of the screen, under a mock home screen of the space (clock, date, list handle and five rows in its palette, font and text size). Drag to move the image, pinch to zoom (it always fills the screen), double-tap to reset. “Brightness” (darker to the left, lighter to the right) and “Blur” apply live. “Other image” picks a new photo, “Save” keeps the visible part at screen resolution, “Cancel” changes nothing. The space settings show a small preview, and “Remove background” deletes the image and resets both sliders. Deleting a space deletes its image; a new space starts without one.

### Changed
- Palette choice is a row of round swatches (background colour with an accent dot, a ring around the chosen one) instead of a dropdown, in Style and in the space settings.
- In the background editor the mock app list sits in the vertical middle of the preview.
- Search settings panel: “Clear space history” is replaced with “All settings”, which closes search and opens the settings.
- Settings → Home screen is now a section of Style; the separate page is gone.
- Icon-button outlines are 2 dp instead of 1 dp, and the search settings button sits further from the result list.
- The “?” hint to put a frequently used app on the home screen is based on a week of data (ADR-0025): a space shows its first hint no earlier than 7 days after it was created, the suggested app must have been launched in that space on at least 3 different days within the last 7 days (in addition to the 3 launches and the 10-point margin), and after a hint is accepted or dismissed the next one in that space waits 7 days. A dismissed app is still not suggested for 14 days. Database v9 adds the `space_launch_day` table (launch days per space and app, kept for 14 days, cleared with usage counters) and `environment.hint_at` by an automatic migration; existing data is kept.
- Spaces are created and deleted from the top of Settings (ADR-0024). A “+” button to the right of the space chips opens the new-space dialog (name, and the type when both a custom and a work space can still be created). A long press on a space chip opens a panel with “Delete space”, which asks for confirmation in a system dialog (“Delete” / “Cancel”). The normal space cannot be deleted: a long press on it only gives a short rejection vibration.
- Swipe typing settings (“Shift penalty”, “Suggestion margin”, “Restore defaults”) moved from Gestures to Settings → Keyboard → Swipe typing; values are kept (ADR-0024).
- “System” is now the last item of the settings root, after “Tips” (ADR-0024).
- Tips and onboarding animations play 1.5 times slower; slide transitions are unchanged.
- Settings → Keyboard → Layouts is a list of the enabled layouts in switching order, each with its native name, its two-letter code and “Remove” (unavailable for the last one), followed by “Add more” with the layouts not yet in the list; a new one goes to the end. The per-layout switches and the move-up buttons are gone. New installs start with the layout of the phone language followed by EN (only EN for English or an unsupported language), and the first one is active; a stored list such as EN, RU is kept (ADR-0023).
- With icons shown, the “Add apps” row has a “+” icon in the icon place and no “+” in its text; without icons the text starts with “+”.
- New installs show app icons on the left and in colour by default (Home screen → Icons: Left; Style → Color icons: on for the built-in and new spaces). Existing settings are kept (ADR-0022).
- Every icon button (edit, help, settings, back) sits on a circle with an outline, and the list drag handle on a small pill, so they keep their contrast over any background image (ADR-0021).
- Home rows over a background image all have a background now: both zebra colours of the palette are drawn at 90% opacity, so the image only shows through faintly and labels always read. This includes the “+ Add apps” row. Without an image the rows look as before (ADR-0020).
- On home the background image of the current space is blurred (up to 24 dp, the image only) and then darkened with black up to 70% or lightened with white up to 50%. Switching spaces switches the image; only the current space's image is kept in memory (ADR-0020).
- Updating from an earlier version moves the single background into every existing space on first start. The old “Background visibility” o becomes the brightness of each space: −(1 − o) on dark palettes, +(1 − o) on light ones; blur starts at 0. Database v8 adds `environment.bg_brightness` and `environment.bg_blur` (default 0) by an automatic migration.
- The “+” button on the home screen is now a list row, “+ Add apps”, with the same height, font, text size and press feedback as app rows; with icons on, its label lines up with app names. It is shown when it fits under the list; in an empty space it is the only row, and the separate “Tap + to add apps” hint is gone (ADR-0018).
- The home list starts lower by default: the gap between the clock and the list is three times larger, but never so large that fewer than four rows fit. A position you set by dragging the handle, including the very top, is kept as before (ADR-0018).
- Updating from 0.2.0 or earlier also gets the new default: the old builds saved an untouched global list offset of 0, which a one-time settings migration clears. Positions set per space by dragging are kept.
- The delete key moved from the bottom letter row of the search keyboard to a compact button at the right edge of the query row. It works as before: “×” after a drawing clears it, “⌫” while typing deletes one character, holding repeats with the system key-repeat timings, and swiping it left clears the query. It is hidden when there is nothing to delete. The letter keys stay exactly where they were, so drawings and learned shapes are unaffected; the bottom row keeps an empty gap where the key used to be (ADR-0019).
- Search recents moved into the main result list: the right-hand recents pane is gone and results use the full width. With an empty query the list starts with up to six recently launched apps of the current space, then all other apps alphabetically, separated by a thin line. Hidden apps are left out unless “Show hidden” is on. Ranking for a typed query is unchanged, and “Clear space history” removes the recent block (ADR-0019).
- Popups stand out from the screen under them (ADR-0019): menus opened by a long press on home and in search, the “?” suggestion panel, search options and settings dropdowns have a shadow and a thin contrasting outline, and the screen behind them is dimmed (40% black on dark palettes, 25% on light ones). On home and in search the content under the popup is also blurred. Tapping the dimmed area closes the popup as before.

### Removed
- Style → Background and the global “Background visibility” slider: the background is chosen and tuned per space (ADR-0020).
- Settings → Spaces: creating and deleting spaces moved to the space chips at the top of Settings, and the rest of a space's settings is opened with ✎ on the home screen (ADR-0024).
- Gestures → Search over apps: Mushin no longer links to the default-assistant settings or offers to add the Quick Settings tile. Both still work when turned on in the system settings (ADR-0024).

### Fixed
- The system back gesture from the screen edge closes settings again: the hidden search keyboard kept its area excluded from system gestures after search was closed; the exclusion now applies only while search is open.

## 0.2.0 — 2026-10-16

### Added
- A “+” button on the home screen under the last row (centred when the space is empty) opens the app picker of the current space (ADR-0017).
- First-run onboarding (ADR-0017): four slides over the home screen with dot indicators, swipes, Skip and Next/Done. Short looping animations drawn in Compose show how to set up a space, customize the launcher and teach it a drawing. It is shown once; Back goes to the previous slide.
- Settings → Tips: the onboarding animations, each on its own screen with a title and a short explanation. With animations turned off in system settings the last frame is shown still.

### Changed
- Home rows show the press feedback style chosen in settings while the finger is on them, like other buttons (ADR-0017).

### Removed
- The “Bound to a system gesture” mark: the search menu item and the mark on home and in search are gone, and such apps can be suggested again. Database v7 drops the `app_override.system_shortcut` column; other app overrides are kept (ADR-0017).

### Fixed
- Nightly UI tests in CI run on an API 36 emulator: GitHub runners have no API 37 image (ADR-0016).

## 0.1.1 — 2026-10-16

From this version on the changelog is kept in English.

### Changed
- New application ID `com.mushinlab.launcher` (was `com.bluecubelabs.env`). Android treats it as a new app: 0.1.0 has to be removed by hand, its spaces and settings are not carried over.
- Contact address `mushin-lab@proton.me` in the privacy policy and in Settings → System → Privacy.

### Code quality (ADR-0015)
- Kotlin packages renamed to `com.mushinlab.launcher`; spaces are `Space` in code, app-wide names use the `Mushin` prefix. Database tables, columns and settings keys are unchanged, so stored data stays as it is.
- UI state moved out of `MainActivity` into the ViewModel and plain state holders; platform actions are requested with events. The settings screen stack survives process death.
- Icon loading split into a renderer and a disk cache; the disk cache ignores files whose size does not match.
- detekt runs with standard thresholds; new unit tests for the ViewModel mappers, navigation, home and search interactions, app launching, icons and storage codecs.
- KDoc on the public API of the core modules and Compose previews for the base components in `core:ui` (preview tooling is debug-only).
- `CONTRIBUTING.md` added.

## 0.1.0 — 2026-10-15

### Аудит безопасности перед публикацией на GitHub
- Release без `keystore.properties` собирается неподписанным; debug-подпись — только с `-Penv.debugSignedRelease=true`.
- Actions в CI закреплены по SHA.
- Убрана ссылка на внешний журнал решений; `security-review.md` обновлён. Отчёт — `docs/release/security-audit-2026-10-15.md`.
- README на английском и лицензия Apache-2.0 (Q14).

### Свой фон (ADR-0013)
- «Стиль → Фон»: картинка из галереи (PNG, JPEG, WebP, AVIF) под списком главного экрана, ползунок «Видимость фона», «Убрать фон». Копия хранится только внутри лаунчера.

### Правки владельца, раунд 10 (ADR-0012)
- Значок-глаз в шапке поиска убран: «Показывать скрытые» — пункт с галочкой в панели под шестерёнкой.
- Положение списка на главном экране хранится у каждого спейса отдельно (БД v6).
- Пять новых тем: Lagoon, Plum, Ember (тёмные), Mist, Rose (светлые) — всего 11.

### Правки владельца, раунд 9 (ADR-0011)
- Метка «Назначено на системный жест» (меню поиска): значок рядом с названием, такие приложения не предлагаются в «?».
- Клавиша удаления: «×» после рисунка (стирает всё), «⌫» при наборе. Enter убран. Штрих вниз от клавиши начинает рисунок, а не закрывает поиск.
- Якоря размера клавиатуры с вибрацией и подсветкой, пунктир доступной области.
- Поиск поверх приложений: Mushin как цифровой помощник и плитка в быстрых настройках.
- Рисунки — векторы с временем удержания в вершинах (БД v5); показаны на клавиатуре, в шторке и в «Памяти»; в шторке — любая фигура.
- Пассивное обучение запоминает только рисунки, похожие на название приложения; «GeForce», «GitHub» и другие слова с заглавными распознаются целиком.
- «Proceed to learning» открывает шторку с усреднённым рисунком из пяти попыток.
- «Стиль → Линия → Точки на линии»; пузырёк цифры не обрезается; равные отступы у значков вариантов.

### Правки владельца, раунд 8 (ADR-0010)
- Полоска над списком главного экрана движется вместе со списком и отмечает его начало.
- «Память → Добавить жест…»: выбор приложения, кнопка «Добавить», шторка на 70 % экрана с клавиатурой для одного рисунка и кнопками «Добавить» / «Повторить» / «Отмена»; шторка закрывается движением вниз.
- Рисунок на главном экране ищет только по «Памяти» и по форме: место на экране не важно, масштаб от 1:2 до 2:1.
- После 5 ненайденных рисунков подряд — плашка «What did you mean? / Proceed to learning» сверху; тап ведёт к обучению, смахивается вбок.
- Клавиатура: кнопка размера слева от пробела с якорями (ширина, сдвиг) — меняется вся панель ввода; enter — клавиша справа от пробела; пробел в 2 раза ниже; клавиатура поднята ещё на высоту пробела; «⌫» — крупный «×». Ползунки ширины в настройках убраны.
- Строка запроса без счётчика и «×», больший верхний отступ панели ввода.
- «Стиль → Линия»: толщина и цвет (12 вариантов) для следа и линий рисунка.

### Правки владельца, раунд 7 (ADR-0009)
- Одна клавиатура: тап печатает, рисунок по клавишам распознаётся; линия не выходит за клавиатуру; переключатель режимов удалён.
- Строка запроса в 2 раза ниже, «×» и «⏎» без текста.
- Рисунок прямо на главном экране с гаснущим следом открывает найденное приложение в поиске.
- Полоска над списком главного экрана: удержание и движение сдвигают список.
- Выпадающие списки во всех настройках с выбором одного варианта; тема спейса в «Стиле»; «Спейсы» — список под «Жестами»; название спейса под заголовком настроек.
- Ширина и сдвиг клавиатуры, цветные иконки в поиске, «Переименовать» в меню поиска, радиокнопки спейсов во всплывающей панели.
- Подсказки не предлагают приложения со свайпов и из других спейсов.

### Правки владельца, раунд 6 (ADR-0008)
- Полезность приложений в спейсе (1–100, БД v4): если приложение не на главном экране заметно полезнее одного из приложений на нём, внизу справа появляется «?» с предложением заменить, добавить или не предлагать.
- Часы: тап открывает «Часы»; удержание 0,7 с — перетаскивание в одно из трёх мест (слева, по центру, справа) с подсветкой и снапом.
- Нижняя панель главного экрана: «✎» открывает настройки текущего спейса.
- Настройки: карточки со скруглением 6dp и перечислением содержимого, ряд спейсов-радиокнопок (текущий первый), новые разделы «Стиль» (5 шрифтов, насыщенность, размер) и «Система → Дополнительно» (анимация нажатия, память). Раздел «Поиск» перенесён в окно поиска.
- Поиск: шапка со значками справа — глаз «показывать скрытые» и шестерёнка с настройками поиска.
- Значки «рука со стрелкой» для свайпов (высота заглавной буквы, своё направление у каждого), счётчик уведомлений — кружок на иконке или справа от названия.
- «Закрыть все приложения в «Недавних»» не сделано: недоступно обычному приложению (Q-89).

### Правки владельца, раунд 5 (ADR-0007)
- Приложение называется Mushin Launcher; «энв» в интерфейсе — «спейс» (en: space).
- B1: после смены иконки приложения (Telegram и др.) свайп, место в спейсе, переименование, история и выученные жесты переезжают на новую точку входа; уже порванные ссылки чинятся при запуске.
- «Клавиатура → Распознавание рисунка»: ползунки «Штраф за сдвиг» и «Порог вариантов».
- Режим вариантов: значки приложений вместо текста, тап запускает.
- Раздел «Память»: выученные рисунки по приложениям (миниатюры на раскладке), частота запусков, стирание.
- «Цветные иконки» — переключатель в настройках каждого спейса (БД v3, по умолчанию выкл.).
- Поиск: «Скрыть» / «Показать» в меню долгого нажатия; глаз в колонке недавних показывает скрытые.
- Стрелки ↑ ↓ ← → у приложений, назначенных на свайп, — на главном экране и в поиске.
- Поиск из трёх скруглённых панелей с отступами (результаты, недавние, ввод).
- Удержание спейса во всплывающей панели 1 с открывает его настройки.
- «Жесты» собирают свайпы и ввод рисунком; общие пункты корня настроек под заголовком «Для всего приложения».
- Темы: белый текст приглушён к ~87 %, фон «Графита» #121212; новые темы «Ночь» (OLED) и «Песок» — всего шесть; уже созданные спейсы получают обновлённые цвета.

### Правки владельца, раунд 4 (ADR-0006)
- Распознавание рисунка: второй канал — форма без учёта сдвига и масштаба и направление штриха в каждой из 32 точек; смещённые и уменьшенные рисунки распознаются.
- Если рисунок похож на несколько приложений, в запросе «варианты…», в результатах — все они; выбор из вариантов обучает жест.
- Подготовка к Google Play: подпись ключом загрузки из `keystore.properties`; значок; вход `INFO` для кнопки «Открыть»; раздел «Конфиденциальность» в «Система»; материалы в `docs/release/`.
- Безопасность по итогам ревью:
  - данные исключены из переноса на новый телефон;
  - фоновые ошибки, повреждённые настройки и чужие иконки больше не роняют лаунчер;
  - длина меток и путей жестов ограничена;
  - убран `EmojiCompatInitializer`;
  - сторож `:app:checkReleasePermissions`.

### Правки владельца, раунд 3 (ADR-0005)
- Режим рисунка: клавиша «⌫» вместо числа результатов стирает нарисованное слово.
- Опция «Буквы на панели рисунка»: бледная раскладка QWERTY под штрихом для обучения.

### Правки владельца, раунд 2 (ADR-0004)
- Клавиатура: ряд с пробелом (высота как у клавиш), вся клавиатура поднята на половину клавиши.
- Ввод рисунком: панель вдвое выше; задержка ≥ 1 с удваивает букву; распознавание по форме пути; в запрос попадает только слово из имени установленного приложения (кириллица — как в названии); штрих стирается при закрытии поиска, блокировке и запуске.
- Переключатель «Жест» / «АБВ» слева в строке запроса; пункт из настроек клавиатуры убран.
- Обучение жестам на устройстве: после исправления жест запоминается за открытым приложением с весом (Room v2, таблица `gesture_sample`, автомиграция).
- Счётчик уведомлений на Home (опция, `NotificationListenerService`, только количество).
- Свайп вниз из верхних 20 % экрана всегда открывает шторку.
- Настройки: выбранный вариант — фон accent без галочки; новый экран «Анимация нажатия» с пятью стилями.

### Правки владельца, раунд 1 (ADR-0003)
- Клавиатура в три ряда; «Открыть ⏎» — большая кнопка справа в строке запроса под recents.
- Свайп вниз закрывает поиск: клавиатура, строка запроса, recents, конец списка результатов.
- Пикер приложений энва применяет каждый тап сразу, «добавлено» — фон accent, без галочки и «Готово»; «Избранное» → «Приложения».
- Корень настроек: секция текущего энва с пунктом «Приложения»; общий пункт переименован в «Скрытые и переименованные».
- Запуск приложения свайпом: Home уезжает в сторону свайпа, затем приложение открывается без системной анимации.
- Опция «Ввод рисунком»: пустая панель вместо клавиш, путь пальца распознаётся по QWERTY (`GestureDecoder`).
- Переходы 200 мс: слой поиска, экраны настроек, вспышка нажатия.
- Долгое нажатие на строке поиска: добавить/убрать из энва, о приложении, удалить.

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
