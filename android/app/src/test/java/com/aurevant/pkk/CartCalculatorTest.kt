package com.aurevant.pkk

import com.aurevant.pkk.domain.model.CartLine
import com.aurevant.pkk.domain.model.CatalogItem
import com.aurevant.pkk.domain.usecase.CartCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class CartCalculatorTest {
    private val oli = CatalogItem(1, "OLI-01", "Oli", "barang", "pcs")

    @Test
    fun diskonPersenLaluPajak() {
        val totals = CartCalculator.lineTotal(CartLine(oli, 50000.0, 2.0, discountPercent = 10.0, taxPercent = 10.0))
        assertEquals(100000.0, totals.gross, 0.001)
        assertEquals(10000.0, totals.discount, 0.001)
        assertEquals(9000.0, totals.tax, 0.001)
        assertEquals(99000.0, totals.grand, 0.001)
    }

    @Test
    fun diskonNominalDipakaiBilaPersenNol() {
        val totals = CartCalculator.lineTotal(CartLine(oli, 50000.0, 1.0, discountAmount = 5000.0))
        assertEquals(45000.0, totals.grand, 0.001)
    }

    @Test
    fun totalKeranjangMenjumlahSemuaBaris() {
        val lines = listOf(
            CartLine(oli, 50000.0, 2.0),
            CartLine(oli, 5000.0, 1.0, discountPercent = 20.0),
        )
        val totals = CartCalculator.cartTotal(lines)
        assertEquals(100000.0 + 5000.0, totals.gross, 0.001)
        assertEquals(1000.0, totals.discount, 0.001)
        assertEquals(104000.0, totals.grand, 0.001)
    }
}
