# 咕咕 ABC · Android 1:1 还原开发约束

> 版本：V1.1 ｜ 面向：AI Coding 执行开发 ｜ 语言：Kotlin / Android 原生
> 性质：**开发规格与约束文档**，不是实现代码。本文所有数值来自设计稿节点读取结果，是唯一允许的实现依据。

---

## 0. 结论先行

1. **主交付 = 平板横屏 5 屏**。基准设备荣耀平板 X9（国行 NDL-W09），设计基准 **1280 × 800 dp 横屏**，物理 1920 × 1200 px。**手机竖屏稿（第一页）不进 V1 实现范围**，仅作为 `sw < 600dp` 的备用态设计留档（附录 B）。
2. **技术栈定为 Kotlin + Jetpack Compose 单 Activity**，不使用 XML 布局、不使用 Material 组件默认外观、不引入跨端框架。理由：Compose 的 `dp/sp` 与设计稿 1:1 对应，声明式布局树可逐节点对照，AI 生成时最不容易"自由发挥"。
3. **1:1 的定义**：尺寸偏差 ≤ 2dp、色值 hex 完全一致、文案逐字一致、插画/图标使用指定资源原样、圆角与偏移完全一致；模糊半径允许 ±2dp 视觉微调。
4. **必须先建 Token 层再写界面**。所有数值集中在 `GuguTokens.kt`，Composable 内禁止出现裸数字（`16.dp`、`#FFC857` 之类）。
5. **设计稿里的"标题区 / 流程标注 / 适配规范卡 / 状态栏 9:41 / 手势条 / 屏幕外框圆角与投影"都不是 App UI**，一律不实现（见 §1.2）。

---

## 1. 唯一真值源与还原范围

### 1.1 真值源（按优先级）

| 级别 | 来源 | 用途 |
|---|---|---|
| 1 | Ardot 设计文件 `https://ardot.tencent.com/file/744836527296696`，第 2 页 `3:225`「平板适配版 · 荣耀平板X9」 | 尺寸 / 色值 / 字号 / 圆角 / 阴影 / 文案的**唯一权威** |
| 2 | `gugu-abc-android-assets/screens/screen_0*_clean.png`（5 张**已裁掉阴影出血**的单屏基准图，2560 × 1600 px = 设计稿 2×） | 像素比对基准（换算见 §11）；同名不带 `_clean` 的原图含出血，仅作视觉参考 |
| 3 | `gugu-abc-android-assets/illustrations/`、`icons/` | 插画与图标资源，**禁止重画** |
| 4 | 本文 §4–§6 的表格 | 上述文件的结构化转写，供 AI 直接消费 |

**冲突裁决**：数值以设计文件节点为准 > 本文表格 > 基准 PNG 目测。发现本文与设计文件不一致时，以设计文件为准并在交付说明中记录，禁止自行"优化"。

**5 屏节点 ID 索引**（AI 可用设计文件读取工具回查）：

| 屏 | Frame ID | 名称 | 尺寸 |
|---|---|---|---|
| ① | `3:240` | 首页 · 横屏 | 1280 × 800 |
| ② | `3:241` | 认一认 · 横屏 | 1280 × 800 |
| ③ | `3:242` | 听一听 · 横屏 | 1280 × 800 |
| ④ | `3:243` | 答对啦 · 横屏 | 1280 × 800 |
| ⑤ | `3:244` | 今天真棒 · 横屏 | 1280 × 800 |

### 1.2 明确排除项（画进 App 即判不合格）

| 元素 | 原因 | 实现替代 |
|---|---|---|
| 设计板背景 `#F5EEDF`、标题区、版本标签、每屏下方「① 首页 · 横屏双栏」等标注 | 画布展示注释 | 无 |
| 「适配规范卡」`3:245` 及其全部文字 | 交付说明卡片，不是界面 | 无 |
| 屏幕 Frame 的 `cornerRadius=32` 与外投影 `#594224 / 12% / dy10 / blur24` | 画布里模拟设备边框的效果 | 真机屏幕为矩形，App 背景全屏铺满，无圆角、无外阴影 |
| 状态栏里的 `9:41`、信号格、电池图标 | 静态示意，硬编码会显示假时间 | 隐藏系统栏（§3.3），顶部按设计稿保留 36dp 空白 |
| 底部手势条（200 × 5，`#4A3423` 20% 透明） | 系统手势导航示意 | 由系统提供；底部预留 16dp 不被内容占用 |

---

## 2. 工程基线（新建项目，无历史工程约束）

| 字段 | 结论 | 依据 / 置信度 |
|---|---|---|
| framework | Kotlin + Jetpack Compose，单 `MainActivity`，Compose Navigation（或 `sealed class` 状态机） | 新建工程，本文档定义【高】 |
| stylesheet | 纯 Compose Modifier + 自建 `GuguTokens`；**无 XML layout、无 MaterialTheme 默认色板、无 Material 组件的默认涟漪/圆角/elevation** | 同上【高】 |
| componentLib | 自建 7 个基础组件（§5），不使用 Material `Button` / `Card` / `Icon` 默认外观 | 同上【高】 |
| tokenSystem | `GuguTokens.kt` 为唯一真值源（Color / Dimens / Typography / Shadows）；禁止在界面代码里写字面量 | 同上【高】 |
| iconLib | `gugu-abc-android-assets/icons/*.svg` 为图标 SSOT；转 VectorDrawable 或导出 2x PNG，**禁止 Material Icons / emoji / 手绘替代** | 已导出 17 个 SVG【高】 |
| responsive | 仅一个断点：`sw600dp` 以下走附录 B 的竖屏降级（V1 可不实现）；`sw600dp` 及以上固定横屏基准；**禁止按宽度等比拉伸** | 适配规范卡 + 本文【高】 |

**版本与配置约束**

```kotlin
// build.gradle.kts —— 版本以 gradle/libs.versions.toml 统一锁定，AI 不得混用 alpha 版本
compileSdk = 34 ; minSdk = 26 ; targetSdk = 34
// 禁止：ExoPlayer、Glide/Coil、Retrofit、广告/统计/推送/登录 SDK、任何网络权限
```

**2.1 实际锁定版本（2026-11-07 经仓库 POM 逐个核实 + 本机 assembleDebug 验证）**

| 组件 | 版本 | 说明 |
|---|---|---|
| Gradle | `8.11.1` | 本机 wrapper 缓存已有 |
| AGP | `8.9.3` | 要求 Gradle ≥ 8.11.1、JDK 17；本机 JDK 21 |
| Kotlin / Compose Compiler Plugin | `2.1.21` / `2.1.21` | `org.jetbrains.kotlin.plugin.compose` 必须与 KGP 同版本 |
| Compose BOM | `2024.12.01` | 该 BOM 不要求 compileSdk > 34，符合上表 |
| activity-compose | `1.9.3` | 单 Activity 必需 |
| core-ktx | `1.13.1` | `WindowInsetsControllerCompat` 必需 |
| datastore-preferences | `1.1.1` | 音效开关与时长记录 |
| kotlinx-coroutines-android | BOM 传递 / 显式锁定 | DataStore 协程 |

