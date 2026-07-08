package com.example.posoffline.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.posoffline.data.entity.TransactionEntity
import com.example.posoffline.data.repository.TransactionRepository
import com.example.posoffline.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class TransactionViewModel(
    private val repo: TransactionRepository
) : ViewModel() {

    data class TodaySummary(
        val revenue: Long,
        val count: Int,
        val average: Long
    )

    val list: StateFlow<List<TransactionEntity>> =
        repo.observeAll()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val today: StateFlow<Pair<List<TransactionEntity>, TodaySummary>> =
        repo.observeAll()
            .map { all ->
                val since = startOfToday()
                val todayList = all.filter { it.createdAt >= since }
                val revenue = todayList.sumOf { it.grandTotal }
                val count = todayList.size
                val avg = if (count > 0) revenue / count else 0L
                todayList to TodaySummary(revenue, count, avg)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList<TransactionEntity>() to TodaySummary(0, 0, 0)
            )

    fun format(value: Long, currency: String): String = Money.format(value, currency)

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    class Factory(private val repo: TransactionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TransactionViewModel::class.java))
            return TransactionViewModel(repo) as T
        }
    }
}
