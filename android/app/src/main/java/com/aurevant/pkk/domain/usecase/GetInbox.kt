package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Approval
import com.aurevant.pkk.domain.repo.ApprovalRepo
import javax.inject.Inject

class GetInbox @Inject constructor(private val repo: ApprovalRepo) {
    suspend operator fun invoke(company: Int): List<Approval> = repo.inbox(company)
}
