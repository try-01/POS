package com.pos.offline.data.repository
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pos.offline.data.local.PosDatabase
import com.pos.offline.data.local.entity.CartItemEntity
import com.pos.offline.data.local.entity.CashierEntity
import com.pos.offline.data.local.entity.DiscountType
import com.pos.offline.data.local.entity.PaymentMethod
import com.pos.offline.data.local.entity.ProductEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class AccountingWalkthroughTest {
    private lateinit var db: PosDatabase
    private lateinit var productRepository: ProductRepository
    private lateinit var cashierRepository: CashierRepository
    private lateinit var shiftRepository: ShiftRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var returnRepository: ReturnRepository
    private lateinit var reportRepository: ReportRepository
    private var idA = 0L
    private var idB = 0L
    private var idC = 0L
    private var idD = 0L
    private var idAsing = 0L
    private var cashierId = 0L
    private var shiftId = 0L
    private val reportStart = 0L
    private val reportEnd = Long.MAX_VALUE
    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        productRepository = ProductRepository(db.productDao())
        cashierRepository = CashierRepository(db.cashierDao())
        shiftRepository = ShiftRepository(db.shiftDao())
        transactionRepository = TransactionRepository(db, db.transactionDao(), db.cartDao(), db.productDao(), shiftRepository)
        returnRepository = ReturnRepository(db, db.returnDao(), db.transactionDao(), db.productDao())
        reportRepository = ReportRepository(db.reportDao())
        idA = productRepository.save(ProductEntity(name = "Produk A", sku = "A", price = 50_000, cost = 20_000, stock = 100.0))
        idB = productRepository.save(ProductEntity(name = "Produk B", sku = "B", price = 100_000, cost = 70_000, stock = 100.0))
        idC = productRepository.save(ProductEntity(name = "Produk C", sku = "C", price = 80_000, cost = 50_000, stock = 100.0))
        idD = productRepository.save(ProductEntity(name = "Produk D", sku = "D", price = 150_000, cost = 120_000, stock = 100.0))
        idAsing = productRepository.save(ProductEntity(name = "Produk Asing", sku = "ASING", price = 50_000, cost = 25_000, stock = 100.0))
        cashierId = cashierRepository.save(CashierEntity(name = "Kasir Uji"))
        val outcome = shiftRepository.startShift(cashierId, "Kasir Uji", startingCash = 100_000)
        shiftId = (outcome as ShiftStartOutcome.Success).shiftId
    }
    @After
    fun tearDown() {
        db.close()
    }
    private fun cartOf(productId: Long, name: String, price: Long, qty: Double = 1.0) =
        listOf(CartItemEntity(productId = productId, name = name, unitPrice = price, quantity = qty))
    private suspend fun assertShiftAndReport(
        label: String,
        expectedGrossProfit: Long,
        expectedCashRevenue: Long,
        expectedQrisRevenue: Long,
        expectedCashRefunds: Long,
        expectedEstimasiLaci: Long,
        expectedJumlahTransaksi: Int,
        expectedPenjualanKotor: Long,
        expectedDiskon: Long,
        expectedPajak: Long,
        expectedBiayaGaransi: Long,
        expectedPendapatanBersih: Long,
        expectedLabaBersih: Long,
    ) {
        val summary = shiftRepository.getShiftSummary(shiftId)
        assertEquals("[$label] Laba Kotor (shift)", expectedGrossProfit, summary.grossProfit)
        assertEquals("[$label] Penjualan Tunai (shift)", expectedCashRevenue, summary.cashRevenue)
        assertEquals("[$label] Penjualan QRIS (shift)", expectedQrisRevenue, summary.qrisRevenue)
        assertEquals("[$label] Refund Tunai (shift)", expectedCashRefunds, summary.cashRefunds)
        assertEquals("[$label] Estimasi di Laci (shift)", expectedEstimasiLaci, summary.expectedCashInDrawer)
        val report = reportRepository.buildSalesReport(reportStart, reportEnd, includeProducts = false)
        assertEquals("[$label] Jumlah Transaksi (laporan)", expectedJumlahTransaksi, report.summary.transactionCount)
        assertEquals("[$label] Penjualan Kotor (laporan)", expectedPenjualanKotor, report.summary.subtotalSum)
        assertEquals("[$label] Diskon (laporan)", expectedDiskon, report.diskon)
        assertEquals("[$label] Pajak (laporan)", expectedPajak, report.summary.taxSum)
        assertEquals("[$label] Biaya Klaim Garansi (laporan)", expectedBiayaGaransi, report.biayaGaransi)
        assertEquals("[$label] Pendapatan Bersih (laporan)", expectedPendapatanBersih, report.pendapatanBersih)
        assertEquals("[$label] Laba Bersih (laporan)", expectedLabaBersih, report.labaBersih)
    }
    @Test
    fun fullWalkthrough_skenario1_sampai_13() = runTest {
        val tx1 = transactionRepository.checkout(
            cart = cartOf(idA, "Produk A", 50_000),
            discountType = DiscountType.NOMINAL, discountValue = 0.0, taxRate = 0.0,
            paid = 50_000, paymentMethod = PaymentMethod.CASH,
            cashierId = cashierId, cashierName = "Kasir Uji", shiftId = shiftId,
        )
        assertShiftAndReport(
            label = "Skenario 1",
            expectedGrossProfit = 30_000, expectedCashRevenue = 50_000, expectedQrisRevenue = 0,
            expectedCashRefunds = 0, expectedEstimasiLaci = 150_000,
            expectedJumlahTransaksi = 1, expectedPenjualanKotor = 50_000,
            expectedDiskon = 0, expectedPajak = 0, expectedBiayaGaransi = 0,
            expectedPendapatanBersih = 50_000, expectedLabaBersih = 30_000,
        )
        val tx2 = transactionRepository.checkout(
            cart = cartOf(idB, "Produk B", 100_000),
            discountType = DiscountType.PERCENT, discountValue = 10.0, taxRate = 0.0,
            paid = 90_000, paymentMethod = PaymentMethod.CASH,
            cashierId = cashierId, cashierName = "Kasir Uji", shiftId = shiftId,
        )
        assertShiftAndReport(
            label = "Skenario 2",
            expectedGrossProfit = 50_000, expectedCashRevenue = 140_000, expectedQrisRevenue = 0,
            expectedCashRefunds = 0, expectedEstimasiLaci = 240_000,
            expectedJumlahTransaksi = 2, expectedPenjualanKotor = 150_000,
            expectedDiskon = 10_000, expectedPajak = 0, expectedBiayaGaransi = 0,
            expectedPendapatanBersih = 140_000, expectedLabaBersih = 50_000,
        )
        val tx3 = transactionRepository.checkout(
            cart = cartOf(idB, "Produk B", 100_000),
            discountType = DiscountType.PERCENT, discountValue = 10.0, taxRate = 0.10,
            paid = 100_000, paymentMethod = PaymentMethod.CASH,
            cashierId = cashierId, cashierName = "Kasir Uji", shiftId = shiftId,
        )
        assertShiftAndReport(
            label = "Skenario 3",
            expectedGrossProfit = 79_000, expectedCashRevenue = 239_000, expectedQrisRevenue = 0,
            expectedCashRefunds = 0, expectedEstimasiLaci = 339_000,
            expectedJumlahTransaksi = 3, expectedPenjualanKotor = 250_000,
            expectedDiskon = 20_000, expectedPajak = 9_000, expectedBiayaGaransi = 0,
            expectedPendapatanBersih = 239_000, expectedLabaBersih = 79_000,
        )
        val receipt1 = transactionRepository.loadReceipt(tx1.transaction.id)!!
        returnRepository.processReturn(
            transactionId = tx1.transaction.id,
            itemInputs = receipt1.items.map {
                ReturnItemInput(
                    transactionItemId = it.id, productId = it.productId, productName = it.productName,
                    unitPrice = it.unitPrice, quantityReturned = it.quantity, restocked = true,
                )
            },
            refundAmount = 50_000, refundMethod = PaymentMethod.CASH,
            shiftId = shiftId, cashierId = cashierId, cashierName = "Kasir Uji",
        )
        assertShiftAndReport(
            label = "Skenario 4",
            expectedGrossProfit = 49_000, expectedCashRevenue = 239_000, expectedQrisRevenue = 0,
            expectedCashRefunds = 50_000, expectedEstimasiLaci = 289_000,
            expectedJumlahTransaksi = 3, expectedPenjualanKotor = 250_000,
            expectedDiskon = 20_000, expectedPajak = 9_000, expectedBiayaGaransi = 0,
            expectedPendapatanBersih = 189_000, expectedLabaBersih = 49_000,
        )
        val receipt2 = transactionRepository.loadReceipt(tx2.transaction.id)!!
        returnRepository.processReturn(
            transactionId = tx2.transaction.id,
            itemInputs = receipt2.items.map {
                ReturnItemInput(
                    transactionItemId = it.id, productId = it.productId, productName = it.productName,
                    unitPrice = it.unitPrice, quantityReturned = it.quantity, restocked = true,
                )
            },
            refundAmount = 80_000, refundMethod = PaymentMethod.CASH,
            shiftId = shiftId, cashierId = cashierId, cashierName = "Kasir Uji",
        )
        assertShiftAndReport(
            label = "Skenario 5",
            expectedGrossProfit = 39_000, expectedCashRevenue = 239_000, expectedQrisRevenue = 0,
            expectedCashRefunds = 130_000, expectedEstimasiLaci = 209_000,
            expectedJumlahTransaksi = 3, expectedPenjualanKotor = 250_000,
            expectedDiskon = 20_000, expectedPajak = 9_000, expectedBiayaGaransi = 0,
            expectedPendapatanBersih = 109_000, expectedLabaBersih = 39_000,
        )
        val productA = productRepository.getById(idA)!!
        returnRepository.processDirectExchangeWarranty(
            brokenProduct = productA, brokenQty = 1.0,
            replacementProduct = productA, replacementQty = 1.0,
            shiftId = shiftId, cashierId = cashierId, cashierName = "Kasir Uji", note = "Skenario 6",
        )
        assertShiftAndReport(
            label = "Skenario 6",
            expectedGrossProfit = 19_000, expectedCashRevenue = 239_000, expectedQrisRevenue = 0,
            expectedCashRefunds = 130_000, expectedEstimasiLaci = 209_000,
            expectedJumlahTransaksi = 3, expectedPenjualanKotor = 250_000,
            expectedDiskon = 20_000, expectedPajak = 9_000, expectedBiayaGaransi = 20_000,
            expectedPendapatanBersih = 109_000, expectedLabaBersih = 19_000,
        )
        val productAsing = productRepository.getById(idAsing)!!
        returnRepository.processDirectExchangeWarranty(
            brokenProduct = productA, brokenQty = 1.0,
            replacementProduct = productAsing, replacementQty = 1.0,
            shiftId = shiftId, cashierId = cashierId, cashierName = "Kasir Uji", note = "Skenario 7",
        )
        assertShiftAndReport(
            label = "Skenario 7",
            expectedGrossProfit = -6_000, expectedCashRevenue = 239_000, expectedQrisRevenue = 0,
            expectedCashRefunds = 130_000, expectedEstimasiLaci = 209_000,
            expectedJumlahTransaksi = 3, expectedPenjualanKotor = 250_000,
            expectedDiskon = 20_000, expectedPajak = 9_000, expectedBiayaGaransi = 45_000,
            expectedPendapatanBersih = 109_000, expectedLabaBersih = -6_000,
        )
        val productB = productRepository.getById(idB)!!
        val productC = productRepository.getById(idC)!!
        returnRepository.processDirectExchangeWarranty(
            brokenProduct = productB, brokenQty = 1.0,
            replacementProduct = productC, replacementQty = 1.0,
            shiftId = shiftId, cashierId = cashierId, cashierName = "Kasir Uji", note = "Skenario 8",
        )
        assertShiftAndReport(
            label = "Skenario 8",
            expectedGrossProfit = -76_000, expectedCashRevenue = 239_000, expectedQrisRevenue = 0,
            expectedCashRefunds = 150_000, expectedEstimasiLaci = 189_000,
            expectedJumlahTransaksi = 3, expectedPenjualanKotor = 250_000,
            expectedDiskon = 20_000, expectedPajak = 9_000, expectedBiayaGaransi = 95_000,
            expectedPendapatanBersih = 89_000, expectedLabaBersih = -76_000,
        )
        val productD = productRepository.getById(idD)!!
        returnRepository.processDirectExchangeWarranty(
            brokenProduct = productC, brokenQty = 1.0,
            replacementProduct = productD, replacementQty = 1.0,
            shiftId = shiftId, cashierId = cashierId, cashierName = "Kasir Uji", note = "Skenario 9",
        )
        assertShiftAndReport(
            label = "Skenario 9",
            expectedGrossProfit = -126_000, expectedCashRevenue = 309_000, expectedQrisRevenue = 0,
            expectedCashRefunds = 150_000, expectedEstimasiLaci = 259_000,
            expectedJumlahTransaksi = 4, expectedPenjualanKotor = 400_000,
            expectedDiskon = 100_000, expectedPajak = 9_000, expectedBiayaGaransi = 95_000,
            expectedPendapatanBersih = 159_000, expectedLabaBersih = -126_000,
        )
        val tx10 = transactionRepository.checkout(
            cart = cartOf(idAsing, "Produk Asing", 50_000),
            discountType = DiscountType.NOMINAL, discountValue = 0.0, taxRate = 0.0,
            paid = 50_000, paymentMethod = PaymentMethod.QRIS,
            cashierId = cashierId, cashierName = "Kasir Uji", shiftId = shiftId,
        )
        assertShiftAndReport(
            label = "Skenario 10",
            expectedGrossProfit = -101_000, expectedCashRevenue = 309_000, expectedQrisRevenue = 50_000,
            expectedCashRefunds = 150_000, expectedEstimasiLaci = 259_000,
            expectedJumlahTransaksi = 5, expectedPenjualanKotor = 450_000,
            expectedDiskon = 100_000, expectedPajak = 9_000, expectedBiayaGaransi = 95_000,
            expectedPendapatanBersih = 209_000, expectedLabaBersih = -101_000,
        )
        transactionRepository.checkout(
            cart = cartOf(idD, "Produk D", 150_000),
            discountType = DiscountType.NOMINAL, discountValue = 0.0, taxRate = 0.0,
            paid = 200_000, paymentMethod = PaymentMethod.QRIS,
            cashierId = cashierId, cashierName = "Kasir Uji", shiftId = shiftId,
            changeGivenOverride = null,
            changeGivenInCash = true,
        )
        assertShiftAndReport(
            label = "Skenario 11",
            expectedGrossProfit = -71_000, expectedCashRevenue = 309_000, expectedQrisRevenue = 200_000,
            expectedCashRefunds = 150_000,
            expectedEstimasiLaci = 209_000,
            expectedJumlahTransaksi = 6, expectedPenjualanKotor = 600_000,
            expectedDiskon = 100_000, expectedPajak = 9_000, expectedBiayaGaransi = 95_000,
            expectedPendapatanBersih = 359_000, expectedLabaBersih = -71_000,
        )
        val receipt3 = transactionRepository.loadReceipt(tx3.transaction.id)!!
        returnRepository.processReturn(
            transactionId = tx3.transaction.id,
            itemInputs = receipt3.items.map {
                ReturnItemInput(
                    transactionItemId = it.id, productId = it.productId, productName = it.productName,
                    unitPrice = it.unitPrice, quantityReturned = it.quantity, restocked = true,
                )
            },
            refundAmount = 99_000, refundMethod = PaymentMethod.QRIS,
            shiftId = shiftId, cashierId = cashierId, cashierName = "Kasir Uji",
        )
        assertShiftAndReport(
            label = "Skenario 12",
            expectedGrossProfit = -100_000,
            expectedCashRevenue = 309_000, expectedQrisRevenue = 200_000,
            expectedCashRefunds = 150_000,
            expectedEstimasiLaci = 209_000,
            expectedJumlahTransaksi = 6, expectedPenjualanKotor = 600_000,
            expectedDiskon = 100_000, expectedPajak = 9_000, expectedBiayaGaransi = 95_000,
            expectedPendapatanBersih = 260_000, expectedLabaBersih = -100_000,
        )
        val summaryAfter12 = shiftRepository.getShiftSummary(shiftId)
        val reportAfter12 = reportRepository.buildSalesReport(reportStart, reportEnd, includeProducts = false)
        assertEquals(
            "[Skenario 12] grossProfit (shift) harus SAMA dengan labaBersih (laporan)",
            reportAfter12.labaBersih, summaryAfter12.grossProfit,
        )
        val receipt10 = transactionRepository.loadReceipt(tx10.transaction.id)!!
        returnRepository.processReturn(
            transactionId = tx10.transaction.id,
            itemInputs = receipt10.items.map {
                ReturnItemInput(
                    transactionItemId = it.id, productId = it.productId, productName = it.productName,
                    unitPrice = it.unitPrice, quantityReturned = it.quantity, restocked = true,
                )
            },
            refundAmount = 50_000, refundMethod = PaymentMethod.CASH,
            shiftId = shiftId, cashierId = cashierId, cashierName = "Kasir Uji",
        )
        assertShiftAndReport(
            label = "Skenario 13",
            expectedGrossProfit = -125_000,
            expectedCashRevenue = 309_000, expectedQrisRevenue = 200_000,
            expectedCashRefunds = 200_000,
            expectedEstimasiLaci = 159_000,
            expectedJumlahTransaksi = 6, expectedPenjualanKotor = 600_000,
            expectedDiskon = 100_000, expectedPajak = 9_000, expectedBiayaGaransi = 95_000,
            expectedPendapatanBersih = 210_000, expectedLabaBersih = -125_000,
        )
        val summaryAfter13 = shiftRepository.getShiftSummary(shiftId)
        val reportAfter13 = reportRepository.buildSalesReport(reportStart, reportEnd, includeProducts = false)
        assertEquals(
            "[Skenario 13] grossProfit (shift) harus SAMA dengan labaBersih (laporan)",
            reportAfter13.labaBersih, summaryAfter13.grossProfit,
        )
    }
    @Test
    fun grossProfitShift_harusSamaDenganLabaBersihLaporan_saatAdaRefundQris() = runTest {
        val tx = transactionRepository.checkout(
            cart = cartOf(idB, "Produk B", 100_000),
            discountType = DiscountType.NOMINAL, discountValue = 0.0, taxRate = 0.0,
            paid = 100_000, paymentMethod = PaymentMethod.CASH,
            cashierId = cashierId, cashierName = "Kasir Uji", shiftId = shiftId,
        )
        val receipt = transactionRepository.loadReceipt(tx.transaction.id)!!
        returnRepository.processReturn(
            transactionId = tx.transaction.id,
            itemInputs = receipt.items.map {
                ReturnItemInput(
                    transactionItemId = it.id, productId = it.productId, productName = it.productName,
                    unitPrice = it.unitPrice, quantityReturned = it.quantity, restocked = true,
                )
            },
            refundAmount = 100_000, refundMethod = PaymentMethod.QRIS,
            shiftId = shiftId, cashierId = cashierId, cashierName = "Kasir Uji",
        )
        val summary = shiftRepository.getShiftSummary(shiftId)
        val report = reportRepository.buildSalesReport(reportStart, reportEnd, includeProducts = false)
        assertEquals(
            "grossProfit (shift) harus SAMA dengan labaBersih (laporan) — keduanya P&L yang sama",
            report.labaBersih, summary.grossProfit,
        )
    }
}
