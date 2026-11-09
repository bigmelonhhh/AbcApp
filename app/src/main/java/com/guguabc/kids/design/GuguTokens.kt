package com.guguabc.kids.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guguabc.kids.R

/**
 * design.md §4 / §6 的唯一真值源。
 * Composable 内禁止出现裸数值、裸色值、裸字号 —— 一律引用本文件。
 * 行尾注释为该数值对应的设计稿 Frame 节点 ID。
 */

object GuguColor {
    val background = Color(0xFFFFFAF0)          // 3:240 / 3:241 / 3:242 / 3:244
    val backgroundCheer = Color(0xFFFFF6DE)     // 3:243
    val surface = Color(0xFFFFFFFF)             // 3:240
    val primary = Color(0xFFFFC857)             // 3:240
    val cream = Color(0xFFFFF1CC)               // 3:240
    val creamCheer = Color(0xFFFFE9A8)          // 3:243
    val coral = Color(0xFFFF8C7A)               // 3:241
    val sky = Color(0xFF6BB6E0)                 // 3:243
    val mint = Color(0xFF7DD3A0)                // 3:243
    val starStroke = Color(0xFFE9A13B)          // 3:243
    val parentCard = Color(0xFFE4F3FB)          // 3:244
    val onSurface = Color(0xFF4A3423)           // 3:240
    val textSecondary = Color(0xFF8A7561)       // 3:243
    val textTertiary = Color(0xFF9A876F)        // 3:240
    val parentTitle = Color(0xFF3F7E9E)         // 3:244
    val parentHint = Color(0xFF6A93A8)          // 3:244
    val divider = Color(0xFFE8DDC8)             // 3:241
    val scrim = Color(0x66000000)               // 附录 C ④ 家长门面板遮罩（实现侧补充，非设计稿内容）
}

val NotoSansSc = FontFamily(                    // design.md §3.5
    Font(R.font.noto_sans_sc_regular, FontWeight.W400),
    Font(R.font.noto_sans_sc_medium, FontWeight.W500),
    Font(R.font.noto_sans_sc_bold, FontWeight.W700),
)

val FredokaBold = FontFamily(Font(R.font.fredoka_bold, FontWeight.W700)) // design.md §3.5

object GuguType {
    private fun cn(size: Int, weight: FontWeight, color: Color) = TextStyle(
        fontFamily = NotoSansSc, fontWeight = weight, fontSize = size.sp, color = color
    )

    // ① 首页 3:240
    val homeGreeting = cn(22, FontWeight.W500, GuguColor.textTertiary)
    val homeBrand = cn(48, FontWeight.W700, GuguColor.onSurface)
    val homeBubble = cn(30, FontWeight.W700, GuguColor.onSurface)
    val lessonLabel = cn(20, FontWeight.W500, GuguColor.textTertiary)
    val lessonTitle = cn(38, FontWeight.W700, GuguColor.onSurface)
    val lessonMeta = cn(20, FontWeight.W400, GuguColor.textTertiary)
    val startButton = cn(40, FontWeight.W700, GuguColor.onSurface)
    val footer = cn(20, FontWeight.W400, GuguColor.textTertiary)

    // ② 认一认 3:241
    val wordEn = TextStyle(fontFamily = FredokaBold, fontWeight = FontWeight.W700, fontSize = 96.sp, color = GuguColor.onSurface)
    val wordZh = cn(30, FontWeight.W500, GuguColor.textTertiary)
    val soundHint = cn(24, FontWeight.W400, GuguColor.textTertiary)
    val nextButton = cn(36, FontWeight.W700, GuguColor.onSurface)

    // ③ 听一听 3:242
    val quizTitle = cn(40, FontWeight.W700, GuguColor.onSurface)
    val quizHint = cn(22, FontWeight.W400, GuguColor.textTertiary)
    val quizFooter = cn(20, FontWeight.W400, GuguColor.textTertiary)

    // ④ 答对啦 3:243
    val cheerTitle = cn(56, FontWeight.W700, GuguColor.onSurface)
    val cheerSubtitle = cn(28, FontWeight.W500, GuguColor.textSecondary)
    val cheerProgress = cn(22, FontWeight.W500, GuguColor.textTertiary)
    val continueButton = cn(34, FontWeight.W700, GuguColor.onSurface)
    val cheerFooter = cn(20, FontWeight.W400, GuguColor.textTertiary)

