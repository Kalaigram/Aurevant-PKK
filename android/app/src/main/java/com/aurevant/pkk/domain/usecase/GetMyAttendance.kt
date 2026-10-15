package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.repo.AttendanceRepo
import javax.inject.Inject

class GetMyAttendance @Inject constructor(private val repo: AttendanceRepo) {
    suspend operator fun invoke(company: Int, from: String, to: String): List<Attendance> = repo.mine(company, from, to)
}
