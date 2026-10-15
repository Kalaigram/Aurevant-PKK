package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.LeaveItem
import com.aurevant.pkk.domain.repo.TeamRepo
import javax.inject.Inject

class GetPendingLeaves @Inject constructor(private val repo: TeamRepo) {
    suspend operator fun invoke(company: Int): List<LeaveItem> = repo.pendingLeaves(company)
}
