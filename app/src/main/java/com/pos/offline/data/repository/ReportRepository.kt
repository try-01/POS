package com.pos.offline.data.repository

import com.pos.offline.data.local.dao.PaymentMethodSummary
import com.pos.offline.data.local.dao.ProfitAndItemsSummary
import com.pos.offline.data.local.dao.ProductSalesRow
import com.pos.offline.data.local.dao.ReportDao
import com.pos.offline.data.local.dao.SalesSummary

data class SalesReportData(
    val summary: SalesSummary,
    val profitItems: ProfitAndItemsSummary,
    val payments: List<PaymentMethodSummary>,
    val returnsTotal: Long,
    val diskon: Long,
    val pendapatanBersih: Long,
    val labaBersih: Long,
    val products: List<ProductSalesRow> = emptyList()
)

class ReportRepository(private val reportDao: ReportDao) {
    suspend fun buildSalesReport(start: Long, end: Long, includeProducts: Boolean = false): SalesReportData {
        val summary = reportDao.getSalesSummary(start, end)
        val profitItems = reportDao.getProfitAndItemsSummary(start, end)
        val payments = reportDao.getPaymentMethodSummary(start, end)
        val returns = reportDao.getReturnsTotal(start, end)
        
        val diskon = summary.subtotalSum + summary.taxSum - summary.totalSum
        val pendapatanBersih = summary.totalSum - returns
        
        val restockedCost = reportDao.getRestockedReturnsCost(start, end)
        val netCogs = profitItems.costSum - restockedCost
        val labaBersih = pendapatanBersih - netCogs

        val products = if (includeProducts) reportDao.getTopSellingProducts(start, end, activeOnly = false) else emptyList()

        return SalesReportData(summary, profitItems, payments, returns, diskon, pendapatanBersih, labaBersih, products)
    }

    fun observeProductsByTopSales(start: Long, end: Long): kotlinx.coroutines.flow<List<com.pos.offline.data.local.entity.ProductEntity>> {
        return reportDao.observeProductsByTopSales(start, end)
    }
}