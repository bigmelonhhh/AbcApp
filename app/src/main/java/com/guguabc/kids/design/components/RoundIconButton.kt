package com.guguabc.kids.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguShadowBox
import com.guguabc.kids.design.GuguShadowSpec
import com.guguabc.kids.design.guguClick

/** design.md §5：白底全圆角圆钮。72dp（②③④）/ 80dp（① 家长）。 */
@Composable
fun RoundIconButton(
    size: Dp,
    iconRes: Int,
    iconSize: Dp,
    tag: String? = null,
    shadow: GuguShadowSpec? = null,
    iconAlpha: Float = 1f,
    onClick: () -> Unit,
) {
    val inner: @Composable BoxScope.() -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GuguColor.surface, CircleShape)
                .guguClick(tag = tag, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize).alpha(iconAlpha),
                contentScale = ContentScale.Fit,
            )
        }
    }
    if (shadow != null) {
        GuguShadowBox(spec = shadow, shape = CircleShape, modifier = Modifier.size(size), content = inner)
    } else {
        Box(modifier = Modifier.size(size), content = inner)
    }
}
