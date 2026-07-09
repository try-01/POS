# KasirKu — Aplikasi POS/Kasir 100% Offline (Android)

Aplikasi kasir ringan, cepat, dan hemat baterai, dibangun 100% offline untuk target **Android 16 (compileSdk/targetSdk 36)**.

## Tech Stack
- **Bahasa:** Kotlin
- **Arsitektur:** MVVM (Model-View-ViewModel) — clean separation antara `data`, `domain`, dan `ui`
- **UI:** Jetpack Compose (Material 3) — declarative, rendering efisien via recomposition minimal
- **Database:** Room (WAL mode, index pada kolom yang sering di-query, Flow untuk observasi reaktif)
- **Async:** Kotlin Coroutines + Flow (StateFlow/SharedFlow, `WhileSubscribed(5000)` agar collector berhenti otomatis saat tidak ada observer → hemat CPU/baterai)
- **DI:** Hilt (siklus hidup objek terkelola, mencegah memory leak dari singleton yang salah scope)

## Prinsip Performa & Efisiensi
1. **Tidak ada polling** — semua data reaktif via Room `Flow`, emit otomatis saat data berubah.
2. **Keranjang belanja disimpan di memori** (bukan di Room) selama sesi kasir berjalan → menghindari I/O disk berulang setiap kali qty berubah. Baru dipersist ke database saat checkout, sebagai satu operasi atomik (`@Transaction`).
3. **Debounce pencarian produk (300ms)** → query database tidak dieksekusi pada setiap ketikan huruf.
4. **Glassmorphism ringan tanpa real-time blur** — efek "kaca" dibuat dari alpha compositing (warna semi-transparan + border tipis), BUKAN `RenderEffect`/blur GPU yang mahal. Tetap elegan, tapi menjaga frame rate tinggi (60–120fps) bahkan di perangkat low-end.
5. **Key-based LazyColumn/LazyVerticalGrid** — Compose hanya me-recompose item yang benar-benar berubah.
6. **Soft delete produk** — histori transaksi lama tetap valid walau produk "dihapus".
7. **Room `withTransaction`** — checkout (potong stok + simpan transaksi + simpan item) berjalan atomik; jika gagal di tengah jalan, otomatis rollback → data selalu konsisten walau tanpa koneksi internet.

## Struktur Direktori (Package)

```
app/src/main/java/com/kasirku/pos/
│
├── KasirApplication.kt                 # @HiltAndroidApp entry point DI
├── MainActivity.kt                     # Single Activity, hosting Compose UI
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt              # RoomDatabase, WAL mode, singleton instance
│   │   ├── dao/
│   │   │   ├── ProductDao.kt
│   │   │   └── TransactionDao.kt
│   │   ├── entity/
│   │   │   ├── ProductEntity.kt
│   │   │   └── TransactionEntity.kt    # berisi TransactionEntity + TransactionItemEntity
│   │   └── relation/
│   │       └── TransactionWithItems.kt # @Relation header transaksi + item detail
│   │
│   └── repository/
│       ├── ProductRepository.kt        # CRUD produk (Single Source of Truth)
│       ├── CartRepository.kt           # Manajemen keranjang in-memory (StateFlow)
│       └── TransactionRepository.kt    # Checkout atomik + laporan/riwayat
│
├── domain/
│   └── model/
│       └── CartItem.kt                 # Domain model keranjang (bukan entity DB)
│
├── di/
│   └── AppModule.kt                    # Hilt module: Database, Dao, Repository
│
├── ui/
│   ├── theme/
│   │   └── Theme.kt                    # Color scheme, dynamic color (Material You)
│   │
│   ├── pos/                            # Layar Kasir utama (modul wajib pada tugas ini)
│   │   ├── PosViewModel.kt
│   │   └── PosScreen.kt
│   │
│   ├── inventory/                      # (pola sama dgn pos/) CRUD Produk — Tambah/Edit/Hapus/List
│   │   ├── InventoryViewModel.kt
│   │   └── InventoryScreen.kt
│   │
│   ├── history/                        # Riwayat transaksi & ringkasan pendapatan harian
│   │   ├── HistoryViewModel.kt
│   │   └── HistoryScreen.kt
│   │
│   └── receipt/                        # Cetak struk
│       ├── EscPosCommandBuilder.kt     # Builder byte command ESC/POS
│       ├── BluetoothPrinterManager.kt  # Koneksi & kirim data ke printer Bluetooth
│       └── PdfReceiptExporter.kt       # Ekspor struk ke PDF via android.graphics.pdf
│
└── util/
    └── CurrencyFormatter.kt            # Format Rupiah konsisten di seluruh app
```

> Catatan: Folder `inventory/`, `history/`, dan `receipt/` mengikuti pola arsitektur yang identik dengan
> `pos/` (Entity → Dao → Repository → ViewModel → Compose Screen). Pada deliverable kode ini kami fokus
> memberi implementasi lengkap untuk **Produk & Keranjang (POS)** sesuai permintaan, sebagai referensi pola
> untuk modul lainnya.

## Alur Data (MVVM)
```
Room DB (SQLite)  <──Flow──  Repository  <──Flow/StateFlow──  ViewModel  <──State──  Compose UI
                                  ▲                                │
                                  └──────── suspend fun (event) ───┘
```
UI hanya mengirim **event** (klik, ketik) ke ViewModel, ViewModel memanggil Repository (suspend/Flow),
dan UI hanya membaca **state** (`StateFlow`) — satu arah, mudah ditelusuri, minim bug & leak.

## Izin (AndroidManifest)
- `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (API 31+) — untuk cetak struk via printer Bluetooth ESC/POS.
- Tidak ada izin INTERNET — aplikasi memang didesain 100% offline.

## Menjalankan
1. Buka proyek di Android Studio (versi terbaru, mendukung Kotlin 2.x & Compose Compiler Plugin).
2. Sync Gradle — dependency ada di `app/build.gradle.kts`.
3. `minSdk 26` (Android 8.0) — `compileSdk`/`targetSdk 36` (Android 16).
4. Jalankan di emulator/perangkat fisik, tidak perlu koneksi internet sama sekali.
