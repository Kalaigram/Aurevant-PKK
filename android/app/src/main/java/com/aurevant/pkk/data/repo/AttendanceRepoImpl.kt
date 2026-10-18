package com.aurevant.pkk.data.repo

import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.unwrap
import com.aurevant.pkk.data.mapper.toDomain
import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.repo.AttendanceRepo
import java.io.File
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class AttendanceRepoImpl @Inject constructor(private val api: ApiService) : AttendanceRepo {
    private fun text(value: String) = value.toRequestBody("text/plain".toMediaType())

    override suspend fun branches(company: Int): List<Branch> = unwrap(api.branches(company)).map { it.toDomain() }

    override suspend fun qrToken(company: Int, branch: Int): Pair<String, Int> {
        val data = unwrap(api.attendanceQr(company, branch))
        return data.token to data.expiresIn
    }

    override suspend fun clockIn(
        company: Int,
        branch: Int,
        qr: String,
        lat: Double,
        lng: Double,
        device: String,
        selfie: File,
    ): Attendance {
        val fields = mapOf(
            "branch_id" to text(branch.toString()),
            "qr_token" to text(qr),
            "lat" to text(lat.toString()),
            "lng" to text(lng.toString()),
            "device_id" to text(device),
        )
        val foto = MultipartBody.Part.createFormData(
            "selfie",
            selfie.name,
            selfie.asRequestBody("image/jpeg".toMediaType()),
        )
        return unwrap(api.clockIn(company, fields, foto)).toDomain()
    }

    override suspend fun clockOut(company: Int, branch: Int, qr: String, lat: Double, lng: Double, device: String): Attendance {
        val fields = mapOf(
            "branch_id" to text(branch.toString()),
            "qr_token" to text(qr),
            "lat" to text(lat.toString()),
            "lng" to text(lng.toString()),
            "device_id" to text(device),
        )
        return unwrap(api.clockOut(company, fields)).toDomain()
    }

    override suspend fun mine(company: Int, from: String, to: String): List<Attendance> =
        unwrap(api.myAttendance(company, from, to)).data.map { it.toDomain() }

    override suspend fun branchDay(company: Int, branch: Int, date: String): List<Attendance> =
        unwrap(api.branchAttendance(company, branch, date)).data.map { it.toDomain() }
}
