package com.pos.offline.ui.inventory

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.offline.data.local.entity.ProductEntity
import com.pos.offline.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State form untuk tambah/edit produk.
 * Konvensi: `id == 0` berarti produk baru (akan auto-generate PK oleh Room).
 */
data class ProductFormState(
    val id: Long = 0L,
    val name: String = "",
    val sku: String = "",
    val price: Long = 0L,
    val cost: Long = 0L,        // harga modal/beli (kolom `cost`, ditambah di v2)
    val stock: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isNew: Boolean get() = id == 0L
}

/**
 * ViewModel modul Inventaris.
 *
 * Prinsip performa sama dengan PosViewModel:
 *  - [products] dipublikasikan sebagai StateFlow dengan [SharingStarted.WhileSubscribed]
 *    → query DB berhenti otomatis saat layar tak terlihat (hemat baterai).
 *  - Pencarian di-debounce + flatMapLatest → tak query di tiap ketikan.
 *  - Umpan balik ke UI (sukses/gagal) dikirim lewat [Channel] satu-kali, BUKAN
 *    StateFlow, agar tidak terpicu ulang saat recompose (anti flicker).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val products: StateFlow<List<ProductEntity>> = _searchQuery
        .debounce(180)                                  // tunda query → hemat beban DB
        .distinctUntilChanged()
        .flatMapLatest { productRepository.search(it) } // ganti query, batalkan lama
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Produk yang sedang ditambah/diedit; null = dialog tertutup.
    private val _form = MutableStateFlow<ProductFormState?>(null)
    val form: StateFlow<ProductFormState?> = _form.asStateFlow()

    // Produk yang menunggu konfirmasi hapus; null = tidak ada.
    private val _pendingDelete = MutableStateFlow<ProductEntity?>(null)
    val pendingDelete: StateFlow<ProductEntity?> = _pendingDelete.asStateFlow()

    // Channel pesan satu-kali untuk Snackbar (BUFFERED → tak memblokir pengirim).
    private val _messages = Channel<String>(capacity = Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    // ---------- Aksi UI ----------

    fun search(q: String) { _searchQuery.value = q }

    fun startAdd() { _form.value = ProductFormState() }

    fun startEdit(product: ProductEntity) {
        _form.value = ProductFormState(
            id = product.id,
            name = product.name,
            sku = product.sku,
            price = product.price,
            cost = product.cost,
            stock = product.stock,
            createdAt = product.createdAt
        )
    }

    fun dismissForm() { _form.value = null }

    /**
     * Simpan (tambah bila baru / edit bila sudah ada). Validasi dasar di sini;
     * keunikan SKU dijamin oleh unique index DB → ditangkap sebagai
     * [SQLiteConstraintException] dan diterjemahkan jadi pesan ramah.
     */
    fun save(state: ProductFormState) = viewModelScope.launch {
        val name = state.name.trim()
        if (name.isBlank()) { notify("Nama produk wajib diisi."); return@launch }
        if (state.price < 0) { notify("Harga tidak boleh negatif."); return@launch }
        if (state.stock < 0) { notify("Stok tidak boleh negatif."); return@launch }

        // SKU kosong → buat otomatis berbasis waktu agar tak bentrok unique index.
        val sku = state.sku.trim().ifBlank { "SKU-${System.currentTimeMillis()}" }
        val now = System.currentTimeMillis()

        val entity = ProductEntity(
            id = state.id,
            name = name,
            sku = sku,
            price = state.price,
            cost = state.cost,
            stock = state.stock,
            active = true,
            // Pertahankan createdAt asli saat edit; pakai sekarang saat baru.
            createdAt = if (state.isNew) now else state.createdAt,
            updatedAt = now
        )

        try {
            productRepository.save(entity)
            notify(if (state.isNew) "Produk ditambahkan." else "Produk diperbarui.")
            _form.value = null
        } catch (e: SQLiteConstraintException) {
            notify("SKU \"$sku\" sudah dipakai produk lain.")
        } catch (e: Exception) {
            notify("Gagal menyimpan: ${e.message ?: "kesalahan tak dikenal"}.")
        }
    }

    fun requestDelete(product: ProductEntity) { _pendingDelete.value = product }
    fun cancelDelete() { _pendingDelete.value = null }

    fun confirmDelete() = viewModelScope.launch {
        val target = _pendingDelete.value ?: return@launch
        try {
            productRepository.softDelete(target.id)
            notify("Produk \"${target.name}\" dihapus.")
        } catch (e: Exception) {
            notify("Gagal menghapus: ${e.message}.")
        } finally {
            _pendingDelete.value = null
        }
    }

    private fun notify(text: String) {
        // trySend: non-blocking; aman walau tak ada penerima (buffer menampung).
        _messages.trySend(text)
    }
}
