package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.EmployeeCard
import com.aurevant.pkk.domain.repo.PeopleRepo
import javax.inject.Inject

class GetCard @Inject constructor(private val repo: PeopleRepo) {
    suspend operator fun invoke(company: Int, user: Int): EmployeeCard = repo.card(company, user)
}
