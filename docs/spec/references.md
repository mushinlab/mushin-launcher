# F. Сравнение референсных open-source лаунчеров

> Код GPL-проектов не открывать и не копировать (D.1.9). Берём только идеи и UX.

| Лаунчер | Лицензия | Стек | Что взять как идею (не код) | Чего избегать |
|---|---|---|---|---|
| Focus Launcher (mslalith) | GPL-3.0 | Kotlin, Compose, Circuit, Hilt, Room, DataStore, Ktor; модули core/feature, baselineprofile | Модульность, отдельный модуль Baseline Profile, крупный текстовый список избранного | Колокольчик и область Work (убраны по ТЗ); Ktor/сеть |
| Focus (com.focus.launcher, F-Droid) | GPL-3.0-or-later | Kotlin, сервис-локатор, ~1,3 MiB | Поиск по именам, инициалам и опечаткам; «новые за 24 ч сверху»; значок-портфель для Work; отсутствие INTERNET | QUERY_ALL_PACKAGES; таймеры и usage stats вне scope |
| Kvaesitso | GPL-3.0 (plugin SDK — Apache-2.0) | Kotlin, Compose | Search-first-парадигма, обработка «hide private space» | Широкий охват (веб, контакты) |
| Olauncher | GPL-3.0 | Kotlin, MVVM, View | До 8 приложений, свайп-жесты на приложения, переименование, поддержка Work | Ежедневные обои (сеть) |
| mLauncher | GPL-3.0 | Kotlin (форк Olauncher) | Без сетевого доступа, кастомизация текста | — |
| KISS Launcher | GPL-3.0-only | Java, View | История поиска, ранжирование по использованию, компактность | — |
| µLauncher | см. репозиторий | Kotlin | Private Space, Work profile, настраиваемые жесты | — |
| Fokus Launcher (luantak) | см. репозиторий | Kotlin, Compose | Private Space (API 35+): lock/unlock и отдельная секция | — |
| Lawnchair | **расхождение**: Apache-2.0 vs GPLv3 — проверить LICENSE | Java/Kotlin, Launcher3 | Opt-in Private Space на Home; themed icons через `grayscale_icon_map` | Сложность Launcher3 |
| AOSP Launcher3 | Apache-2.0 | Java | Нормализация иконок, обработка профилей. Заимствование допустимо с NOTICE | Объём |
