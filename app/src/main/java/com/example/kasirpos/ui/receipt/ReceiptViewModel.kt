package com.example.kasirpos.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kasirpos.data.local.entity.TransactionEntity
import com.example.kasirpos.data.local.entity.TransactionItemEntity
import com.example.kasirpos.data.repository.TransactionRepository
import com.example.kasirpos.util.PrinterUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReceiptUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val selectedTransaction: TransactionEntity? = null,
    val selectedItems: List<TransactionItemEntity> = emptyList(),
    val isPrinting: Boolean = false,
    val printResult: String? = null,
    val isLoading: Boolean = true
)

class ReceiptViewModel(
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptUiState())
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transactionRepo.allTransactions.collect { transactions ->
                _uiState.update { it.copy(transactions = transactions, isLoading = false) }
            }
        }
    }

    fun selectTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val items = transactionRepo.getTransactionItems(transaction.id)
            _uiState.update { it.copy(selectedTransaction = transaction, selectedItems = items) }
        }
    }

    /**
     * Cetak struk via Bluetooth.
     * @param printerAddress MAC address printer Bluetooth
     * @param storeName nama toko untuk header struk
     */
    fun printReceipt(printerAddress: String, storeName: String) {
        val tx = _uiState.value.selectedTransaction ?: return
        val items = _uiState.value.selectedItems

        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true, printResult = null) }

            val itemTuples = items.map {
                Triple(it.productName, it.quantity, it.unitPrice)
            }
            val receiptText = PrinterUtil.formatReceiptText(
                storeName = storeName,
                transactionId = tx.id,
                date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("id"))
                    .format(java.util.Date(tx.createdAt)),
                items = itemTuples,
                subtotal = tx.subtotal,
                discount = tx.totalDiscount,
                taxAmount = tx.taxAmount,
                grandTotal = tx.grandTotal,
                cash = tx.cashReceived,
                change = tx.change
            )
            val result = PrinterUtil.printReceipt(printerAddress, receiptText)
            _uiState.update {
                it.copy(
                    isPrinting = false,
                    printResult = result.fold(
                        onSuccess = { "Struk berhasil dicetak ✅" },
                        onFailure = { "Gagal mencetak: ${it.message}" }
                    )
                )
            }
        }
    }

    class Factory(private val transactionRepo: TransactionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiptViewModel(transactionRepo) as T
        }
    }
}
