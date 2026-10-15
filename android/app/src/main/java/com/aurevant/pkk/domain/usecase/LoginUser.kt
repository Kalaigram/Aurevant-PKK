package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.AuthSession
import com.aurevant.pkk.domain.repo.AuthRepo
import com.aurevant.pkk.domain.repo.SessionRepo
import javax.inject.Inject

class LoginUser @Inject constructor(
    private val auth: AuthRepo,
    private val session: SessionRepo,
) {
    suspend operator fun invoke(login: String, password: String): AuthSession {
        val result = auth.login(login.trim(), password)
        session.saveLogin(result)
        return result
    }
}
