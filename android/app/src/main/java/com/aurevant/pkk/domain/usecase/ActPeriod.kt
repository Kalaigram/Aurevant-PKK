package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.PayrollRepo
import javax.inject.Inject

/** Satu transisi periode: calculate, review, approve, atau pay. */
class ActPeriod @Inject constructor(private val repo: PayrollRepo) {
    suspend operator fun invoke(company: Int, id: Int, aksi: String) = repo.periodAct(company, id, aksi)
}
