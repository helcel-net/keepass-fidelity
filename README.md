<!--suppress ALL -->
<div align="center">
  <h1>Keepass Fidelity</h1>
  <img width="100px" src="./metadata/en-US/images/icon.png" alt="Logo">
  
  <p>A minimalist fidelity/loyalty card app with Keepass Database storage</p>

  <a href="https://ko-fi.com/I2I615VP5M"><img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="ko-fi"></a>
  <br>
  <img src="https://forthebadge.com/images/badges/built-for-android.svg" alt="Built for Android">
  <img src="https://forthebadge.com/images/badges/built-with-love.svg" alt="Built with love">
  <br>
    <a href="https://github.com/helcel-net/keepass-fidelity/actions/workflows/build.yml">
    <img src="https://github.com/helcel-net/keepass-fidelity/actions/workflows/build.yml/badge.svg?branch=main" alt="Build Status">
  </a>
</div>

## 🌄 Screenshots

<div align="center">
  <table>
    <tr>
      <td style="width: 33%; height: 100px;"><img src="./metadata/en-US/images/phoneScreenshots/launcher.jpg" alt="Launcher" style="width: 100%; height: 100%;"></td>
      <td style="width: 33%; height: 100px;"><img src="./metadata/en-US/images/phoneScreenshots/view.jpg" alt="View" style="width: 100%; height: 100%;"></td>
      <td style="width: 33%; height: 100px;"><img src="./metadata/en-US/images/phoneScreenshots/edit.jpg" alt="Edit" style="width: 100%; height: 100%;"></td>
    </tr>
  </table>
</div>

## ⭐ Features

- Two storage modes, chosen on first start (switchable later from the launcher):
  - **Standalone**: opens a `.kdbx` database directly (bundled KeePassDX engine)
  - **Keepass2Android plugin**: queries and creates entries through the installed Keepass2Android app
- Search entries in Keepass Database
- Scan & Create entries
- Recently used history for fast access
- Protect entries from caching
- Minimalist design and features
- Supported Formats: CODE_39, CODE_93, CODE_128, EAN_8, EAN_13, UPC_A, UPC_E, CODE_QR, PDF_417, AZTEC, CODABAR, DATA_MATRIX, ITF

## 📳 Installation

<div style="display: flex; justify-content: center; align-items: center; flex-direction: row;">
    <a href="https://apt.izzysoft.de/fdroid/index/apk/net.helcel.fidelity">
        <img width="200" height="84" alt="Izzy Download" src=".github/images/izzy.png">
    </a>
    <a href="https://github.com/helcel-net/keepass-fidelity/releases/latest">
        <img width="200" height="84" alt="APK Download" src=".github/images/apk.png">
    </a>
    <a future-href="https://f-droid.org/en/packages/net.helcel.fidelity">
        <img width="200" height="84" alt="Fdroid Download" src=".github/images/fdroid.png">
    </a>
    <a href="https://play.google.com/store/apps/details?id=net.helcel.fidelity">
        <img width="200" height="84" alt="GooglePlay Download" src=".github/images/playstore.png">
    </a>
</div>

Note:
 - Fdroid: requested, see https://gitlab.com/fdroid/rfp/-/work_items/4411
 - PlayStore: alpha test restriction by Google. To access, join https://groups.google.com/g/helcel-android-test

## ⚙️ Permissions

- `CAMERA`: necessary for importing barcodes from camera
- `READ_MEDIA_VISUAL_USER_SELECTED`: necessary for the importing barcode from images

## 📝 Contribute

Keepass-Fidelity is a user-driven project. We welcome any contribution, big or small.

- **🖥️ Development:** Fix bugs, implement features, or research issues. Open a PR for review.
- **🍥 Design:** Improve interfaces, including accessibility and usability.
- **📂 Issue Reporting:** Report bugs and edge cases with relevant info.
- **🌍 Localization:** Translate if it doesn't support your language.

## ✏️ Acknowledgements

Thanks to all contributors, the developers of our dependencies, and our users.

## Signinig key

```
net.helcel.fidelity
D1:96:54:FE:1D:79:16:76:47:D7:A6:12:0E:F6:AB:7D:56:9D:45:AD:FD:41:EF:D6:FA:80:9D:26:52:73:F6
```

## 📝 License

```
Copyright (C) 2026 Helcel

Licensed under the Unlicense
For more information, please refer to <https://unlicense.org>
```