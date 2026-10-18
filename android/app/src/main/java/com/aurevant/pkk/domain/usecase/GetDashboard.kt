package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Dashboard
import com.aurevant.pkk.domain.repo.DashboardRepo
import javax.inject.Inject

class GetDashboard @Inject constructor(private val repo: DashboardRepo) {
    suspend operator fun invoke(company: Int): Dashboard = repo.dashboard(company)
}
