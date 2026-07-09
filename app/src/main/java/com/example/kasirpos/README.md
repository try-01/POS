# KasirPOS - Android Point of Sale (100% Offline)

## Struktur Direktori

```
com.example.kasirpos/
├── KasirApp.kt                          # Application class (Room DB singleton)
│
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   ├── ProductEntity.kt         # Tabel produk (inventaris)
│   │   │   ├── CartItemEntity.kt        # Tabel keranjang sementara
│   │   │   ├── TransactionEntity.kt     # Tabel header transaksi
│   │   │   └── TransactionItemEntity.kt # Tabel detail item per transaksi
│   │   ├── dao/
│   │   │   ├── ProductDao.kt            # Akses data produk
│   │   │   ├── CartDao.kt               # Akses data keranjang
│   │   │   └── TransactionDao.kt        # Akses data transaksi + laporan
│   │   └── database/
│   │       └── AppDatabase.kt           # Room DB + migrasi
│   └── repository/
│       ├── ProductRepository.kt         # Repo produk (single source of truth)
│       ├── CartRepository.kt            # Repo keranjang
│       └── TransactionRepository.kt     # Repo transaksi & laporan
│
├── domain/
│   └── model/
│       ├── Product.kt                   # Domain model produk
│       ├── CartItem.kt                  # Domain model item keranjang
│       └── Transaction.kt               # Domain model transaksi
│
├── ui/
│   ├── pos/
│   │   ├── PosScreen.kt                # Layar utama kasir (keranjang + checkout)
│   │   └── PosViewModel.kt             # VM layar kasir
│   ├── inventory/
│   │   ├── InventoryScreen.kt          # Layar manajemen inventaris
│   │   └── InventoryViewModel.kt       # VM inventaris (CRUD)
│   ├── receipt/
│   │   ├── ReceiptScreen.kt            # Layar struk & cetak
│   │   └── ReceiptViewModel.kt         # VM struk
│   ├── report/
│   │   ├── ReportScreen.kt             # Layar laporan harian
│   │   └── ReportViewModel.kt          # VM laporan
│   └── theme/
│       └── Theme.kt                    # Tema aplikasi (warna, typography)
│
└── util/
    ├── PrinterUtil.kt                  # Utility cetak via Bluetooth ESC/POS
    └── PdfExportUtil.kt                # Utility ekspor struk ke PDF
```

---

## Struktur Root Proyek

```
KasirPOS/
├── build.gradle.kts                         # Project-level: plugin declaration
├── settings.gradle.kts                      # Repository & module config
├── gradle.properties                        # JVM & build optimization flags
│
└── app/
    ├── build.gradle.kts                     # App-level: dependencies (Room, Compose, etc.)
    ├── proguard-rules.pro                   # ProGuard rules untuk release build
    │
    └── src/main/
        ├── AndroidManifest.xml              # Izin Bluetooth, entry activity
        │
        ├── res/values/
        │   ├── themes.xml                   # Native splash/fallback theme
        │   └── colors.xml                   # Native color values
        │
        └── java/com/example/kasirpos/
            ├── KasirApp.kt                  # Application (Room singleton)
            ├── MainActivity.kt              # Entry point + BottomNavigation
            │
            ├── data/
            │   ├── local/
            │   │   ├── entity/              # 4 tabel Room
            │   │   ├── dao/                 # 3 DAO (Product, Cart, Transaction)
            │   │   └── database/            # AppDatabase (WAL mode)
            │   └── repository/              # 3 Repository (single source of truth)
            │
            ├── domain/
            │   └── model/                   # 3 Domain model (pure Kotlin)
            │
            ├── ui/
            │   ├── pos/                     # Layar Kasir + ViewModel
            │   ├── inventory/               # Layar Inventaris + ViewModel
            │   ├── receipt/                 # Layar Cetak Struk + ViewModel
            │   ├── report/                  # Layar Laporan + ViewModel
            │   └── theme/                   # Palet warna + glass effect
            │
            └── util/
                ├── PrinterUtil.kt           # ESC/POS Bluetooth printing
                └── PdfExportUtil.kt         # Android PdfDocument export
```


## Tech Stack
- **Bahasa:** Kotlin 2.1
- **UI:** Jetpack Compose (Material 3, BOM 2024.12)
- **Arsitektur:** MVVM bersih (StateFlow + SharedFlow events)
- **DB:** Room 2.6.1 — WAL journal mode, fully offline
- **Async:** Kotlin Coroutines 1.9 + Flow (cold stream)
- **Build:** KSP (bukan KAPT), Gradle 8.7+
- **Target SDK:** Android 16 (API 36), minSdk 24

## Key Design Decisions
- Semua query Room mengembalikan `Flow` agar UI reaktif dan hemat resource.
- Repository bertindak sebagai single source of truth.
- `CartItemEntity` disimpan di Room (bukan in-memory) agar tahan terhadap process death.
- Printer ESC/POS menggunakan Bluetooth Classic SPP — tanpa library pihak ketiga.
- **Glassmorphism ringan:** efek dicapai murni via warna semi-transparan + border halus — tanpa `Modifier.blur()` yang mahal di GPU.
- **State holder pattern:** semua kalkulasi (subtotal, pajak, kembalian) di `PosUiState` sebagai derived properties — dihitung sekali, tidak berulang di Composable.
- **Event channel:** `SharedFlow` untuk one-shot event (snackbar, navigasi) agar tidak hilang saat rotasi.

## Fitur Lengkap

| Modul | Fitur |
|-------|-------|
| **POS** | Keranjang belanja, search produk by nama/SKU, kalkulasi total + diskon + pajak + kembalian otomatis, checkout dengan potong stok atomik, dialog pembayaran multi-metode |
| **Inventaris** | CRUD produk (nama, SKU, harga, stok, gambar), alert stok rendah (< 10), search |
| **Struk** | Pilih transaksi, preview struk, cetak via Bluetooth ESC/POS, ekspor PDF |
| **Laporan** | Ringkasan pendapatan harian (reaktif via Flow), riwayat transaksi lengkap |

## Cara Menjalankan

### Prasyarat
- **JDK 17+** terinstall
- **Android Studio** (Hedgehog 2023.1.1 atau lebih baru)
- Device/emulator **API 24+** (Android 7.0)

### ⚠️ Langkah Wajib: Download `gradle-wrapper.jar`

File `gradle-wrapper.jar` adalah **binary** (~60 KB) yang tidak bisa dibuat sebagai teks.
Pilih salah satu cara:

**Cara A — Generate via Gradle (rekomendasi):**
```bash
# Jika Gradle sudah terinstall di sistem:
gradle wrapper --gradle-version 8.9
```

**Cara B — Download langsung:**
```bash
# Dari terminal di folder root proyek:
# macOS / Linux:
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  https://github.com/gradle/gradle/raw/v8.9.0/gradle/wrapper/gradle-wrapper.jar

# Windows (PowerShell):
Invoke-WebRequest -Uri \
  "https://github.com/gradle/gradle/raw/v8.9.0/gradle/wrapper/gradle-wrapper.jar" \
  -OutFile "gradle/wrapper/gradle-wrapper.jar"
```

**Cara C — Buka dengan Android Studio:**
Android Studio akan otomatis mendeteksi bahwa `gradle-wrapper.jar` hilang dan menawarkan untuk mengunduhnya saat pertama kali sync.

### Build & Run
```bash
# Beri izin eksekusi (macOS/Linux):
chmod +x gradlew

# Build dari terminal:
./gradlew assembleDebug

# Atau buka di Android Studio dan klik Run ▶️
```
