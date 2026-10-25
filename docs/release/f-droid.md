# Публикация в F-Droid и Obtainium

## Что уже готово в репозитории
- `fastlane/metadata/android/` — карточка для F-Droid: название, краткое и полное описание (en-US, ru-RU), значок, обложка, пять скриншотов (en-US, 1080 × 2400) и журнал изменений `changelogs/<versionCode>.txt` (не длиннее 500 символов). Скриншоты сняты на тестовом эмуляторе с демо-режимом строки состояния.
- В release-APK нет блока зависимостей (`dependenciesInfo.includeInApk = false`): его может прочитать только Google, F-Droid такие APK не принимает. В AAB для Play блок остаётся.
- В release-APK нет `META-INF/version-control-info.textproto` (`vcsInfo.include = false`): в нём хеш git-коммита, из-за которого сборки одного и того же кода расходились.
- Сборка воспроизводима локально: подписанный APK без блока подписи побайтно совпадает с неподписанным, собранным из чистого клона в другой папке.
- Obtainium: ссылка и инструкция в README, публиковать ничего не нужно. Obtainium сам берёт APK из GitHub Releases.

## Подпись
В метаданных указаны `Binaries` и `AllowedAPKSigningKeys`. F-Droid соберёт APK сам, сравнит его с APK из GitHub Releases и, если они совпадают, опубликует APK с нашей подписью. Тогда пользователи могут переходить между F-Droid, GitHub и Obtainium без переустановки. Если сборки на сервере F-Droid не совпадут (другая JDK, другая ОС), F-Droid сообщит об этом в merge request; тогда либо чиним расхождение, либо убираем эти два поля, и F-Droid подписывает своим ключом.

## Файл для fdroiddata
`metadata/com.mushinlab.launcher.yml`. `commit` — полный хеш коммита тега `v0.5.1`, вписывается после релиза.

```yaml
Categories:
  - Launcher
License: Apache-2.0
AuthorName: Mushin Lab
SourceCode: https://github.com/mushinlab/mushin-launcher
IssueTracker: https://github.com/mushinlab/mushin-launcher/issues
Changelog: https://github.com/mushinlab/mushin-launcher/blob/main/docs/CHANGELOG.md

AutoName: Mushin Launcher

RepoType: git
Repo: https://github.com/mushinlab/mushin-launcher.git
Binaries: https://github.com/mushinlab/mushin-launcher/releases/download/v%v/mushin-launcher-%v.apk

Builds:
  - versionName: 0.5.1
    versionCode: 8
    commit: <хеш коммита v0.5.1>
    subdir: app
    gradle:
      - yes

AllowedAPKSigningKeys: 50547d7843aa0a87fae34ce20a283aa5b14f470505cb981b2aa28716210f8729

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.5.1
CurrentVersionCode: 8
```

## Порядок подачи
1. Выпустить 0.5.1 обычным порядком релиза (тег `v0.5.1`, APK `mushin-launcher-0.5.1.apk` в GitHub Releases).
2. Войти на gitlab.com через GitHub, сделать форк `fdroid/fdroiddata`, ветку `com.mushinlab.launcher`.
3. Положить файл выше в `metadata/com.mushinlab.launcher.yml`, вписать хеш коммита.
4. Закоммитить с сообщением `New App: com.mushinlab.launcher`, запушить в форк — CI форка соберёт приложение и проверит метаданные.
5. Открыть merge request в `fdroid/fdroiddata`, заполнить чек-лист шаблона. В описании честно указать, что код написан с помощью ИИ-агентов (Claude, Codex), а владелец проверяет и отвечает за каждое изменение: временная политика F-Droid по генеративному ИИ этого не запрещает, но советует раскрывать.
6. Отвечать на вопросы ревьюеров. После слияния приложение появляется в F-Droid примерно через 1–2 дня. Дальше F-Droid сам находит новые теги `v*`.

## Перед каждым следующим релизом
- `fastlane/metadata/android/<язык>/changelogs/<versionCode>.txt` для новой версии.
- APK в Releases называется `mushin-launcher-<versionName>.apk`, тег — `v<versionName>`: на эти имена опирается `Binaries`.
- APK собирается из того же кода, что лежит в теге, без незакоммиченных изменений.

## Почему не IzzyOnDroid
Политика IzzyOnDroid отклоняет приложения, код которых полностью или частично написан генеративным ИИ.