    // ⑤ 今天真棒 3:244
    val doneTitle = cn(52, FontWeight.W700, GuguColor.onSurface)
    val doneSubtitle = cn(26, FontWeight.W500, GuguColor.textSecondary)
    val replayButton = cn(32, FontWeight.W700, GuguColor.onSurface)
    val restButton = cn(28, FontWeight.W500, GuguColor.textSecondary)
    val parentTitle = cn(20, FontWeight.W700, GuguColor.parentTitle)
    val parentData = cn(20, FontWeight.W400, GuguColor.onSurface)
    val parentHint = cn(18, FontWeight.W400, GuguColor.parentHint)
    val doneBadge = cn(22, FontWeight.W500, GuguColor.textTertiary)

    // 家长门最小面板（design.md 附录 C ④，实现侧补充样式）
    val panelTitle = cn(28, FontWeight.W700, GuguColor.onSurface)
    val panelLabel = cn(20, FontWeight.W400, GuguColor.textSecondary)
    val panelValue = cn(20, FontWeight.W500, GuguColor.onSurface)
    val panelClose = cn(20, FontWeight.W500, GuguColor.parentTitle)

    // V2 · 贴纸册（design_v2.md §5.1）
    val stickerBookTitle = cn(40, FontWeight.W700, GuguColor.onSurface)
    val stickerBookHint = cn(22, FontWeight.W400, GuguColor.textTertiary)
    val stickerBookEntry = cn(24, FontWeight.W500, GuguColor.parentTitle)

    // V2 · 家长课程包列表（design_v2.md §5.2）
    val packNo = cn(18, FontWeight.W400, GuguColor.textTertiary)
    val packName = cn(20, FontWeight.W500, GuguColor.onSurface)
    val packL2 = cn(18, FontWeight.W400, GuguColor.parentHint)
    val ttsNotice = cn(16, FontWeight.W400, GuguColor.textTertiary)
}

object GuguDimens {
    // 设计画布 §3.1
    val screenWidth = 1280.dp                  // 3:225
    val screenHeight = 800.dp                 // 3:225
    val statusBarSpace = 36.dp                // 3:240
    val bottomReserved = 16.dp                // 3:240

    // ① 首页 3:240
    val homePadding = 48.dp
    val homeGap = 56.dp
    val homeLeftWidth = 560.dp
    val homeRightWidth = 568.dp               // 1280 - 96 - 56 - 560
    val homeLeftGap = 24.dp
    val homeRightGap = 30.dp
    val homeBrandGap = 4.dp
    val homeBubbleRadius = 32.dp
    val homeBubblePadding = 22.dp
    val homeMascotCircle = 400.dp
    val homeMascotArt = 360.dp
    val homeLessonCardHeight = 184.dp
    val homeLessonRadius = 40.dp
    val homeLessonPadding = 30.dp
    val homeLessonGap = 16.dp
    val homeAvatarSize = 60.dp
    val homeAvatarGap = 8.dp
    val homeStartHeight = 112.dp
    val homeStartGap = 16.dp
    val homePlayIcon = 36.dp
    val homeParentX = 1140.dp
    val homeParentY = 48.dp
    val homeParentSize = 80.dp
    val homeParentIcon = 36.dp

    // ② 认一认 3:241
    val cardPadding = 96.dp
    val cardGap = 48.dp
    val cardWidth = 620.dp                    // 3:321
    val cardRightWidth = 420.dp               // 1280 - 192 - 48 - 620
    val cardRadius = 48.dp                    // 3:321
    val cardInnerGap = 16.dp
    val cardCircle = 320.dp
    val cardArt = 288.dp
    val cardRightGap = 28.dp
    val soundLgSize = 140.dp
    val soundLgIcon = 64.dp
    val nextButtonHeight = 108.dp
    val nextButtonGap = 14.dp
    val nextIcon = 32.dp
    val progressX = 595.dp
    val progressY = 56.dp
    val progressW = 90.dp
    val progressH = 20.dp

    // 通用绝对定位圆钮 3:241 / 3:242 / 3:243
    val backX = 48.dp
    val backY = 44.dp
    val volumeX = 1160.dp
    val roundButton = 72.dp
    val roundButtonIcon = 32.dp

    // ③ 听一听 3:242
    val quizGap = 20.dp
    val quizHintWidth = 760.dp
    val quizHintHeight = 120.dp
    val quizHintRadius = 40.dp
    val quizHintPadding = 24.dp
    val quizHintGap = 24.dp
    val soundMdSize = 84.dp
    val soundMdIcon = 46.dp
    val quizTextGap = 8.dp
    val optionWidth = 420.dp                  // 3:357
    val optionHeight = 370.dp                // 3:357
    val optionRadius = 48.dp
    val optionGap = 72.dp                    // 3:356
    val optionCircle = 300.dp
    val optionArt = 276.dp

