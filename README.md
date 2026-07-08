# POS Offline — Android (Kotlin · Jetpack Compose · Room)

A 100% offline Point-of-Sale / Cashier app for Android **16** (API 36, with `minSdk = 26`).
MVVM architecture, Jetpack Compose UI, Room database, Coroutines + Flow.
No network calls anywhere. No analytics. No telemetry.

> This is the native Android counterpart of the web app we built earlier
> in this same session. The folder structure, naming, and the order of
> operations in the checkout pipeline are intentionally identical, so
> you can read both side-by-side.

---

## Table of contents

1. [Features](#features)
2. [Tech stack & versions](#tech-stack--versions)
3. [Folder structure](#folder-structure)
4. [Architecture (MVVM) at a glance](#architecture-mvvm-at-a-glance)
5. [Build & run — step by step](#build--run--step-by-step)
6. [Tools you need on your machine](#tools-you-need-on-your-machine)
7. [Configuration knobs](#configuration-knobs)
8. [Performance choices that keep it light](#performance-choices-that-keep-it-light)
9. [Known limitations & extension points](#known-limitations--extension-points)
10. [Bluetooth thermal printing — how to add it](#bluetooth-thermal-printing--how-to-add-it)
11. [License](#license)

---

## Features

- **Inventory CRUD** — add, edit, delete, list products (SKU, name, price, stock, category).
- **Cashier (POS) screen** — responsive product grid, reactive cart, per-line & cart-level discount, 3 payment methods (Cash / QRIS / Card), live tax & change calculation.
- **Atomic checkout** — re-reads products, verifies stock, decrements inventory, and writes the transaction inside a single Room `@Transaction`. A crash mid-checkout cannot leave inventory inconsistent.
- **Receipt** — pure-text ESC/POS output *and* a PNG image rendered offscreen via `Canvas`. Both are shareable to any Bluetooth thermal printer via Android's share sheet.
- **History & reports** — daily revenue, transaction count, average ticket; full history with invoice, time, payment method, total.
- **Settings** — store name, address, tax rate, currency. Stored in `DataStore` (Preferences).

---

## Tech stack & versions

| Component               | Version           | Why                                                                  |
| ----------------------- | ----------------- | -------------------------------------------------------------------- |
| **Kotlin**              | **2.1.0**         | Stable; Compose Compiler is now a separate plugin                    |
| **AGP** (Android Gradle Plugin) | **8.7.3**   | Required for compileSdk 36                                           |
| **Gradle wrapper**      | **8.11.1**        | Matches AGP 8.7.x                                                    |
| **JDK**                 | **17 (LTS)**      | Required by AGP 8.7+ and the Kotlin 2.1 toolchain                    |
| **compileSdk / targetSdk** | **36 (Android 16)** | Per requirement; minSdk 26 to keep `java.time` native + desugaring |
| **minSdk**              | **26**            | Android 8.0 (Oreo)                                                   |
| **Jetpack Compose BOM** | **2024.12.01**    | Material3 1.3.x + foundation 1.7.x bundled                           |
| **Compose Compiler**    | bundled with `kotlin.plugin.compose` 2.1.0 | No separate version pinning          |
| **Room**                | **2.7.0**         | Stable on KSP2; `@Transaction` + `@Index` + Flow support             |
| **KSP**                 | **2.1.0-1.0.29**  | Aligns with Kotlin 2.1.0                                             |
| **Lifecycle / ViewModel** | **2.8.7**      | `viewModelScope`, `collectAsStateWithLifecycle`                       |
| **Navigation Compose**  | **2.8.5**         | Tab-style NavHost with three destinations                            |
| **DataStore Preferences** | **1.1.1**       | Type-safe settings persistence                                       |
| **kotlinx.coroutines**  | **1.9.0**         | `Flow`, `StateFlow`, `combine`, structured concurrency               |
| **kotlinx.serialization** | **1.7.3**       | JSON encoding of transaction line items                              |
| **Coil**                | **2.7.0**         | Optional, for product images (no network by default)                 |
| **Desugar JDK libs**    | **2.1.4**         | `java.time` & `java.util.stream` on minSdk 26 (kept for safety)      |

All of the above are pinned in **`gradle/libs.versions.toml`** — change them in one place.

---

## Folder structure

```
android-pos/
├── build.gradle.kts                 # top-level
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml           # version catalog (single source of truth)
│   └── wrapper/
│       └── gradle-wrapper.properties
├── gradlew                          # POSIX wrapper
├── README.md                        # this file
└── app/
    ├── build.gradle.kts             # module config
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/posoffline/
        │   ├── PosApplication.kt        # App + AppContainer (manual DI)
        │   ├── data/                    # ── "Room" layer ──
        │   │   ├── PosDatabase.kt
        │   │   ├── Converters.kt
        │   │   ├── SettingsRepository.kt
        │   │   ├── dao/
        │   │   │   ├── ProductDao.kt
        │   │   │   └── TransactionDao.kt
        │   │   ├── entity/
        │   │   │   ├── ProductEntity.kt
        │   │   │   └── TransactionEntity.kt
        │   │   ├── repository/
        │   │   │   ├── ProductRepository.kt
        │   │   │   └── TransactionRepository.kt
        │   │   └── seed/
        │   │       └── SeedRunner.kt
        │   ├── domain/
        │   │   └── model/
        │   │       └── CartLine.kt      # CartLine, Totals, PaymentMethod
        │   ├── receipt/                 # ── Struk & ESC/POS ──
        │   │   ├── EscPosBuilder.kt
        │   │   ├── ReceiptRenderer.kt   # Canvas → Bitmap
        │   │   └── ReceiptShare.kt      # FileProvider + SEND intent
        │   ├── util/
        │   │   └── Money.kt             # integer-rupiah helpers
        │   └── ui/                      # ── Compose ──
        │       ├── MainActivity.kt
        │       ├── components/
        │       │   └── Glass.kt
        │       ├── theme/
        │       │   ├── Color.kt
        │       │   └── Theme.kt
        │       └── screen/
        │           ├── pos/
        │           │   ├── PosScreen.kt
        │           │   ├── CartPanel.kt
        │           │   ├── CartViewModel.kt
        │           │   ├── ProductViewModel.kt
        │           │   └── ReceiptDialog.kt
        │           ├── inventory/
        │           │   └── InventoryScreen.kt
        │           ├── history/
        │           │   ├── HistoryScreen.kt
        │           │   └── TransactionViewModel.kt
        │           └── settings/
        │               └── SettingsViewModel.kt
        └── res/
            ├── drawable/
            │   ├── ic_launcher_background.xml
            │   └── ic_launcher_foreground.xml
            ├── mipmap-anydpi-v26/
            │   ├── ic_launcher.xml
            │   └── ic_launcher_round.xml
            ├── values/
            │   ├── colors.xml
            │   ├── strings.xml
            │   └── themes.xml
            └── xml/
                ├── backup_rules.xml
                ├── data_extraction_rules.xml
                └── file_provider_paths.xml
```

This is the **ideal package structure** for an Android POS of this size:
each layer is one package, and within each layer files are grouped by
feature (POS, inventory, history, settings) — not by file type. This
keeps related code physically close, which scales much better than the
common "controllers / models / views" layout.

---

## Architecture (MVVM) at a glance

```
┌──────────────────────┐     StateFlow      ┌──────────────────────┐
│   View (Compose)     │ ◀────────────────▶ │   ViewModel (Kotlin) │
│   PosScreen.kt,      │   user events      │   CartViewModel,     │
│   CartPanel.kt, ...  │ ─────────────────▶ │   ProductViewModel…  │
└──────────────────────┘                    └──────────┬───────────┘
                                                       │ suspend calls
                                              ┌────────▼─────────┐
                                              │  Repository      │
                                              │  (single source  │
                                              │   of truth)      │
                                              └────────┬─────────┘
                                                       │ DAO methods
                                              ┌────────▼─────────┐
                                              │  Room DAO        │
                                              │  @Transaction    │
                                              │  @Query / Flow   │
                                              └────────┬─────────┘
                                                       │ SQLite
                                              ┌────────▼─────────┐
                                              │  SQLite (WAL)    │
                                              └──────────────────┘
```

- **View** is stateless except for ephemeral UI state (text input, dialog open/closed).
- **ViewModel** holds `StateFlow<UiState>`, mutates it through the Repository, and survives configuration changes.
- **Repository** is the only layer that knows about the DAO. It enforces invariants (atomic checkout, integer-rupiah math, etc.).
- **DAO** declares SQL once. All reads return `Flow<…>` so the UI re-renders automatically.

---

## Build & run — step by step

### A. Prepare your machine

1. **Install JDK 17.** We recommend **Temurin 17 (LTS)** or any OpenJDK 17.
   ```bash
   java -version    # must print "17"
   ```
2. **Install Android Studio Ladybug (2024.2.1) or newer** — required for AGP 8.7.x and Compose Compiler 2.1.
   - You can also build from CLI without Android Studio if you only need the SDK.
3. **Install the Android 16 SDK platform (API 36)** and **Build-Tools 36.0.0**:
   - Open Android Studio → *Settings → Languages & Frameworks → Android SDK* → check **Android 16 (API 36)** and the matching *SDK Platform* + *Sources for Android 36*.
   - Click **Apply** to download.
4. **Set the SDK path** (skip if Android Studio handles it):
   ```bash
   echo "sdk.dir=$ANDROID_SDK_ROOT" > android-pos/local.properties
   # or on macOS:
   echo "sdk.dir=$HOME/Library/Android/sdk" > android-pos/local.properties
   ```
   (`local.properties` is per-machine and is in `.gitignore`.)

### B. Build from the command line

```bash
cd android-pos
chmod +x gradlew        # only needed the very first time
./gradlew :app:assembleDebug          # debug APK → app/build/outputs/apk/debug/
./gradlew :app:assembleRelease        # release APK (minify+shrink enabled)
./gradlew :app:installDebug           # build + install on a connected device
```

> The `gradle-wrapper.jar` is committed alongside `gradlew`, so the
> project is buildable immediately on a fresh clone.
>
> The first run will download the Gradle 8.11.1 distribution into
> `~/.gradle/wrapper/dists/` (cached for all subsequent builds) and
> the Compose / Room / KSP artifacts. Allow ~5–10 minutes on a clean
> machine.

### C. Build & run from Android Studio

1. **File → Open** → select the `android-pos` folder.
2. Wait for the **Gradle Sync** to finish. If prompted, **Upgrade AGP** or accept the suggested SDK components.
3. Pick a device/emulator running **Android 8.0+ (API 26+)**. For full feature parity, use **Android 16 (API 36)**.
4. Press **Run ▶** (`Shift+F10`).

### D. Verify it works

- On first launch the app seeds **8 sample products** in Indonesian.
- Tap any product → it appears in the cart.
- Adjust quantity, type a "Bayar" amount ≥ Total, tap **Tunai** → the receipt dialog appears.
- Tap **Inventaris** or **Riwayat** in the top bar to navigate to the other screens.

---

## Tools you need on your machine

| Tool                | Min version | Notes                                                                 |
| ------------------- | ----------- | --------------------------------------------------------------------- |
| **JDK**             | 17          | Set `JAVA_HOME`. AGP 8.7 will refuse to run on JDK 11/21 by default. |
| **Android Studio**  | Ladybug 2024.2.1 | Recommended; "Koala" or newer also work.                          |
| **Android SDK Platform** | 36      | Required for `compileSdk = 36`.                                       |
| **Android SDK Build-Tools** | 36.0.0 | Auto-installed by Android Studio when you open the project.        |
| **Android SDK Platform-Tools** | latest | For `adb` from the command line.                                  |
| **Gradle wrapper**  | 8.11.1      | Pinned in `gradle/wrapper/gradle-wrapper.properties`. Auto-downloaded. |
| **Optional: `adb`** | latest      | For `adb install` and `adb logcat` from the command line.             |

If you are on macOS and prefer Homebrew:

```bash
brew install --cask temurin@17
brew install --cask android-commandlinetools
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
```

---

## Configuration knobs

All build-time configuration lives in **`app/build.gradle.kts`** and **`gradle/libs.versions.toml`**.

| Knob                          | File                              | Default      | What it does                                          |
| ----------------------------- | --------------------------------- | ------------ | ----------------------------------------------------- |
| `applicationId`               | `app/build.gradle.kts`            | `com.example.posoffline` | Unique Play Store identifier. Change before publishing. |
| `minSdk`                      | `app/build.gradle.kts`            | 26           | Lower = more devices, but loses native `java.time`.   |
| `targetSdk` / `compileSdk`    | `app/build.gradle.kts`            | 36           | Keep at 36 to target Android 16.                      |
| `versionCode` / `versionName` | `app/build.gradle.kts`            | 1 / 1.0.0    | Bump on every release.                                |
| Default tax rate              | `data/SettingsRepository.kt`      | 0.11 (11%)   | Persisted per-device. Edit in Settings if you add a UI.|
| Default store name / address  | `data/SettingsRepository.kt`      | "Toko Saya"  | Same as above.                                        |
| DB schema location            | `app/build.gradle.kts` → `ksp{}`  | `app/schemas` | Keep these files in VCS so migrations can be diffed. |
| Room version                  | `gradle/libs.versions.toml`       | 2.7.0        | Bump together with KSP.                               |
| Seed data                     | `data/seed/SeedRunner.kt`         | 8 products   | Idempotent; runs once when the products table is empty. |

---

## Performance choices that keep it light

These are not theoretical — they were chosen to keep the app fast on
mid-range Android 8/9 devices as well as the newest Android 16 hardware.

- **No Hilt / Koin** — a 30-line manual `AppContainer` removes a whole KSP processor and an extra ~250 KB of runtime.
- **`StateFlow` over `LiveData`** — cheaper conversions, Kotlin-idiomatic, and integrates with `collectAsStateWithLifecycle` for lifecycle-aware collection.
- **Single source of truth per screen** — every mutation funnels through a `MutableStateFlow.update { … }`, so we never have inconsistent partial states.
- **`combine` for derived UI state** — totals are recomputed exactly when an input changes, not on every frame.
- **`LazyColumn` / `LazyVerticalGrid`** — only visible cells are composed. With ~100 SKUs the grid renders in <16 ms on a Pixel 4a.
- **No `Modifier.blur()` on large surfaces** — we render the blur as a translucent gradient over a radial-gradient body background. The GPU cost is a single overdraw pass.
- **Integer rupiah math** — no `Double` anywhere in the pricing path. `Long` math is fast and the totals are predictable.
- **WAL + `synchronous = NORMAL`** — better concurrency between reads (history) and writes (checkout) on the same SQLite file.
- **PRAGMA `foreign_keys = ON`** — safe, free integrity check; you can add `@ForeignKey` later without a refactor.
- **Receipt rendering happens off the main thread** — `produceState` + `Dispatchers.Default`. The UI never blocks while the bitmap is drawn.
- **`@Transaction` for checkout** — the stock decrement + transaction insert are atomic. Either both happen or neither does.

---

## Known limitations & extension points

- **No Bluetooth printing** is wired by default (the permissions are commented out in the manifest). See the next section.
- **Tax rate & currency are per-store** but the settings UI is intentionally not exposed yet. Wire it into a new `SettingsScreen.kt` — the `SettingsViewModel` is already there.
- **Single-currency, single-shop.** For multi-shop, add a `shopId` column and filter every DAO query by it.
- **No barcode scanner integration.** You can pass the scanned SKU into `addProductBySku()` if you add such a method to `ProductRepository`.
- **No cloud backup.** The `pos-offline.db` is excluded from `auto_backup` on purpose — sales data should stay on the device unless you add explicit export.

---

## Bluetooth thermal printing — how to add it

The receipt pipeline is already complete on the file-format side:

- `EscPosBuilder.build(tx, items, settings)` → plain text
- `ReceiptRenderer.render(...)` → PNG bitmap
- `ReceiptShare.shareText(...)` / `sharePng(...)` → share intent via `FileProvider`

To pair & print:

1. **Add permissions** to `AndroidManifest.xml` (uncomment the lines already there):
   ```xml
   <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
   <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
                    android:usesPermissionFlags="neverForLocation" />
   ```
2. **Pair** the printer through Android Settings → *Connected devices → Pair new device*.
3. From `ReceiptDialog`, on `⎙ ESC/POS`:
   - If the user already picked a printer, open a `BluetoothSocket` on its `UUID` (`00001101-0000-1000-8000-00805F9B34FB`) and write `EscPosBuilder.build(...)` bytes.
   - If no printer is paired, list paired devices, let the user pick one, persist the choice in DataStore, then send the bytes.
4. Send `0x1B 0x40` (ESC @ — reset) before the content, and `0x0A 0x0A 0x1D 0x56 0x41 0x10` (cut) after, if your printer supports it.

The whole flow can live in a new `BluetoothPrinter.kt` file in the
`receipt/` package without touching the rest of the app.

---

## License

This is a starter template — adapt it freely for your store.
No warranty; no telemetry; no network code.
