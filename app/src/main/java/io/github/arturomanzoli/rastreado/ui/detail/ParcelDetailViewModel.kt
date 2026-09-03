package io.github.arturomanzoli.rastreado.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.arturomanzoli.rastreado.AppContainer
import io.github.arturomanzoli.rastreado.data.ParcelRepository
import io.github.arturomanzoli.rastreado.domain.Parcel
import io.github.arturomanzoli.rastreado.domain.TrackingEvent
import io.github.arturomanzoli.rastreado.work.providerLanguageTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ParcelDetailUiState(
    val parcel: Parcel? = null,
    val events: List<TrackingEvent> = emptyList(),
    val isRefreshing: Boolean = false,
    val deleted: Boolean = false,
)

class ParcelDetailViewModel(
    private val repository: ParcelRepository,
    private val code: String,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<ParcelDetailUiState> = combine(
        repository.observeParcel(code),
        repository.observeEvents(code),
        refreshing,
        deleted,
    ) { parcel, events, isRefreshing, isDeleted ->
        ParcelDetailUiState(
            parcel = parcel,
            events = events,
            isRefreshing = isRefreshing,
            deleted = isDeleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ParcelDetailUiState())

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            try {
                repository.sync(code, providerLanguageTag())
            } finally {
                refreshing.value = false
            }
        }
    }

    fun rename(name: String) = viewModelScope.launch { repository.rename(code, name) }

    fun setArchived(archived: Boolean) = viewModelScope.launch { repository.setArchived(code, archived) }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setNotificationsEnabled(code, enabled)
    }

    fun setAlarmEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setAlarmEnabled(code, enabled)
    }

    fun delete() = viewModelScope.launch {
        repository.delete(code)
        deleted.value = true
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer, code: String) = viewModelFactory {
            initializer { ParcelDetailViewModel(container.parcelRepository, code) }
        }
    }
}
