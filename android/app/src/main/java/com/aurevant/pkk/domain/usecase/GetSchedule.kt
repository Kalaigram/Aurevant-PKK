package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.JadwalHari
import com.aurevant.pkk.domain.repo.TeamRepo
import javax.inject.Inject

class GetSchedule @Inject constructor(private val repo: TeamRepo) {
    suspend operator fun invoke(company: Int, user: Int, from: String, to: String): List<JadwalHari> =
        repo.scheduleOf(company, user, from, to)
}
