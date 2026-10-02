# C. UX-спецификация

## C.1 Экраны и состояния

| Экран | Состояния | Вход | Выход |
|---|---|---|---|
| Home | `Idle`, `RoleMissing` (карточка), `EmptyEnv` | Старт, Home, Back из поиска | Запуск приложения, поиск, popup |
| Popup (Env/App) | `EnvOnly`, `WithAppSection` | Long-press | Тап вне, Back, Home, выбор |
| Search | `Typing` (клавиатура видна), `Browsing` (клавиатура свёрнута — ALL_APPS или аппаратная клавиатура), `NoResults` | Двойной тап, свайп SEARCH/ALL_APPS, type-to-search | Back, Home, запуск |
| Settings root | — | ⚙ в popup | Back |
| Settings → Энвы | список энвов + «Создать» | | |
| Env editor | `Viewing`, `Dragging`, `LimitReached` | Тап по энву | Back (автосохранение) |
| App picker | `MultiSelect` / `SingleSelect` (для жеста) | Редактор, жесты | Готово / тап |
| Settings → Главный экран | icon_mode, формат часов, показ даты, размер текста (S 22sp / M 26sp / L 30sp) | | |
| Settings → Поиск | fuzzy, раскладка, транслит (вкл/выкл), очистить историю энва | | |
| Settings → Клавиатура | раскладки (порядок, вкл), haptics, подсказки цифр | | |
| Settings → Жесты | 4 строки направлений → пикер действия → пикер приложения | | |
| Settings → Приложения | скрытые, переименованные | | |
| Settings → Система | «Лаунчер по умолчанию», версия, лицензии сторонних библиотек | | |

## C.2 Навигационная карта

```
Home ──long-press──▶ Popup ──⚙──▶ Settings(root) ─┬─▶ Энвы ─▶ Env editor ─▶ App picker(multi)
  │                    └─энв──▶ Home(новый энв)    ├─▶ Главный экран
  ├──double-tap / swipe(SEARCH)──▶ Search(Typing)   ├─▶ Поиск
  ├──swipe(ALL_APPS)──▶ Search(Browsing)            ├─▶ Клавиатура
  ├──swipe(NOTIFICATIONS)──▶ системная шторка       ├─▶ Жесты ─▶ Action picker ─▶ App picker(single)
  └──tap row──▶ приложение                          ├─▶ Приложения
Search ──tap/Enter──▶ приложение (поиск сброшен)    └─▶ Система
Любое состояние ──Home──▶ Home(Idle)
```

Settings — стек внутри той же Activity: `sealed interface SettingsRoute`, свой `SnapshotStateList` в роли back stack, без библиотеки Navigation. Переходы мгновенные.

## C.3 Жесты на Home и их приоритеты

| Приоритет | Жест | Где | Результат |
|---|---|---|---|
| 1 | Системные: back с бокового края, home/recents снизу | Зоны `systemGestures` / `mandatorySystemGestures` | Система. Back приходит в `BackHandler`. |
| 2 | Long-press | Везде | Popup (фон → EnvOnly, строка → WithAppSection) |
| 3 | Свайп | Старт вне системных зон | Действие биндинга |
| 4 | Tap | Строка | Запуск |
| 5 | Double-tap | Фон (не строка) | Поиск |
| — | Tap | Фон | Ничего |

Разрешение конфликтов внутри одного указателя:

- touchSlop превышен до long-press таймаута → long-press отменён, начинается кандидат в свайп;
- long-press сработал → свайп и тап для этого указателя запрещены;
- на строке превышен touchSlop → запуск отменяется, кандидат в свайп.

## C.4 Пороги свайпов (фиксированные)

- Направление определяется по доминирующей оси: |dx| ≥ 1,5·|dy| — горизонталь, |dy| ≥ 1,5·|dx| — вертикаль. Иначе свайп не засчитывается.
- Срабатывание: дистанция ≥ 96dp, **или** скорость на UP ≥ 1000 dp/s при дистанции ≥ 48dp. Скорость считает `VelocityTracker` (Compose `VelocityTracker`).
- Срабатывание — на UP. Во время движения визуальной обратной связи нет (принцип «ноль анимаций»). Haptic `GESTURE_END` в момент срабатывания.
- Стартовые зоны:
  - свайп вверх засчитывается, только если DOWN выше верхней границы `WindowInsets.mandatorySystemGestures.bottom` + 16dp;
  - горизонтальные свайпы — только если DOWN дальше `WindowInsets.systemGestures.left/right` + 8dp от края;
  - свайп вниз — откуда угодно ниже статус-бара.
