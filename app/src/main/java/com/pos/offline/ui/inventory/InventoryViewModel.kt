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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductFormState(
    val id: Long = 0L,
    val name: String = "",
    val sku: String = "",
    val price: Long = 0L,
    val cost: Long = 0L,
    val stock: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isNew: Boolean get() = id == 0L
}

/** Opsi urutan daftar produk. Sorting dilakukan in-memory (bukan query DB ulang)
 *  karena hasil pencarian sudah ada di RAM — jauh lebih murah & instan. */
enum class ProductSortOption(val label: String) {
    NAME_ASC("Nama (A-Z)"),
    NAME_DESC("Nama (Z-A)"),
    RECENTLY_EDITED("Terakhir Diedit"),
    RECENTLY_ADDED("Terakhir Ditambahkan"),
    STOCK_LOW_FIRST("Stok Terendah");

    val comparator: Comparator<ProductEntity>
        get() = when (this) {
            NAME_ASC -> compareBy { it.name.lowercase() }
            NAME_DESC -> compareByDescending { it.name.lowercase() }
            RECENTLY_EDITED -> compareByDescending { it.updatedAt }
            RECENTLY_ADDED -> compareByDescending { it.createdAt }
            STOCK_LOW_FIRST -> compareBy { it.stock }
        }
}

@OptIn(kotlinx.coroutines.FlowPreview::class, ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(ProductSortOption.NAME_ASC)
    val sortOption: StateFlow<ProductSortOption> = _sortOption.asStateFlow()

    // Hasil mentah dari DB (belum diurutkan sesuai pilihan user).
    private val rawProducts = _searchQuery
        .debounce(180)
        .distinctUntilChanged()
        .flatMapLatest { productRepository.search(it) }

    // Digabung dengan pilihan sort → hanya recompute saat salah satu berubah.
    // Sorting di sini murni operasi CPU ringan (List.sortedWith), tidak menyentuh DB.
    val products: StateFlow<List<ProductEntity>> = combine(rawProducts, _sortOption) { list, sort ->
        list.sortedWith(sort.comparator)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow<ProductFormState?>(null)
    val form: StateFlow<ProductFormState?> = _form.asStateFlow()

    private val _pendingDelete = MutableStateFlow<ProductEntity?>(null)
    val pendingDelete: StateFlow<ProductEntity?> = _pendingDelete.asStateFlow()

    private val _messages = Channel<String>(capacity = Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun search(q: String) { _searchQuery.value = q }
    fun setSortOption(option: ProductSortOption) { _sortOption.value = option }

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

    fun save(state: ProductFormState) = viewModelScope.launch {
        val name = state.name.trim()
        if (name.isBlank()) { notify("Nama produk wajib diisi."); return@launch }
        if (state.price < 0) { notify("Harga tidak boleh negatif."); return@launch }
        if (state.stock < 0) { notify("Stok tidak boleh negatif."); return@launch }

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
        _messages.trySend(text)
    }
}