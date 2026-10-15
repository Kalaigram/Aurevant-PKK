package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.TeamRepo
import javax.inject.Inject

class ResetDevice @Inject constructor(private val repo: TeamRepo) {
    suspend operator fun invoke(company: Int, user: Int) = repo.resetDevice(company, user)
}
