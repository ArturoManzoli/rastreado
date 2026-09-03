package io.github.arturomanzoli.rastreado.scan

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Reads parcel-label barcodes off the preview stream.
 *
 * The format list is restricted to what carriers actually print, which keeps the detector from
 * locking onto the product's own EAN barcode sitting next to the label.
 */
class BarcodeAnalyzer(private val onDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_PDF417,
            )
            .build(),
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                    ?.let(onDetected)
            }
            .addOnFailureListener { Log.w(TAG, "Barcode detection failed", it) }
            .addOnCompleteListener { proxy.close() }
    }

    fun close() = scanner.close()

    private companion object {
        const val TAG = "BarcodeAnalyzer"
    }
}
