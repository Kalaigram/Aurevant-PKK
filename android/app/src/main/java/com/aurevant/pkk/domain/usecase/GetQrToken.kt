package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.repo.AttendanceRepo
import javax.inject.Inject

class GetQrToken @Inject constructor(private val repo: AttendanceRepo) {
    suspend operator fun invoke(company: Int, branch: Int): Pair<String, Int> = repo.qrToken(company, branch)
}
