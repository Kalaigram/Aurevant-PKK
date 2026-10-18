package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.CartLine

/** Hitungan keranjang murni (tanpa Android), cermin aturan server: diskon % ATAU nominal, lalu pajak. */
object CartCalculator {
    data class Totals(val gross: Double, val discount: Double, val tax: Double, val grand: Double)

    fun lineTotal(line: CartLine): Totals {
        val gross = line.price * line.qty
        val discount = if ((line.discountPercent ?: 0.0) > 0) {
            gross * (line.discountPercent ?: 0.0) / 100.0
        } else {
            line.discountAmount ?: 0.0
        }
        val taxable = (gross - discount).coerceAtLeast(0.0)
        val tax = taxable * (line.taxPercent ?: 0.0) / 100.0
        return Totals(gross, discount, tax, taxable + tax)
    }

    fun cartTotal(lines: List<CartLine>): Totals = lines
        .map(::lineTotal)
        .fold(Totals(0.0, 0.0, 0.0, 0.0)) { acc, t ->
            Totals(acc.gross + t.gross, acc.discount + t.discount, acc.tax + t.tax, acc.grand + t.grand)
        }
}
