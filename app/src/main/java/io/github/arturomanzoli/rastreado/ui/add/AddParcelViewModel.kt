package io.github.arturomanzoli.rastreado.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.arturomanzoli.rastreado.AppContainer
import io.github.arturomanzoli.rastreado.R
import io.github.arturomanzoli.rastreado.data.ParcelRepository
import io.github.arturomanzoli.rastreado.data.remote.ProviderResult
import io.github.arturomanzoli.rastreado.domain.OcrFieldExtractor
import io.github.arturomanzoli.rastreado.domain.ParcelStatus
import io.github.arturomanzoli.rastreado.domain.TrackingCode
import io.github.arturomanzoli.rastreado.work.providerLanguageTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CaptureMode { MANUAL, BARCODE, PHOTO }

data class AddParcelUiState(
    val code: String = "",
    val name: String = "",
    val mode: CaptureMode = CaptureMode.MANUAL,
    val isBusy: Boolean = false,
    val previewStatus: ParcelStatus? = null,
    val previewDescription: String? = null,
    val messageRes: Int? = null,
    val savedCode: String? = null,
) {
    val codeLooksValid: Boolean get() = TrackingCode.isPlausible(code)
    val canSave: Boolean get() = code.isNotBlank() && !isBusy
}

class AddParcelViewModel(private val repository: ParcelRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AddParcelUiState())
    val uiState: StateFlow<AddParcelUiState> = _uiState.asStateFlow()

    fun setCode(code: String) = _uiState.update {
        it.copy(code = code.uppercase(), previewStatus = null, previewDescription = null, messageRes = null)
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name) }

    fun setMode(mode: CaptureMode) = _uiState.update { it.copy(mode = mode, messageRes = null) }

    fun consumeMessage() = _uiState.update { it.copy(messageRes = null) }

    /** A scanned barcode replaces the code and leaves the capture pane. */
    fun onBarcodeScanned(raw: String) {
        val code = TrackingCode.normalize(raw)
        if (code.isEmpty()) return
        _uiState.update {
            it.copy(
                code = code,
                mode = CaptureMode.MANUAL,
                previewStatus = null,
                previewDescription = null,
                messageRes = if (TrackingCode.isPlausible(code)) null else R.string.add_code_unusual_shape,
            )
        }
    }

    /** OCR fills the name too, but never overwrites a name the user already typed. */
    fun onScreenRead(result: OcrFieldExtractor.Result) {
        if (result.code == null && result.productName == null) {
            _uiState.update { it.copy(mode = CaptureMode.MANUAL, messageRes = R.string.add_ocr_nothing_found) }
            return
        }
        _uiState.update { current ->
            current.copy(
                code = result.code ?: current.code,
                name = current.name.ifBlank { result.productName.orEmpty() },
                mode = CaptureMode.MANUAL,
                previewStatus = null,
                previewDescription = null,
            )
        }
    }

    /** Looks the code up without saving, so a typo is caught before it joins the list. */
    fun checkCode() {
        val code = TrackingCode.normalize(_uiState.value.code)
        if (code.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, messageRes = null) }
            val result = repository.preview(code, providerLanguageTag())
            _uiState.update { current ->
                when (result) {
                    is ProviderResult.Success -> current.copy(
                        isBusy = false,
                        previewStatus = result.snapshot.status,
                        previewDescription = result.snapshot.statusDescription,
                    )

                    is ProviderResult.Empty -> current.copy(
                        isBusy = false,
                        previewStatus = null,
                        messageRes = R.string.add_code_not_found_yet,
                    )

                    is ProviderResult.Failure -> current.copy(
                        isBusy = false,
                        previewStatus = null,
                        messageRes = R.string.add_check_failed,
                    )
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val code = repository.add(state.code, state.name.takeIf { name -> name.isNotBlank() })
            // Sync straight away so the parcel does not sit blank until the next period.
            repository.sync(code, providerLanguageTag())
            _uiState.update { it.copy(isBusy = false, savedCode = code) }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AddParcelViewModel(container.parcelRepository) }
        }
    }
}
