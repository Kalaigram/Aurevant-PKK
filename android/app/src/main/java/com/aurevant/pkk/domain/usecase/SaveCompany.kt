package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Company
import com.aurevant.pkk.domain.repo.SessionRepo
import javax.inject.Inject

class SaveCompany @Inject constructor(private val session: SessionRepo) {
    suspend operator fun invoke(company: Company) = session.saveCompany(company)
}
