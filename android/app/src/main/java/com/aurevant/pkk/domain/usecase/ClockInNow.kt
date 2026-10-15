package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.repo.AttendanceRepo
import java.io.File
import javax.inject.Inject

class ClockInNow @Inject constructor(private val repo: AttendanceRepo) {
    suspend operator fun invoke(
        company: Int,
        branch: Int,
        qr: String,
        lat: Double,
        lng: Double,
        device: String,
        selfie: File,
    ): Attendance = repo.clockIn(company, branch, qr, lat, lng, device, selfie)
}
