package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Approval
import com.aurevant.pkk.domain.repo.ApprovalRepo
import javax.inject.Inject

class DecideApproval @Inject constructor(private val repo: ApprovalRepo) {
    suspend operator fun invoke(company: Int, id: Int, approve: Boolean, note: String?): Approval =
        if (approve) repo.approve(company, id, note) else repo.reject(company, id, note)
}
