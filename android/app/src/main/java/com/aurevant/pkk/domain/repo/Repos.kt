package com.aurevant.pkk.domain.repo

import com.aurevant.pkk.domain.model.Approval
import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.model.AuthSession
import com.aurevant.pkk.domain.model.Bill
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.CartLine
import com.aurevant.pkk.domain.model.CatalogItem
import com.aurevant.pkk.domain.model.Company
import com.aurevant.pkk.domain.model.Customer
import com.aurevant.pkk.domain.model.Dashboard
import com.aurevant.pkk.domain.model.EmployeeCard
import com.aurevant.pkk.domain.model.Expense
import com.aurevant.pkk.domain.model.JadwalHari
import com.aurevant.pkk.domain.model.LeaveItem
import com.aurevant.pkk.domain.model.Movement
import com.aurevant.pkk.domain.model.Notif
import com.aurevant.pkk.domain.model.Opname
import com.aurevant.pkk.domain.model.OpnameInput
import com.aurevant.pkk.domain.model.OrderInput
import com.aurevant.pkk.domain.model.PayInput
import com.aurevant.pkk.domain.model.PayrollPeriod
import com.aurevant.pkk.domain.model.ProfitLoss
import com.aurevant.pkk.domain.model.Receivable
import com.aurevant.pkk.domain.model.ReturnLine
import com.aurevant.pkk.domain.model.Sale
import com.aurevant.pkk.domain.model.ServiceOrder
import com.aurevant.pkk.domain.model.Shift
import com.aurevant.pkk.domain.model.Slip
import com.aurevant.pkk.domain.model.Transfer
import com.aurevant.pkk.domain.model.TransferInput
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
    suspend fun receiptPdf(sale: Sale, companyName: String, branchName: String, names: Map<Int, String>): File
    suspend fun notas(company: Int, branch: Int): List<Sale>
    suspend fun notaDetail(company: Int, branch: Int, sale: Int): Sale
    suspend fun voidSale(company: Int, branch: Int, sale: Int, reason: String)
    suspend fun createReturn(company: Int, branch: Int, sale: Int, lines: List<ReturnLine>, reason: String, method: String)
}

interface ApprovalRepo {
    suspend fun inbox(company: Int): List<Approval>
    suspend fun approve(company: Int, id: Int, note: String?): Approval
    suspend fun reject(company: Int, id: Int, note: String?): Approval
    suspend fun request(company: Int, action: String, entityId: Int?, payload: Map<String, Any?>, note: String?)
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

interface OpsRepo {
    suspend fun stock(company: Int, branch: Int): List<com.aurevant.pkk.domain.model.StockLine>
    suspend fun movements(company: Int, branch: Int): List<Movement>
    suspend fun createMovement(company: Int, branch: Int, itemId: Int, type: String, qty: Double, unitCost: Double?)
    suspend fun transfers(company: Int): List<Transfer>
    suspend fun createTransfer(company: Int, from: Int, to: Int, items: List<TransferInput>, note: String?)
    suspend fun transferAct(company: Int, id: Int, aksi: String)
    suspend fun opnames(company: Int, branch: Int): List<Opname>
    suspend fun createOpname(company: Int, branch: Int, lines: List<OpnameInput>, note: String?)
    suspend fun opnameAct(company: Int, branch: Int, id: Int, aksi: String)
}

interface ServiceRepo {
    suspend fun orders(company: Int, branch: Int): List<ServiceOrder>
    suspend fun createOrder(company: Int, branch: Int, customerId: Int?, items: List<OrderInput>, note: String?)
    suspend fun deposit(company: Int, branch: Int, order: Int, amount: Double)
    suspend fun process(company: Int, branch: Int, order: Int, assigneeId: Int?)
    suspend fun complete(company: Int, branch: Int, order: Int)
    suspend fun pay(company: Int, branch: Int, order: Int, pays: List<PayInput>)
    suspend fun cancel(company: Int, branch: Int, order: Int)
}

interface CustomerRepo {
    suspend fun customers(company: Int): List<Customer>
}

interface TeamRepo {
    suspend fun pendingLeaves(company: Int): List<LeaveItem>
    suspend fun decideLeave(company: Int, leave: Int, approve: Boolean)
    suspend fun correctAttendance(company: Int, id: Int, status: String, note: String?)
    suspend fun resetDevice(company: Int, user: Int)
    suspend fun scheduleOf(company: Int, user: Int, from: String, to: String): List<JadwalHari>
}

interface FinanceRepo {
    suspend fun categories(company: Int): List<com.aurevant.pkk.domain.model.ExpenseCategory>
    suspend fun expenses(company: Int, status: String?): List<Expense>
    suspend fun createExpense(company: Int, branch: Int, categoryId: Int, amount: Double, method: String, date: String, description: String)
    suspend fun expenseAct(company: Int, id: Int, setuju: Boolean)
    suspend fun bills(company: Int, onlyUnpaid: Boolean): List<Bill>
    suspend fun payBill(company: Int, id: Int, amount: Double, method: String, note: String?)
    suspend fun receivables(company: Int, onlyUnpaid: Boolean): List<Receivable>
    suspend fun payReceivable(company: Int, id: Int, amount: Double, method: String, note: String?)
    suspend fun profitLoss(company: Int, from: String, to: String, branchId: Int?): ProfitLoss
}

interface PayrollRepo {
    suspend fun periods(company: Int): List<PayrollPeriod>
    suspend fun periodAct(company: Int, id: Int, aksi: String)
}
