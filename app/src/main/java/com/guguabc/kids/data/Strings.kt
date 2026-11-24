package com.guguabc.kids.data

/**
 * 全部界面文案常量。数值与文字逐字取自 design.md §6 / §7.3，标点原样。
 * Composable 内禁止出现裸中文字符串或拼接常量。
 * 运行时数据用 %1$s / %1$d 占位，由 SessionState 传入，不得写死。
 */
object Strings {
    const val COLOR_WORLD = "颜色乐园"
    const val COLOR_WORLD_EN = "Colors"
    const val COLOR_PRIMARY = "红黄蓝"
    const val COLOR_SECONDARY = "绿橙紫"
    const val COLOR_LEARN = "认识颜色"
    const val COLOR_LEARN_HINT = "点一下，听发音"
    const val COLOR_PLAY = "玩颜色"
    const val COLOR_MATCH = "同色配对"
    const val COLOR_MATCH_HINT = "找一个颜色一样的"
    const val COLOR_LISTEN = "听音找色"
    const val COLOR_LISTEN_HINT = "听一听，选出这个颜色"
    const val COLOR_PAINT = "颜色小画家"
    const val COLOR_PAINT_HINT = "选一种颜色给气球上色"
    const val COLOR_PREVIOUS = "上一个"
    const val COLOR_NEXT = "下一个"
    const val COLOR_FINISH = "完成"
    const val COLOR_CURRENT_BALLOON = "给这只气球上色"
    const val COLOR_BACK = "返回乐园"
    const val COLOR_REPLAY = "再玩一次"
    const val COLOR_SWITCH = "换个玩法"
    const val COLOR_REST = "休息"
    const val COLOR_DONE = "颜色游戏完成啦"
    const val COLOR_LEARN_DONE = "认识完这一组啦"
    const val COLOR_PRAISE = "找对啦"
    const val COLOR_TRY_AGAIN = "看看这个颜色，再试一次"
    const val COLOR_TARGET = "目标颜色"
    const val COLOR_HEAR = "重听提示"
    const val COLOR_SETTINGS = "颜色提示"
    const val COLOR_CHINESE = "中文辅助"
    const val COLOR_ENGLISH = "英文"
    const val COLOR_RECORD = "颜色贴纸是游戏参与记录"
    const val COLOR_AT_HOME = "和爸爸妈妈找一件%s的东西"
    const val COLOR_TIMES_UP = "今天的 5 分钟用完啦"
    const val COLOR_PAINTED_BALLOON = "涂好的气球"
    const val COLOR_EMPTY_BALLOON = "待上色的气球"
    const val COLOR_STICKER_LOCKED = "%s，完成游戏后解锁"
    const val EXPLORE_TITLE = "今天想认识什么？"
    const val CHOOSE_LESSON = "选一组，开始认识吧"
    const val HOME = "首页"
    const val PARENT = "家长"
    const val MY_STICKERS = "我的贴纸"
    const val MORE_LESSONS = "更多朋友 →"
    const val MORE_ANIMALS = "更多动物 →"
    const val PREVIOUS_LESSONS = "← 前一页"
    const val CHOOSE_ANOTHER = "← 换一组"

    // ① 首页
    const val HOME_GREETING = "早上好呀"
    const val BRAND = "咕咕 ABC"
    const val MASCOT_BUBBLE = "嗨，我是咕咕！"
    const val LESSON_LABEL = "今天的一课"
    const val LESSON_TITLE = "小动物朋友"
    const val LESSON_META = "3 个新词 · 约 5 分钟"
    const val START = "开始玩"
    const val HOME_FOOTER = "每天 5 分钟 · 不设关卡，不比分数"

    // ② 认一认
    const val SOUND_HINT = "点一下，听发音"
    const val NEXT_CARD = "下一张"

    // ③ 听一听
    val QUIZ_TITLE = "哪个是 %1${'$'}s ？"
    const val QUIZ_HINT = "点一点你听到的那个"
    const val QUIZ_FOOTER = "听不清就再点一次 · 选错不扣分，只会再放一遍"

    // ④ 答对啦
    const val CHEER_TITLE = "答对啦！"
    val CHEER_SUBTITLE = "你找到了 %1${'$'}s"
    val CHEER_PROGRESS = "第 %1${'$'}d 题"
    const val CONTINUE = "再玩一个"
    const val CHEER_FOOTER = "没有「答错」，只有再听一次"

    // ⑤ 今天真棒
    const val DONE_TITLE = "今天真棒！"
    val DONE_SUBTITLE = "认识了 %1${'$'}d 个新朋友"
    const val REPLAY = "再玩一次"
    const val REST = "休息啦"
    const val PARENT_CARD_TITLE = "给爸爸妈妈"
    val PARENT_CARD_DATA = "今天 %1${'$'}d 分钟 · 认识 %2${'$'}s"
    const val PARENT_CARD_HINT = "点击右上角「家长」可设置每日时长"
    const val DONE_BADGE = "今天到这里"

    // 家长门最小设置面板（§7.5，样式由实现侧补充，无新增页面）
    const val PARENT_PANEL_TITLE = "家长设置"
    const val PARENT_PANEL_LIMIT = "每日时长上限"
    const val PARENT_PANEL_SOUND = "音效"
    val PARENT_PANEL_TODAY = "今日已玩 %1${'$'}d 分钟"
    const val PARENT_PANEL_CLOSE = "关闭"
    const val MINUTES_5 = "5 分钟"
    const val STATE_ON = "开"
    const val STATE_OFF = "关"

    // V2 · 首页（基准课沿用上面的 V1 文案，保证 5 张像素基准图可复现）
    const val LESSON_META_SHORT = "3 个新词 · 约 2 分钟"

    // V2 · 完成页贴纸册入口
    const val STICKER_BOOK_ENTRY = "看看我的贴纸册"

    // V2 · 贴纸册
    const val STICKER_BOOK_TITLE = "我的贴纸"
    const val STICKER_BOOK_HINT = "点一点，再听一次"

    // V2 · 家长课程包列表
    const val PARENT_PANEL_TODAY_PACK = "今日课程"
    val PACK_NO = "第 %1${'$'}d 课"
    const val PACK_LEVEL_L2 = "大朋友"
    const val TTS_NOTICE_LINE = "词汇使用合成语音，颜色乐园含中英文合成语音。"

    // 2026-11-09 交互修复（design_v2.md 附录「交互修复决议」⑤⑦⑧）
    const val TIME_UP_HINT = "今天的 5 分钟用完啦，家长可重置"
    const val RESET_TODAY = "重置今日时长"
    const val RESET_DONE = "已重置"
}