依赖白名单（超出需申请）：`compose ui / foundation / animation / ui-tooling-preview(debug) / ui-tooling(debug)`、
`activity-compose`、`core-ktx`、`datastore-preferences`、`kotlinx-coroutines-android`。
`compose animation` 仅用于 §7.1 要求的 200ms `Crossfade` 淡入淡出，不引入滑动手势。
`navigation-compose` **不使用**：状态机用 `sealed interface` + 状态容器，依赖更少。
构建期联网下载上述依赖，不等于运行期联网；APK 内 `uses-permission` 仍为 0 个。

- `AndroidManifest.xml`：`android:screenOrientation="landscape"`（锁定横屏，不随传感器翻转）；`<uses-permission>` **只允许 0 个**（V1 全离线、无网络）。
- `android:resizeableActivity="false"`、`android:supportsRtl="false"`，避免分屏/RTL 破坏固定布局。
- 应用图标与启动页不在本稿范围内，V1 用默认占位即可，**不要自行设计**。

---

## 3. 设备、单位与系统行为

### 3.1 单位换算（1:1 的地基）

| 项 | 规定 |
|---|---|
| 设计稿 1 单位 | = **1 dp**（尺寸、间距、圆角、坐标） |
| 设计稿 1 字号 | = **1 sp**，但必须锁定 `fontScale = 1f`（见 §3.4） |
| 物理像素 | 1280 × 800 dp × 1.5 = 1920 × 1200 px |
| 密度假设 | `densityDpi = 240`（1.5×）。**官方只公布 207 PPI，未公布 densityDpi** → 见 §13 待确认项 ① |
| 禁止 | 使用 px、使用 `fillMaxWidth` 后靠 `weight` 猜比例、按屏宽百分比缩放、把手机稿整体放大 |

**容器规则**：容器一律用 `fillMaxSize` / `fillMaxWidth` / `wrapContentSize`，**只有叶子元素（图标、插画、圆形底盘、贴纸）允许硬编码 dp 尺寸**，且必须与 §6 表格一致。

### 3.2 屏幕方向与安全区

- 锁定横屏；`WindowCompat.setDecorFitsSystemWindows(window, false)`，内容自己处理 insets。
- 内容区 padding **逐屏不同，以 §6 为准**（① 48dp，②③④⑤ 96dp）。适配规范卡写的「四边 96dp」是概括值，**不覆盖 ① 的 48dp**。
- 左右下角 120dp 内不放假可点元素（双手握持遮挡）；主按钮一律水平居中。
- 右上角家长入口**故意**位于安全区之外（① `x=1140,y=48`、⑤ `x=1160,y=44`），属于刻意设计，不要"修正"到居中。

### 3.3 系统栏（幼儿防误触）

- 隐藏状态栏与导航栏：`WindowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars())`，`systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`。
- **不要**把设计稿的 36dp 状态栏画出来；但保留该区域的空白，使内容与基准图一致。
- 因为系统栏被隐藏，必须提供退出路径：⑤「休息啦」→ `finish()`（回桌面）；④/②/③ 的返回按钮走应用内返回栈，不退出应用。
- 多指/长按系统手势不做拦截（V1 不引入 `FLAG_SECURE` 之类的窗口拦截）。

### 3.4 字体缩放锁定

```kotlin
@Composable fun GuguApp() {
    val ld = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = ld.density, fontScale = 1f)
    ) { GuguTheme { GuguNavHost() } }
}
```

理由：系统「大字体」会把 96sp 的 `Cat` 撑出卡片，破坏 1:1。这是**有意牺牲可访问性缩放**，已列入 §13 待确认项 ②。

### 3.5 字体资源

| 设计字体 | 用途 | Android 资源 | 字重 |
|---|---|---|---|
| Noto Sans SC | 全部中文 | `res/font/noto_sans_sc_{regular,medium,bold}.ttf` | 400 / 500 / 700 |
| Fredoka Bold | 英文单词 `Cat` / `Dog` / `Bear` | `res/font/fredoka_bold.ttf` | 700 |
| Inter SemiBold | 状态栏时间（示意，不实现） | 可选 | 600 |
| Outfit SemiBold | 版本标签（示意，不实现） | 不需要 | — |

- **字体文件必须打包**，禁止依赖系统默认 sans（MagicOS 上是 HarmonyOS Sans，字形与行高都不同，1:1 必然失败）。
- Noto Sans SC 全字库体积大：用 `pyftsubset` 按 `Strings.kt` 的全部字符做子集化；**子集必须覆盖所有界面文案，缺字触发 fallback 判为不合格**。
- `FontWeight` 映射：`Regular→400`、`Medium→500`、`SemiBold→600`、`Bold→700`。
- 设计稿未指定 `lineHeight`：使用字体默认行高；若文本块高度与基准图偏差 > 4dp，用 `lineHeight` 显式微调并写入交付说明（§13 ③）。

---

## 4. 设计 Token

### 4.1 颜色（`GuguColor`）

| Token | Hex | 用途 |
|---|---|---|
| `background` | `#FFFAF0` | ① ② ③ ⑤ 页面底色 |
| `backgroundCheer` | `#FFF6DE` | ④ 庆祝页底色 |
| `surface` | `#FFFFFF` | 所有白卡、气泡、按钮描边底 |
| `primary` | `#FFC857` | 主按钮、进度点激活、星星填充 |
| `cream` | `#FFF1CC` | 插画圆形底盘 |
| `creamCheer` | `#FFE9A8` | ④ 咕咕底盘 |
| `coral` | `#FF8C7A` | 发音喇叭、彩纸 |
| `sky` | `#6BB6E0` | 彩纸 |
| `mint` | `#7DD3A0` | 彩纸 |
| `starStroke` | `#E9A13B` | 星星描边 |
| `parentCard` | `#E4F3FB` | 家长小结卡底色 |
| `onSurface` | `#4A3423` | 主文字、图标、手势条 |
| `textSecondary` | `#8A7561` | 副标题、次级说明 |
| `textTertiary` | `#9A876F` | 卡片小标、玩法提示、底部说明 |
| `parentTitle` | `#3F7E9E` | 家长卡标题 |
| `parentHint` | `#6A93A8` | 家长卡说明 |
| `divider` | `#E8DDC8` | 分隔线、进度点未激活、休息按钮描边 2dp |
| （不实现）`board` | `#F5EEDF` | 仅设计板背景 |

