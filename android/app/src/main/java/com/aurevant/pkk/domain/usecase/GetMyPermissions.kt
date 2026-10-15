package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.DashboardRepo
import javax.inject.Inject

class GetMyPermissions @Inject constructor(private val repo: DashboardRepo) {
    suspend operator fun invoke(company: Int): List<String> = repo.myPermissions(company)
}
