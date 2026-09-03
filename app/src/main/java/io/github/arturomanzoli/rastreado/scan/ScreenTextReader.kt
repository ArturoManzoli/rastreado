package io.github.arturomanzoli.rastreado.scan

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.arturomanzoli.rastreado.domain.OcrLine
import io.github.arturomanzoli.rastreado.domain.PhotoRegion

/** Recognises the text in a photo of an order screen, keeping where each line sits. */
class ScreenTextReader {

    /** The upright photo and its recognised lines, both in the same coordinate space. */
    data class Capture(val photo: Bitmap, val lines: List<OcrLine>)

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Recognises from a bitmap rather than the captured media image, because `ImageCapture`
     * delivers JPEG and ML Kit only accepts YUV or NV21 media images. Rotating the bitmap upright
     * first also means the reported boxes and the photo the user drags over share one geometry.
     */
    suspend fun read(proxy: ImageProxy): Capture? {
        val photo = proxy.toUprightBitmap() ?: return null
        val text = recognizer.process(InputImage.fromBitmap(photo, 0)).await()
        return Capture(photo, text.toLines(photo.width, photo.height))
    }

    fun close() = recognizer.close()
}

/**
 * Rotates the capture upright and caps its long edge: a full-resolution frame is tens of
 * megabytes held in memory for the whole review, and text on a photographed screen is legible
 * well below that.
 */
private fun ImageProxy.toUprightBitmap(): Bitmap? {
    val source = runCatching { toBitmap() }.getOrNull() ?: return null
    val scale = (MAX_EDGE.toFloat() / maxOf(source.width, source.height)).coerceAtMost(1f)
    val rotation = imageInfo.rotationDegrees.toFloat()
    if (scale == 1f && rotation == 0f) return source

    val matrix = Matrix().apply {
        postRotate(rotation)
        postScale(scale, scale)
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        .also { if (it !== source) source.recycle() }
}

private fun Text.toLines(width: Int, height: Int): List<OcrLine> {
    if (width <= 0 || height <= 0) return emptyList()
    return textBlocks
        .flatMap { it.lines }
        .mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            OcrLine(
                text = line.text,
                bounds = PhotoRegion(
                    left = box.left.toFloat() / width,
                    top = box.top.toFloat() / height,
                    right = box.right.toFloat() / width,
                    bottom = box.bottom.toFloat() / height,
                ),
            )
        }
}

private const val MAX_EDGE = 1920
