package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.VideoItem
import com.example.ui.theme.LocalGlassPalette
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class TapGlowPulse(
    val id: Long,
    val center: Offset,
    val progress: Animatable<Float, *> = Animatable(0f)
)

/**
 * Root container that renders a luminous liquid-crystal double-ripple glow at ANY point
 * on screen where the user taps, without intercepting or blocking child gestures.
 */
@Composable
fun TapGlowOverlayBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassPalette.current
    val pulses = remember { mutableStateListOf<TapGlowPulse>() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { change ->
                        if (change.changedToDown()) {
                            val pulse = TapGlowPulse(
                                id = System.nanoTime(),
                                center = change.position
                            )
                            pulses.add(pulse)
                            scope.launch {
                                pulse.progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
                                )
                                pulses.remove(pulse)
                            }
                        }
                    }
                }
            }
        }
    ) {
        content()
        Canvas(modifier = Modifier.fillMaxSize()) {
            pulses.forEach { pulse ->
                val p = pulse.progress.value
                val alpha = (1f - p)
                val maxRadius = 190f
                val radius = 20f + maxRadius * p

                // Outer luminous radial bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = alpha * 0.34f),
                            glass.tapGlowPrimary.copy(alpha = alpha * 0.56f),
                            glass.tapGlowSecondary.copy(alpha = alpha * 0.30f),
                            Color.Transparent
                        ),
                        center = pulse.center,
                        radius = radius
                    ),
                    radius = radius,
                    center = pulse.center
                )

                // Specular caustic shockwave ring
                val ringRadius = 14f + (maxRadius * 0.78f) * p
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            glass.tapGlowPrimary.copy(alpha = alpha * 0.78f),
                            Color.White.copy(alpha = alpha * 0.85f),
                            glass.tapGlowSecondary.copy(alpha = alpha * 0.68f),
                            glass.tapGlowPrimary.copy(alpha = alpha * 0.78f)
                        ),
                        center = pulse.center
                    ),
                    radius = ringRadius,
                    center = pulse.center,
                    style = Stroke(width = (3.4f * (1f - p * 0.7f)).dp.toPx())
                )
            }
        }
    }
}

