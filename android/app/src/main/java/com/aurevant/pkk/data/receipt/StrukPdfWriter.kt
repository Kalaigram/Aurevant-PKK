package com.aurevant.pkk.data.receipt

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.aurevant.pkk.domain.model.Sale
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrukPdfWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun write(sale: Sale, companyName: String, branchName: String, names: Map<Int, String>): File {
        val doc = PdfDocument()
        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = 0xFF1A1A1A.toInt() }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = 0xFF616161.toInt() }
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            isFakeBoldText = true
            color = 0xFF1A1A1A.toInt()
        }
        val right = Paint(regular).apply { textAlign = Paint.Align.RIGHT }
        val rightBold = Paint(bold).apply { textAlign = Paint.Align.RIGHT; textSize = 12f }
        val hairline = Paint().apply { strokeWidth = 0.7f; color = 0xFF9E9E9E.toInt() }

        val width = 595
        val height = 842
        val margin = 40f
        val rowH = 18f
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
        fun money(v: String?): String = "Rp" + formatter.format(v?.toDoubleOrNull() ?: 0.0)
        fun qty(v: String): String = v.toDoubleOrNull()?.let {
            if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
        } ?: v

        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNo).create())
        var canvas = page.canvas
        var y = margin

        fun newPage() {
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNo).create())
            canvas = page.canvas
            y = margin
        }

        fun ensure(needed: Float) {
            if (y + needed > height - margin) newPage()
        }

        fun divider() {
            ensure(rowH)
            canvas.drawLine(margin, y - 5f, width - margin, y - 5f, hairline)
            y += rowH
        }

        fun row(left: String, rightText: String, lp: Paint = regular, rp: Paint = right, indent: Float = 0f) {
            ensure(rowH)
            canvas.drawText(left, margin + indent, y, lp)
            canvas.drawText(rightText, width - margin, y, rp)
            y += rowH
        }

        canvas.drawText(companyName.ifBlank { "Aurevant" }, margin, y, Paint(bold).apply { textSize = 18f })
        y += 24f
        canvas.drawText("Cabang $branchName", margin, y, small)
        y += 24f
        divider()
        row("Nota ${sale.number}", sale.status, bold, rightBold)
        divider()

        if (sale.lines.isEmpty()) {
            row("(rincian item tidak tersedia)", "")
        } else {
            sale.lines.forEach { l ->
                ensure(rowH * 2)
                canvas.drawText(names[l.itemId] ?: "Item #${l.itemId}", margin, y, regular)
                canvas.drawText(money(l.lineTotal), width - margin, y, right)
                y += rowH * 0.8f
                canvas.drawText("  ${qty(l.qty)} x ${money(l.price)}", margin, y, small)
                y += rowH * 1.2f
            }
        }
        divider()
        row("Subtotal", money(sale.subtotal))
        if ((sale.discountTotal.toDoubleOrNull() ?: 0.0) != 0.0) row("Diskon", "-" + money(sale.discountTotal))
        if ((sale.taxTotal.toDoubleOrNull() ?: 0.0) != 0.0) row("Pajak", money(sale.taxTotal))
        divider()
        row("TOTAL", money(sale.grandTotal), bold, rightBold)
        if (sale.payments.isNotEmpty()) {
            divider()
            canvas.drawText("Pembayaran", margin, y, small)
            y += rowH
            sale.payments.forEach { p -> row(p.method.replaceFirstChar { c -> c.uppercase() }, money(p.amount)) }
        }
        divider()
        canvas.drawText("Terima kasih telah berbelanja.", margin, y, small)

        doc.finishPage(page)
        val file = File(context.cacheDir, "struk-${sale.number}.pdf")
        file.outputStream().use { out -> doc.writeTo(out) }
        doc.close()
        return file
    }
}
