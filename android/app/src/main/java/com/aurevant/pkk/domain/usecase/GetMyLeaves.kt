package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.LeaveItem
import com.aurevant.pkk.domain.repo.PeopleRepo
import javax.inject.Inject

class GetMyLeaves @Inject constructor(private val repo: PeopleRepo) {
    suspend operator fun invoke(company: Int, user: Int): List<LeaveItem> = repo.myLeaves(company, user)
}