/**
 * Ambient liquid glass background with multi-layered flowing aurora orbs and caustic light wave.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassPalette.current
    val bgColor = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDrift"
    )
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraPhase"
    )

    Box(
        modifier = modifier
            .background(bgColor)
            .drawBehind {
                val w = size.width
                val h = size.height

                // Orb 1: Top-left crimson-rose liquid orb
                val orb1Center = Offset(
                    x = w * (0.14f + 0.16f * cos(phase * 0.5f)),
                    y = h * (0.10f + 0.05f * drift)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glass.ambientAuroraOne, Color.Transparent),
                        center = orb1Center,
                        radius = w * 0.75f
                    ),
                    radius = w * 0.75f,
                    center = orb1Center
                )

                // Orb 2: Mid-right cyan refraction orb
                val orb2Center = Offset(
                    x = w * (0.86f - 0.18f * drift),
                    y = h * (0.36f + 0.07f * sin(phase * 0.5f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glass.ambientAuroraTwo, Color.Transparent),
                        center = orb2Center,
                        radius = w * 0.80f
                    ),
                    radius = w * 0.80f,
                    center = orb2Center
                )

                // Orb 3: Center-left violet crystal bloom
                val orb3Center = Offset(
                    x = w * (0.24f + 0.14f * drift),
                    y = h * (0.64f - 0.06f * cos(phase))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glass.ambientAuroraThree, Color.Transparent),
                        center = orb3Center,
                        radius = w * 0.72f
                    ),
                    radius = w * 0.72f,
                    center = orb3Center
                )

                // Orb 4: Bottom ambient refraction glow behind floating bottom bar
                val orb4Center = Offset(
                    x = w * (0.55f - 0.12f * drift),
                    y = h * 0.90f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glass.ambientAuroraTwo.copy(alpha = glass.ambientAuroraTwo.alpha * 0.8f),
                            Color.Transparent
                        ),
                        center = orb4Center,
                        radius = w * 0.82f
                    ),
                    radius = w * 0.82f,
                    center = orb4Center
                )

                // Subtle flowing caustic light ribbon
                val wavePath = Path().apply {
                    val yBase = h * (0.28f + 0.04f * drift)
                    moveTo(-w * 0.1f, yBase)
                    cubicTo(
                        w * 0.25f, yBase - h * 0.10f * sin(phase),
                        w * 0.65f, yBase + h * 0.12f * cos(phase),
                        w * 1.1f, yBase - h * 0.04f
                    )
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            glass.tapGlowPrimary.copy(alpha = if (glass.isDark) 0.10f else 0.12f),
                            glass.tapGlowSecondary.copy(alpha = if (glass.isDark) 0.09f else 0.10f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 64.dp.toPx())
                )
            }
    ) {
        content()
    }
}

@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(22.dp),
    glowHighlighted: Boolean = false,
    elevated: Boolean = false
): Modifier {
    val glass = LocalGlassPalette.current
    val fillBrush = Brush.linearGradient(
        colors = listOf(
            glass.glassFillTop,
            glass.glassFillMid,
            glass.glassFillBottom
        ),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    val borderBrush = if (glowHighlighted) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (glass.isDark) 0.9f else 1f),
                glass.tapGlowPrimary.copy(alpha = 0.92f),
                glass.tapGlowSecondary.copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                glass.glassBorderHighlight,
                glass.glassRefractionTint,
                glass.glassBorderEdge
            )
        )
    }
    val shadowElevation = when {
        glowHighlighted -> 14.dp
        elevated -> 10.dp
        else -> 4.dp
    }
    val glowSpotColor = if (glowHighlighted) glass.tapGlowPrimary else glass.glassRefractionTint

    return this
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            clip = false,
            ambientColor = glowSpotColor.copy(alpha = if (glass.isDark) 0.45f else 0.25f),
            spotColor = glowSpotColor.copy(alpha = if (glass.isDark) 0.55f else 0.30f)
        )
        .clip(shape)
        .background(fillBrush, shape)
        .drawWithContent {
            // Diagonal crystal specular reflection across upper glass facet
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        glass.glassSpecular.copy(alpha = if (glowHighlighted) 0.36f else 0.24f),
                        glass.glassSpecular.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.72f, size.height * 0.56f)
                )
            )
            // Subtle bottom-right chromatic refraction tint
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.glassRefractionTint.copy(alpha = if (glowHighlighted) 0.28f else 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.92f),
                    radius = size.maxDimension * 0.65f
                )
            )
            drawContent()
            // Top liquid meniscus highlight sheen along upper edge
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (glass.isDark) 0.22f else 0.45f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = (size.height * 0.18f).coerceAtMost(28.dp.toPx())
                ),
                size = Size(size.width, (size.height * 0.18f).coerceAtMost(28.dp.toPx()))
            )
        }
        .border(
            width = if (glowHighlighted) 1.5.dp else 1.15.dp,
            brush = borderBrush,
            shape = shape
        )
}

@Composable
fun Modifier.glowScaleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tapScale"
    )
    val glass = LocalGlassPalette.current
    return this
        .scale(scale)
        .drawWithContent {
            drawContent()
            if (isPressed) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.24f),
                            glass.tapGlowPrimary.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.maxDimension * 0.75f
                    )
                )
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    contentPadding: Dp = 14.dp,
    glowHighlighted: Boolean = false,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier.liquidGlass(
        shape = shape,
        glowHighlighted = glowHighlighted,
        elevated = elevated
    )
    val finalModifier = if (onClick != null) {
        baseModifier.glowScaleClickable(onClick = onClick)
    } else {
        baseModifier
    }
    Box(
        modifier = finalModifier.padding(contentPadding),
        content = content
    )
}

@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        LiquidGlassBox(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            cornerRadius = 26.dp,
            contentPadding = 20.dp,
            glowHighlighted = true,
            elevated = true
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
fun DynamicIslandPill(
    video: VideoItem?,
    isPlaying: Boolean,
    visible: Boolean,
    onTogglePlay: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    val shimmerTransition = rememberInfiniteTransition(label = "islandShimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "islandSweep"
    )

    AnimatedVisibility(
        visible = visible && video != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn() + scaleIn(initialScale = 0.85f),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        if (video != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = glass.tapGlowPrimary.copy(alpha = 0.5f),
                        spotColor = glass.tapGlowSecondary.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(glass.islandSurface)
                    .drawWithContent {
                        // Top specular crystal gleam inside the Dynamic Island
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.48f
                            )
                        )
                        // Moving liquid shimmer sweep
                        val sweepX = size.width * shimmerOffset
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    glass.tapGlowPrimary.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.16f),
                                    Color.Transparent
                                ),
                                start = Offset(sweepX - size.width * 0.25f, 0f),
                                end = Offset(sweepX + size.width * 0.25f, size.height)
                            )
                        )
                        drawContent()
                    }
                    .border(
                        width = 1.4.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.7f),
                                glass.tapGlowPrimary.copy(alpha = 0.92f),
                                glass.tapGlowSecondary.copy(alpha = 0.88f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .glowScaleClickable { onExpand() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("dynamic_island_pill"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = thumbnailResForKey(video.thumbnailResKey)),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.2.dp, glass.tapGlowPrimary.copy(alpha = 0.85f), CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = glass.tapGlowPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlaying) "DYNAMIC ISLAND • BACKGROUND PLAY" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.tapGlowPrimary,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                MiniEqualizerBars(isPlaying = isPlaying, barColor = glass.tapGlowPrimary)
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .testTag("island_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onExpand,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInFull,
                        contentDescription = "Expand player",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close mini player",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniEqualizerBars(isPlaying: Boolean, barColor: Color) {
    val transition = rememberInfiniteTransition(label = "eq")
    val b1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(380), RepeatMode.Reverse),
        label = "eq1"
    )
    val b2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(460), RepeatMode.Reverse),
        label = "eq2"
    )
    val b3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(320), RepeatMode.Reverse),
        label = "eq3"
    )
    val heights = if (isPlaying) listOf(b1, b2, b3) else listOf(0.25f, 0.25f, 0.25f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(16.dp)
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16f * h).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

fun thumbnailResForKey(key: String): Int = when (key) {
    "cosmos" -> R.drawable.img_thumb_cosmos
    "lofi_studio" -> R.drawable.img_thumb_lofi_studio
    "tech_glass" -> R.drawable.img_thumb_tech_glass
    "alpine_4k" -> R.drawable.img_thumb_alpine_4k
    else -> R.drawable.img_thumb_cosmos
}
