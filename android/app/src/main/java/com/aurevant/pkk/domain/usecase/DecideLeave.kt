package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.TeamRepo
import javax.inject.Inject

class DecideLeave @Inject constructor(private val repo: TeamRepo) {
    suspend operator fun invoke(company: Int, leave: Int, approve: Boolean) =
        repo.decideLeave(company, leave, approve)
}
