package com.aurevant.pkk.data.repo

import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.unwrap
import com.aurevant.pkk.data.mapper.toDomain
import com.aurevant.pkk.domain.model.Movement
import com.aurevant.pkk.domain.model.Opname
import com.aurevant.pkk.domain.model.OpnameInput
import com.aurevant.pkk.domain.model.StockLine
import com.aurevant.pkk.domain.model.Transfer
import com.aurevant.pkk.domain.model.TransferInput
import com.aurevant.pkk.domain.repo.OpsRepo
import javax.inject.Inject

class OpsRepoImpl @Inject constructor(private val api: ApiService) : OpsRepo {
    override suspend fun stock(company: Int, branch: Int): List<StockLine> =
        unwrap(api.stock(company, branch)).map {
            StockLine(it.itemId, it.itemCode, it.itemName, it.qty, it.avgCost)
        }

    override suspend fun movements(company: Int, branch: Int): List<Movement> =
        unwrap(api.movements(company, branch)).data.map { it.toDomain() }

    override suspend fun createMovement(company: Int, branch: Int, itemId: Int, type: String, qty: Double, unitCost: Double?) {
        val body = mutableMapOf<String, Any?>("item_id" to itemId, "type" to type, "qty" to qty)
        if (type == "pembelian" && unitCost != null) body["unit_cost"] = unitCost
        api.createMovement(company, branch, body)
    }

    override suspend fun transfers(company: Int): List<Transfer> =
        unwrap(api.transfers(company)).data.map { it.toDomain() }

    override suspend fun createTransfer(company: Int, from: Int, to: Int, items: List<TransferInput>, note: String?) {
        api.createTransfer(
            company,
            mapOf(
                "from_branch_id" to from,
                "to_branch_id" to to,
                "items" to items.map { mapOf("item_id" to it.itemId, "qty" to it.qty) },
                "note" to note,
            ),
        )
    }

    override suspend fun transferAct(company: Int, id: Int, aksi: String) {
        when (aksi) {
            "submit" -> api.submitTransfer(company, id)
            "approve" -> api.approveTransfer(company, id)
            "ship" -> api.shipTransfer(company, id)
            "receive" -> api.receiveTransfer(company, id)
            "cancel" -> api.cancelTransfer(company, id)
            else -> throw IllegalArgumentException("Aksi transfer tidak dikenal: $aksi")
        }
    }

    override suspend fun opnames(company: Int, branch: Int): List<Opname> =
        unwrap(api.opnames(company, branch)).data.map { it.toDomain() }

    override suspend fun createOpname(company: Int, branch: Int, lines: List<OpnameInput>, note: String?) {
        api.createOpname(
            company,
            branch,
            mapOf(
                "items" to lines.map { mapOf("item_id" to it.itemId, "qty_counted" to it.counted) },
                "note" to note,
            ),
        )
    }

    override suspend fun opnameAct(company: Int, branch: Int, id: Int, aksi: String) {
        when (aksi) {
            "submit" -> api.submitOpname(company, branch, id)
            "approve" -> api.approveOpname(company, branch, id)
            "reject" -> api.rejectOpname(company, branch, id)
            else -> throw IllegalArgumentException("Aksi opname tidak dikenal: $aksi")
        }
    }
}
