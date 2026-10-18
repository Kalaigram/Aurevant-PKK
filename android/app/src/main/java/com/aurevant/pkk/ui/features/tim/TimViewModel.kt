package com.aurevant.pkk.ui.features.tim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.model.JadwalHari
import com.aurevant.pkk.domain.model.LeaveItem
import com.aurevant.pkk.domain.usecase.CorrectAttendance
import com.aurevant.pkk.domain.usecase.DecideLeave
import com.aurevant.pkk.domain.usecase.GetBranchDay
import com.aurevant.pkk.domain.usecase.GetBranches
import com.aurevant.pkk.domain.usecase.GetMyPermissions
import com.aurevant.pkk.domain.usecase.GetPendingLeaves
import com.aurevant.pkk.domain.usecase.GetSchedule
import com.aurevant.pkk.domain.usecase.ResetDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface TimAction {
    data object Load : TimAction
    data class PickBranch(val branch: Branch) : TimAction
    data class Decide(val leave: LeaveItem, val approve: Boolean) : TimAction
    data class Correct(val attendanceId: Int, val status: String) : TimAction
    data class Reset(val userId: Int) : TimAction
    data object ClearMessage : TimAction
}

data class TimState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val companyId: Int? = null,
    val myUserId: Int? = null,
    val branches: List<Branch> = emptyList(),
    val branch: Branch? = null,
    val hadir: List<Attendance> = emptyList(),
    val cuti: List<LeaveItem> = emptyList(),
    val jadwal: List<JadwalHari> = emptyList(),
    val perms: Set<String> = emptySet(),
)

@HiltViewModel
class TimViewModel @Inject constructor(
    private val session: SessionStore,
    private val getBranches: GetBranches,
    private val getMyPermissions: GetMyPermissions,
    private val getBranchDay: GetBranchDay,
    private val getPendingLeaves: GetPendingLeaves,
    private val decideLeave: DecideLeave,
    private val correctAttendance: CorrectAttendance,
    private val resetDevice: ResetDevice,
    private val getSchedule: GetSchedule,
) : ViewModel() {
    private val _state = MutableStateFlow(TimState(busy = true))
    val state: StateFlow<TimState> = _state

    private inline fun update(f: (TimState) -> TimState) {
        _state.value = f(_state.value)
    }

    private fun err(e: Exception): String = (e as? ApiError)?.message ?: e.message ?: "Terjadi kesalahan."

    fun handleAction(action: TimAction) {
        when (action) {
            TimAction.Load -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch run {
                    _state.value = TimState(error = "Pilih perusahaan dulu.")
                }
                update { it.copy(busy = true, error = null, companyId = company, myUserId = session.userId.first()) }
                update {
                    try {
                        it.copy(busy = false, branches = getBranches(company), perms = getMyPermissions(company).toSet())
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
            }
            is TimAction.PickBranch -> {
                update { it.copy(branch = action.branch, message = null) }
                refresh()
            }
            is TimAction.Decide -> viewModelScope.launch {
                val company = _state.value.companyId ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        decideLeave(company, action.leave.id, action.approve)
                        it.copy(busy = false, message = if (action.approve) "Cuti disetujui." else "Cuti ditolak.")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            is TimAction.Correct -> viewModelScope.launch {
                val company = _state.value.companyId ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        correctAttendance(company, action.attendanceId, action.status, "Koreksi dari aplikasi")
                        it.copy(busy = false, message = "Kehadiran dikoreksi.")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
                refresh()
            }
            is TimAction.Reset -> viewModelScope.launch {
                val company = _state.value.companyId ?: return@launch
                update { it.copy(busy = true, error = null, message = null) }
                update {
                    try {
                        resetDevice(company, action.userId)
                        it.copy(busy = false, message = "Perangkat direset. Karyawan bisa tautkan HP baru.")
                    } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
                }
            }
            TimAction.ClearMessage -> update { it.copy(message = null, error = null) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val s = _state.value
            val company = s.companyId ?: return@launch
            val branch = s.branch ?: return@launch
            val today = LocalDate.now().toString()
            update { it.copy(busy = true, error = null) }
            update {
                try {
                    val jadwal = s.myUserId?.let { uid ->
                        getSchedule(company, uid, today, LocalDate.now().plusDays(6).toString())
                    } ?: emptyList()
                    it.copy(
                        busy = false,
                        hadir = getBranchDay(company, branch.id, today),
                        cuti = getPendingLeaves(company),
                        jadwal = jadwal,
                    )
                } catch (e: Exception) { it.copy(busy = false, error = err(e)) }
            }
        }
    }
}
