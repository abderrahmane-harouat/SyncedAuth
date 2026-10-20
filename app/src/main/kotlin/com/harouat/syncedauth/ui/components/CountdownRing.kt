package com.harouat.syncedauth.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harouat.syncedauth.ui.theme.LocalSaColors
import com.harouat.syncedauth.ui.theme.SaType
import kotlin.math.abs

/**
 * Mirrored countdown ring: blue above 10s, amber 5–10s, red under 5s.
 * Track is the same color at 20%; progress drains linearly over the second.
 *
 * Performance note: the drain is animated with an `Animatable` that is read
 * ONLY inside the draw phase (the Canvas lambda). That confines each-frame
 * invalidation to redrawing this tiny canvas — with the previous
 * `animateFloatAsState`/`animateColorAsState` pair (whose values were read
 * during composition), every visible card recomposed 60×/s forever, making
 * scrolling and idle rendering janky on slower devices.
 */
@Composable
fun CountdownRing(
    secondsRemaining: Int,
    period: Int,
    modifier: Modifier = Modifier,
    ringSize: Dp = 36.dp,
) {
    val sa = LocalSaColors.current
    val ringColor = when {
        secondsRemaining <= 5 -> sa.danger
        secondsRemaining <= 10 -> sa.warning
        else -> sa.focus
    }

    val fraction = remember(secondsRemaining) { Animatable(secondsRemaining / period.toFloat()) }
    LaunchedEffect(secondsRemaining, period) {
        val target = secondsRemaining / period.toFloat()
        if (abs(target - fraction.value) < 0.0001f) {
            fraction.snapTo(target) // fresh card: nothing to drain, stay static
        } else {
            fraction.animateTo(target, tween(durationMillis = 1000, easing = LinearEasing))
        }
    }

    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 3.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = ringColor.copy(alpha = 0.20f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = secondsRemaining.toString(),
            style = SaType.compact.copy(color = sa.textNormal),
        )
    }
}
