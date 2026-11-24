package com.guguabc.kids.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.guguabc.kids.data.ColorId
import com.guguabc.kids.data.ColorObject
import com.guguabc.kids.data.Strings
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguDimens

internal val showcaseColors = listOf(ColorId.RED, ColorId.YELLOW, ColorId.BLUE)

internal val ColorId.swatch: Color
    get() = when (this) {
        ColorId.RED -> GuguColor.learningRed
        ColorId.YELLOW -> GuguColor.learningYellow
        ColorId.BLUE -> GuguColor.learningBlue
        ColorId.GREEN -> GuguColor.learningGreen
        ColorId.ORANGE -> GuguColor.learningOrange
        ColorId.PURPLE -> GuguColor.learningPurple
    }

@Composable
internal fun ColorArt(kind: ColorObject, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val factor = size.minDimension / 100f
        translate((size.width - 100f * factor) / 2, (size.height - 100f * factor) / 2) {
            scale(factor, factor, Offset.Zero) { drawColorObject(kind, tint) }
        }
    }
}

/** Vector paths use the same 100-unit viewport for every color of an object. */
private fun DrawScope.drawColorObject(kind: ColorObject, fill: Color) {
    val stroke = GuguColor.artOutline
    when (kind) {
        ColorObject.BALLOON -> {
            val thread = Path().apply {
                moveTo(50f, 69f)
                cubicTo(62f, 81f, 39f, 81f, 49f, 95f)
            }
            drawPath(thread, stroke, style = Stroke(width = 2f, cap = StrokeCap.Round))
            val knot = Path().apply {
                moveTo(50f, 64f)
                lineTo(56f, 73f)
                lineTo(44f, 73f)
                close()
            }
            drawPath(knot, fill)
            drawPath(knot, stroke, style = Stroke(2f))
            drawOval(fill, Offset(24f, 5f), Size(52f, 63f))
            drawOval(stroke, Offset(24f, 5f), Size(52f, 63f), style = Stroke(2f))
            drawOval(GuguColor.artShine, Offset(33f, 17f), Size(9f, 17f))
        }
        ColorObject.BLOCK -> {
            drawRoundRect(fill, Offset(15f, 15f), Size(70f, 70f), CornerRadius(12f))
            drawRoundRect(stroke, Offset(15f, 15f), Size(70f, 70f), CornerRadius(12f), style = Stroke(2f))
            drawRoundRect(GuguColor.artShine, Offset(25f, 24f), Size(30f, 7f), CornerRadius(4f))
        }
        ColorObject.CAR -> {
            val cabin = Path().apply {
                moveTo(21f, 49f)
                lineTo(34f, 31f)
                quadraticTo(37f, 27f, 42f, 27f)
                lineTo(62f, 27f)
                quadraticTo(68f, 27f, 71f, 32f)
                lineTo(84f, 50f)
                close()
            }
            drawPath(cabin, GuguColor.artWindow)
            drawPath(cabin, stroke, style = Stroke(2f))
            drawLine(stroke, Offset(53f, 28f), Offset(53f, 49f), 2f)
            drawRoundRect(fill, Offset(8f, 47f), Size(84f, 25f), CornerRadius(10f))
            drawRoundRect(stroke, Offset(8f, 47f), Size(84f, 25f), CornerRadius(10f), style = Stroke(2f))
            for (x in listOf(27f, 73f)) {
                drawCircle(GuguColor.artWheel, 10f, Offset(x, 72f))
                drawCircle(GuguColor.artWindow, 4f, Offset(x, 72f))
            }
        }
    }
}

@Composable
internal fun ColorBalloons(colors: List<ColorId>, modifier: Modifier) {
    Canvas(modifier) {
        val factor = size.minDimension / 110f
        translate((size.width - 110f * factor) / 2, (size.height - 110f * factor) / 2) {
            scale(factor, factor, Offset.Zero) {
                colors.forEachIndexed { index, color ->
                    translate(25f * index, if (index == 1) 4f else 26f) {
                        scale(0.6f, 0.6f, Offset.Zero) { drawColorObject(ColorObject.BALLOON, color.swatch) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BalloonPainting(colors: List<ColorId?>, modifier: Modifier, activeIndex: Int? = null) {
    Row(modifier, horizontalArrangement = Arrangement.Center) {
        colors.forEachIndexed { index, color ->
            val active = index == activeIndex
            val label = when {
                color != null -> "${Strings.COLOR_PAINTED_BALLOON} ${color.zh}"
                active -> Strings.COLOR_CURRENT_BALLOON
                else -> Strings.COLOR_EMPTY_BALLOON
            }
            val highlight = if (active) {
                Modifier.background(GuguColor.colorWorld, RoundedCornerShape(GuguDimens.menuCardRadius))
            } else Modifier
            ColorArt(
                ColorObject.BALLOON, color?.swatch ?: GuguColor.surface,
                Modifier.fillMaxHeight().aspectRatio(0.66f).then(highlight).semantics { contentDescription = label },
            )
        }
    }
}
