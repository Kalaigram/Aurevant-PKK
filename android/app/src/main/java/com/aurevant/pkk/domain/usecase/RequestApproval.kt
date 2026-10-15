package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.ApprovalRepo
import javax.inject.Inject

class RequestApproval @Inject constructor(private val repo: ApprovalRepo) {
    suspend operator fun invoke(company: Int, action: String, entityId: Int?, payload: Map<String, Any?>, note: String?) =
        repo.request(company, action, entityId, payload, note)
}
