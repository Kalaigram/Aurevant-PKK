package com.aurevant.pkk.data.repo

import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.unwrap
import com.aurevant.pkk.data.mapper.toDomain
import com.aurevant.pkk.domain.model.JadwalHari
import com.aurevant.pkk.domain.model.LeaveItem
import com.aurevant.pkk.domain.repo.TeamRepo
import javax.inject.Inject

class TeamRepoImpl @Inject constructor(private val api: ApiService) : TeamRepo {
    override suspend fun pendingLeaves(company: Int): List<LeaveItem> =
        unwrap(api.leaves(company, null, "diajukan")).data.map {
            LeaveItem(it.id, it.type, it.fromDate, it.toDate, it.status)
        }

    override suspend fun decideLeave(company: Int, leave: Int, approve: Boolean) {
        api.decideLeave(company, leave, mapOf("approve" to approve))
    }

    override suspend fun correctAttendance(company: Int, id: Int, status: String, note: String?) {
        api.correctAttendance(company, id, mapOf("status" to status, "note" to note))
    }

    override suspend fun resetDevice(company: Int, user: Int) {
        api.resetDevice(company, user)
    }

    override suspend fun scheduleOf(company: Int, user: Int, from: String, to: String): List<JadwalHari> =
        unwrap(api.mySchedule(company, user, from, to)).map { it.toDomain() }
}
