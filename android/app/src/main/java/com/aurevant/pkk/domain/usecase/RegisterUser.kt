package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.AuthSession
import com.aurevant.pkk.domain.repo.AuthRepo
import com.aurevant.pkk.domain.repo.SessionRepo
import javax.inject.Inject

class RegisterUser @Inject constructor(
    private val auth: AuthRepo,
    private val session: SessionRepo,
) {
    suspend operator fun invoke(name: String, email: String, phone: String?, password: String, confirm: String): AuthSession {
        val result = auth.register(name.trim(), email.trim(), phone?.trim()?.ifEmpty { null }, password, confirm)
        session.saveLogin(result)
        return result
    }
}
