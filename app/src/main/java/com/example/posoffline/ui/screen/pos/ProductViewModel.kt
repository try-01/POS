package com.example.posoffline.ui.screen.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.posoffline.data.entity.ProductEntity
import com.example.posoffline.data.repository.ProductRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(
    private val repo: ProductRepository
) : ViewModel() {

    val products: StateFlow<List<ProductEntity>> =
        repo.observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun refresh() {
        // observeAll() is a Flow — no manual refresh needed, but we expose
        // this hook in case future logic needs to force-reload (e.g. after
        // importing a catalog).
    }

    fun create(
        sku: String,
        name: String,
        price: Long,
        stock: Int,
        category: String?,
        onDone: (Result<ProductEntity>) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                repo.create(sku, name, price, stock, category)
            }.also(onDone)
        }
    }

    fun update(
        id: String,
        sku: String,
        name: String,
        price: Long,
        stock: Int,
        category: String?,
        onDone: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            runCatching { repo.update(id, sku, name, price, stock, category) }
                .also(onDone)
        }
    }

    fun remove(id: String, onDone: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repo.remove(id) }.also(onDone)
        }
    }

    class Factory(private val repo: ProductRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProductViewModel::class.java))
            return ProductViewModel(repo) as T
        }
    }
}
