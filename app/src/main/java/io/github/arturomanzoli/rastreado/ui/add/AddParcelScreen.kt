package io.github.arturomanzoli.rastreado.ui.add

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arturomanzoli.rastreado.R
import io.github.arturomanzoli.rastreado.domain.OcrFieldExtractor
import io.github.arturomanzoli.rastreado.scan.BarcodeAnalyzer
import io.github.arturomanzoli.rastreado.scan.ScreenTextReader
import io.github.arturomanzoli.rastreado.ui.components.CameraPane
import io.github.arturomanzoli.rastreado.ui.components.PickerRegion
import io.github.arturomanzoli.rastreado.ui.components.RegionPicker
import io.github.arturomanzoli.rastreado.ui.labelRes
import io.github.arturomanzoli.rastreado.ui.rememberContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddParcelScreen(onClose: () -> Unit, onSaved: (String) -> Unit) {
    val container = rememberContainer()
    val viewModel: AddParcelViewModel = viewModel(factory = AddParcelViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var pendingMode by remember { mutableStateOf<CaptureMode?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (granted) pendingMode?.let(viewModel::setMode)
        pendingMode = null
    }

    fun requestCapture(mode: CaptureMode) {
        if (hasCameraPermission) {
            viewModel.setMode(mode)
        } else {
            pendingMode = mode
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    state.savedCode?.let { code ->
        LaunchedEffect(code) { onSaved(code) }
    }

    state.messageRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes) {
            snackbarHost.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        val surface = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)

        // A capture pane owns the whole surface: it has its own actions, the form below it is of
        // no use mid-capture, and its drag gestures must not compete with a scrolling ancestor.
        when (state.mode) {
            CaptureMode.BARCODE -> Box(surface) {
                BarcodeCapture(
                    onDetected = viewModel::onBarcodeScanned,
                    onCancel = { viewModel.setMode(CaptureMode.MANUAL) },
                )
            }

            CaptureMode.PHOTO -> Box(surface) {
                PhotoCapture(
                    onCaptured = viewModel::onScreenRead,
                    onNoText = viewModel::onScreenUnreadable,
                    onCancel = { viewModel.setMode(CaptureMode.MANUAL) },
                )
            }

            CaptureMode.MANUAL -> ManualForm(
                state = state,
                viewModel = viewModel,
                onCapture = ::requestCapture,
                modifier = surface,
            )
        }
    }
}

@Composable
private fun ManualForm(
    state: AddParcelUiState,
    viewModel: AddParcelViewModel,
    onCapture: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { onCapture(CaptureMode.BARCODE) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                Text(
                    text = stringResource(R.string.add_scan_barcode),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(
                onClick = { onCapture(CaptureMode.PHOTO) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Text(
                    text = stringResource(R.string.add_photograph_screen),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        OutlinedTextField(
            value = state.code,
            onValueChange = viewModel::setCode,
            label = { Text(stringResource(R.string.add_code_label)) },
            supportingText = {
                Text(
                    if (state.code.isBlank() || state.codeLooksValid) {
                        stringResource(R.string.add_code_hint)
                    } else {
                        stringResource(R.string.add_code_unusual_shape)
                    },
                )
            },
            isError = state.code.isNotBlank() && !state.codeLooksValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text(stringResource(R.string.add_name_label)) },
            supportingText = { Text(stringResource(R.string.add_name_hint)) },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        state.previewStatus?.let { status ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(status.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    state.previewDescription?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = viewModel::checkCode,
                enabled = state.code.isNotBlank() && !state.isBusy,
            ) {
                Text(stringResource(R.string.add_check_code))
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = viewModel::save, enabled = state.canSave) {
                if (state.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BarcodeCapture(onDetected: (String) -> Unit, onCancel: () -> Unit) {
    val analyzer = remember { BarcodeAnalyzer(onDetected) }
    DisposableEffect(analyzer) { onDispose { analyzer.close() } }

    CaptureFrame(
        instruction = stringResource(R.string.add_barcode_instruction),
        onCancel = onCancel,
    ) {
        CameraPane(modifier = Modifier.fillMaxSize(), analyzer = analyzer)
    }
}

/**
 * Photographing the order screen, then marking the two fields on the still.
 *
 * The recognised text is kept with the photo so moving a region only re-reads which lines it
 * covers, rather than running the recogniser again per adjustment.
 */
@Composable
private fun PhotoCapture(
    onCaptured: (OcrFieldExtractor.Result) -> Unit,
    onNoText: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reader = remember { ScreenTextReader() }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var isReading by remember { mutableStateOf(false) }
    var review by remember { mutableStateOf<ScreenTextReader.Capture?>(null) }

    DisposableEffect(reader) { onDispose { reader.close() } }

    review?.let { captured ->
        MarkRegions(
            captured = captured,
            onConfirm = onCaptured,
            onRetake = { review = null },
            onCancel = onCancel,
        )
        return
    }

    CaptureFrame(
        instruction = stringResource(R.string.add_photo_instruction),
        onCancel = onCancel,
        action = {
            Button(
                onClick = {
                    val imageCapture = capture ?: return@Button
                    isReading = true
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                scope.launch {
                                    try {
                                        val read = reader.read(image)
                                        if (read == null || read.lines.isEmpty()) {
                                            onNoText()
                                        } else {
                                            review = read
                                        }
                                    } finally {
                                        image.close()
                                        isReading = false
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                isReading = false
                                onNoText()
                            }
                        },
                    )
                },
                enabled = capture != null && !isReading,
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Text(stringResource(R.string.add_take_photo), modifier = Modifier.padding(start = 8.dp))
            }
        },
    ) {
        CameraPane(modifier = Modifier.fillMaxSize(), onImageCapture = { capture = it })
    }
}

@Composable
private fun MarkRegions(
    captured: ScreenTextReader.Capture,
    onConfirm: (OcrFieldExtractor.Result) -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
) {
    val suggestion = remember(captured) { OcrFieldExtractor.suggestRegions(captured.lines) }
    var codeRegion by remember(captured) { mutableStateOf(suggestion.code) }
    var nameRegion by remember(captured) { mutableStateOf(suggestion.name) }

    val photo = remember(captured) { captured.photo.asImageBitmap() }
    val codeText = OcrFieldExtractor.textIn(captured.lines, codeRegion)
    val nameText = OcrFieldExtractor.textIn(captured.lines, nameRegion)
    val code = OcrFieldExtractor.codeIn(codeText)

    val codeColor = MaterialTheme.colorScheme.primary
    val nameColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The photo takes whatever the readouts and actions leave, so the step never needs to
        // scroll and a drag over the image is unambiguous.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            RegionPicker(
                photo = photo,
                regions = listOf(
                    PickerRegion(
                        region = codeRegion,
                        label = stringResource(R.string.add_region_code),
                        color = codeColor,
                        resizeDescription = stringResource(R.string.add_region_resize_code),
                        onChange = { codeRegion = it },
                    ),
                    PickerRegion(
                        region = nameRegion,
                        label = stringResource(R.string.add_region_name),
                        color = nameColor,
                        resizeDescription = stringResource(R.string.add_region_resize_name),
                        onChange = { nameRegion = it },
                    ),
                ),
            )
        }

        Text(
            text = stringResource(R.string.add_regions_instruction),
            style = MaterialTheme.typography.bodySmall,
        )
        RegionReadout(
            label = stringResource(R.string.add_region_code),
            value = code ?: codeText.ifBlank { null },
            isValid = code != null,
            color = codeColor,
        )
        RegionReadout(
            label = stringResource(R.string.add_region_name),
            value = nameText.ifBlank { null },
            isValid = nameText.isNotBlank(),
            color = nameColor,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            OutlinedButton(onClick = onRetake) { Text(stringResource(R.string.add_retake_photo)) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    onConfirm(
                        OcrFieldExtractor.Result(
                            code = code,
                            productName = OcrFieldExtractor.nameIn(nameText),
                        ),
                    )
                },
                enabled = code != null || nameText.isNotBlank(),
            ) {
                Text(stringResource(R.string.add_use_regions))
            }
        }
    }
}

@Composable
private fun RegionReadout(label: String, value: String?, isValid: Boolean, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value ?: stringResource(R.string.add_region_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isValid) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun CaptureFrame(
    instruction: String,
    onCancel: () -> Unit,
    action: @Composable (() -> Unit)? = null,
    preview: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            preview()
        }
        Text(text = instruction, style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            Spacer(Modifier.weight(1f))
            action?.invoke()
        }
    }
}