> 所有颜色必须写成上述 hex 常量，**禁止 `Color(0xFF...)` 内联、禁止近似色、禁止 Material 动态取色**。

### 4.2 圆角 / 间距 / 触控

| Token | 值 | 出现位置 |
|---|---|---|
| `radiusScreen`（不实现） | 32 | 仅画布外框 |
| `radiusCard` | 48 / 40 / 36 / 32 | 单词大卡 48；今日一课卡 40；提示条 40；贴纸 36；家长卡 32；气泡 32 |
| `radiusButton` | 高 ÷ 2（全圆角） | 所有主按钮、圆按钮 |
| `radiusSticker` | 36 | ⑤ 贴纸 |
| `gapScreen` | 逐屏不同 | ① 56；② 48；③ 72（选项间）；④ 12；⑤ 40（左右栏） |
| `touchMin` | ≥ 72dp（图标钮）/ ≥ 96dp（主按钮）/ 420 × 370dp（图卡） | 3 岁儿童触控，**禁止缩小到 48dp 的 Android 通用最小值** |

### 4.3 阴影（`GuguShadow`）

设计稿为 Figma `DROP_SHADOW`，参数如下（**颜色、dy 必须精确**）：

| Token | 颜色 | 透明度 | dy | blur | 用于 |
|---|---|---|---|---|---|
| `shadowCard` | `#FFC759` | 16% | 10 | 22 | ② 单词大卡 |
| `shadowCardSm` | `#FFC759` | 16% | 8 | 18 | ① 气泡、今日一课卡 |
| `shadowOption` | `#FFC759` | 20% | 10 | 22 | ③ 两张选项卡 |
| `shadowSticker` | `#FFC759` | 20% | 8 | 18 | ⑤ 三张贴纸 |
| `shadowParentGate` | `#FFC759` | 18% | 6 | 14 | ① 家长入口 |
| `shadowPrimary` | `#E8A133` | 40% | 8 | 18 | 所有黄色主按钮 |
| `shadowCoral` | `#FF8C7A` | 36% | 8 | 18 | ② 大喇叭按钮 |

**实现约束**：

1. **禁止**用 `Modifier.shadow(elevation)` 或 Material `Surface(tonalElevation)` 代替 —— 它们产生黑色材质阴影，颜色和角度都不对。
2. 若锁定的 Compose BOM 提供 `Modifier.dropShadow(shape) { radius/offset/color }`，直接用，参数是上表值；否则用 `Modifier.drawBehind { }` + `Paint`/`BlurMaskFilter(Blur.NORMAL)`，先按形状绘制、按 dy 平移、再绘制内容。
3. `blur` 从 Figma radius 换到 `BlurMaskFilter` 半径的经验换算约为 **÷2**（如 18 → 9dp），需真机对齐；允许 ±2dp 微调并记录，颜色/dy 不允许调整。
4. 阴影只画 **1 层**；静态卡片用 `graphicsLayer` 缓存，禁止在每帧重组里重算模糊（骁龙 680 + LCD 的性能约束）。

---

## 5. 全局组件规格（必须先建，再拼屏）

| 组件 | 构造要点 | 复用屏 |
|---|---|---|
| `GuguScreenContainer` | `Box(fillMaxSize)` + 背景色 + 隐藏系统栏后的 16dp 底部保留区；子内容按 §6 的 padding | 全部 |
| `PrimaryButton(text, icon?, onClick)` | 全圆角黄底 `#FFC857` + `shadowPrimary`；文字色 `#4A3423`；高度按调用方传入，禁止内部写死 | ① ② ④ ⑤ |
| `RoundIconButton(size, icon, onClick)` | 白底全圆角，72dp（② ③ ④）/ 80dp（① 家长）；图标居中 | ① ② ③ ④ ⑤ |
| `IllustrationCircle(size, imageRes, bg)` | 圆形底盘 + `Image(contentScale = Fit)`，**底盘与插画分别按 §6 尺寸**，禁止 `Crop` | ① ② ③ ④ ⑤ |
| `ProgressDots(count, activeIndex)` | 激活点 20dp `#FFC857`，未激活 14dp `#E8DDC8`，圆心距 35dp；容器 90 × 20 | ② ③ |
| `SoundButton(size)` | 珊瑚底 `#FF8C7A` + 白色喇叭图标；② 140dp / ③ 84dp | ② ③ |
| `ParentGateButton(size)` | 白底圆钮 + 家长人像图标；**长按 ≥ 1500ms** 才响应 | ① ⑤ |

**通用规则**：
- 所有可点击元素使用 `Modifier.testTag(...)`（命名见 §9.2），`clickable` 不带涟漪（`indication = null, interactionSource = remember { MutableInteractionSource() }`），避免 Material 默认按压效果破坏视觉。
- 按压反馈统一为 `scale 0.97 / 100ms` 回弹，不出现变色、红叉、抖动。
- 所有 `Image` 使用 `contentScale = ContentScale.Fit`，插画透明底禁止加白色/灰色底板（除非 §6 明确写了圆形底盘）。

---

## 6. 五屏逐屏规格

> 坐标系：以屏幕左上角为 (0,0)，单位 dp。`ABSOLUTE` 表示相对屏幕 Frame 绝对定位；其余由布局容器决定。
> 文案列中的 `·`、`？`、`！` 均为全角/半角原样，逐字复制。

### 6.1 ① 首页（`3:240`）

- 页面底色 `#FFFAF0`；根布局 `Column`；状态栏占位 36dp（空白，不绘制）；内容区 `Row(padding 48, gap 56)`，垂直居中。

| 元素 | 位置 / 尺寸 | 样式 | 文案 / 资源 |
|---|---|---|---|
| 左栏 | 宽 560，高 fill，`Column(gap 24)`，水平+垂直居中 | — | — |
| 品牌字组 | `Column(gap 4)`，居中 | — | 问候 `早上好呀` 22sp / Medium / `#9A876F`；`咕咕 ABC` 48sp / Bold / `#4A3423` |
| 说话气泡 | hug，圆角 32，padding 22，`shadowCardSm` | `#FFFFFF` | `嗨，我是咕咕！` 30sp / Bold / `#4A3423` |
| 咕咕底盘 | 400 × 400，圆 200 | `#FFF1CC` | 插画 360 × 360 → `ic_gugu_mascot.png` |
| 右栏 | 宽 fill，高 fill，`Column(gap 30)`，垂直居中、水平 START | — | — |
| 今日一课卡 | 宽 fill，高 184，圆角 40，padding 30，gap 16，`SPACE_BETWEEN`，`shadowCardSm` | `#FFFFFF` | 小标 `今天的一课` 20sp/Medium/`#9A876F`；课程名 `小动物朋友` 38sp/Bold/`#4A3423`；说明 `3 个新词 · 约 5 分钟` 20sp/Regular/`#9A876F` |
| 三词头像 | 3 × 60 × 60 圆形，gap 8 | 插画 Fit | Cat / Dog / Bear 图 |
| 开始玩按钮 | 宽 fill，高 112，圆角 56，gap 16，`shadowPrimary` | `#FFC857` | 播放图标 36；`开始玩` 40sp/Bold/`#4A3423` |
| 底部说明 | 宽 fill，居中 | 20sp/Regular/`#9A876F` | `每天 5 分钟 · 不设关卡，不比分数` |
| 家长入口 | ABSOLUTE (1140, 48)，80 × 80，圆 40，`shadowParentGate` | `#FFFFFF` | 家长图标 36 → `ic_parent.svg` |

