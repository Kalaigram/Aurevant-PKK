package com.aurevant.pkk.di

import com.aurevant.pkk.data.repo.ApprovalRepoImpl
import com.aurevant.pkk.data.repo.AttendanceRepoImpl
import com.aurevant.pkk.data.repo.AuthRepoImpl
import com.aurevant.pkk.data.repo.CompanyRepoImpl
import com.aurevant.pkk.data.repo.CustomerRepoImpl
import com.aurevant.pkk.data.repo.DashboardRepoImpl
import com.aurevant.pkk.data.repo.FinanceRepoImpl
import com.aurevant.pkk.data.repo.NotifRepoImpl
import com.aurevant.pkk.data.repo.OpsRepoImpl
import com.aurevant.pkk.data.repo.PayrollRepoImpl
import com.aurevant.pkk.data.repo.PeopleRepoImpl
import com.aurevant.pkk.data.repo.SaleRepoImpl
import com.aurevant.pkk.data.repo.ServiceRepoImpl
import com.aurevant.pkk.data.repo.SessionRepoImpl
import com.aurevant.pkk.data.repo.TeamRepoImpl
import com.aurevant.pkk.domain.repo.ApprovalRepo
import com.aurevant.pkk.domain.repo.AttendanceRepo
import com.aurevant.pkk.domain.repo.AuthRepo
import com.aurevant.pkk.domain.repo.CompanyRepo
import com.aurevant.pkk.domain.repo.CustomerRepo
import com.aurevant.pkk.domain.repo.DashboardRepo
import com.aurevant.pkk.domain.repo.FinanceRepo
import com.aurevant.pkk.domain.repo.NotifRepo
import com.aurevant.pkk.domain.repo.OpsRepo
import com.aurevant.pkk.domain.repo.PayrollRepo
import com.aurevant.pkk.domain.repo.PeopleRepo
import com.aurevant.pkk.domain.repo.SaleRepo
import com.aurevant.pkk.domain.repo.ServiceRepo
import com.aurevant.pkk.domain.repo.SessionRepo
import com.aurevant.pkk.domain.repo.TeamRepo
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
    @Binds abstract fun ops(repo: OpsRepoImpl): OpsRepo
    @Binds abstract fun service(repo: ServiceRepoImpl): ServiceRepo
    @Binds abstract fun customer(repo: CustomerRepoImpl): CustomerRepo
    @Binds abstract fun team(repo: TeamRepoImpl): TeamRepo
    @Binds abstract fun finance(repo: FinanceRepoImpl): FinanceRepo
    @Binds abstract fun payroll(repo: PayrollRepoImpl): PayrollRepo
}
