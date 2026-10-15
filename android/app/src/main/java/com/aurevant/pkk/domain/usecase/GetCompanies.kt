package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Company
import com.aurevant.pkk.domain.repo.CompanyRepo
import javax.inject.Inject

class GetCompanies @Inject constructor(private val repo: CompanyRepo) {
    suspend operator fun invoke(): List<Company> = repo.companies()
}
