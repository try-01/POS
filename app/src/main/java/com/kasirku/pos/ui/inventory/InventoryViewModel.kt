package com.kasirku.pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.pos.data.local.entity.ProductEntity
import com.kasirku.pos.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val products: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Semua",
    val categories: List<String> = listOf("Semua", "Minuman", "Makanan", "Camilan", "Umum")
)

sealed class InventoryEvent {
    data class Success(val message: String) : InventoryEvent()
    data class Error(val message: String) : InventoryEvent()
}

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("Semua")

    private val _event = MutableSharedFlow<InventoryEvent>()
    val event = _event.asSharedFlow()

    private val rawProductsFlow = searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query -> productRepository.observeProducts(query) }

    val uiState: StateFlow<InventoryUiState> = combine(
        rawProductsFlow, searchQuery, selectedCategory
    ) { products, query, category ->
        val filtered = if (category == "Semua") {
            products
        } else {
            products.filter { it.category.equals(category, ignoreCase = true) }
        }
        val allCategories = listOf("Semua") + products.map { it.category }.distinct().filter { it.isNotBlank() }
        InventoryUiState(
            products = filtered,
            searchQuery = query,
            selectedCategory = category,
            categories = allCategories.distinct()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InventoryUiState()
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        selectedCategory.value = category
    }

    fun saveProduct(
        id: Long?,
        sku: String,
        name: String,
        sellPrice: Double,
        costPrice: Double,
        stock: Int,
        category: String
    ) {
        viewModelScope.launch {
            if (id == null || id == 0L) {
                // Tambah produk baru
                val result = productRepository.addProduct(sku, name, sellPrice, costPrice, stock, category)
                result.fold(
                    onSuccess = { _event.emit(InventoryEvent.Success("Produk \"$name\" berhasil ditambahkan")) },
                    onFailure = { _event.emit(InventoryEvent.Error(it.message ?: "Gagal menambah produk")) }
                )
            } else {
                // Update produk existing
                val existing = productRepository.getProduct(id)
                if (existing != null) {
                    val updated = existing.copy(
                        sku = sku.trim(),
                        name = name.trim(),
                        sellPrice = sellPrice,
                        costPrice = costPrice,
                        stock = stock,
                        category = category.trim()
                    )
                    productRepository.updateProduct(updated)
                    _event.emit(InventoryEvent.Success("Produk \"$name\" berhasil diperbarui"))
                }
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.deleteProduct(product.id)
            _event.emit(InventoryEvent.Success("Produk \"${product.name}\" dihapus"))
        }
    }

    fun adjustStock(product: ProductEntity, delta: Int) {
        viewModelScope.launch {
            val newStock = (product.stock + delta).coerceAtLeast(0)
            productRepository.updateProduct(product.copy(stock = newStock))
        }
    }
}
