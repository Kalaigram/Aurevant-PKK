package com.aurevant.pkk.data.repo

import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.unwrap
import com.aurevant.pkk.data.mapper.dashboardOf
import com.aurevant.pkk.data.mapper.toDomain
import com.aurevant.pkk.domain.model.Company
import com.aurevant.pkk.domain.model.Dashboard
import com.aurevant.pkk.domain.repo.CompanyRepo
import com.aurevant.pkk.domain.repo.DashboardRepo
import javax.inject.Inject

class CompanyRepoImpl @Inject constructor(private val api: ApiService) : CompanyRepo {
    override suspend fun companies(): List<Company> = unwrap(api.companies()).map { it.toDomain() }
}

class DashboardRepoImpl @Inject constructor(private val api: ApiService) : DashboardRepo {
    override suspend fun dashboard(company: Int): Dashboard = dashboardOf(unwrap(api.dashboard(company)))

    override suspend fun myPermissions(company: Int): List<String> =
        unwrap(api.myPermissions(company)).permissions
}
