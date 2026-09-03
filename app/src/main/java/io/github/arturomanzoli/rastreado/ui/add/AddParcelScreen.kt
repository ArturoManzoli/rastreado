package io.github.arturomanzoli.rastreado.ui.add

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state.mode) {
                CaptureMode.BARCODE -> BarcodeCapture(
                    onDetected = viewModel::onBarcodeScanned,
                    onCancel = { viewModel.setMode(CaptureMode.MANUAL) },
                )

                CaptureMode.PHOTO -> PhotoCapture(
                    onCaptured = viewModel::onScreenRead,
                    onCancel = { viewModel.setMode(CaptureMode.MANUAL) },
                )

                CaptureMode.MANUAL -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { requestCapture(CaptureMode.BARCODE) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                        Text(
                            text = stringResource(R.string.add_scan_barcode),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = { requestCapture(CaptureMode.PHOTO) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Text(
                            text = stringResource(R.string.add_photograph_screen),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
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

@Composable
private fun PhotoCapture(onCaptured: (OcrFieldExtractor.Result) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reader = remember { ScreenTextReader() }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var isReading by remember { mutableStateOf(false) }

    DisposableEffect(reader) { onDispose { reader.close() } }

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
                                        onCaptured(reader.read(image))
                                    } finally {
                                        image.close()
                                        isReading = false
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                isReading = false
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
