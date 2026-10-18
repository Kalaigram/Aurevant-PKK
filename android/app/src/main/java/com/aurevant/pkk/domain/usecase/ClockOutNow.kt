package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.repo.AttendanceRepo
import javax.inject.Inject

class ClockOutNow @Inject constructor(private val repo: AttendanceRepo) {
    suspend operator fun invoke(
        company: Int,
        branch: Int,
        qr: String,
        lat: Double,
        lng: Double,
        device: String,
    ): Attendance = repo.clockOut(company, branch, qr, lat, lng, device)
}
