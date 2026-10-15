package com.aurevant.pkk.data.repo

import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.unwrap
import com.aurevant.pkk.data.mapper.slipOf
import com.aurevant.pkk.domain.model.Approval
import com.aurevant.pkk.domain.model.EmployeeCard
import com.aurevant.pkk.domain.model.LeaveItem
import com.aurevant.pkk.domain.model.Notif
import com.aurevant.pkk.domain.model.Slip
import com.aurevant.pkk.domain.repo.ApprovalRepo
import com.aurevant.pkk.domain.repo.NotifRepo
import com.aurevant.pkk.domain.repo.PeopleRepo
import javax.inject.Inject

class ApprovalRepoImpl @Inject constructor(private val api: ApiService) : ApprovalRepo {
    override suspend fun inbox(company: Int): List<Approval> =
        unwrap(api.approvals(company)).data.map {
            Approval(it.id, it.action, it.actionLabel, it.status)
        }

    override suspend fun approve(company: Int, id: Int, note: String?): Approval {
        val data = unwrap(api.approve(company, id, mapOf("note" to note)))
        return Approval(data.id, data.action, data.actionLabel, data.status)
    }

    override suspend fun reject(company: Int, id: Int, note: String?): Approval {
        val data = unwrap(api.reject(company, id, mapOf("note" to note)))
        return Approval(data.id, data.action, data.actionLabel, data.status)
    }
}

class NotifRepoImpl @Inject constructor(private val api: ApiService) : NotifRepo {
    override suspend fun list(company: Int?, unreadOnly: Boolean): List<Notif> =
        unwrap(api.notifications(company, if (unreadOnly) true else null)).data.map {
            Notif(it.id, it.type, it.title, it.body, it.readAt != null)
        }

    override suspend fun unread(): Int = unwrap(api.unreadCount()).unread

    override suspend fun read(id: Int) {
        api.markRead(id)
    }

    override suspend fun readAll(company: Int?) {
        api.markAllRead(company)
    }
}

class PeopleRepoImpl @Inject constructor(private val api: ApiService) : PeopleRepo {
    override suspend fun requestLeave(company: Int, user: Int, type: String, from: String, to: String, reason: String) {
        api.requestLeave(
            company,
            user,
            mapOf("type" to type, "from_date" to from, "to_date" to to, "reason" to reason),
        )
    }

    override suspend fun myLeaves(company: Int, user: Int): List<LeaveItem> =
        unwrap(api.leaves(company, user, null)).data.map {
            LeaveItem(it.id, it.type, it.fromDate, it.toDate, it.status)
        }

    override suspend fun slip(company: Int, period: Int, user: Int): Slip =
        slipOf(unwrap(api.slip(company, period, user)))

    override suspend fun card(company: Int, user: Int): EmployeeCard {
        val data = unwrap(api.employeeCard(company, user))
        return EmployeeCard(data.name, data.company, data.branch, data.role, data.qrContent)
    }
}
