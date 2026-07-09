package com.example.kasirpos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kasirpos.data.local.entity.ProductEntity
import com.example.kasirpos.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryUiState(
    val products: List<ProductEntity> = emptyList(),
    val lowStockProducts: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val showAddEditDialog: Boolean = false,
    val editingProduct: ProductEntity? = null, // null = mode tambah
    val error: String? = null
)

class InventoryViewModel(
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        // Observasi semua produk
        viewModelScope.launch {
            productRepo.allProducts.collect { products ->
                _uiState.update { it.copy(products = products, isLoading = false) }
            }
        }
        // Observasi stok rendah
        viewModelScope.launch {
            productRepo.observeLowStock().collect { lowStock ->
                _uiState.update { it.copy(lowStockProducts = lowStock) }
            }
        }
    }

    // ── CRUD ──────────────────────────────────────────────────

    fun showAddDialog() {
        _uiState.update { it.copy(showAddEditDialog = true, editingProduct = null) }
    }

    fun showEditDialog(product: ProductEntity) {
        _uiState.update { it.copy(showAddEditDialog = true, editingProduct = product) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddEditDialog = false, editingProduct = null) }
    }

    fun saveProduct(name: String, sku: String, price: Long, stock: Int, imageUri: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = _uiState.value.editingProduct
            val product = if (existing != null) {
                existing.copy(
                    name = name, sku = sku, price = price,
                    stock = stock, imageUri = imageUri, updatedAt = now
                )
            } else {
                ProductEntity(
                    name = name, sku = sku, price = price,
                    stock = stock, imageUri = imageUri, createdAt = now, updatedAt = now
                )
            }
            productRepo.upsert(product)
            dismissDialog()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepo.delete(product)
        }
    }

    // ── Factory ───────────────────────────────────────────────

    class Factory(private val productRepo: ProductRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InventoryViewModel(productRepo) as T
        }
    }
}
