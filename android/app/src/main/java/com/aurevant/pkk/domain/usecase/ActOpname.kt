package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.OpsRepo
import javax.inject.Inject

/** Satu aksi opname: submit, approve, atau reject. */
class ActOpname @Inject constructor(private val repo: OpsRepo) {
    suspend operator fun invoke(company: Int, branch: Int, id: Int, aksi: String) =
        repo.opnameAct(company, branch, id, aksi)
}
