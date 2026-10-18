package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.NotifRepo
import javax.inject.Inject

class MarkAllRead @Inject constructor(private val repo: NotifRepo) {
    suspend operator fun invoke(company: Int?) = repo.readAll(company)
}
