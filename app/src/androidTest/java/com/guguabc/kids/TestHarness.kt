package com.guguabc.kids

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.guguabc.kids.design.GuguTheme

/**
 * 组件级 Compose 测试的公共容器：固定 1280×800dp 设计画布。
 * density 取 0.6f → 768×480px，竖屏模拟器（1200×1920）也能完整容纳首页
 * (1140,48) 一类的绝对定位元素，performClick 不会因节点超出视口而失败。
 * fontScale 锁 1f，与 GuguApp 的固定设计 density 口径保持一致。
 */
fun ComposeContentTestRule.setGuguScreen(content: @Composable () -> Unit) {
    setContent {
        CompositionLocalProvider(
            LocalDensity provides Density(density = 0.6f, fontScale = 1f)
        ) {
            GuguTheme {
                Box(Modifier.size(1280.dp, 800.dp)) { content() }
            }
        }
    }
}

/**
 * guguClick 的 400ms 防连点读真实 SystemClock，且 lastClickAt 是「每个点击点」
 * 各自 remember 的，因此只有连续点同一个 tag 才需要真实等待。
 */
fun ComposeContentTestRule.tap(tag: String, sameTagDelayMs: Long = 0L) {
    if (sameTagDelayMs > 0) Thread.sleep(sameTagDelayMs)
    waitForIdle()
    onNodeWithTag(tag).performClick()
    waitForIdle()
}
