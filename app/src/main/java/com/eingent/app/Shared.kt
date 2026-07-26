package com.eingent.app

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val BgColor = Color(0xFF0F0F11)
val BubbleColor = Color(0xFF282930)

fun Modifier.glassBorder(cornerRadius: Dp): Modifier = drawBehind {
    val strokePx = 0.5.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x55FFFFFF), Color(0x0FFFFFFF))
        ),
        topLeft = Offset(strokePx / 2, strokePx / 2),
        size = Size(size.width - strokePx, size.height - strokePx),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(strokePx)
    )
}

fun Modifier.bottomFade(height: Dp): Modifier = drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to BgColor,
            startY = size.height - height.toPx(),
            endY = size.height
        )
    )
}
