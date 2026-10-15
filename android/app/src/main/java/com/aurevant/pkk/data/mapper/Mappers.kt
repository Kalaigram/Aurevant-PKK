package com.aurevant.pkk.data.mapper

import com.aurevant.pkk.core.network.AttendanceDto
import com.aurevant.pkk.core.network.BranchDto
import com.aurevant.pkk.core.network.CompanyDto
import com.aurevant.pkk.core.network.SaleDto
import com.aurevant.pkk.core.network.UserDto
import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.Company
import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.model.User
import com.google.gson.JsonObject

fun UserDto.toDomain() = User(id, name, email, phone, isSuperAdmin)

fun CompanyDto.toDomain() = Company(id, name, slug, status)

fun BranchDto.toDomain() = Branch(id, code, name, timezone)

fun AttendanceDto.toDomain() = Attendance(id, status, clockInAt, clockOutAt, lateMinutes)

fun SaleDto.toDomain() = Sale(id, number, status, grandTotal, subtotal)

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
