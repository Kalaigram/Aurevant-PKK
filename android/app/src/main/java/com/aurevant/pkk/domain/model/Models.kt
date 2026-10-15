package com.aurevant.pkk.domain.model

data class User(val id: Int, val name: String, val email: String, val phone: String?, val isSuperAdmin: Boolean)

data class AuthSession(val user: User, val token: String)

data class Company(val id: Int, val name: String, val slug: String, val status: String)

data class Branch(val id: Int, val code: String, val name: String, val timezone: String?)

data class Attendance(
    val id: Int,
    val status: String?,
    val clockInAt: String?,
    val clockOutAt: String?,
    val lateMinutes: Int,
)

data class CatalogItem(val id: Int, val code: String, val name: String, val type: String?, val unit: String?)

data class CartLine(
    val item: CatalogItem,
    val price: Double,
    val qty: Double,
    val discountPercent: Double? = null,
    val discountAmount: Double? = null,
    val taxPercent: Double? = null,
)

data class PayInput(val method: String, val amount: Double, val tendered: Double? = null, val reference: String? = null)

data class Shift(val id: Int, val status: String?, val difference: String?)

data class Sale(
    val id: Int,
    val number: String,
    val status: String,
    val grandTotal: String,
    val subtotal: String,
)

data class Approval(val id: Int, val action: String, val actionLabel: String?, val status: String)

data class Notif(val id: Int, val type: String, val title: String, val body: String?, val read: Boolean)

data class LeaveItem(val id: Int, val type: String, val from: String?, val to: String?, val status: String)

data class SlipLine(val label: String, val amount: Double)

data class Slip(val periodName: String, val from: String?, val to: String?, val status: String?, val lines: List<SlipLine>, val net: Double)

data class EmployeeCard(
    val name: String,
    val company: String?,
    val branch: String?,
    val role: String?,
    val qrContent: String?,
)

data class Dashboard(val companyName: String, val subscriptionStatus: String?, val sections: Map<String, Map<String, String>>)
