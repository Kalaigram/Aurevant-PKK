package com.aurevant.pkk.ui.features.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.usecase.ClockInNow
import com.aurevant.pkk.domain.usecase.ClockOutNow
import com.aurevant.pkk.domain.usecase.GetBranches
import com.aurevant.pkk.domain.usecase.GetMyAttendance
import com.aurevant.pkk.domain.usecase.GetQrToken
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface AttendanceAction {
    data object Load : AttendanceAction
    data class PickBranch(val branch: Branch) : AttendanceAction
    data class QrScanned(val token: String) : AttendanceAction
    data class SetLocation(val lat: Double, val lng: Double) : AttendanceAction
    data class SetSelfie(val file: File) : AttendanceAction
    data class SetDevice(val id: String) : AttendanceAction
    data object ClockIn : AttendanceAction
    data object ClockOut : AttendanceAction
    data object History : AttendanceAction
    data object ClearMessage : AttendanceAction
}

data class AttendanceState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val companyId: Int? = null,
    val branches: List<Branch> = emptyList(),
    val branch: Branch? = null,
    val qr: String = "",
    val qrExpiry: Int = 0,
    val lat: Double? = null,
    val lng: Double? = null,
    val device: String = "",
    val selfie: File? = null,
    val history: List<Attendance> = emptyList(),
)

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val session: SessionStore,
    private val getBranches: GetBranches,
    private val getQrToken: GetQrToken,
    private val clockInNow: ClockInNow,
    private val clockOutNow: ClockOutNow,
    private val getMyAttendance: GetMyAttendance,
) : ViewModel() {
    private val _state = MutableStateFlow(AttendanceState(busy = true))
    val state: StateFlow<AttendanceState> = _state

    private fun update(f: (AttendanceState) -> AttendanceState) {
        _state.value = f(_state.value)
    }

    fun handleAction(action: AttendanceAction) {
        when (action) {
            AttendanceAction.Load -> viewModelScope.launch {
                val company = session.companyId.first() ?: return@launch run {
                    _state.value = AttendanceState(error = "Pilih perusahaan dulu.")
                }
                update { it.copy(busy = true, error = null, companyId = company) }
                update {
                    try {
                        it.copy(busy = false, branches = getBranches(company))
                    } catch (e: ApiError) {
                        it.copy(busy = false, error = e.message)
                    } catch (e: Exception) {
                        it.copy(busy = false, error = e.message ?: "Terjadi kesalahan.")
                    }
                }
            }
            is AttendanceAction.PickBranch -> {
                update { it.copy(branch = action.branch, qr = "", selfie = null, message = null) }
                refreshQr()
            }
            is AttendanceAction.QrScanned -> {
                update { it.copy(qr = action.token) }
            }
            is AttendanceAction.SetLocation -> update { it.copy(lat = action.lat, lng = action.lng) }
            is AttendanceAction.SetSelfie -> update { it.copy(selfie = action.file) }
            is AttendanceAction.SetDevice -> update { it.copy(device = action.id) }
            AttendanceAction.ClockIn -> submit(inOut = true)
            AttendanceAction.ClockOut -> submit(inOut = false)
            AttendanceAction.History -> viewModelScope.launch {
                val s = _state.value
                val company = s.companyId ?: return@launch
                update { it.copy(busy = true, error = null) }
                update {
                    try {
                        val today = LocalDate.now().toString()
                        it.copy(busy = false, history = getMyAttendance(company, today.minusMonth(), today))
                    } catch (e: ApiError) {
                        it.copy(busy = false, error = e.message)
                    } catch (e: Exception) {
                        it.copy(busy = false, error = e.message ?: "Terjadi kesalahan.")
                    }
                }
            }
            AttendanceAction.ClearMessage -> update { it.copy(message = null, error = null) }
        }
    }

    private fun LocalDate.minusMonth(): String = minusMonths(1).toString()

    private fun refreshQr() {
        viewModelScope.launch {
            val s = _state.value
            val company = s.companyId ?: return@launch
            val branch = s.branch ?: return@launch
            update {
                try {
                    val (token, exp) = getQrToken(company, branch.id)
                    it.copy(qr = token, qrExpiry = exp)
                } catch (e: ApiError) {
                    it.copy(error = e.message)
                } catch (e: Exception) {
                    it.copy(error = e.message ?: "Terjadi kesalahan.")
                }
            }
        }
    }

    private fun submit(masuk: Boolean) {
        val s = _state.value
        val company = s.companyId ?: return
        val branch = s.branch ?: return run { update { it.copy(error = "Pilih cabang dulu.") } }
        if (masuk && s.selfie == null) {
            update { it.copy(error = "Swafoto wajib saat masuk.") }
            return
        }
        update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            update {
                try {
                    if (masuk) {
                        clockInNow(company, branch.id, s.qr, s.lat ?: 0.0, s.lng ?: 0.0, s.device.ifEmpty { "android" }, s.selfie!!)
                        it.copy(busy = false, message = "Absen masuk tercatat.", selfie = null)
                    } else {
                        clockOutNow(company, branch.id, s.qr, s.lat ?: 0.0, s.lng ?: 0.0, s.device.ifEmpty { "android" })
                        it.copy(busy = false, message = "Absen pulang tercatat.")
                    }
                } catch (e: ApiError) {
                    it.copy(busy = false, error = e.message)
                } catch (e: Exception) {
                    it.copy(busy = false, error = e.message ?: "Terjadi kesalahan.")
                }
            }
        }
    }
}
