package com.kasirku.pos.ui.screens.receipt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.local.entity.TransactionItemEntity
import com.kasirku.pos.data.repository.TransactionRepository
import com.kasirku.pos.export.PdfReceiptExporter
import com.kasirku.pos.print.BluetoothPrinterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val printerManager: BluetoothPrinterManager,
    private val pdfExporter: PdfReceiptExporter
) : ViewModel() {

    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: 0L

    private val _transaction = MutableStateFlow<TransactionEntity?>(null)
    val transaction: StateFlow<TransactionEntity?> = _transaction.asStateFlow()

    private val _items = MutableStateFlow<List<TransactionItemEntity>>(emptyList())
    val items: StateFlow<List<TransactionItemEntity>> = _items.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ReceiptUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            _transaction.value = transactionRepository.getTransactionById(transactionId)
            _items.value = transactionRepository.getTransactionItems(transactionId)
        }
    }

    fun printReceipt() {
        viewModelScope.launch {
            val tx = _transaction.value ?: return@launch
            val result = printerManager.printReceipt(tx, _items.value)
            result.onSuccess {
                _uiEvent.emit(ReceiptUiEvent.ShowSuccess("Struk berhasil dicetak"))
            }.onFailure { e ->
                _uiEvent.emit(ReceiptUiEvent.ShowError(e.message ?: "Gagal mencetak"))
            }
        }
    }

    fun exportPdf() {
        viewModelScope.launch {
            val tx = _transaction.value ?: return@launch
            val result = pdfExporter.exportToPdf(tx, _items.value)
            result.onSuccess { path ->
                _uiEvent.emit(ReceiptUiEvent.ShowSuccess("PDF disimpan: \$path"))
            }.onFailure { e ->
                _uiEvent.emit(ReceiptUiEvent.ShowError(e.message ?: "Gagal export"))
            }
        }
    }
}

sealed class ReceiptUiEvent {
    data class ShowSuccess(val message: String) : ReceiptUiEvent()
    data class ShowError(val message: String) : ReceiptUiEvent()
}
