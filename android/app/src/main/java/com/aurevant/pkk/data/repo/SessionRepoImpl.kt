package com.aurevant.pkk.data.repo

import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.AuthSession
import com.aurevant.pkk.domain.model.Company
import com.aurevant.pkk.domain.repo.SessionRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SessionRepoImpl @Inject constructor(private val store: SessionStore) : SessionRepo {
    override suspend fun saveLogin(session: AuthSession) =
        store.saveLogin(session.token, session.user.id, session.user.name)

    override suspend fun saveCompany(company: Company) = store.saveCompany(company.id, company.name)

    override suspend fun clear() = store.clear()

    override suspend fun companyId(): Int? = store.companyId.first()

    override suspend fun userId(): Int? = store.userId.first()
}
