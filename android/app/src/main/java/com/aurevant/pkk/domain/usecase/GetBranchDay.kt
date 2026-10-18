package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Attendance
import com.aurevant.pkk.domain.repo.AttendanceRepo
import javax.inject.Inject

class GetBranchDay @Inject constructor(private val repo: AttendanceRepo) {
    suspend operator fun invoke(company: Int, branch: Int, date: String): List<Attendance> =
        repo.branchDay(company, branch, date)
}
