package com.aurevant.pkk.core.network

import com.google.gson.annotations.SerializedName

data class AuthData(
    val user: UserDto,
    val token: String,
)

data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val isSuperAdmin: Boolean = false,
)

data class CompanyDto(
    val id: Int,
    val name: String,
    val slug: String,
    val status: String,
)

data class PermissionsData(
    val permissions: List<String> = emptyList(),
)

data class BranchDto(
    val id: Int,
    val code: String,
    val name: String,
    val type: String?,
    val timezone: String?,
    val status: String?,
)

data class QrData(
    val token: String,
    @SerializedName("expires_in") val expiresIn: Int = 0,
)

data class AttendanceDto(
    val id: Int,
    val branchId: Int?,
    val date: String?,
    val status: String?,
    val clockInAt: String?,
    val clockOutAt: String?,
    val lateMinutes: Int = 0,
)

data class PageMeta(
    val total: Int = 0,
    @SerializedName("per_page") val perPage: Int = 15,
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
)

data class PageDto<T>(
    val data: List<T> = emptyList(),
    val meta: PageMeta? = null,
)

data class ItemDto(
    val id: Int,
    val code: String,
    val name: String,
    val type: String?,
    val unit: String?,
)

data class PriceData(
    val price: String,
)

data class ShiftDto(
    val id: Int,
    val status: String?,
    @SerializedName("opening_cash") val openingCash: String? = null,
    val difference: String? = null,
)

data class SaleItemRequest(
    @SerializedName("item_id") val itemId: Int,
    val qty: Double,
    @SerializedName("discount_percent") val discountPercent: Double? = null,
    @SerializedName("discount_amount") val discountAmount: Double? = null,
    @SerializedName("tax_percent") val taxPercent: Double? = null,
)

data class SalePaymentRequest(
    val method: String,
    val amount: Double,
    val tendered: Double? = null,
    val reference: String? = null,
)

data class SaleRequest(
    @SerializedName("idempotency_key") val idempotencyKey: String? = null,
    val items: List<SaleItemRequest>,
    val payments: List<SalePaymentRequest>,
    @SerializedName("customer_id") val customerId: Int? = null,
)

data class SaleLineDto(
    val itemId: Int,
    val qty: String,
    val price: String,
    val lineTotal: String,
)

data class SalePayDto(
    val method: String,
    val amount: String,
    val tendered: String?,
)

data class SaleDto(
    val id: Int,
    val number: String,
    val status: String,
    val subtotal: String,
    val discountTotal: String,
    val taxTotal: String,
    val grandTotal: String,
    val items: List<SaleLineDto> = emptyList(),
    val payments: List<SalePayDto> = emptyList(),
)

data class ApprovalDto(
    val id: Int,
    val branchId: Int?,
    val entityType: String?,
    val entityId: Int?,
    val action: String,
    val actionLabel: String?,
    val status: String,
    val requestedBy: Int,
    val note: String?,
)

data class NotifDto(
    val id: Int,
    val type: String,
    val title: String,
    val body: String?,
    val link: String?,
    @SerializedName("read_at") val readAt: String?,
)

data class UnreadData(
    val unread: Int = 0,
)

data class LeaveDto(
    val id: Int,
    val userId: Int?,
    val type: String,
    val fromDate: String?,
    val toDate: String?,
    val reason: String?,
    val status: String,
)

data class CardData(
    val name: String,
    val email: String?,
    val phone: String?,
    val employeeUuid: String?,
    val company: String?,
    val branch: String?,
    val role: String?,
    val qrContent: String?,
)

data class SubscriptionData(
    val planId: Int?,
    val planName: String?,
    val status: String?,
    val trialEndsAt: String?,
    val endsAt: String?,
    val allowsWrites: Boolean = true,
)

data class ServiceOrderDto(
    val id: Int,
    val status: String,
    val customerId: Int? = null,
    val depositAmount: String? = null,
)

data class StockDto(
    val itemId: Int,
    val itemCode: String?,
    val itemName: String?,
    val qty: String,
    val avgCost: String?,
)
