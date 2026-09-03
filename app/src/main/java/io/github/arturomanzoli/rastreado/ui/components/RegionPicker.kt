package io.github.arturomanzoli.rastreado.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.arturomanzoli.rastreado.domain.PhotoRegion
import kotlin.math.roundToInt

/** A rectangle the user drags over the photo, and what it is for. */
data class PickerRegion(
    val region: PhotoRegion,
    val label: String,
    val color: Color,
    val resizeDescription: String,
    val onChange: (PhotoRegion) -> Unit,
)

/**
 * Shows a captured photo with draggable, resizable rectangles over it.
 *
 * The frame takes the photo's own aspect ratio so the drawn image fills it exactly, which is what
 * lets a region in the photo's normalised space be positioned straight against the frame and
 * still mean the same thing when it is matched against the recognised text.
 */
@Composable
fun RegionPicker(
    photo: ImageBitmap,
    regions: List<PickerRegion>,
    modifier: Modifier = Modifier,
) {
    var frame by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            // Height first, so the picker fits a slot that is shorter than the photo is tall
            // rather than overflowing it.
            .aspectRatio(
                ratio = photo.width.toFloat() / photo.height.toFloat(),
                matchHeightConstraintsFirst = true,
            )
            .clip(RoundedCornerShape(16.dp))
            .onSizeChanged { frame = it },
    ) {
        Image(
            bitmap = photo,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        if (frame != IntSize.Zero) {
            regions.forEach { RegionBox(it, frame) }
        }
    }
}

@Composable
private fun BoxScope.RegionBox(region: PickerRegion, frame: IntSize) {
    val density = LocalDensity.current
    // The gesture callbacks outlive the composition that installed them, so the rectangle they
    // move has to be read fresh on each event or the box snaps back to its old place mid-drag.
    val current by rememberUpdatedState(region)
    val rect = region.region

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset {
                IntOffset(
                    x = (rect.left * frame.width).roundToInt(),
                    y = (rect.top * frame.height).roundToInt(),
                )
            }
            .width(with(density) { (rect.width * frame.width).toDp() })
            .height(with(density) { (rect.height * frame.height).toDp() })
            .border(2.dp, region.color, RoundedCornerShape(6.dp))
            .background(region.color.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .pointerInput(frame) {
                detectDragGestures { _, drag ->
                    current.onChange(
                        current.region.translated(drag.x / frame.width, drag.y / frame.height),
                    )
                }
            },
    ) {
        Text(
            text = region.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(3.dp)
                .background(region.color, RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .size(28.dp)
                .background(region.color, RoundedCornerShape(6.dp))
                .pointerInput(frame) {
                    detectDragGestures { _, drag ->
                        current.onChange(
                            current.region.resized(drag.x / frame.width, drag.y / frame.height),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.OpenInFull,
                contentDescription = region.resizeDescription,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
