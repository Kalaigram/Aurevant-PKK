package com.aurevant.pkk.ui.features.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.aurevant.pkk.domain.model.EmployeeCard
import com.aurevant.pkk.domain.usecase.GetCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface CardAction {
    data object Load : CardAction
}

data class CardState(
    val busy: Boolean = false,
    val error: String? = null,
    val card: EmployeeCard? = null,
)

@HiltViewModel
class CardViewModel @Inject constructor(
    private val session: SessionStore,
    private val getCard: GetCard,
) : ViewModel() {
    private val _state = MutableStateFlow(CardState(busy = true))
    val state: StateFlow<CardState> = _state

    fun handleAction(action: CardAction) {
        when (action) {
            CardAction.Load -> viewModelScope.launch {
                val company = session.companyId.first()
                val user = session.userId.first()
                if (company == null || user == null) {
                    _state.value = CardState(error = "Pilih perusahaan dulu.")
                    return@launch
                }
                _state.value = try {
                    CardState(card = getCard(company, user))
                } catch (e: ApiError) {
                    CardState(error = e.message)
                } catch (e: Exception) {
                    CardState(error = e.message ?: "Terjadi kesalahan.")
                }
            }
        }
    }
}
