package com.example.kasirpos.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kasirpos.data.local.entity.TransactionEntity
import com.example.kasirpos.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class ReportUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val dailyRevenue: Long = 0,
    val dailyTransactionCount: Int = 0,
    val isLoading: Boolean = true
)

class ReportViewModel(
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        // Observasi semua transaksi
        viewModelScope.launch {
            transactionRepo.allTransactions.collect { transactions ->
                _uiState.update { it.copy(transactions = transactions, isLoading = false) }
            }
        }
        // Observasi ringkasan harian
        viewModelScope.launch {
            val (start, end) = getTodayRange()
            transactionRepo.dailySummary(start, end).collect { summary ->
                _uiState.update { it.copy(
                    dailyRevenue = summary.revenue,
                    dailyTransactionCount = summary.count
                )}
            }
        }
    }

    /** Dapatkan timestamp awal & akhir hari ini (00:00 – 23:59) */
    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return start to end
    }

    class Factory(private val transactionRepo: TransactionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportViewModel(transactionRepo) as T
        }
    }
}
