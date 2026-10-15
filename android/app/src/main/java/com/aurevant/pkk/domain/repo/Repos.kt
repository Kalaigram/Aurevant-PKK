package com.aurevant.pkk.domain.repo

import com.aurevant.pkk.domain.model.Approval
import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.model.AuthSession
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.CartLine
import com.aurevant.pkk.domain.model.CatalogItem
import com.aurevant.pkk.domain.model.Company
import com.aurevant.pkk.domain.model.Dashboard
import com.aurevant.pkk.domain.model.EmployeeCard
import com.aurevant.pkk.domain.model.LeaveItem
import com.aurevant.pkk.domain.model.Notif
import com.aurevant.pkk.domain.model.PayInput
import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.model.Shift
import com.aurevant.pkk.domain.model.Slip
import com.aurevant.pkk.domain.model.User
import java.io.File

interface AuthRepo {
    suspend fun login(login: String, password: String): AuthSession
    suspend fun register(name: String, email: String, phone: String?, password: String, confirm: String): AuthSession
    suspend fun logout()
    suspend fun me(): User
}

interface SessionRepo {
    suspend fun saveLogin(session: AuthSession)
    suspend fun saveCompany(company: Company)
    suspend fun clear()
    suspend fun companyId(): Int?
    suspend fun userId(): Int?
}

interface CompanyRepo {
    suspend fun companies(): List<Company>
}

interface DashboardRepo {
    suspend fun dashboard(company: Int): Dashboard
    suspend fun myPermissions(company: Int): List<String>
}

interface AttendanceRepo {
    suspend fun branches(company: Int): List<Branch>
    suspend fun qrToken(company: Int, branch: Int): Pair<String, Int>
    suspend fun clockIn(company: Int, branch: Int, qr: String, lat: Double, lng: Double, device: String, selfie: File): Attendance
    suspend fun clockOut(company: Int, branch: Int, qr: String, lat: Double, lng: Double, device: String): Attendance
    suspend fun mine(company: Int, from: String, to: String): List<Attendance>
    suspend fun branchDay(company: Int, branch: Int, date: String): List<Attendance>
}

interface SaleRepo {
    suspend fun items(company: Int, type: String?): List<CatalogItem>
    suspend fun price(company: Int, item: Int, branch: Int): Double
    suspend fun openShift(company: Int, branch: Int, cash: Double): Shift
    suspend fun closeShift(company: Int, branch: Int, shift: Int, counted: Double): Shift
    suspend fun createSale(company: Int, branch: Int, lines: List<CartLine>, pays: List<PayInput>, key: String): Sale
    suspend fun receiptPdf(company: Int, branch: Int, sale: Int, number: String): File
}

interface ApprovalRepo {
    suspend fun inbox(company: Int): List<Approval>
    suspend fun approve(company: Int, id: Int, note: String?): Approval
    suspend fun reject(company: Int, id: Int, note: String?): Approval
}

interface NotifRepo {
    suspend fun list(company: Int?, unreadOnly: Boolean): List<Notif>
    suspend fun unread(): Int
    suspend fun read(id: Int)
    suspend fun readAll(company: Int?)
}

interface PeopleRepo {
    suspend fun requestLeave(company: Int, user: Int, type: String, from: String, to: String, reason: String)
    suspend fun myLeaves(company: Int, user: Int): List<LeaveItem>
    suspend fun slip(company: Int, period: Int, user: Int): Slip
    suspend fun card(company: Int, user: Int): EmployeeCard
}
