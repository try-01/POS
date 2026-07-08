package com.kasirku.pos.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.pos.data.local.entity.TransactionEntity
import com.kasirku.pos.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    transactionRepository: TransactionRepository
) : ViewModel() {

    val transactions: StateFlow<List<TransactionEntity>> = transactionRepository
        .allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayTransactions: StateFlow<List<TransactionEntity>> = transactionRepository
        .getTodayTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalRevenue: StateFlow<Long> = transactionRepository
        .totalRevenue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
}
