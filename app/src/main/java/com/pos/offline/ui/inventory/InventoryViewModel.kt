package com.pos.offline.ui.inventory

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.repository.ProductRepository
import com.pos.offline.data.repository.ReportRepository
import com.pos.offline.util.ExcelImportResult
import com.pos.offline.util.ExcelManager
import com.pos.offline.util.ExcelOutcome
import com.pos.offline.util.ImportedProductRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDate
import java.time.ZoneId

data class ProductFormState(
    val id: Long = 0L,
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val category: String = "",
    val price: Long = 0L,
    val cost: Long = 0L,
    val stock: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isNew: Boolean get() = id == 0L
}

enum class ProductSortOption(val label: String) {
    NAME_ASC("Nama (A-Z)"),
    NAME_DESC("Nama (Z-A)"),
    RECENTLY_EDITED("Terakhir Diedit"),
    RECENTLY_ADDED("Terakhir Ditambahkan"),
    STOCK_LOW_FIRST("Stok Terendah"),
    TERLARIS("Terlaris"),
    ;

    val comparator: Comparator<ProductEntity>
        get() = when (this) {
            NAME_ASC -> compareBy { it.name.lowercase() }
            NAME_DESC -> compareByDescending { it.name.lowercase() }
            RECENTLY_EDITED -> compareByDescending { it.updatedAt }
            RECENTLY_ADDED -> compareByDescending { it.createdAt }
            STOCK_LOW_FIRST -> compareBy { it.stock }
            TERLARIS -> compareBy { it.id } // Fallback, tidak dipakai karena pakai DB Query
        }
}

enum class TopSalesRange(val label: String) {
    HARI_INI("Hari Ini"),
    BULAN_INI("Bulan Ini")
}

// Top-level agar bisa dipakai ulang di InventoryScreen.kt (form scanner inline)
// tanpa perlu melewati referensi ViewModel.
internal fun sanitizeScannedCode(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val cleaned = raw.trim().filter { c -> c.isLetterOrDigit() || c in "-_./: #" }.take(128)
    return cleaned.ifBlank { null }
}

