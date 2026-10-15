package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Notif
import com.aurevant.pkk.domain.repo.NotifRepo
import javax.inject.Inject

class GetNotifications @Inject constructor(private val repo: NotifRepo) {
    suspend operator fun invoke(company: Int, unreadOnly: Boolean = false): List<Notif> = repo.list(company, unreadOnly)
}
