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

data class TransferItem(val itemId: Int, val diminta: String, val dikirim: String?, val diterima: String?)

data class Transfer(
    val id: Int,
    val fromBranchId: Int,
    val toBranchId: Int,
    val status: String,
    val note: String?,
    val items: List<TransferItem>,
)

data class TransferInput(val itemId: Int, val qty: Double)

data class OpnameLine(val itemId: Int, val sistem: String, val hitung: String, val selisih: String)

data class Opname(
    val id: Int,
    val branchId: Int,
    val status: String,
    val note: String?,
    val lines: List<OpnameLine>,
)

data class OpnameInput(val itemId: Int, val counted: Double)

data class Movement(
    val id: Int,
    val itemId: Int,
    val type: String,
    val qty: String,
    val unitCost: String?,
    val saldoAkhir: String,
    val note: String?,
    val createdAt: String?,
)

data class StockLine(
    val itemId: Int,
    val itemCode: String?,
    val itemName: String?,
    val qty: String,
    val avgCost: String?,
)

data class ExpenseCategory(val id: Int, val name: String)

data class ReturnLine(val saleItemId: Int, val qty: Double, val condition: String)

data class ServiceOrder(
    val id: Int,
    val branchId: Int?,
    val customerId: Int?,
    val assigneeId: Int?,
    val saleId: Int?,
    val status: String,
    val deposit: String?,
    val note: String?,
)

data class OrderInput(val itemId: Int, val qty: Double)

data class Customer(val id: Int, val name: String, val phone: String?, val address: String?)

data class JadwalHari(
    val date: String,
    val shiftName: String?,
    val jam: String?,
    val sumber: String?,
    val libur: Boolean,
)

data class Expense(
    val id: Int,
    val branchId: Int,
    val categoryName: String?,
    val amount: String,
    val method: String?,
    val date: String?,
    val description: String?,
    val status: String,
)

data class Bill(
    val id: Int,
    val amount: String,
    val paid: String,
    val remaining: String,
    val dueAt: String?,
    val lunas: Boolean,
)

data class Receivable(
    val id: Int,
    val branchId: Int?,
    val customerId: Int?,
    val amount: String,
    val paid: String,
    val remaining: String,
    val dueAt: String?,
    val lunas: Boolean,
)

data class ProfitLoss(
    val pendapatan: Double,
    val hpp: Double,
    val kotor: Double,
    val beban: Double,
    val bersih: Double,
)

data class PayrollPeriod(
    val id: Int,
    val name: String,
    val from: String?,
    val to: String?,
    val divisor: Int,
    val status: String,
)
