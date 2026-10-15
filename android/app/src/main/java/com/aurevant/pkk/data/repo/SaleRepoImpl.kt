package com.aurevant.pkk.data.repo

import android.content.Context
import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.SaleItemRequest
import com.aurevant.pkk.core.network.SalePaymentRequest
import com.aurevant.pkk.core.network.SaleRequest
import com.aurevant.pkk.core.network.unwrap
import com.aurevant.pkk.data.mapper.toDomain
import com.aurevant.pkk.domain.model.CartLine
import com.aurevant.pkk.domain.model.CatalogItem
import com.aurevant.pkk.domain.model.PayInput
import com.aurevant.pkk.domain.model.ReturnLine
import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.model.Shift
import com.aurevant.pkk.domain.repo.SaleRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class SaleRepoImpl @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) : SaleRepo {
    override suspend fun items(company: Int, type: String?): List<CatalogItem> =
        unwrap(api.items(company, type)).data.map {
            CatalogItem(it.id, it.code, it.name, it.type, it.unit)
        }

    override suspend fun price(company: Int, item: Int, branch: Int): Double =
        unwrap(api.itemPrice(company, item, branch)).price.toDouble()

    override suspend fun openShift(company: Int, branch: Int, cash: Double): Shift {
        val data = unwrap(api.openShift(company, branch, mapOf("opening_cash" to cash)))
        return Shift(data.id, data.status, data.difference)
    }

    override suspend fun closeShift(company: Int, branch: Int, shift: Int, counted: Double): Shift {
        val data = unwrap(api.closeShift(company, branch, shift, mapOf("closing_cash_counted" to counted)))
        return Shift(data.id, data.status, data.difference)
    }

    override suspend fun createSale(company: Int, branch: Int, lines: List<CartLine>, pays: List<PayInput>, key: String): Sale {
        val body = SaleRequest(
            idempotencyKey = key,
            items = lines.map {
                SaleItemRequest(it.item.id, it.qty, it.discountPercent, it.discountAmount, it.taxPercent)
            },
            payments = pays.map { SalePaymentRequest(it.method, it.amount, it.tendered, it.reference) },
        )
        return unwrap(api.createSale(company, branch, body)).toDomain()
    }

    override suspend fun receiptPdf(company: Int, branch: Int, sale: Int, number: String): File {
        val response = api.receiptPdf(company, branch, sale)
        if (!response.isSuccessful) throw com.aurevant.pkk.core.network.ApiError(response.code(), "Gagal mengunduh struk.")
        val file = File(context.cacheDir, "struk-$number.pdf")
        response.body()?.byteStream()?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }

    override suspend fun notas(company: Int, branch: Int): List<Sale> =
        unwrap(api.sales(company, branch)).data.map { it.toDomain() }

    override suspend fun notaDetail(company: Int, branch: Int, sale: Int): Sale =
        unwrap(api.saleDetail(company, branch, sale)).toDomain()

    override suspend fun voidSale(company: Int, branch: Int, sale: Int, reason: String) {
        api.voidSale(company, branch, sale, mapOf("reason" to reason))
    }

    override suspend fun createReturn(company: Int, branch: Int, sale: Int, lines: List<ReturnLine>, reason: String, method: String) {
        api.createReturn(
            company,
            branch,
            sale,
            mapOf(
                "items" to lines.map { mapOf("sale_item_id" to it.saleItemId, "qty" to it.qty, "condition" to it.condition) },
                "reason" to reason,
                "refund_method" to method,
            ),
        )
    }
}
