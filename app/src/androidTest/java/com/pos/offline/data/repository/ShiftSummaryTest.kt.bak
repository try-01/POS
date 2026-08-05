package com.pos.offline.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftSummaryTest {

    @Test
    fun grossProfit_hariNormal_tanpaRetur() {
        val s = ShiftSummary(
            startingCash = 100_000, cashRevenue = 239_000, qrisRevenue = 0,
            totalCost = 160_000, restockedReturnsCost = 0, cashRefunds = 0,
        )
        assertEquals(79_000, s.grossProfit)
        assertEquals(339_000, s.expectedCashInDrawer)
    }

    @Test
    fun grossProfit_harusTurunSetelahRetur_bukanNaik() {
        // Regression guard untuk bug lama: sebelum fix netRevenue, grossProfit MALAH
        // naik saat ada retur bernilai besar (formula lama: totalRevenue - netCost).
        val sebelumRetur = ShiftSummary(
            startingCash = 100_000, cashRevenue = 239_000, qrisRevenue = 0,
            totalCost = 160_000, restockedReturnsCost = 0, cashRefunds = 0,
        )
        val sesudahRetur = ShiftSummary(
            startingCash = 100_000, cashRevenue = 239_000, qrisRevenue = 0,
            totalCost = 160_000, restockedReturnsCost = 20_000, cashRefunds = 50_000,
        )
        assertTrue(
            "Laba Kotor harus TURUN setelah retur, bukan naik",
            sesudahRetur.grossProfit < sebelumRetur.grossProfit,
        )
        assertEquals(49_000, sesudahRetur.grossProfit)
    }

    @Test
    fun warrantyExchangeCost_dikreditKembali_grossProfitTurunSesuaiBiayaGaransi() {
        val tanpaGaransi = ShiftSummary(
            startingCash = 0, cashRevenue = 100_000, qrisRevenue = 0,
            totalCost = 70_000, restockedReturnsCost = 0, cashRefunds = 0,
        )
        val denganGaransiDeltaNol = ShiftSummary(
            startingCash = 0, cashRevenue = 100_000, qrisRevenue = 0,
            totalCost = 70_000, restockedReturnsCost = 0, cashRefunds = 0,
            warrantyExchangeCost = 20_000,
        )
        assertEquals(30_000, tanpaGaransi.grossProfit)
        assertEquals(10_000, denganGaransiDeltaNol.grossProfit) // turun tepat sebesar biaya garansi
    }

    @Test
    fun expectedCashInDrawer_tidakTerpengaruhRefundQris() {
        val s = ShiftSummary(
            startingCash = 100_000, cashRevenue = 100_000, qrisRevenue = 50_000,
            totalCost = 0, restockedReturnsCost = 0, cashRefunds = 0,
        )
        // Refund QRIS tidak pernah masuk field cashRefunds -> laci tidak berkurang.
        assertEquals(200_000, s.expectedCashInDrawer)
    }

    @Test
    fun expectedCashInDrawer_berkurangKarenaQrisCashChangeOut() {
        val s = ShiftSummary(
            startingCash = 100_000, cashRevenue = 0, qrisRevenue = 150_000,
            totalCost = 0, restockedReturnsCost = 0, cashRefunds = 0,
            qrisCashChangeOut = 50_000,
        )
        assertEquals(50_000, s.expectedCashInDrawer)
    }

    @Test
    fun qrisRevenue_tidakDoubleCounting_saatAdaKembalianTunai() {
        // qrisRevenueForShift (SQL) sudah sum(`total`), bukan sum(`paidAmount`) —
        // artinya overpay yang dikembalikan sebagai kembalian TIDAK pernah masuk revenue.
        // netRevenue karenanya TIDAK perlu mengurangi qrisCashChangeOut lagi (sudah netral
        // sejak level query). Test ini mengunci asumsi tsb secara eksplisit di level formula.
        val s = ShiftSummary(
            startingCash = 0, cashRevenue = 0, qrisRevenue = 150_000, // sudah net dari overpay
            totalCost = 120_000, restockedReturnsCost = 0, cashRefunds = 0,
            qrisCashChangeOut = 50_000,
        )
        assertEquals(30_000, s.grossProfit) // 150.000 - 120.000, TANPA pengurangan ganda
        assertEquals(-50_000, s.expectedCashInDrawer) // tapi laci tetap berkurang fisik
    }
}