**交互**：点「开始玩」→ ②；页面任意空白区点击**不响应**（不给宝宝多余选择）；家长入口长按 1.5s → 家长门（§7.5）。

### 6.2 ② 认一认（`3:241`）

- 底色 `#FFFAF0`；内容区 `Row(padding 96, gap 48)`，垂直居中。

| 元素 | 位置 / 尺寸 | 样式 | 文案 / 资源 |
|---|---|---|---|
| 单词大卡 | 宽 620，高 fill，圆角 48，`Column(gap 16)` 居中，`shadowCard` | `#FFFFFF` | — |
| 插画底盘 | 320 × 320，圆 160 | `#FFF1CC` | 288 × 288 插画（当前词） |
| 英文单词 | hug | **96sp / Fredoka Bold / `#4A3423`** | `Cat` |
| 中文释义 | hug | 30sp / Medium / `#9A876F` | `小猫` |
| 右栏 | 宽 fill，高 fill，`Column(gap 28)` 双向居中 | — | — |
| 大喇叭按钮 | 140 × 140，圆 70，`shadowCoral` | `#FF8C7A` | 喇叭图标 64 → `ic_sound_lg.svg` |
| 发音提示 | hug | 24sp/Regular/`#9A876F` | `点一下，听发音` |
| 下一张按钮 | 宽 fill，高 108，圆角 54，gap 14，`shadowPrimary` | `#FFC857` | `下一张` 36sp/Bold；右箭头 32 → `ic_arrow_right.svg` |
| 进度点 | ABSOLUTE (595, 56)，90 × 20 | 第 1 个激活（`dots_card_1.svg` 为静态示意） | 运行时 `activeIndex = cardIndex` |
| 返回按钮 | ABSOLUTE (48, 44)，72 × 72，圆 36 | `#FFFFFF` | `ic_back.svg` 32 |
| 音效开关 | ABSOLUTE (1160, 44)，72 × 72，圆 36 | `#FFFFFF` | `ic_volume.svg` 32 |

**交互**：进入本屏自动播报一次当前单词发音；点插画底盘 = 点喇叭（都发声）；点「下一张」→ 下一张卡（Cat → Dog → Bear），第 3 张后进入 ③；返回按钮回上一张/首页；音效开关切换全局静音并持久化。

### 6.3 ③ 听一听（`3:242`）

- 底色 `#FFFAF0`；内容区 `Column(padding 96, gap 20)`，双向居中。

| 元素 | 位置 / 尺寸 | 样式 | 文案 / 资源 |
|---|---|---|---|
| 听音提示条 | 760 × 120，圆角 40，padding 24，gap 24，`Row` 居中，`shadowCardSm` | `#FFFFFF` | — |
| 小喇叭 | 84 × 84，圆 42 | `#FF8C7A` | 图标 46 → `ic_sound_md.svg` |
| 题目文字组 | `Column(gap 8)` | — | `哪只是 Cat ？` 40sp/Bold/`#4A3423`；`点一点你听到的那个` 22sp/Regular/`#9A876F` |
| 二选一容器 | `Row(gap 72)` 居中 | — | — |
| 选项 A / B | **420 × 370**，圆角 48，`shadowOption` | `#FFFFFF` | 各含 300 × 300 圆形底盘 `#FFF1CC` + 276 × 276 插画（A=Cat，B=Dog） |
| 底部说明 | 宽 fill，居中 | 20sp/Regular/`#9A876F` | `听不清就再点一次 · 选错不扣分，只会再放一遍` |
| 进度点 | ABSOLUTE (595, 56)，90 × 20 | `dots_quiz_2.svg` 为静态示意 | 运行时 `activeIndex = quizIndex` |
| 返回 / 音效 | ABSOLUTE (48,44) / (1160,44)，72 × 72 | 同 ② | 同 ② |

**交互**：
- 进入本屏自动播报目标词发音（英语原声，**不念中文**）；点提示条喇叭重播；**10 秒无操作自动重播一次**。
- **选错不提示错误**：不出现红叉、不变色、不扣分、不弹文案；仅重播发音 + 一次 100ms 的 `scale 0.97` 反馈，选项保持可点。
- 选对 → ④。选项图上**禁止写任何文字**（含答案字母、单词、序号）。
- 两卡间距必须 72dp，不得改成 `SPACE_BETWEEN` 撑满屏宽。

### 6.4 ④ 答对啦（`3:243`）

- 底色 `#FFF6DE`（唯一不同底色）；内容区 `Column(padding 96, gap 12)`，双向居中。

| 元素 | 位置 / 尺寸 | 样式 | 文案 / 资源 |
|---|---|---|---|
| 左侧彩纸 | ABSOLUTE (60, 80)，180 × 360 | `confetti_left.svg` | 静帧，不做动画 |
| 右侧彩纸 | ABSOLUTE (1040, 80)，180 × 360 | `confetti_right.svg` | 静帧 |
| 咕咕底盘 | 200 × 200，圆 100 | `#FFE9A8` | 插画 176 × 176 |
| 主标题 | hug | 56sp/Bold/`#4A3423` | `答对啦！` |
| 副标题 | hug | 28sp/Medium/`#8A7561` | `你找到了 Cat`（词由运行时传入） |
| 三颗星星 | `Row(gap 16)`，3 × 48 × 48 | 2 实 `#FFC857` + 描边 `#E9A13B`；第 3 颗空心 `#FFFFFF` | `star_filled_1/2.svg`、`star_empty.svg` |
| 继续玩按钮 | **520 × 96**，圆角 48，gap 14，`shadowPrimary` | `#FFC857` | `再玩一个` 34sp/Bold；右箭头 30 → `ic_arrow_right_sm.svg` |
| 底部说明 | 宽 fill，居中 | 20sp/Regular/`#9A876F` | `没有「答错」，只有再听一次` |
| 返回按钮 | ABSOLUTE (48, 44)，72 × 72 | 同 ② | `ic_back.svg` |
| 进度说明 | ABSOLUTE (580, 58)，120 × 32，居中 | 22sp/Medium/`#9A876F` | `第 1 题`（**运行时题号，静态稿仅为示例**） |

