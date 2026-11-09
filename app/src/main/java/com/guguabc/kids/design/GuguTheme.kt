package com.guguabc.kids.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * 自定义主题容器：不套用 MaterialTheme 色板，不启用动态取色 / 深色模式。
 * design.md §9.1 / §10。
 */
data class GuguColors(
    val background: Color = GuguColor.background,
    val backgroundCheer: Color = GuguColor.backgroundCheer,
    val surface: Color = GuguColor.surface,
    val primary: Color = GuguColor.primary,
    val cream: Color = GuguColor.cream,
    val creamCheer: Color = GuguColor.creamCheer,
    val coral: Color = GuguColor.coral,
    val onSurface: Color = GuguColor.onSurface,
    val textSecondary: Color = GuguColor.textSecondary,
    val textTertiary: Color = GuguColor.textTertiary,
    val divider: Color = GuguColor.divider,
    val parentCard: Color = GuguColor.parentCard,
)

private val LocalGuguColors = staticCompositionLocalOf { GuguColors() }

object GuguTheme {
    val colors: GuguColors
        @Composable get() = LocalGuguColors.current
}

@Composable
fun GuguTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGuguColors provides GuguColors()) {
        content()
    }
}
