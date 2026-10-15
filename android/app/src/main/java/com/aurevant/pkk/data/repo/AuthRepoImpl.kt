package com.aurevant.pkk.data.repo

import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.unwrap
import com.aurevant.pkk.data.mapper.toDomain
import com.aurevant.pkk.domain.model.AuthSession
import com.aurevant.pkk.domain.model.User
import com.aurevant.pkk.domain.repo.AuthRepo
import javax.inject.Inject

class AuthRepoImpl @Inject constructor(private val api: ApiService) : AuthRepo {
    override suspend fun login(login: String, password: String): AuthSession {
        val data = unwrap(api.login(mapOf("login" to login, "password" to password)))
        return AuthSession(data.user.toDomain(), data.token)
    }

    override suspend fun register(name: String, email: String, phone: String?, password: String, confirm: String): AuthSession {
        val data = unwrap(
            api.register(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "password" to password,
                    "password_confirmation" to confirm,
                ),
            ),
        )
        return AuthSession(data.user.toDomain(), data.token)
    }

    override suspend fun logout() {
        runCatching { api.logout() }
    }

    override suspend fun me(): User = unwrap(api.me()).toDomain()
}