**交互**：进入播放一次短促庆祝音（本地资源，非网络）；**1.2 秒后才允许点击「再玩一个」**（防宝宝连点误跳）；点后 → 下一题 ③，3 题完成 → ⑤。

> ⚠️ 设计稿中 ③ 的进度点亮第 2 个、④ 的题号写「第 1 题」，属于两屏分别取不同静态示例。实现时**必须由 `SessionState` 驱动**，不得硬编码；验收只检查位置/字号/颜色，不检查这两个示例数值。

### 6.5 ⑤ 今天真棒（`3:244`）

- 底色 `#FFFAF0`；内容区 `Row(padding 96, gap 40)`，垂直居中。

| 元素 | 位置 / 尺寸 | 样式 | 文案 / 资源 |
|---|---|---|---|
| 左栏 | 宽 580，高 fill，`Column(gap 24)` 双向居中 | — | — |
| 咕咕底盘 | 160 × 160，圆 80 | `#FFF1CC` | 插画 140 × 140 |
| 主标题 / 副标题 | hug | 52sp/Bold/`#4A3423`；26sp/Medium/`#8A7561` | `今天真棒！` / `认识了 3 个新朋友` |
| 贴纸墙 | `Row(gap 16)`，3 × 160 × 160，圆角 36，`shadowSticker` | `#FFFFFF` | 各含 132 × 132 插画（Cat / Dog / Bear） |
| 右栏 | 宽 fill，高 fill，`Column(gap 20)` 双向居中 | — | — |
| 再玩一次 | 宽 fill，高 100，圆角 50，`shadowPrimary` | `#FFC857` | `再玩一次` 32sp/Bold |
| 休息啦 | 宽 fill，高 84，圆角 42，描边 2dp | `#FFFFFF` / `#E8DDC8` | `休息啦` 28sp/Medium/`#8A7561` |
| 家长小结卡 | 宽 fill，高 200，圆角 32，padding 24，gap 10，`Column` 垂直居中 | `#E4F3FB` | 标题 `给爸爸妈妈` 20sp/Bold/`#3F7E9E`；数据 `今天 5 分钟 · 认识 Cat / Dog / Bear` 20sp/Regular/`#4A3423`；说明 `长按右上角「家长」可设置每日时长` 18sp/Regular/`#6A93A8` |
| 完成提示 | ABSOLUTE (48, 58)，200 × 32 | 22sp/Medium/`#9A876F` | `今天到这里` |
| 家长入口 | ABSOLUTE (1160, 44)，72 × 72，圆 36 | `#FFFFFF` | `ic_parent_sm.svg` 32 |

**交互**：「再玩一次」重开一轮（词序可轮换，但不新增内容）；「休息啦」→ `finish()` 回桌面；若今日累计时长已达上限，「再玩一次」置灰且不可点，只保留「休息啦」。家长卡里的分钟数与词表**必须来自运行时数据**，不得写死。

---

## 7. 状态机、数据与行为约束

### 7.1 状态机（唯一流程，禁止加页面）

```
HOME ──开始玩──> CARD(index=0..2) ──第3张──> QUIZ(index=0..2)
                                              │ 选对
                                              ▼
                                          CHEER ──再玩一个──> QUIZ(next)
                                              │ 3 题完成
                                              ▼
                                           DONE ──再玩一次──> CARD(0)
                                              └──休息啦──> finish()
```

- 用 `sealed interface GuguRoute` + `remember` 状态容器实现；**不引入底部导航、抽屉、关卡地图、侧边栏、Tab**。
- 返回栈：CARD/QUIZ 的返回按钮回上一状态；CHEER/DONE 的返回不回退到答题中途（避免"再来一次骗时长"）。
- 页面切换：仅 200ms 淡入淡出，**禁止滑动翻页手势**（3 岁误触高发）。

### 7.2 词表（`Words.kt`，V1 只有 3 个词）

| id | en | zh | imageRes | audioRes |
|---|---|---|---|---|
| cat | `Cat` | `小猫` | `img_cat.png` | `word_cat.ogg` |
| dog | `Dog` | `小狗` | `img_dog.png` | `word_dog.ogg` |
| bear | `Bear` | `小熊` | `img_bear.png` | `word_bear.ogg` |

- 题目目标词、两张选项图、CHEER 副标题、DONE 家长卡词表，全部由 `SessionState` + 词表生成。
- V1 **不新增词、不新增主题包**；数据层留出 `List<Word>` 结构即可，不要提前做主题管理。

### 7.3 文案常量表（`Strings.kt`）

所有中文文案逐字取 §6；UI 内禁止拼接字符串常量、禁止硬编码中文在 Composable 里。标点原样：`·`（间隔点）、`？`（全角问号）、`！`（全角叹号）、`「」`（直角引号）。

### 7.4 音频与无失败态

| 规则 | 约束 |
|---|---|
| 音频来源 | 全部 `res/raw/*.ogg` 本地预置，**禁止 TTS、禁止网络请求、禁止在线音频** |
| 播放方式 | `SoundPool`（短音效）或轻量 `MediaPlayer`；切页释放资源；不引入 ExoPlayer |
| 自动播报 | 进 CARD 播 1 次；进 QUIZ 播 1 次；10s 无操作重播 1 次（仅 QUIZ） |
| 选错 | **无失败态**：重播发音 + 100ms 缩放反馈；不显示错误、不扣星、不锁按钮、不记录错误次数 |
| 音效开关 | ② ③ 右上角圆钮，状态存 `DataStore`，全局生效，默认开 |
| 音频焦点 | 申请 `AUDIOFOCUS_GAIN_TRANSIENT`，失去焦点时暂停；App 退到后台立即停止播放 |
| 防连点 | 任意可点元素点击后 400ms 内忽略重复点击；CHEER 的按钮额外延迟 1.2s 可点 |

### 7.5 家长门与时长（合规下限）

- 家长入口长按 ≥ 1500ms 才触发；短按无任何反应（防宝宝误入）。
- V1 **不做家长中心页面**（设计稿已标 V1.2）；长按后弹出最小设置面板：每日时长上限（默认 5 分钟）、音效开关、今日进度只读。面板样式不在本稿范围内，**但必须有反馈，禁止做成死按钮**。
- `DataStore` 记录 `todayDate` / `usedSeconds`；跨天清零；达到上限后强制进入 DONE 且禁用「再玩一次」。
- **V1 禁止**：账号、登录、上传、埋点、广告、外链、应用内购买、任何读取个人信息或设备标识符的逻辑。

