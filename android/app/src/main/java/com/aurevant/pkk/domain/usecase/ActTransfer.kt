package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.OpsRepo
import javax.inject.Inject

/** Satu aksi transfer: submit, approve, ship, receive, atau cancel. */
class ActTransfer @Inject constructor(private val repo: OpsRepo) {
    suspend operator fun invoke(company: Int, id: Int, aksi: String) = repo.transferAct(company, id, aksi)
}
