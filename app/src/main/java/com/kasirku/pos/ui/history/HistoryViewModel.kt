package com.kasirku.pos.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.pos.data.local.dao.DailySummary
import com.kasirku.pos.data.local.relation.TransactionWithItems
import com.kasirku.pos.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class TimeRange(val label: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini")
}

data class HistoryUiState(
    val transactions: List<TransactionWithItems> = emptyList(),
    val summary: DailySummary = DailySummary(0, 0.0),
    val selectedRange: TimeRange = TimeRange.TODAY
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val selectedRange = MutableStateFlow(TimeRange.TODAY)

    private val timeBoundsFlow = selectedRange.combine(MutableStateFlow(Unit)) { range, _ ->
        getStartAndEndTime(range)
    }

    val uiState: StateFlow<HistoryUiState> = timeBoundsFlow.flatMapLatest { (start, end) ->
        combine(
            transactionRepository.observeHistory(),
            transactionRepository.observeDailySummary(start, end),
            selectedRange
        ) { allHistory, summary, range ->
            // Filter daftar transaksi sesuai rentang waktu yang dipilih
            val filteredHistory = allHistory.filter { it.transaction.createdAt in start..end }
            HistoryUiState(
                transactions = filteredHistory,
                summary = summary,
                selectedRange = range
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun onTimeRangeChanged(range: TimeRange) {
        selectedRange.value = range
    }

    private fun getStartAndEndTime(range: TimeRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis

        when (range) {
            TimeRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimeRange.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            TimeRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
        }
        return Pair(calendar.timeInMillis, end)
    }
}