---

## 8. 资源包清单与图标规则

目录：`D:\英语学习\gugu-abc-android-assets\`

| 子目录 | 文件 | 用途 |
|---|---|---|
| `illustrations/` | `ic_gugu_mascot.png`、`img_cat.png`、`img_dog.png`、`img_bear.png` | 4 张 1024 × 1024 **透明底**插画；已去水印；禁止再加白底或二次绘制 |
| `icons/` | `ic_play.svg`、`ic_parent.svg`、`ic_parent_sm.svg`、`ic_sound_lg.svg`、`ic_sound_md.svg`、`ic_volume.svg`、`ic_back.svg`、`ic_arrow_right.svg`、`ic_arrow_right_sm.svg`、`star_filled_1.svg`、`star_filled_2.svg`、`star_empty.svg`、`confetti_left.svg`、`confetti_right.svg`、`dots_card_1.svg`、`dots_quiz_2.svg`、`status_icons_ref.svg` | 17 个图标，已补 `viewBox`；`status_icons_ref.svg` 仅参考，**不实现** |
| `screens/` | `screen_01_home_clean.png` … `screen_05_done_clean.png`（**像素基准**）、`reference_06_spec_clean.png`、以及同名不带 `_clean` 的原始导出图 | 干净基准图为 2560 × 1600（从含阴影出血的 2656 × 1716 原图按 `(48, 48, 2560, 1600)` 裁出）；原始图仅作视觉参考；规范卡不是 UI |

**图标落地两条路（二选一，全项目统一）**：

1. **推荐 · VectorDrawable**：把 SVG 转为 `res/drawable/*.xml`。转换规则：
   - `<svg width=W height=H viewBox="0 0 W H">` → `<vector android:viewportWidth="W" android:viewportHeight="H">`，`android:width/height` 用 §6 的 dp 值。
   - 每个 `<path transform="matrix(1 0 0 1 tx ty)">` → 包一层 `<group android:translateX="tx" android:translateY="ty">`，`android:pathData` 原样复制 `d`，`android:fillColor` 取 `fill`。
   - `<circle>` / `<rect rx>` 在 `confetti_*.svg`、`dots_*.svg` 中存在，VectorDrawable 不支持 → **这两个类别改用下面的 PNG 路线**，或用路径展开工具转换，不得用近似形状手画。
   - 线条图标的 `stroke` 在导出时已被烘焙成填充路径，不要再补描边。
2. **备选 · 位图**：从设计文件对应图标节点导出 `scale=2` PNG，全部放 `res/drawable-nodpi/`，控件尺寸用 dp 固定、`ContentScale.Fit`。`nodpi` 可避免系统按 1.5x 再缩放造成尺寸漂移。

**插画落地**：4 张 PNG 放 `res/drawable-nodpi/`，按 §6 的 dp 尺寸显示、`ContentScale.Fit`。上线前建议降采样到 ≤ 768 × 768 并转 WebP（保持 alpha）以控制内存；**验收阶段可直接使用原图，不得因优化改变画面**。

**实现决议（2026-11-07）**：采用路线 1 的「路径展开工具转换」例外 —— `app/tools/prepare_assets.py`
把 `<circle>` 展开为两段精确圆弧（`A/a` 指令）、`<rect rx>` 展开为带精确圆弧的圆角矩形路径，
**是数学等价转换，不是手画近似**。14 个图标全部转 `res/drawable/*.xml`（VectorDrawable），
不产生位图缩放漂移；`dots_*` / `status_icons_ref` 不转换、不入包。

**音频占位（2026-11-07）**：V1 尚无真人录音，`res/raw/` 的 5 个 `.ogg`（`word_cat / word_dog / word_bear / cheer / tap`）
由 `prepare_assets.py` 本地合成 OGG Vorbis 柔和占位音，**无外部素材、无网络请求**；
播放链路与 `R.raw` 资源名完全按 §7.4 实现。上线前必须替换为真人童声慢速发音（§13 ⑤）。

**绝对禁止**：用 Material Icons / emoji / 系统 emoji 字体 / 手写 Path 近似 / 在线图标库替换上述任何图标。

---

## 9. 工程结构与命名

### 9.1 目录

**工程根映射（2026-11-07 确定）**：`D:\英语学习\app\` 即 Gradle **单模块根工程**（`settings.gradle.kts` 只声明 rootProject），
**没有 `:app` 子模块**。下表里的 `app/src/main/` 直接落到 `D:\英语学习\app\src\main\`，不产生 `app/app/` 双层目录。
包名由占位的 `com.example.guguabc` 改为 `com.guguabc.kids`。

```
app/src/main/                     # = D:\英语学习\app\src\main\
├── java/com/guguabc/kids/
│   ├── MainActivity.kt            # 单 Activity，隐藏系统栏，setContent { GuguApp() }
│   ├── GuguApp.kt                # fontScale 锁定 + GuguTheme + 状态机容器
│   ├── design/
│   │   ├── GuguTokens.kt         # Color / Dimens / Typography / Shadow 唯一真值源
│   │   ├── GuguTheme.kt          # 自定义 CompositionLocal，不套用 Material 色板
│   │   └── components/           # §5 的 7 个基础组件，每个一个文件
│   ├── screens/                  # HomeScreen / WordCardScreen / ListenQuizScreen / CheerScreen / DoneScreen
│   ├── state/                    # SessionState.kt / GuguRoute.kt / Prefs.kt (DataStore)
│   ├── data/                     # Words.kt / Strings.kt
│   └── audio/                    # AudioPlayer.kt (SoundPool 封装)
└── res/
    ├── drawable-nodpi/           # 4 插画（+ 备选图标 PNG）
    ├── drawable/                 # VectorDrawable XML
    ├── font/                     # 4 个字体文件 + OFL LICENSE
    └── raw/                      # word_cat.ogg / word_dog.ogg / word_bear.ogg / cheer.ogg / tap.ogg
```

- 变量/文件命名用英文小写驼峰；**禁止中文拼音命名、禁止 `Screen1`/`Frame2` 之类的无语义名**。
- `GuguTokens.kt` 里每个数值必须带注释标注来源节点 ID，例如 `val cardWidth = 620.dp // 3:321`。

### 9.2 testTag 命名（截图自动化比对用）

| testTag | 对应元素 |
|---|---|
| `screen_home` / `screen_card` / `screen_quiz` / `screen_cheer` / `screen_done` | 5 个根容器 |
| `btn_start` / `btn_next` / `btn_continue` / `btn_replay` / `btn_rest` | 主按钮 |
| `btn_sound` / `btn_back` / `btn_volume` / `btn_parent` | 圆按钮 |
| `option_a` / `option_b` | ③ 两张图卡 |
| `word_en` / `word_zh` / `progress_dots` / `sticker_wall` / `parent_summary` | 关键文本与容器 |

---

## 10. 禁止清单（命中任意一条判返工）

1. 把手机竖屏 390 × 844 稿等比放大/拉伸到平板；或把横屏稿纵向堆叠。
2. 在 ③ 的两张图卡上写字、写字母、写序号、写答案提示。
3. 新增底部导航栏、关卡地图、积分商城、排行榜、勋章墙、签到、广告位。
4. 出现「答错」态：红叉、红色描边、扣分、错误音效、失败弹窗、重试按钮。
5. 使用 Material 组件默认圆角/涟漪/elevation/主题色，或 `Modifier.shadow(elevation)` 冒充设计稿阴影。
6. 用近似色、动态取色、深色模式、随系统字体缩放。
7. 硬编码 `9:41` 状态栏、手势条、进度点激活位置、题号、「今天 5 分钟」等运行时数据。
8. 用截图目测估值代替本文/设计文件的数值；用节点名猜尺寸。
9. 重画插画/图标、给透明插画加白底、把 `Fit` 改成 `Crop`、拉伸图片。
10. 在 Composable 里写裸数值或裸中文字符串；绕过 `GuguTokens` / `Strings`。
11. 引入网络权限、第三方 SDK、账号体系、埋点、TTS、在线音频。
12. 把设计板的标题区、流程标注、适配规范卡、Frame 圆角与外投影画进 App。
13. 自行"修复"设计稿：把家长入口挪到安全区内、把两卡间距改成撑满、把 ④ 的第三颗星填满。

---

## 11. 验收走查（逐条编号，交付时逐项打勾）

| # | 检查项 | 合格标准 |
|---|---|---|
| 1 | 屏幕方向 | 真机/模拟器 1920 × 1200 横屏启动，旋转设备不翻转 |
| 2 | 系统栏 | 状态栏/导航栏隐藏；顶部 36dp 空白存在；底部 16dp 无内容 |
| 3 | 尺寸 | 用 Layout Inspector 抽查 §6 全部固定尺寸元素，偏差 ≤ 2dp |
| 4 | 坐标 | 全部 ABSOLUTE 元素坐标偏差 ≤ 2dp |
| 5 | 颜色 | 逐项比对 §4.1 hex，**完全一致**，无近似值 |
| 6 | 字体 | 4 个字体文件已打包；`Cat` 96sp Fredoka Bold；中文无缺字 fallback；`fontScale=1f` 生效 |
| 7 | 圆角 | 逐项与 §4.2 / §6 一致 |
| 8 | 阴影 | 颜色/dy 与 §4.3 一致，仅 1 层，无黑色材质阴影 |
| 9 | 文案 | 与 §6 表格逐字比对（含标点、空格） |
| 10 | 插画 | 4 张指定 PNG、透明底、无白框、无裁切、无拉伸 |
| 11 | 图标 | **14 个应实现图标**全部有对应使用点（10 个纯 path + 2 个含 circle 的 parent 图标 + 2 个 confetti，全部转 VectorDrawable）；`dots_card_1/dots_quiz_2` 是静态示意，由 `ProgressDots` 运行时绘制，`status_icons_ref.svg` 是状态栏参考，二者**不打入 APK**；无 Material Icons / emoji / 手画替代 |
| 12 | 触控 | 主按钮 ≥ 96dp 高、圆钮 ≥ 72dp、图卡 420 × 370dp |
| 13 | 交互 | 5 屏按 §7.1 状态机走通；选错只重播；无失败态 |
| 14 | 音频 | 全本地 `res/raw`；进页自动播；静音开关生效；后台停止 |
| 15 | 防连点 | 400ms 防重复、CHEER 1.2s 延迟可点 |
| 16 | 家长 | 长按 1.5s 有反馈；短按无反应；时长上限达 5 分钟后禁用「再玩一次」 |
| 17 | 数据 | 词表/题号/进度点/家长卡数据全部由 `SessionState` 驱动，无硬编码假数据 |
| 18 | 性能 | 骁龙 680 上无实时模糊、无多层阴影、静态 PNG；冷启动无网络请求 |
| 19 | 合规 | Manifest 无网络权限、无第三方 SDK、无账号/广告/埋点 |
| 20 | 像素比对 | `adb exec-out screencap -p` 截图与 `screens/screen_0*_clean.png` 叠加，主要元素包围盒偏差 ≤ 2dp |

**比对方法（注意两套倍率）**：`*_clean.png` 是设计稿 **2×（2560 × 1600 px）**；真机截图是 **1920 × 1200 px**（= 设计 dp × 1.5）。统一到设计 dp 再叠加：`*_clean.png ÷ 2 → 1280 × 800`，`真机截图 ÷ 1.5 → 1280 × 800`。先看主要容器包围盒，再看文字基线与插画圆心，最后看阴影扩散范围。
**比对时排除**：顶部 0–36dp（设计稿的假状态栏）、底部手势条区域、四角 32dp 圆角（画布外框效果）——这三处按 §1.2 不实现，属于预期差异。截图仅用于**验收**，不得作为取值依据。

---

## 12. 给 AI 的执行顺序（建议分阶段，每阶段单独验收）

| 阶段 | 产出 | 完成标志 | 注意事项 |
|---|---|---|---|
| 1 · 骨架 | Gradle 工程、单 Activity、横屏锁定、隐藏系统栏、`fontScale=1f` | 空屏幕 1280 × 800 dp 背景色正确 | 先确认 `DisplayMetrics.density`，见 §13 ① |
| 2 · Token | `GuguTokens.kt` / `GuguTheme.kt` / 字体与资源落盘 | 所有常量有节点 ID 注释；字体子集覆盖全部文案 | 缺字会静默 fallback，务必逐字检查 |
| 3 · 组件 | §5 的 7 个组件 + `testTag` | 组件预览与 §5 尺寸一致 | 阴影实现先做 1 个并真机确认，避免全量返工 |
| 4 · 五屏 | 5 个 Screen 静态还原（可先接示例 state） | 通过验收表 #3–#11 | **此阶段不写业务逻辑**，先保证像素 |
| 5 · 状态与音频 | 状态机、词表、音频、DataStore、家长门 | 通过 #13–#17 | 进度点/题号必须由 state 驱动 |
| 6 · 走查 | 逐条 #1–#20 + 截图叠加 | 全部打勾，偏差项列表留档 | 偏差不得以"看起来更好"为由保留 |

---

## 13. 待确认项与风险（不自行决定，需人确认）

| # | 事项 | 影响 | 建议 |
|---|---|---|---|
| ① | 官方只公布 207 PPI，**未公布 `densityDpi`**；本文按 240dpi / 1.5× 换算 | 若真机为 tvdpi（≈213），dp 基准应改为约 1440 × 900，否则整体偏小 | 真机跑 `DisplayMetrics.density` 后回写本文 §3.1 与 Token，再开工阶段 1 |
| ② | 锁定 `fontScale = 1f` 牺牲系统大字体可访问性 | 家长开启大字体时界面不放大 | 产品侧确认；若需兼顾，改为按 `fontScale` 缩放并同步放大容器，但会破坏 1:1 |
| ③ | 设计稿未给 `lineHeight` | 中文文本块高度可能与基准图差 2–5dp | 阶段 4 用 Layout Inspector 实测，必要时显式 `lineHeight` 并留档 |
| ④ | 海外 HONOR Pad X9 是 11.5 英寸 2000 × 1200（5:3） | 横屏基准应为 1333 × 800 dp | 确认手中机型；国行版不用改 |
| ⑤ | 音频素材未录制 | V1 只能静音或用占位音 | 需真人童声/慢速清晰发音，Cat/Dog/Bear 各 1 条；上线前替换 |
| ⑥ | 字体授权文件 | Noto Sans SC / Fredoka / Inter 均为 SIL OFL | 随包附 `LICENSE` 文件，不改字体文件名混淆来源 |
| ⑦ | 家长中心 | 规范卡标为 V1.2 | 本次只做长按后的最小设置面板，不新增完整页面 |

---

## 附录 A · 与设计文件的数值溯源示例

| 本文数值 | 节点 ID | 读取字段 |
|---|---|---|
| 首页内容区 padding 48 / gap 56 | `3:256` | `padding` / `gap` |
| 单词大卡 620 宽、圆角 48 | `3:321` | `width` / `cornerRadius` |
| `Cat` 96sp Fredoka Bold | `3:324` | `fontSize` / `fontName` |
| 选项卡 420 × 370、间距 72 | `3:357` / `3:360` / `3:356` | `width` / `height` / `gap` |
| 星星 48 × 48、gap 16 | `3:393`–`3:399` | `width` / `height` / `gap` |
| 家长小结卡 200 高、圆角 32、`#E4F3FB` | `3:426` | `height` / `cornerRadius` / `fills` |
| 阴影 `#E8A133` 40% dy8 blur18 | 主按钮节点 `effects` | `effects[0]` |

## 附录 B · 竖屏备用态（V1 不实现，仅留档）

手机稿第 1 页 ID `3:9`–`3:13`，基准 390 × 844 竖屏，背景 `#FFFAF0`，内容区 padding 24，主按钮高 78–84dp，圆钮 46dp，英文词 54sp，选项卡 155 × 220dp，字号整体约为平板稿 ×0.55–0.6。

若未来启用 `sw < 600dp`：沿用平板稿的**同一套 Token 与组件**，只切换布局为竖屏单列，**禁止复用旧稿的独立数值**再维护两套 Token。V1 交付不包含这部分，验收表也不检查。

## 附录 C · 2026-11-07 实现决议（补充 §13 待确认项）

| # | 事项 | 决议 |
|---|---|---|
| ① | `densityDpi` 未公布、当前无真机 | `MainActivity` 加 **仅 DEBUG 生效** 的 `DisplayMetrics` logcat 输出，不进 UI。`GuguApp` 默认采用**固定设计 density** = `min(屏宽px/1280, 屏高px/800)`，使 1dp 恒等于设计 1 单位、**不重算任何 Token**；在 1920×1200 上正好 = 1.5，与 §3.1 假设一致，在其它分辨率上自动兜底。`FIXED_DESIGN_DENSITY` 常量可切回系统 density。真机 logcat 回来后再决定是否回写 §3.1 |
| ② | `fontScale = 1f` | 按 V1.1 原样锁定；产品侧后续如需兼顾系统大字体，另行评审 |
| ③ | 阴影实现 | 自建 `GuguShadow.kt`：一层独立柔和形状 + Compose 官方 `Modifier.blur`（§4.3 的 ÷2 经验值，`BlurredEdgeTreatment.Unbounded`），颜色 / 透明度 / dy 精确取自 Token，只画 1 层；**不用** `Modifier.shadow(elevation)`。未采用 `BlurMaskFilter`，因为硬件加速 Canvas 会忽略 MaskFilter；目标机 Android 14 支持 RenderEffect，低于 API 31 时退化为无模糊纯色阴影。真机连接后仍需按 §13 ③ 做 ±2dp 校准 |
| ④ | 家长门最小面板（§7.5 样式留白） | 长按 1.5s 后弹出**底部居中白色圆角 32dp 卡片**（40% 黑色遮罩、无新页面、无跳转），含每日时长上限 / 音效开关 / 今日进度 3 项，8 秒无操作自动关闭；短按无反应 |
| ⑤ | 应用图标与启动页 | 按 §2「默认占位」：`mipmap-*` 放纯 `#FFC857` 色块，不自行设计 |
| ⑥ | 字体授权文件 | `OFL.txt` 放 `app/src/main/assets/OFL.txt`（随 APK 携带，避免 aapt2 把非字体文件当 font 资源解析）；4 个 TTF 仍在 `res/font/` |
| ⑦ | 音效开关的静音态 | 设计稿只给一个 `ic_volume.svg`。静音时该图标 `alpha = 0.45`，**不新增图标、不重画**，状态同步存 DataStore |
| ⑧ | 版本链与目标 SDK | 采用方案 A（§2.1）：`compileSdk/targetSdk = 34`。Google Play 自 2026-08-31 要求新应用/更新 targetSdk ≥ 36，国内渠道要求未核实；V1 为真机验收原型不上架，上架前单独升级并复验横屏锁定与 edge-to-edge |
| ⑨ | 中文工程路径 | `D:\英语学习\app` 含非 ASCII 字符，AGP 8.9 默认拒绝构建 → `gradle.properties` 加官方提供的 `android.overridePathCheck=true`。本机 `assembleDebug` 已验证通过；若将来某台机器的 aapt2 / native 工具仍报路径错，把工程复制到纯 ASCII 临时目录构建再回拷产物，不改源码 |
| ⑩ | 文本组件 | 用 `foundation.text.BasicText` 封装成 `GuguText`，不引入 Material / Material3 组件默认外观；`styles.xml` 只用系统裸主题承载 Compose |
| ⑪ | 计时 | 后台不计入 `usedSeconds`（`ON_STOP` 暂停计时并停止播放）；跨天按 `LocalDate` 清零 |

