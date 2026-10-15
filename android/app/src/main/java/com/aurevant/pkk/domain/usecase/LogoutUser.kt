package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.AuthRepo
import com.aurevant.pkk.domain.repo.SessionRepo
import javax.inject.Inject

class LogoutUser @Inject constructor(
    private val auth: AuthRepo,
    private val session: SessionRepo,
) {
    suspend operator fun invoke() {
        runCatching { auth.logout() }
        session.clear()
    }
}
