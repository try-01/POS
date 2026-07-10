# 🏪 Kasir Offline — Point of Sale (POS) untuk Android 16

Aplikasi kasir **100% offline**, dirancang **sangat ringan**, hemat RAM/baterai,
bebas memory leak, dengan UI modern (glassmorphism ringan) berbasis **Jetpack Compose**.

<!-- Ganti OWNER/REPO di bawah dengan path repo GitHub Anda (mis. budi/kasir-offline). -->
[![Migration Test](https://github.com/OWNER/REPO/actions/workflows/migration-test.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/migration-test.yml)

> Target: **Android 16 (API 36)** · Bahasa: **Kotlin** · Arsitektur: **Clean MVVM**

---

## 1. Teknologi & Alasan Performa

| Lapisan | Teknologi | Mengapa dipilih (alasan efisiensi) |
|---|---|---|
| UI | **Jetpack Compose (Material 3)** | Hanya merender yang berubah (skippable), tankan XML inflation. |
| Async | **Coroutines + Flow (StateFlow)** | Cooperative, hemat thread; `Flow` bisa dibatalkan → hemat baterai. |
| DB | **Room (KSP)** | Codegen cepat, query terkompilasi, akses offline instan di SQLite. |
| DI | **ServiceLocator manual (lazy)** | Tanpa runtime reflection/Hilt → start-up cepat & footprint RAM kecil. |
| Koleksi state | `collectAsStateWithLifecycle` | Berhenti mengumpulkan Flow saat background → hemat baterai. |
| Kalkulasi | `Long` (Rupiah penuh) + `combine`/`derivedStateOf` | Bebas error floating-point; hanya recompute saat input berubah. |

---

## 2. Hierarki Struktur Proyek (Clean MVVM)

```
KasirOffline/
├── settings.gradle.kts              # deklarasi plugin & repositori
├── build.gradle.kts                 # root config (plugin block)
├── gradle.properties
└── app/
    ├── build.gradle.kts             # compileSdk 36, Compose, Room/KSP, R8 shrink
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml      # izin Bluetooth + Export PDF, application class
        └── java/com/pos/offline/
            │
            ├── PosApplication.kt        # (ada di ServiceLocator.kt) init DI
            ├── MainActivity.kt          # host Compose + tema
            │
            ├── data/                    # ===== Lapisan DATA (Model) =====
            │   ├── local/
            │   │   ├── PosDatabase.kt          # RoomDatabase singleton
            │   │   ├── entity/
            │   │   │   ├── ProductEntity.kt
            │   │   │   ├── CartItemEntity.kt
            │   │   │   └── TransactionEntities.kt  # Header + Detail
            │   │   └── dao/
            │   │       ├── ProductDao.kt
            │   │       ├── CartDao.kt
            │   │       └── TransactionDao.kt
            │   ├── repository/
            │   │   ├── ProductRepository.kt
            │   │   ├── CartRepository.kt
            │   │   └── TransactionRepository.kt   # logika checkout atomik
            │   └── di/
            │       └── ServiceLocator.kt          # DI manual + ViewModelFactory
            │
            ├── domain/                  # (opsional) use-case pure; di sini di-merge ke VM
            │
            ├── ui/                      # ===== Lapisan UI (View + ViewModel) =====
            │   ├── theme/
            │   │   └── Theme.kt                # Color + Type + Material3 Theme
            │   ├── components/
            │   │   └── GlassCard.kt            # glassmorphism ringan & performant
            │   ├── pos/
            │   │   ├── PosViewModel.kt         # StateFlow, debounce search, totals
            │   │   └── PosScreen.kt            # layar kasir utama (responsif)
            │   ├── inventory/                  # (stub) CRUD produk
            │   ├── receipt/
            │   │   └── ReceiptManager.kt       # ESC/POS Bluetooth + Export PDF/Bitmap
            │   └── MainActivity.kt
            │
            └── util/
                └── Money.kt                   # format Rupiah bebas floating-point
```

**Prinsip pemisahan:** `data` tidak tahu soal UI; `ui` hanya bicara ke `ViewModel`;
`ViewModel` hanya bicara ke `Repository`. Aliran satu arah (**unidirectional data flow**)
mencegah coupling & memory leak.

---

## 3. Cara Menjalankan

1. Buka folder `KasirOffline/` di **Android Studio (Koala/Ladybug+)**.
2. Sync Gradle (KSP & Compose plugin ter-apply otomatis via `settings.gradle.kts`).
3. Jalankan di perangkat **API 26+** (target Android 16 / API 36).
4. Tambahkan beberapa produk via modul Inventaris, lalu transaksi di Layar Kasir.

---

## 4. Catatan Performa & Anti-Memory-Leak

- **Tidak ada konteks Activity/View global** → `applicationContext` dipakai untuk DB.
- **Flow dibatalkan** otomatis saat layar hilang (`WhileSubscribed(5000)`).
- **Lazy list/grid** memakai `key()` + `contentType` → daur ulang slot efisien.
- **Kalkulasi** memakai `Long` & `combine` (bukan `Double`) → tepat & murah.
- **Glassmorphism** disimulasikan dengan gradient+border (bukan `RenderEffect` per-frame),
  agar GPU tetap ringan saat scroll.
- R8 (`isMinifyEnabled`) membuang kode tak terpakai → APK kecil & RAM hemat.

---

## 5. Migrasi Database & Pengujian

Skema Room versi-ke-versi dikelola dengan **migrasi eksplisit** (bukan destruktif):

- **Versi saat ini: 2.** Migrasi `MIGRATION_1_2` (`data/local/Migrations.kt`) menambah
  kolom `cost` (harga modal) pada tabel `products` secara aditif (`ALTER TABLE ... ADD COLUMN`).
- Skema diekspor sebagai JSON ke `app/schemas/` via plugin `id("androidx.room")`.
  **Berkas `1.json` & `2.json` wajib di-commit ke VCS.**
- Database memakai `addMigrations(...)` (data pengguna terjaga saat update).
  `fallbackToDestructiveMigrationOnDowngrade()` hanya untuk skenario downgrade saat develop.

### Uji migrasi (androidTest)
`PosDatabaseMigrationTest` memverifikasi dua jaminan sekaligus:
1. **Skema valid** — `runMigrationsAndValidate` membandingkan hasil migrasi dengan `2.json`
   (gagal = fail-loud bila tidak cocok).
2. **Data terjaga** — baris lama tetap utuh & kolom baru `cost` ber-default `0`.

Jalankan (butuh emulator/perangkat):
```bash
./gradlew :app:connectedDebugAndroidTest
```

> **Jika `1.json` belum ada:** skema `2.json` dibangkitkan otomatis saat build, tapi
> `1.json` hanya ada bila pernah di-build saat database masih `version = 1`. Untuk
> membuatnya: set `version = 1` di `PosDatabase.kt`, build sekali, commit `1.json`,
> lalu kembalikan ke `version = 2`.
