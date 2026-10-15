package com.aurevant.pkk.di

import com.aurevant.pkk.data.repo.ApprovalRepoImpl
import com.aurevant.pkk.data.repo.AttendanceRepoImpl
import com.aurevant.pkk.data.repo.AuthRepoImpl
import com.aurevant.pkk.data.repo.CompanyRepoImpl
import com.aurevant.pkk.data.repo.DashboardRepoImpl
import com.aurevant.pkk.data.repo.NotifRepoImpl
import com.aurevant.pkk.data.repo.PeopleRepoImpl
import com.aurevant.pkk.data.repo.SaleRepoImpl
import com.aurevant.pkk.data.repo.SessionRepoImpl
import com.aurevant.pkk.domain.repo.ApprovalRepo
import com.aurevant.pkk.domain.repo.AttendanceRepo
import com.aurevant.pkk.domain.repo.AuthRepo
import com.aurevant.pkk.domain.repo.CompanyRepo
import com.aurevant.pkk.domain.repo.DashboardRepo
import com.aurevant.pkk.domain.repo.NotifRepo
import com.aurevant.pkk.domain.repo.PeopleRepo
import com.aurevant.pkk.domain.repo.SaleRepo
import com.aurevant.pkk.domain.repo.SessionRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds abstract fun auth(repo: AuthRepoImpl): AuthRepo
    @Binds abstract fun session(repo: SessionRepoImpl): SessionRepo
    @Binds abstract fun companies(repo: CompanyRepoImpl): CompanyRepo
    @Binds abstract fun dashboard(repo: DashboardRepoImpl): DashboardRepo
    @Binds abstract fun attendance(repo: AttendanceRepoImpl): AttendanceRepo
    @Binds abstract fun sale(repo: SaleRepoImpl): SaleRepo
    @Binds abstract fun approvals(repo: ApprovalRepoImpl): ApprovalRepo
    @Binds abstract fun notif(repo: NotifRepoImpl): NotifRepo
    @Binds abstract fun people(repo: PeopleRepoImpl): PeopleRepo
}
