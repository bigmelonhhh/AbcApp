package com.guguabc.kids

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.guguabc.kids.design.GuguDimens
import com.guguabc.kids.design.GuguTheme
import com.guguabc.kids.state.GuguNavHost
import kotlin.math.min

/**
 * design.md §3.1 / §3.4 / 附录 C ①：
 * 固定 1280 × 800 设计画布。1dp 恒等于设计稿 1 单位，fontScale 锁 1f。
 * 默认采用「固定设计 density」：在 1920×1200 上正好 = 1.5，与 §3.1 假设一致；
 * 在其它分辨率上自动兜底，不会出现整体偏小 / 偏大，也不需要重算任何 Token。
 * 真机 DisplayMetrics 由 MainActivity 仅 DEBUG 输出。
 */
private const val FIXED_DESIGN_DENSITY = true

@Composable
fun GuguApp() {
    val dm = LocalContext.current.resources.displayMetrics
    val fixed = min(
        dm.widthPixels / GuguDimens.screenWidth.value,
        dm.heightPixels / GuguDimens.screenHeight.value,
    )
    val chosen = if (FIXED_DESIGN_DENSITY) fixed else LocalDensity.current.density

    CompositionLocalProvider(
        LocalDensity provides Density(density = chosen, fontScale = 1f)
    ) {
        GuguTheme {
            GuguNavHost()
        }
    }
}
