package com.aurevant.pkk.core.network

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

/** Cerminan server/routes/api.php (prefix /api/v1). Satu fungsi satu endpoint. */
interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String?>): Response<Envelope<AuthData>>

    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String?>): Response<Envelope<AuthData>>

    @POST("auth/logout")
    suspend fun logout(): Response<Envelope<JsonObject>>

    @GET("auth/me")
    suspend fun me(): Response<Envelope<UserDto>>

    @GET("companies")
    suspend fun companies(): Response<Envelope<List<CompanyDto>>>

    @GET("companies/{company}/access/my-permissions")
    suspend fun myPermissions(@Path("company") company: Int): Response<Envelope<PermissionsData>>

    @GET("companies/{company}/dashboard")
    suspend fun dashboard(@Path("company") company: Int): Response<Envelope<JsonObject>>

    @GET("companies/{company}/branches")
    suspend fun branches(@Path("company") company: Int): Response<Envelope<List<BranchDto>>>

    @GET("companies/{company}/branches/{branch}/attendance-qr")
    suspend fun attendanceQr(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
    ): Response<Envelope<QrData>>

    @POST("companies/{company}/attendance/clock-in")
    @Multipart
    suspend fun clockIn(
        @Path("company") company: Int,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part selfie: MultipartBody.Part,
    ): Response<Envelope<AttendanceDto>>

    @POST("companies/{company}/attendance/clock-out")
    @Multipart
    suspend fun clockOut(
        @Path("company") company: Int,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
    ): Response<Envelope<AttendanceDto>>

    @GET("companies/{company}/attendance/me")
    suspend fun myAttendance(
        @Path("company") company: Int,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("per_page") perPage: Int = 15,
    ): Response<Envelope<PageDto<AttendanceDto>>>

    @GET("companies/{company}/branches/{branch}/attendance")
    suspend fun branchAttendance(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Query("date") date: String,
        @Query("per_page") perPage: Int = 50,
    ): Response<Envelope<PageDto<AttendanceDto>>>

    @GET("companies/{company}/items")
    suspend fun items(
        @Path("company") company: Int,
        @Query("type") type: String?,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
    ): Response<Envelope<PageDto<ItemDto>>>

    @GET("companies/{company}/items/{item}/prices/{branch}")
    suspend fun itemPrice(
        @Path("company") company: Int,
        @Path("item") item: Int,
        @Path("branch") branch: Int,
    ): Response<Envelope<PriceData>>

    @POST("companies/{company}/branches/{branch}/shifts")
    suspend fun openShift(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Body body: Map<String, Double>,
    ): Response<Envelope<ShiftDto>>

    @POST("companies/{company}/branches/{branch}/shifts/{shift}/close")
    suspend fun closeShift(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Path("shift") shift: Int,
        @Body body: Map<String, Double>,
    ): Response<Envelope<ShiftDto>>

    @POST("companies/{company}/branches/{branch}/sales")
    suspend fun createSale(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Body body: SaleRequest,
    ): Response<Envelope<SaleDto>>

    @GET("companies/{company}/branches/{branch}/sales/{sale}/receipt")
    suspend fun receiptPdf(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Path("sale") sale: Int,
    ): Response<ResponseBody>

    @GET("companies/{company}/approvals")
    suspend fun approvals(
        @Path("company") company: Int,
        @Query("status") status: String = "diajukan",
        @Query("per_page") perPage: Int = 20,
    ): Response<Envelope<PageDto<ApprovalDto>>>

    @POST("companies/{company}/approvals")
    suspend fun requestApproval(
        @Path("company") company: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Envelope<ApprovalDto>>

    @POST("companies/{company}/approvals/{approval}/approve")
    suspend fun approve(
        @Path("company") company: Int,
        @Path("approval") approval: Int,
        @Body body: Map<String, String?>,
    ): Response<Envelope<ApprovalDto>>

    @POST("companies/{company}/approvals/{approval}/reject")
    suspend fun reject(
        @Path("company") company: Int,
        @Path("approval") approval: Int,
        @Body body: Map<String, String?>,
    ): Response<Envelope<ApprovalDto>>

    @GET("notifications")
    suspend fun notifications(
        @Query("company_id") companyId: Int?,
        @Query("unread") unread: Boolean?,
        @Query("per_page") perPage: Int = 15,
    ): Response<Envelope<PageDto<NotifDto>>>

    @GET("notifications/unread-count")
    suspend fun unreadCount(): Response<Envelope<UnreadData>>

    @POST("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Int): Response<Envelope<JsonObject>>

    @POST("notifications/read-all")
    suspend fun markAllRead(@Query("company_id") companyId: Int?): Response<Envelope<JsonObject>>

    @POST("device-tokens")
    suspend fun registerToken(@Body body: Map<String, String?>): Response<Envelope<JsonObject>>

    @GET("companies/{company}/leaves")
    suspend fun leaves(
        @Path("company") company: Int,
        @Query("user_id") userId: Int?,
        @Query("status") status: String?,
    ): Response<Envelope<PageDto<LeaveDto>>>

    @POST("companies/{company}/users/{user}/leaves")
    suspend fun requestLeave(
        @Path("company") company: Int,
        @Path("user") user: Int,
        @Body body: Map<String, String?>,
    ): Response<Envelope<LeaveDto>>

    @GET("companies/{company}/payroll-periods/{period}/users/{user}/slip")
    suspend fun slip(
        @Path("company") company: Int,
        @Path("period") period: Int,
        @Path("user") user: Int,
    ): Response<Envelope<JsonObject>>

    @GET("companies/{company}/users/{user}/card")
    suspend fun employeeCard(
        @Path("company") company: Int,
        @Path("user") user: Int,
    ): Response<Envelope<CardData>>

    @GET("companies/{company}/subscription")
    suspend fun subscription(@Path("company") company: Int): Response<Envelope<SubscriptionData>>

    @GET("companies/{company}/branches/{branch}/service-orders")
    suspend fun serviceOrders(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Query("per_page") perPage: Int = 20,
    ): Response<Envelope<PageDto<ServiceOrderDto>>>

    @POST("companies/{company}/branches/{branch}/service-orders/{order}/process")
    suspend fun processOrder(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Path("order") order: Int,
        @Body body: Map<String, Int?>,
    ): Response<Envelope<ServiceOrderDto>>

    @POST("companies/{company}/branches/{branch}/service-orders/{order}/complete")
    suspend fun completeOrder(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Path("order") order: Int,
    ): Response<Envelope<ServiceOrderDto>>

    @GET("companies/{company}/branches/{branch}/stock")
    suspend fun stock(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Query("per_page") perPage: Int = 100,
    ): Response<Envelope<List<StockDto>>>

    // ---- Nota (Kasir/Manajer) ----
    @GET("companies/{company}/branches/{branch}/sales")
    suspend fun sales(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Query("per_page") perPage: Int = 20,
    ): Response<Envelope<PageDto<SaleDto>>>

    @GET("companies/{company}/branches/{branch}/sales/{sale}")
    suspend fun saleDetail(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Path("sale") sale: Int,
    ): Response<Envelope<SaleDto>>

    @POST("companies/{company}/branches/{branch}/sales/{sale}/void")
    suspend fun voidSale(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Path("sale") sale: Int,
        @Body body: Map<String, String?>,
    ): Response<Envelope<SaleDto>>

    @POST("companies/{company}/branches/{branch}/sales/{sale}/returns")
    suspend fun createReturn(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Path("sale") sale: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Envelope<JsonObject>>

    // ---- Gudang: transfer ----
    @GET("companies/{company}/stock-transfers")
    suspend fun transfers(
        @Path("company") company: Int,
        @Query("per_page") perPage: Int = 20,
    ): Response<Envelope<PageDto<TransferDto>>>

    @POST("companies/{company}/stock-transfers")
    suspend fun createTransfer(
        @Path("company") company: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Envelope<TransferDto>>

    @POST("companies/{company}/stock-transfers/{transfer}/submit")
    suspend fun submitTransfer(@Path("company") company: Int, @Path("transfer") transfer: Int): Response<Envelope<TransferDto>>

    @POST("companies/{company}/stock-transfers/{transfer}/approve")
    suspend fun approveTransfer(@Path("company") company: Int, @Path("transfer") transfer: Int): Response<Envelope<TransferDto>>

    @POST("companies/{company}/stock-transfers/{transfer}/ship")
    suspend fun shipTransfer(@Path("company") company: Int, @Path("transfer") transfer: Int): Response<Envelope<TransferDto>>

    @POST("companies/{company}/stock-transfers/{transfer}/receive")
    suspend fun receiveTransfer(@Path("company") company: Int, @Path("transfer") transfer: Int): Response<Envelope<TransferDto>>

    @POST("companies/{company}/stock-transfers/{transfer}/cancel")
    suspend fun cancelTransfer(@Path("company") company: Int, @Path("transfer") transfer: Int): Response<Envelope<TransferDto>>

    // ---- Gudang: opname & mutasi ----
    @GET("companies/{company}/branches/{branch}/stock/opnames")
    suspend fun opnames(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Query("per_page") perPage: Int = 20,
    ): Response<Envelope<PageDto<OpnameDto>>>

    @POST("companies/{company}/branches/{branch}/stock/opnames")
    suspend fun createOpname(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Envelope<OpnameDto>>

    @POST("companies/{company}/branches/{branch}/stock/opnames/{opname}/submit")
    suspend fun submitOpname(@Path("company") company: Int, @Path("branch") branch: Int, @Path("opname") opname: Int): Response<Envelope<OpnameDto>>

    @POST("companies/{company}/branches/{branch}/stock/opnames/{opname}/approve")
    suspend fun approveOpname(@Path("company") company: Int, @Path("branch") branch: Int, @Path("opname") opname: Int): Response<Envelope<OpnameDto>>

    @POST("companies/{company}/branches/{branch}/stock/opnames/{opname}/reject")
    suspend fun rejectOpname(@Path("company") company: Int, @Path("branch") branch: Int, @Path("opname") opname: Int): Response<Envelope<OpnameDto>>

    @GET("companies/{company}/branches/{branch}/stock/movements")
    suspend fun movements(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Query("per_page") perPage: Int = 30,
    ): Response<Envelope<PageDto<MovementDto>>>

    @POST("companies/{company}/branches/{branch}/stock/movements")
    suspend fun createMovement(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Envelope<MovementDto>>

    // ---- Jasa ----
    @POST("companies/{company}/branches/{branch}/service-orders")
    suspend fun createOrder(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Envelope<ServiceOrderFullDto>>

    @POST("companies/{company}/branches/{branch}/service-orders/{order}/deposit")
    suspend fun depositOrder(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Path("order") order: Int,
        @Body body: Map<String, Double>,
    ): Response<Envelope<ServiceOrderFullDto>>

    @POST("companies/{company}/branches/{branch}/service-orders/{order}/pay")
    suspend fun payOrder(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Path("order") order: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Envelope<SaleDto>>

    @POST("companies/{company}/branches/{branch}/service-orders/{order}/cancel")
    suspend fun cancelOrder(
        @Path("company") company: Int,
        @Path("branch") branch: Int,
        @Path("order") order: Int,
    ): Response<Envelope<ServiceOrderFullDto>>

    // ---- Pelanggan ----
    @GET("companies/{company}/customers")
    suspend fun customers(
        @Path("company") company: Int,
        @Query("per_page") perPage: Int = 50,
    ): Response<Envelope<PageDto<CustomerDto>>>

    // ---- Tim (HR/Manajer) ----
    @POST("companies/{company}/leaves/{leave}/decide")
    suspend fun decideLeave(
        @Path("company") company: Int,
        @Path("leave") leave: Int,
        @Body body: Map<String, Boolean>,
    ): Response<Envelope<LeaveDto>>

    @PUT("companies/{company}/attendances/{attendance}")
    suspend fun correctAttendance(
        @Path("company") company: Int,
        @Path("attendance") attendance: Int,
        @Body body: Map<String, String?>,
    ): Response<Envelope<AttendanceDto>>

    @DELETE("companies/{company}/users/{user}/device")
    suspend fun resetDevice(
        @Path("company") company: Int,
        @Path("user") user: Int,
    ): Response<Envelope<JsonObject>>

    @GET("companies/{company}/users/{user}/schedule")
    suspend fun mySchedule(
        @Path("company") company: Int,
        @Path("user") user: Int,
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<Envelope<List<ScheduleDayDto>>>

    // ---- Keuangan ----
    @GET("companies/{company}/expense-categories")
    suspend fun expenseCategories(
        @Path("company") company: Int,
        @Query("per_page") perPage: Int = 50,
    ): Response<Envelope<PageDto<ExpenseCategoryDto>>>

    @GET("companies/{company}/expenses")
    suspend fun expenses(
        @Path("company") company: Int,
        @Query("status") status: String?,
        @Query("per_page") perPage: Int = 20,
    ): Response<Envelope<PageDto<ExpenseDto>>>

    @POST("companies/{company}/expenses")
    suspend fun createExpense(
        @Path("company") company: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Envelope<ExpenseDto>>

    @POST("companies/{company}/expenses/{expense}/approve")
    suspend fun approveExpense(@Path("company") company: Int, @Path("expense") expense: Int): Response<Envelope<ExpenseDto>>

    @POST("companies/{company}/expenses/{expense}/reject")
    suspend fun rejectExpense(@Path("company") company: Int, @Path("expense") expense: Int): Response<Envelope<ExpenseDto>>

    @GET("companies/{company}/supplier-bills")
    suspend fun bills(
        @Path("company") company: Int,
        @Query("unpaid") unpaid: Boolean?,
        @Query("per_page") perPage: Int = 20,
    ): Response<Envelope<PageDto<BillDto>>>

    @POST("companies/{company}/supplier-bills/{bill}/pay")
    suspend fun payBill(
        @Path("company") company: Int,
        @Path("bill") bill: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Envelope<BillDto>>

    @GET("companies/{company}/receivables")
    suspend fun receivables(
        @Path("company") company: Int,
        @Query("unpaid") unpaid: Boolean?,
        @Query("per_page") perPage: Int = 20,
    ): Response<Envelope<PageDto<ReceivableDto>>>

    @POST("companies/{company}/receivables/{receivable}/pay")
    suspend fun payReceivable(
        @Path("company") company: Int,
        @Path("receivable") receivable: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Envelope<ReceivableDto>>

    @GET("companies/{company}/reports/profit-loss")
    suspend fun profitLoss(
        @Path("company") company: Int,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("branch_id") branchId: Int?,
    ): Response<Envelope<ProfitLossDto>>

    // ---- Penggajian ----
    @GET("companies/{company}/payroll-periods")
    suspend fun periods(
        @Path("company") company: Int,
        @Query("per_page") perPage: Int = 20,
    ): Response<Envelope<PageDto<PayrollPeriodDto>>>

    @POST("companies/{company}/payroll-periods/{period}/calculate")
    suspend fun calculatePeriod(@Path("company") company: Int, @Path("period") period: Int): Response<Envelope<JsonObject>>

    @POST("companies/{company}/payroll-periods/{period}/review")
    suspend fun reviewPeriod(@Path("company") company: Int, @Path("period") period: Int): Response<Envelope<JsonObject>>

    @POST("companies/{company}/payroll-periods/{period}/approve")
    suspend fun approvePeriod(@Path("company") company: Int, @Path("period") period: Int): Response<Envelope<JsonObject>>

    @POST("companies/{company}/payroll-periods/{period}/pay")
    suspend fun payPeriod(@Path("company") company: Int, @Path("period") period: Int): Response<Envelope<JsonObject>>
}
