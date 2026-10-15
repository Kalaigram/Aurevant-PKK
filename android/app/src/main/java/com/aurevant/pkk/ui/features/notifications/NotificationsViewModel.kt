package com.aurevant.pkk.ui.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.Notif
import com.aurevant.pkk.domain.usecase.GetNotifications
import com.aurevant.pkk.domain.usecase.MarkAllRead
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface NotificationsAction {
    data object Load : NotificationsAction
    data object ReadAll : NotificationsAction
}

data class NotificationsState(
    val busy: Boolean = false,
    val error: String? = null,
    val showUnreadOnly: Boolean = false,
    val items: List<Notif> = emptyList(),
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val session: SessionStore,
    private val getNotifications: GetNotifications,
    private val markAllRead: MarkAllRead,
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState(busy = true))
    val state: StateFlow<NotificationsState> = _state

    fun handleAction(action: NotificationsAction) {
        when (action) {
            NotificationsAction.Load -> load()
            NotificationsAction.ReadAll -> viewModelScope.launch {
                markAllRead(session.companyId.first())
                load()
            }
        }
    }

    fun toggleUnread() {
        _state.value = _state.value.copy(showUnreadOnly = !_state.value.showUnreadOnly)
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val company = session.companyId.first()
            _state.value = try {
                NotificationsState(
                    showUnreadOnly = _state.value.showUnreadOnly,
                    items = getNotifications(company ?: 0, _state.value.showUnreadOnly),
                )
            } catch (e: ApiError) {
                NotificationsState(error = e.message)
            } catch (e: Exception) {
                NotificationsState(error = e.message ?: "Terjadi kesalahan.")
            }
        }
    }
}
