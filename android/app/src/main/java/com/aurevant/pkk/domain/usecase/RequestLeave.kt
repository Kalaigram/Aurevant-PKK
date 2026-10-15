package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.PeopleRepo
import javax.inject.Inject

class RequestLeave @Inject constructor(private val repo: PeopleRepo) {
    suspend operator fun invoke(company: Int, user: Int, type: String, from: String, to: String, reason: String) =
        repo.requestLeave(company, user, type, from, to, reason)
}
