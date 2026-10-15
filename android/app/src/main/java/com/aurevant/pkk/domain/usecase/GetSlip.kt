package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Slip
import com.aurevant.pkk.domain.repo.PeopleRepo
import javax.inject.Inject

class GetSlip @Inject constructor(private val repo: PeopleRepo) {
    suspend operator fun invoke(company: Int, period: Int, user: Int): Slip = repo.slip(company, period, user)
}
