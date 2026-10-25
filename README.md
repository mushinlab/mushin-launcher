<p align="center">
  <img src="docs/release/assets/banner.png" width="100%" alt="Mushin launcher">
</p>

# Mushin Launcher

A calm, minimalist launcher for Android. One touch to the app you need, and nothing else.

## Why Mushin

- **Nothing between you and your phone.** No ads, no widgets, no feeds, nothing competing for your attention. No data collection and no analytics either: Mushin works fully offline and does not even ask for internet access.
- **The fastest way to your apps.** The apps you use are one tap away, and everything else is a few letters or a quick drawn word away, with search that opens instantly.
- **A launcher that adapts to you.** Mushin learns which apps you reach for and keeps separate spaces for work, home or travel, each with its own list and colours.
- **Inspired by Japan.** Mushin (無心) is the Zen idea of a still, empty mind. The launcher borrows that calm: quiet colours, generous space and backgrounds of Mount Fuji, sakura and ink.

## Languages

The interface is available in English, Español, Português (Brasil), Deutsch, Français, Bahasa Indonesia and Русский; it follows the phone language or the one chosen in Settings → System → Language. The keyboard has native layouts for the same languages (QWERTY, ЙЦУКЕН, QWERTZ, AZERTY), and search finds names with accents when you type the plain letters.

## Install

Requires Android 13 or newer.

1. Download the APK from [Releases](https://github.com/mushinlab/mushin-launcher/releases/latest) and install it.
2. Open Mushin Launcher and agree to make it your home app. You can change this later in Settings → Apps → Default apps → Home app.

To get updates automatically, add Mushin to [Obtainium](https://github.com/ImranR98/Obtainium): tap [this link](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/mushinlab/mushin-launcher) on your phone, or in Obtainium tap “Add app” and paste `https://github.com/mushinlab/mushin-launcher`.

To verify the download, check that the signing certificate matches the SHA-256 fingerprint published in the release notes:

```
apksigner verify --print-certs mushin-launcher-<version>.apk
```

## Build

JDK 17 or newer and the Android SDK are required.

```
./gradlew assembleDebug                         # debug build
./gradlew testDebugUnitTest test                # unit tests
./gradlew assembleRelease                       # release, unsigned unless keystore.properties exists
```

To sign a release, put `keystore.properties` in the project root with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. The file is ignored by git.

Requirements, decisions and the changelog live in [`docs/`](docs/). Requirements and decisions are in Russian.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the architecture, the rules and the checks every change has to pass.

## Privacy

Mushin Launcher collects no data. Everything stays on your device. See the [privacy policy](docs/release/privacy-policy.md).

## License

Copyright 2026 Mushin Lab. Licensed under the [Apache License 2.0](LICENSE).

Fonts Inter, Montserrat and Open Sans are under the SIL Open Font License 1.1 ([licenses](core/ui/src/main/assets/licenses/)).