    // ④ 答对啦 3:243
    val cheerGap = 12.dp
    val cheerMascotCircle = 200.dp
    val cheerMascotArt = 176.dp
    val confettiWidth = 180.dp
    val confettiHeight = 360.dp
    val confettiLeftX = 60.dp
    val confettiRightX = 1040.dp
    val confettiY = 80.dp
    val starSize = 48.dp                     // 3:393
    val starGap = 16.dp                      // 3:393
    val continueWidth = 520.dp
    val continueHeight = 96.dp
    val continueGap = 14.dp
    val continueIcon = 30.dp
    val cheerProgressX = 580.dp
    val cheerProgressY = 58.dp
    val cheerProgressWidth = 120.dp
    val cheerProgressHeight = 32.dp

    // ⑤ 今天真棒 3:244
    val donePadding = 96.dp
    val doneGap = 40.dp
    val doneLeftWidth = 580.dp
    val doneRightWidth = 468.dp               // 1280 - 192 - 40 - 580
    val doneLeftGap = 24.dp
    val doneMascotCircle = 160.dp
    val doneMascotArt = 140.dp
    val stickerSize = 160.dp
    val stickerGap = 16.dp
    val stickerRadius = 36.dp
    val stickerArt = 132.dp
    val doneRightGap = 20.dp
    val replayHeight = 100.dp
    val restHeight = 84.dp
    val restStroke = 2.dp
    val parentCardHeight = 200.dp            // 3:426
    val parentCardRadius = 32.dp             // 3:426
    val parentCardPadding = 24.dp
    val parentCardGap = 10.dp
    val doneBadgeX = 48.dp
    val doneBadgeY = 58.dp
    val doneBadgeWidth = 200.dp
    val doneBadgeHeight = 32.dp
    val doneParentX = 1160.dp
    val doneParentY = 44.dp
    val doneParentSize = 72.dp
    val doneParentIcon = 32.dp

    // 家长门面板（实现侧补充）
    val panelWidth = 520.dp
    val panelRadius = 32.dp
    val panelPadding = 28.dp
    val panelGap = 16.dp
    val panelBottom = 64.dp

    // V2 · 贴纸册 6 列 × 4 行单屏铺满（修正 §5.1 的 4×6：后者高 1040dp 超出 800dp 画布）
    const val stickerBookColumns = 6
    val stickerBookCell = 128.dp
    val stickerBookGap = 16.dp
    val stickerBookRadius = 28.dp
    val stickerBookArt = 104.dp
    val stickerBookTitleY = 44.dp
    val stickerBookHintGap = 8.dp
    val stickerBookGridGap = 32.dp
    val stickerBookBackX = 48.dp
    val stickerBookBackY = 44.dp
    val stickerBookLockedAlpha = 0.28f

    // V2 · DONE 页贴纸册入口：绝对定位，不参与 Column 布局，V1 五屏元素坐标零改动
    val stickerBookEntryX = 96.dp
    val stickerBookEntryY = 690.dp
    val stickerBookEntryWidth = 580.dp
    val stickerBookEntryHeight = 40.dp

    // V2 · 家长面板课程包列表
    val panelWidthV2 = 720.dp
    val panelHeight = 420.dp
    val packRowHeight = 56.dp
    val packRowRadius = 16.dp
    val packRowGap = 10.dp
    val packRowPadding = 12.dp
    val packRowStroke = 1.dp
    val packAvatar = 32.dp

    // 2026-11-09 交互修复：按钮禁用态 / 家长门长按反馈（design_v2.md 附录⑦⑧）
    val disabledAlpha = 0.32f
    val disabledTextAlpha = 0.6f
    val parentGateRingWidth = 6.dp
    const val parentGateHoldMs = 1500L
}

/** design.md §4.3：颜色、透明度、dy 必须精确；blur 为 Figma radius。 */
data class GuguShadowSpec(
    val color: Color,
    val alpha: Float,
    val dy: androidx.compose.ui.unit.Dp,
    val blur: androidx.compose.ui.unit.Dp,
)

object GuguShadow {
    val card = GuguShadowSpec(Color(0xFFFFC759), 0.16f, 10.dp, 22.dp)       // 3:241
    val cardSm = GuguShadowSpec(Color(0xFFFFC759), 0.16f, 8.dp, 18.dp)      // 3:240
    val option = GuguShadowSpec(Color(0xFFFFC759), 0.20f, 10.dp, 22.dp)     // 3:242
    val sticker = GuguShadowSpec(Color(0xFFFFC759), 0.20f, 8.dp, 18.dp)     // 3:244
    val parentGate = GuguShadowSpec(Color(0xFFFFC759), 0.18f, 6.dp, 14.dp)  // 3:240
    val primary = GuguShadowSpec(Color(0xFFE8A133), 0.40f, 8.dp, 18.dp)     // 3:240
    val coral = GuguShadowSpec(Color(0xFFFF8C7A), 0.36f, 8.dp, 18.dp)       // 3:241
}
