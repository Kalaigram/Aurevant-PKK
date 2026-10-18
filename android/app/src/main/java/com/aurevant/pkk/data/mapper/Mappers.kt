package com.aurevant.pkk.data.mapper

import com.aurevant.pkk.core.network.AttendanceDto
import com.aurevant.pkk.core.network.BillDto
import com.aurevant.pkk.core.network.BranchDto
import com.aurevant.pkk.core.network.CompanyDto
import com.aurevant.pkk.core.network.CustomerDto
import com.aurevant.pkk.core.network.ExpenseDto
import com.aurevant.pkk.core.network.MovementDto
import com.aurevant.pkk.core.network.OpnameDto
import com.aurevant.pkk.core.network.PayrollPeriodDto
import com.aurevant.pkk.core.network.ProfitLossDto
import com.aurevant.pkk.core.network.ReceivableDto
import com.aurevant.pkk.core.network.SaleDto
import com.aurevant.pkk.core.network.ScheduleDayDto
import com.aurevant.pkk.core.network.ServiceOrderFullDto
import com.aurevant.pkk.core.network.TransferDto
import com.aurevant.pkk.core.network.UserDto
import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.model.Bill
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.Company
import com.aurevant.pkk.domain.model.Customer
import com.aurevant.pkk.domain.model.Expense
import com.aurevant.pkk.domain.model.JadwalHari
import com.aurevant.pkk.domain.model.Movement
import com.aurevant.pkk.domain.model.Opname
import com.aurevant.pkk.domain.model.OpnameLine
import com.aurevant.pkk.domain.model.PayrollPeriod
import com.aurevant.pkk.domain.model.ProfitLoss
import com.aurevant.pkk.domain.model.Receivable
import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.model.SaleLine
import com.aurevant.pkk.domain.model.SalePay
import com.aurevant.pkk.domain.model.ServiceOrder
import com.aurevant.pkk.domain.model.Transfer
import com.aurevant.pkk.domain.model.TransferItem
import com.aurevant.pkk.domain.model.User
import com.google.gson.JsonObject

fun UserDto.toDomain() = User(id, name, email, phone, isSuperAdmin)

fun CompanyDto.toDomain() = Company(id, name, slug, status)

fun BranchDto.toDomain() = Branch(id, code, name, timezone)

fun AttendanceDto.toDomain() = Attendance(id, status, clockInAt, clockOutAt, lateMinutes)

fun SaleDto.toDomain() = Sale(
    id, number, status, grandTotal, subtotal, discountTotal, taxTotal,
    items.map { SaleLine(it.itemId, it.qty, it.price, it.lineTotal) },
    payments.map { SalePay(it.method, it.amount, it.tendered) },
)

fun TransferDto.toDomain() = Transfer(
    id, fromBranchId, toBranchId, status, note,
    items.map { TransferItem(it.itemId, it.qtyRequested, it.qtyShipped, it.qtyReceived) },
)

fun OpnameDto.toDomain() = Opname(
    id, branchId, status, note,
    items.map { OpnameLine(it.itemId, it.qtySystem, it.qtyCounted, it.diff) },
)

fun MovementDto.toDomain() = Movement(id, itemId, type, qty, unitCost, balanceAfter, note, createdAt)

fun ServiceOrderFullDto.toDomain() = ServiceOrder(id, branchId, customerId, assigneeId, saleId, status, depositAmount, note)

fun CustomerDto.toDomain() = Customer(id, name, phone, address)

fun ScheduleDayDto.toDomain(): JadwalHari {
    val jam = if (startsAt != null && endsAt != null) "$startsAt–$endsAt" else null
    return JadwalHari(date, shiftName, jam, source, isOff || shiftName == null)
}

fun ExpenseDto.toDomain() = Expense(id, branchId, categoryName, amount, method, date, description, status)

fun BillDto.toDomain() = Bill(id, amount, paidAmount, remaining, dueAt, isPaid)

fun ReceivableDto.toDomain() = Receivable(id, branchId, customerId, amount, paidAmount, remaining, dueAt, isPaid)

fun ProfitLossDto.toDomain() = ProfitLoss(
    pendapatan?.toDoubleOrNull() ?: 0.0,
    hpp?.toDoubleOrNull() ?: 0.0,
    labaKotor?.toDoubleOrNull() ?: 0.0,
    beban?.toDoubleOrNull() ?: 0.0,
    labaBersih?.toDoubleOrNull() ?: 0.0,
)

fun PayrollPeriodDto.toDomain() = PayrollPeriod(id, name, from, to, divisor, status)

private fun JsonObject.str(key: String): String? =
    if (has(key) && !get(key).isJsonNull) get(key).asString else null

fun dashboardOf(root: JsonObject): com.aurevant.pkk.domain.model.Dashboard {
    val company = if (root.has("company")) root.getAsJsonObject("company") else JsonObject()
    val sub = if (root.has("subscription")) root.getAsJsonObject("subscription") else JsonObject()
    val sections = mutableMapOf<String, Map<String, String>>()
    for (key in listOf("keuangan", "penjualan", "operasional", "sdm", "penggajian", "karyawan")) {
        if (root.has(key) && root.get(key).isJsonObject) {
            val map = mutableMapOf<String, String>()
            for ((k, v) in root.getAsJsonObject(key).entrySet()) {
                map[k] = if (v.isJsonNull) "" else v.asString
            }
            sections[key] = map
        }
    }
    return com.aurevant.pkk.domain.model.Dashboard(
        companyName = company.str("name") ?: "",
        subscriptionStatus = sub.str("status"),
        sections = sections,
    )
}

fun slipOf(root: JsonObject): com.aurevant.pkk.domain.model.Slip {
    val period = if (root.has("period")) root.getAsJsonObject("period") else JsonObject()
    val lines = mutableListOf<com.aurevant.pkk.domain.model.SlipLine>()
    if (root.has("lines") && root.get("lines").isJsonArray) {
        for (el in root.getAsJsonArray("lines")) {
            if (!el.isJsonObject) continue
            val o = el.asJsonObject
            lines += com.aurevant.pkk.domain.model.SlipLine(
                label = o.str("label") ?: "-",
                amount = o.str("amount")?.toDoubleOrNull() ?: 0.0,
            )
        }
    }
    return com.aurevant.pkk.domain.model.Slip(
        periodName = period.str("name") ?: "",
        from = period.str("from"),
        to = period.str("to"),
        status = period.str("status"),
        lines = lines,
        net = root.str("net")?.toDoubleOrNull() ?: 0.0,
    )
}
