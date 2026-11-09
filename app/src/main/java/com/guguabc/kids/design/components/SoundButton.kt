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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguShadowBox
import com.guguabc.kids.design.GuguShadowSpec
import com.guguabc.kids.design.guguClick

/** design.md §5：珊瑚底 + 白色喇叭；② 140dp / ③ 84dp。 */
@Composable
fun SoundButton(
    size: Dp,
    iconRes: Int,
    iconSize: Dp,
    shadow: GuguShadowSpec? = null,
    tag: String? = null,
    onClick: () -> Unit,
) {
    val inner: @Composable BoxScope.() -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GuguColor.coral, CircleShape)
                .guguClick(tag = tag, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
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
