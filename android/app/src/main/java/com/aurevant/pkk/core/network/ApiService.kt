package com.aurevant.pkk.core.network

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
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
}
