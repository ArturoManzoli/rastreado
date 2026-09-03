package io.github.arturomanzoli.rastreado.ui.parcels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.arturomanzoli.rastreado.AppContainer
import io.github.arturomanzoli.rastreado.data.ParcelRepository
import io.github.arturomanzoli.rastreado.domain.Parcel
import io.github.arturomanzoli.rastreado.work.providerLanguageTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ParcelListUiState(
    val active: List<Parcel> = emptyList(),
    val archived: List<Parcel> = emptyList(),
    val isRefreshing: Boolean = false,
)

class ParcelListViewModel(private val repository: ParcelRepository) : ViewModel() {

    private val refreshing = MutableStateFlow(false)

    val uiState: StateFlow<ParcelListUiState> = combine(
        repository.observeActive(),
        repository.observeArchived(),
        refreshing,
    ) { active, archived, isRefreshing ->
        ParcelListUiState(active = active, archived = archived, isRefreshing = isRefreshing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ParcelListUiState())

    /**
     * A foreground refresh, which deliberately posts nothing: the user is looking at the list, so
     * the updated rows are the feedback and a notification would duplicate them.
     */
    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            try {
                repository.syncAll(providerLanguageTag())
            } finally {
                refreshing.value = false
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ParcelListViewModel(container.parcelRepository) }
        }
    }
}
