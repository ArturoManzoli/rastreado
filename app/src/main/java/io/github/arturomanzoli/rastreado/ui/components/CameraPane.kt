package io.github.arturomanzoli.rastreado.ui.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * A camera preview bound to the composition's lifecycle.
 *
 * [onAnalyzer] receives a frame analyser to attach when the caller wants live analysis, and
 * [onImageCapture] hands back the capture use case so the caller can take a still.
 */
@Composable
fun CameraPane(
    modifier: Modifier = Modifier,
    analyzer: ImageAnalysis.Analyzer? = null,
    onImageCapture: (ImageCapture) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
    }

    LaunchedEffect(imageCapture) { onImageCapture(imageCapture) }

    DisposableEffect(analyzer, lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener({
            provider = providerFuture.get()

            val preview = Preview.Builder().build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val analysis = analyzer?.let { frameAnalyzer ->
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, frameAnalyzer) }
            }

            val useCases = listOfNotNull(preview, imageCapture, analysis).toTypedArray()

            runCatching {
                provider?.unbindAll()
                provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, *useCases)
            }
        }, executor)

        onDispose { provider?.unbindAll() }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
