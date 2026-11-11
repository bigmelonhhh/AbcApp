package com.guguabc.kids.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.guguabc.kids.design.GuguColor
import com.guguabc.kids.design.GuguDimens
import com.guguabc.kids.design.GuguShadow
import com.guguabc.kids.design.GuguShadowBox
import com.guguabc.kids.design.GuguText
import com.guguabc.kids.design.GuguType
import com.guguabc.kids.design.guguClick

/** 点击即打开家长设置；复用通用点击反馈与防连点。 */
@Composable
fun ParentGateButton(
    size: Dp,
    iconRes: Int,
    iconSize: Dp,
    tag: String = "btn_parent",
    label: String? = null,
    onOpen: () -> Unit,
) {
    val width = if (label == null) size else GuguDimens.menuParentWidth
    val shape = RoundedCornerShape(size / 2)
    GuguShadowBox(
        spec = GuguShadow.parentGate,
        shape = shape,
        modifier = Modifier.size(width, size),
    ) {
        Box(
            modifier = Modifier
                .size(width, size)
                .background(GuguColor.surface, shape)
                .guguClick(tag = tag, onClick = onOpen),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GuguDimens.menuTextGap)) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
                if (label != null) GuguText(label, GuguType.categoryAction)
            }
        }
    }
}
