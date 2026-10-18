package com.aurevant.pkk.domain.usecase

import com.aurevant.pkk.domain.model.Branch
import com.aurevant.pkk.domain.repo.AttendanceRepo
import javax.inject.Inject

class GetBranches @Inject constructor(private val repo: AttendanceRepo) {
    suspend operator fun invoke(company: Int): List<Branch> = repo.branches(company)
}
