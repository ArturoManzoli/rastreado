package io.github.arturomanzoli.rastreado.scan

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.arturomanzoli.rastreado.domain.OcrFieldExtractor

/** Recognises the text in a photo of an order screen and pulls the tracking fields out of it. */
class ScreenTextReader {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    suspend fun read(proxy: ImageProxy): OcrFieldExtractor.Result {
        val mediaImage = proxy.image ?: return OcrFieldExtractor.Result()
        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        val text = recognizer.process(image).await().text
        return OcrFieldExtractor.extract(text)
    }

    fun close() = recognizer.close()
}
