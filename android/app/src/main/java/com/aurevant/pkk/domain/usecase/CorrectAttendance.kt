package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.TeamRepo
import javax.inject.Inject

class CorrectAttendance @Inject constructor(private val repo: TeamRepo) {
    suspend operator fun invoke(company: Int, id: Int, status: String, note: String?) =
        repo.correctAttendance(company, id, status, note)
}