@OptIn(kotlinx.coroutines.FlowPreview::class, ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val appContext: Context,
    private val productRepository: ProductRepository,
    private val reportRepository: ReportRepository,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(ProductSortOption.NAME_ASC)
    val sortOption: StateFlow<ProductSortOption> = _sortOption.asStateFlow()

    private val _topSalesRange = MutableStateFlow(TopSalesRange.HARI_INI)
    val topSalesRange: StateFlow<TopSalesRange> = _topSalesRange.asStateFlow()

    // Debounce HANYA diterapkan pada sumber query pencarian, bukan pada hasil
    // combine gabungan (query, sort, range). Delay dinamis:
    // - query kosong (initial load / pencarian dihapus) -> 0ms, instan.
    // - query terisi -> 180ms, hindari query DB tiap keystroke.
    // Perubahan sort/range tidak lagi ikut ter-delay karena tidak digabung
    // SEBELUM debounce seperti versi lama.
    private val debouncedSearchQuery: Flow<String> =
        _searchQuery
            .debounce { query -> if (query.isBlank()) 0L else 180L }
            .distinctUntilChanged()

    val products: StateFlow<List<ProductEntity>> =
        combine(debouncedSearchQuery, _sortOption, _topSalesRange) { query, sort, range -> Triple(query, sort, range) }
            .distinctUntilChanged()
            .flatMapLatest { (query, sort, range) ->
                if (sort == ProductSortOption.TERLARIS) {
                    val (start, end) = getRangeMillis(range)
                    reportRepository.observeProductsByTopSales(start, end).map { dbList ->
                        if (query.isBlank()) dbList else dbList.filter { it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true) }
                    }
                } else {
                    productRepository.search(query).map { list -> list.sortedWith(sort.comparator) }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> =
        productRepository
            .observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow<ProductFormState?>(null)
    val form: StateFlow<ProductFormState?> = _form.asStateFlow()

    private val _pendingDelete = MutableStateFlow<ProductEntity?>(null)
    val pendingDelete: StateFlow<ProductEntity?> = _pendingDelete.asStateFlow()

    // Snapshot produk yang sedang diedit, agar hapus/simpan tidak bergantung
    // pada `products.value` yang bisa saja sudah difilter/di-sort sehingga
    // tidak memuat produk tsb (mis. saat search query aktif atau mode "Terlaris").
    private var editingProductSnapshot: ProductEntity? = null

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _messages = Channel<String>(capacity = Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    data class ScanNotFoundState(val barcode: String)
    private val _scanNotFound = MutableStateFlow<ScanNotFoundState?>(null)
    val scanNotFound: StateFlow<ScanNotFoundState?> = _scanNotFound.asStateFlow()

    // Opsi C: produk dengan barcode yang di-scan DITEMUKAN tapi berstatus
    // soft-deleted (active = false). Dipisah dari ScanNotFoundState agar UI
    // bisa menawarkan "Pulihkan produk ini?" alih-alih "Tambah produk baru"
    // yang ujung-ujungnya akan gagal karena barcode masih dianggap terpakai.
    data class DeletedProductFoundState(val product: ProductEntity)
    private val _deletedProductFound = MutableStateFlow<DeletedProductFoundState?>(null)
    val deletedProductFound: StateFlow<DeletedProductFoundState?> = _deletedProductFound.asStateFlow()

    enum class ImportStatus { NEW, CONFLICT, DUPLICATE_IN_FILE }
    data class ImportReviewItem(val row: ImportedProductRow, val status: ImportStatus, val conflictWith: ProductEntity? = null)
    data class ExcelUiState(
        val isExporting: Boolean = false,
        val isImporting: Boolean = false,
        val isCommitting: Boolean = false,
        val reviewItems: List<ImportReviewItem> = emptyList(),
        val parseErrors: List<String> = emptyList(),
        val showReviewDialog: Boolean = false,
    )

    private val _excelState = MutableStateFlow(ExcelUiState())
    val excelState: StateFlow<ExcelUiState> = _excelState.asStateFlow()

    private fun getRangeMillis(range: TopSalesRange): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)
        return when (range) {
            TopSalesRange.HARI_INI -> {
                val start = now.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }
            TopSalesRange.BULAN_INI -> {
                val start = now.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }
        }
    }

    fun setTopSalesRange(range: TopSalesRange) { _topSalesRange.value = range }

    fun dismissReviewDialog() {
        _excelState.value = _excelState.value.copy(showReviewDialog = false, reviewItems = emptyList(), parseErrors = emptyList())
    }

    fun exportToExcel(destinationUri: Uri) {
        if (_excelState.value.isExporting) return
        viewModelScope.launch {
            _excelState.value = _excelState.value.copy(isExporting = true)
            try {
                val products = productRepository.getAllProductsOnce()
                if (products.isEmpty()) { notify("Tidak ada produk untuk diekspor."); return@launch }
                when (val result = ExcelManager.exportProducts(appContext, products, destinationUri)) {
                    is ExcelOutcome.Success -> notify("Berhasil mengekspor ${products.size} produk ke Excel.")
                    is ExcelOutcome.Error -> notify("Gagal ekspor: ${result.throwable.message ?: "kesalahan tak dikenal"}")
                }
            } catch (e: Exception) { notify("Gagal ekspor: ${e.message ?: "kesalahan tak dikenal"}") }
            finally { _excelState.value = _excelState.value.copy(isExporting = false) }
        }
    }

    fun importFromExcel(sourceUri: Uri) {
        if (_excelState.value.isImporting) return
        viewModelScope.launch {
            _excelState.value = _excelState.value.copy(isImporting = true)
            try {
                val result: ExcelImportResult = ExcelManager.importProducts(appContext, sourceUri)
                if (result.rows.isEmpty() && result.errors.isEmpty()) { notify("File Excel kosong atau tidak ada data valid."); return@launch }
                val reviewItems = validateImportedRows(result.rows)
                _excelState.value = _excelState.value.copy(reviewItems = reviewItems, parseErrors = result.errors, showReviewDialog = true)
            } catch (e: Exception) { notify("Gagal membaca file: ${e.message ?: "format tidak didukung"}") }
            finally { _excelState.value = _excelState.value.copy(isImporting = false) }
        }
    }

    private suspend fun validateImportedRows(rows: List<ImportedProductRow>): List<ImportReviewItem> {
        val barcodeCounts = rows.mapNotNull { it.barcode }.groupingBy { it }.eachCount()
        val skuCounts = rows.groupingBy { it.sku }.eachCount()
        // Batas konkurensi disengaja KECIL (4) dan pakai Semaphore (bukan
        // chunk statis) supaya:
        // 1. AMAN untuk file impor sangat besar (ribuan baris) — async
        //    dibuat untuk SEMUA baris sekaligus, tapi hanya 4 yang benar2
        //    berjalan bersamaan; sisanya antre otomatis lewat Semaphore.
        //    Coroutine itu ringan (bukan thread), jadi tidak akan membebani
        //    memori meski jumlah baris sangat banyak.
        // 2. Tidak ada batch-stall: baris lambat di satu grup tidak lagi
        //    menahan grup berikutnya (beda dari pendekatan chunked+awaitAll
        //    sebelumnya).
        // 3. Aman terlepas dari konfigurasi WAL PosDatabase: kalau WAL aktif
        //    -> dapat speedup nyata dari query paralel; kalau tidak -> Room
        //    tetap menyerialisasi akses secara internal, hasilnya seperti
        //    sekuensial tapi TIDAK crash/corrupt.
        val concurrencyLimit = Semaphore(4)
        return coroutineScope {
            rows
                .map { row ->
                    async(Dispatchers.IO) {
                        concurrencyLimit.withPermit {
                            val duplicateInFile =
                                (row.barcode != null && (barcodeCounts[row.barcode] ?: 0) > 1) || (skuCounts[row.sku] ?: 0) > 1
                            val dbConflict =
                                row.barcode?.let { productRepository.getProductByBarcodeAny(it) }
                                    ?: productRepository.getProductBySku(row.sku)
                            val status = when {
                                duplicateInFile -> ImportStatus.DUPLICATE_IN_FILE
                                dbConflict != null -> ImportStatus.CONFLICT
                                else -> ImportStatus.NEW
                            }
                            ImportReviewItem(row, status, dbConflict)
                        }
                    }
                }.awaitAll()
        }
    }

    fun commitImport() {
        if (_excelState.value.isCommitting) return
        val newRows = _excelState.value.reviewItems.filter { it.status == ImportStatus.NEW }.map { it.row }
        if (newRows.isEmpty()) { notify("Tidak ada produk baru yang bisa diimpor (semua konflik/duplikat)."); return }
        viewModelScope.launch {
            _excelState.value = _excelState.value.copy(isCommitting = true)
            try {
                val now = System.currentTimeMillis()
                val toInsert = newRows.map { row ->
                    ProductEntity(id=0, name=row.name, sku=row.sku, barcode=row.barcode, category=row.category ?: "", price=row.price, cost=row.cost, stock=row.stock, active=true, createdAt=now, updatedAt=now)
                }
                productRepository.bulkInsert(toInsert)
                notify("Berhasil mengimpor ${toInsert.size} produk baru.")
                dismissReviewDialog()
            } catch (e: SQLiteConstraintException) { notify("Gagal impor: ada SKU/barcode dobel yang lolos validasi.") }
            catch (e: Exception) { notify("Gagal impor: ${e.message ?: "kesalahan tak dikenal"}") }
            finally { _excelState.value = _excelState.value.copy(isCommitting = false) }
        }
    }

    /**
     * Opsi C: intersepsi status barcode SEJAK SCAN, bukan saat "Simpan".
     * - Barcode tidak ditemukan sama sekali -> alur lama (tawarkan tambah produk baru).
     * - Barcode ditemukan & aktif -> buka form edit seperti biasa.
     * - Barcode ditemukan tapi produk sudah soft-deleted -> tawarkan pemulihan,
     *   supaya user tidak capek isi form baru lalu ditolak saat Simpan.
     */
    fun onBarcodeScanned(raw: String?) {
        val sanitized = sanitizeScannedCode(raw)
        if (sanitized == null) { notify("Gagal memindai kode. Coba pindai ulang."); return }
        viewModelScope.launch {
            val product =
                try {
                    productRepository.getProductByBarcodeAny(sanitized)
                } catch (e: Exception) {
                    notify("Gagal memindai: ${e.message ?: "kesalahan tak dikenal"}.")
                    return@launch
                }
            when {
                product == null -> _scanNotFound.value = ScanNotFoundState(sanitized)
                product.active -> startEdit(product)
                else -> _deletedProductFound.value = DeletedProductFoundState(product)
            }
        }
    }

    fun dismissScanNotFound() { _scanNotFound.value = null }
    fun startAddFromScanned() {
        val barcode = _scanNotFound.value?.barcode ?: return
        _scanNotFound.value = null
        editingProductSnapshot = null
        _form.value = ProductFormState(barcode = barcode)
    }

    fun dismissDeletedProductFound() { _deletedProductFound.value = null }

    /**
     * User setuju memulihkan produk yang sudah soft-deleted: aktifkan kembali
     * lalu langsung buka form edit agar kasir bisa perbarui harga/stok sebelum
     * dipakai lagi (data lama seperti nama/kategori tetap dipertahankan).
     */
    fun restoreDeletedProduct() {
        val target = _deletedProductFound.value?.product ?: return
        _deletedProductFound.value = null
        viewModelScope.launch {
            try {
                productRepository.setActive(target.id, true)
                val refreshed = productRepository.getById(target.id) ?: target.copy(active = true)
                startEdit(refreshed)
                notify("Produk \"${refreshed.name}\" dipulihkan. Perbarui datanya jika perlu, lalu simpan.")
            } catch (e: Exception) {
                notify("Gagal memulihkan produk: ${e.message ?: "kesalahan tak dikenal"}.")
            }
        }
    }

    fun search(q: String) { _searchQuery.value = q }
    fun setSortOption(option: ProductSortOption) { _sortOption.value = option }

    fun startAdd() {
        editingProductSnapshot = null
        _form.value = ProductFormState()
    }

    fun startEdit(product: ProductEntity) {
        editingProductSnapshot = product
        _form.value = ProductFormState(id=product.id, name=product.name, sku=product.sku, barcode=product.barcode ?: "", category=product.category, price=product.price, cost=product.cost, stock=product.stock, createdAt=product.createdAt)
    }

    fun dismissForm() { _form.value = null }

    fun save(state: ProductFormState) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val name = state.name.trim()
                if (name.isBlank()) { notify("Nama produk wajib diisi."); return@launch }
                if (state.price < 0) { notify("Harga tidak boleh negatif."); return@launch }
                if (state.stock < 0.0) { notify("Stok tidak boleh negatif."); return@launch }

                val sku = state.sku.trim().ifBlank { "SKU-${System.currentTimeMillis()}" }
                val barcode = state.barcode.trim().ifBlank { null }
                val category = state.category.trim()
                val now = System.currentTimeMillis()

                val entity = ProductEntity(id=state.id, name=name, sku=sku, barcode=barcode, category=category, price=state.price, cost=state.cost, stock=state.stock, active=true, createdAt=if (state.isNew) now else state.createdAt, updatedAt=now)
                productRepository.save(entity)
                notify(if (state.isNew) "Produk ditambahkan." else "Produk diperbarui.")
                _form.value = null
            } catch (e: SQLiteConstraintException) {
                notify("Gagal menyimpan: SKU atau Barcode sudah dipakai produk lain.")
            } catch (e: Exception) {
                notify("Gagal menyimpan: ${e.message ?: "kesalahan tak dikenal"}.")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun cancelDelete() { _pendingDelete.value = null }
    fun confirmDelete() = viewModelScope.launch {
        val target = _pendingDelete.value ?: return@launch
        try { productRepository.softDelete(target.id); notify("Produk \"${target.name}\" dihapus.") }
        catch (e: Exception) { notify("Gagal menghapus: ${e.message ?: "kesalahan tak dikenal"}.") }
        finally { _pendingDelete.value = null }
    }

    fun requestDeleteFromForm(id: Long) {
        // Prioritaskan snapshot produk yang sedang diedit (selalu akurat),
        // baru fallback ke list yang sedang tampil (bisa saja sudah difilter/disortir).
        val target = editingProductSnapshot?.takeIf { it.id == id }
            ?: products.value.find { it.id == id }
        if (target == null) {
            notify("Produk tidak ditemukan (mungkin sudah dihapus atau tersaring dari daftar).")
            return
        }
        _form.value = null
        _pendingDelete.value = target
    }

    private fun notify(text: String) { _messages.trySend(text) }

    suspend fun checkBarcodeConflict(barcode: String, excludeId: Long): String? {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return null
        val existing = productRepository.getProductByBarcodeAny(trimmed)
        return if (existing != null && existing.id != excludeId) existing.name else null
    }

    suspend fun checkSkuConflict(sku: String, excludeId: Long): String? {
        val trimmed = sku.trim()
        if (trimmed.isBlank()) return null
        val existing = productRepository.getProductBySku(trimmed)
        return if (existing != null && existing.id != excludeId) existing.name else null
    }
}
