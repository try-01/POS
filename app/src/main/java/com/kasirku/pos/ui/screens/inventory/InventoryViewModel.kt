package com.kasirku.pos.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.pos.data.local.entity.ProductEntity
import com.kasirku.pos.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    val products: StateFlow<List<ProductEntity>> = productRepository.allProducts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val lowStockProducts: StateFlow<List<ProductEntity>> = productRepository.lowStockProducts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    private val _formState = MutableStateFlow(ProductFormState())
    val formState: StateFlow<ProductFormState> = _formState.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _editingProduct = MutableStateFlow<ProductEntity?>(null)
    val editingProduct: StateFlow<ProductEntity?> = _editingProduct.asStateFlow()

    private val _event = MutableSharedFlow<InventoryEvent>()
    val event = _event.asSharedFlow()

    fun showAddDialog() {
        _formState.value = ProductFormState()
        _editingProduct.value = null
        _showDialog.value = true
    }

    fun showEditDialog(product: ProductEntity) {
        _formState.value = ProductFormState(
            name = product.name,
            sku = product.sku,
            price = product.sellPrice.toString(),
            stock = product.stock.toString(),
            category = product.category
        )
        _editingProduct.value = product
        _showDialog.value = true
    }

    fun dismissDialog() {
        _showDialog.value = false
        _editingProduct.value = null
    }

    fun updateFormField(field: FormField, value: String) {
        _formState.value = when (field) {
            FormField.NAME -> _formState.value.copy(name = value)
            FormField.SKU -> _formState.value.copy(sku = value)
            FormField.PRICE -> _formState.value.copy(price = value)
            FormField.STOCK -> _formState.value.copy(stock = value)
            FormField.CATEGORY -> _formState.value.copy(category = value)
        }
    }

    fun saveProduct() {
        viewModelScope.launch {
            val form = _formState.value
            val editing = _editingProduct.value

            val product = ProductEntity(
                id = editing?.id ?: 0,
                name = form.name.trim(),
                sku = form.sku.trim(),
                sellPrice = form.price.toLongOrNull() ?: 0L,
                stock = form.stock.toIntOrNull() ?: 0,
                category = form.category.trim(),
                createdAt = editing?.createdAt ?: System.currentTimeMillis()
            )

            val result = if (editing != null) {
                productRepository.updateProduct(product)
            } else {
                productRepository.addProduct(product).map { }
            }

            result.onSuccess {
                _showDialog.value = false
                val msg = if (editing != null) "Produk diperbarui" else "Produk ditambahkan"
                _event.emit(InventoryEvent.ShowSuccess(msg))
            }.onFailure { e ->
                _event.emit(InventoryEvent.ShowError(e.message ?: "Gagal menyimpan"))
            }
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            productRepository.deleteProduct(id)
            _event.emit(InventoryEvent.ShowSuccess("Produk dihapus"))
        }
    }
}

data class ProductFormState(
    val name: String = "",
    val sku: String = "",
    val price: String = "",
    val stock: String = "",
    val category: String = ""
)

enum class FormField { NAME, SKU, PRICE, STOCK, CATEGORY }

sealed class InventoryEvent {
    data class ShowSuccess(val message: String) : InventoryEvent()
    data class ShowError(val message: String) : InventoryEvent()
}