- `setSystemGestureExclusionRects` **не используется**. Технически лаунчер не ограничен лимитом 200dp, но забирать back-зону у пользователя противоречит принципу предсказуемости.
- Список на Home не скроллится (Q4), так что конфликта со скроллом нет. В поиске свайпы отключены: там работают только скролл списков и клавиатура.

## C.5 Визуальные токены палитры энва

Роли цвета (все — ARGB, хранятся в Room):

| Роль | Назначение |
|---|---|
| `background` | Фон окна, Home, Search |
| `zebraA` | Фон чётных строк |
| `zebraB` | Фон нечётных строк (на 3–5 % светлоты отличается от `zebraA`) |
| `textPrimary` | Названия, часы |
| `textSecondary` | Дата, подписи, подсказки цифр, «скрыто» |
| `accent` | Текущий энв ✓, курсор запроса, онбординг-карточка, подсветка drag |
| `iconTint` | Цвет монохромного фильтра иконок |
| `divider` | Разделители, рамки |
| `surface` | Фон popup и диалогов |
| `keyBackground` | Фон клавиш |
| `keyPressed` | Фон нажатой клавиши |
| `isDark` (bool) | Стиль иконок системных баров |

Пресеты (MVP):

| Пресет | background | zebraA | zebraB | textPrimary | textSecondary | accent | iconTint | divider | surface | keyBackground | keyPressed | isDark |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Graphite (NORMAL по умолчанию) | #0E0E0E | #0E0E0E | #171717 | #EDEDED | #8A8A8A | #B5C7FF | #D0D0D0 | #262626 | #1A1A1A | #1C1C1C | #2E2E2E | true |
| Navy (WORK по умолчанию) | #0B1220 | #0B1220 | #111A2C | #E6ECF7 | #8C9AB3 | #6EA8FE | #C9D6EE | #1E2A40 | #131D30 | #152036 | #22324F | true |
| Paper (светлый) | #F5F5F2 | #F5F5F2 | #EBEBE7 | #141414 | #5F5F5F | #2F5BD3 | #2A2A2A | #D9D9D4 | #FFFFFF | #E6E6E1 | #D2D2CC | false |
| Moss | #0F130F | #0F130F | #161C16 | #E4EBE2 | #8C9989 | #9FD39A | #CBD6C8 | #243024 | #182018 | #1A221A | #283428 | true |

Контраст `textPrimary`/`background` ≥ 7:1, `textSecondary`/`background` ≥ 4,5:1. Проверяется unit-тестом по формуле WCAG relative luminance для каждого пресета.

## C.6 Типографика и отступы

- Шрифт: `FontFamily.Default` (системный). Внешних шрифтов в MVP нет.
- Часы 72sp/300; дата 18sp/400; строка Home 26sp/400 (S/M/L = 22/26/30sp); результат поиска 22sp/400; recent 16sp/400; строка запроса 24sp/400; клавиша 22sp/400; подсказка цифры 10sp; popup-пункт 16sp/400, его заголовок 14sp.
- Значок рабочего профиля: контурный «портфель» 14dp `textSecondary` через 6dp после метки. Векторный ресурс рисуется вручную или берётся из Material Symbols (Apache-2.0).
- Высоты: строка Home 64dp, результат 56dp, recent 52dp, пункт popup 48dp, клавиша 52dp, строка запроса 56dp.
- Горизонтальные отступы текста: 24dp на Home, 16dp в поиске, 16dp в popup.

## C.7 Экран поиска (раскладка сверху вниз)

1. Инсет статус-бара.
2. Область панелей (вес 1): слева результаты (2/3), справа recents (1/3). Обе панели прижаты к низу (`reverseLayout = true`): лучший результат — сразу над строкой запроса, ближе к большому пальцу.
3. Строка запроса 56dp: текст запроса 24sp `textPrimary` и статичный курсор «▏» `accent` без мигания. Справа — счётчик результатов 14sp `textSecondary`.
4. Клавиатура: 4 ряда × 52dp + 8dp паддинг, фон `background`, клавиши `keyBackground` со скруглением 6dp и зазором 4dp.
5. Инсет навбара (`WindowInsets.navigationBars`).

В режиме Browsing пункт 4 скрыт. Тап по строке запроса возвращает клавиатуру.

## C.8 Системные бары

`enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT))` при `isDark`, иначе `SystemBarStyle.light(TRANSPARENT, TRANSPARENT)`. Вызывается при каждой смене палитры. `window.isNavigationBarContrastEnforced = false`.
