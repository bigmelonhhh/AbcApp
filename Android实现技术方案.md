# 咕咕 ABC · Android 工程搭建与 1:1 还原技术方案（待审阅）

> 版本：V1.0 ｜ 日期：2026-11-07 ｜ 状态：**待你确认，未开工**
> 真值源：`design.md` V1.1、`gugu-abc-android-assets/`、Ardot `744836527296696` 第 2 页
> 本轮交付物：仅本文档。不新建 `app/`、不写代码、不修改 `design.md`。

---

## 0. 结论先行

1. **工程形态**：在 `D:\英语学习\app\` 建 **Gradle 单模块根工程**（不再套一层 `:app` 模块），源码直接落在 `app/src/main/`，避免 `app/app/` 双层目录。
2. **实现策略**：固定 1280 × 800 设计画布 + `GuguTokens` 单一真值源 + 7 个自建组件 + 5 个静态 Screen，先像素后逻辑；状态机最后接。
3. **最大的技术风险不是 UI，是密度**：`design.md` 假设 `densityDpi = 240 / 1.5×`，但官方只给 207 PPI，且**当前无真机连接**（`adb devices` 为空），无法实测。方案给出了兜底策略，需要你拍板。
4. **本机可编译，但需联网拉依赖**：Maven(Google/Central) 可达、JDK 21 / Android Studio / SDK 34 / Gradle 8.11.1 已具备；Compose 相关依赖本机无缓存，构建期需下载（**运行期仍为 0 权限、全离线**）。
5. **`design.md` 有 4 处内部冲突/空白**，需你授权我同步修订后再开工，否则 AI 执行时会自行"猜"，直接破坏 1:1（详见 §2）。

---

## 1. 现状盘点（已实测，非推测）

### 1.1 本机环境

| 项 | 实测结果 | 对方案的影响 |
|---|---|---|
| JDK | OpenJDK 21.0.9（`JAVA_HOME` = Android Studio JBR） | AGP 8.x 要求 ≥17，21 可用；阶段 1 实测验证 |
| Android Studio | 已安装 `C:\Program Files\Android\Android Studio` | 可用 GUI 建平板 AVD、Sync、Layout Inspector |
| SDK Platform | `android-30 / 34 / 36 / 36.1`（**无 35、无 37**） | 直接支持 `compileSdk=34`；升级到 36/37 需先下载平台 |
| Build Tools | `30.0.1 / 30.0.3 / 34.0.0 / 35.0.1 / 36.1.0` | AGP 8.9 默认 35.0.0 已具备 |
| Gradle 发行包 | 缓存 `8.5 / 8.11.1`（**无 9.x**） | 选 8.11.1 可离线取发行包 |
| AGP 缓存 | 仅 `7.4.2`，无 Compose BOM | 依赖需联网下载 |
| 仓库连通 | Google Maven `200`、Maven Central `200`、PyPI `200` | 依赖/字体/工具可下载 |
| 字体源 | Google Fonts CSS 可用，4 个 TTF 直链 `200` | 见 §1.3 |
| ADB | 35.0.2（HBuilderX 附带）可用 | **无设备连接**；AVD 只有 `Medium_Phone`、`Pixel_9a`（手机） |
| Python | 3.13.14；`fontTools` **未安装** | 子集化需 `pip install fonttools` |
| `raw.githubusercontent.com` | 连接失败（SSL 000） | OFL LICENSE 需改用其他官方分发渠道 |

### 1.2 设计资源（已落盘）

| 类别 | 数量 | 状态 |
|---|---|---|
| 插画 PNG | 4 张（1024²，透明底，已去水印） | 可直接用 |
| 图标 SVG | 17 个 | **13 个纯 `<path>`；2 个含 `<circle>`（ic_parent / ic_parent_sm）；2 个 confetti 含 `<circle>+<rect rx>`；2 个 dots 含 `<circle>`；`status_icons_ref` 含 6 `<rect>`** |
| 单屏基准图 | 5 张 `*_clean.png`（2560 × 1600，已裁阴影出血）+ 5 张原图 | 像素比对真值源 |
| 规范卡 | `reference_06_spec*.png` | 非 UI，不实现 |

### 1.3 字体

| 文件 | 来源 | 体积 | 处理 |
|---|---|---|---|
| `fredoka_bold.ttf` | Google Fonts 直链（wght 700） | 48 KB | 直接打包 |
| `noto_sans_sc_regular/medium/bold.ttf` | Google Fonts 直链（400/500/700） | 各约 10.5 MB，**合计约 31 MB** | **必须子集化**：`pyftsubset` 按 `Strings.kt` 全字符裁剪，预计每个降到数十 KB |
| 系统 `NotoSansSC-VF.ttf` | 本机已有 | — | 仅作缺字兜底校验，不作为交付字体 |

> 设计稿要求 4 个字体文件全部打包并附 OFL LICENSE；MagicOS 系统字体是 HarmonyOS Sans，用系统默认必然 1:1 失败。

---

## 2. 需你拍板的决策点（**获批后才开工**）

| # | 决策点 | 我的建议（推荐） | 若选另一项 |
|---|---|---|---|
| ① | **版本链**：`design.md §2` 写死 `compileSdk/targetSdk = 34`，而当前官方稳定线已是 Kotlin 2.4.x / Compose BOM 2026.09 / AGP 9.x / compileSdk 37 | **方案 A · 严格守文档**：`compileSdk=34 / targetSdk=34 / minSdk=26`，AGP 8.9.x + Gradle 8.11.1（本机已缓存）+ Kotlin 2.1.x + 与 34 兼容的 Compose BOM（阶段 1 实测锁定后回写文档）。理由：本机 SDK/Gradle 齐备、构建链最短；且 targetSdk 34 不会被 Android 15+ 的强制 edge-to-edge 和大屏方向限制绕过，最利于锁定横屏 | 方案 B · 升到 compileSdk/targetSdk 36（或 37）+ AGP 8.13/9.x + Gradle 8.13/9.x + Kotlin 2.3/2.4 + 新 BOM：需下载新 Gradle 发行包与 SDK 平台，且**必须同步修订 design.md §2**；好处是满足 Google Play 自 2026-08-31 起 targetSdk ≥ 36 的要求（国内渠道要求需另行核实，本方案不预判） |
| ② | **真机 density 未知**（`densityDpi` 官方未公布、当前无设备） | 阶段 1 加一个**仅 DEBUG 生效**的 logcat 输出 `DisplayMetrics`，不动 UI。若实测 = 1.5 → 完全按 `design.md §3.4` 用系统 density；若 ≠ 1.5 → **默认兜底：固定设计 density = min(屏宽px/1280, 屏高px/800)**，使 1dp 恒等于设计 1 单位，无需重算 100+ 个 Token | 另一种兜底是按实测 density 重算全部 dp（改 `GuguTokens` + `design.md §3.1`），改动面大且易漏；或直接等真机数据再开工 |
| ③ | **阴影实现**：8 组精确参数，禁用 `Modifier.shadow(elevation)` | 用自建 `GuguShadow.kt`：`Modifier.drawBehind` + `BlurMaskFilter(Blur.NORMAL)`，按 §4.3 的 ÷2 经验值先做 **1 个组件**在模拟器上校准再全量铺开；配合 `graphicsLayer` 缓存，只画 1 层 | 若旧 BOM 下 BlurMaskFilter 在硬件加速下不生效，退路是**按 Token 参数离线生成透明阴影 PNG**（脚本产出、非手画），不改颜色/dy；若改用 2026 新 BOM 则可直接用 `Modifier.dropShadow(shape)`，但会绑到方案 B |
| ④ | **图标转换**：`design.md §8` 说"两条路二选一、全项目统一"，但 17 个 SVG 里 6 个含 `<circle>/<rect>` | 11 个纯 path → VectorDrawable；`ic_parent / ic_parent_sm` 的 `<circle>` 用**数学等价的 arc path 展开**（不是手画近似）；`confetti_left/right` + `dots_*` 按文档明确要求走 PNG 路线 | 全项目统一走 PNG（从 Ardot 节点 `scale=2` 导出）——最省事但矢量清晰度依赖导出倍率 |
| ⑤ | **`design.md` 内部矛盾 4 处** | 授权我同步修订（只改这几处，不动设计数值）：<br>a) §11 #11「17 个资源全部有使用点」与 §1.2/§8/§7 冲突 → 改为「**14 个应实现图标全部有使用点**；`dots_card_1/dots_quiz_2` 为静态示意由 `ProgressDots` 运行时绘制，`status_icons_ref` 不入包」；<br>b) §2 依赖白名单补 `activity-compose`、`core-ktx`（单 Activity + 隐藏系统栏必需，均为官方 AndroidX，非第三方 SDK）；<br>c) §9.1 目录 `app/src/main/` 明确映射到新建的 `D:\英语学习\app\src\main\`；<br>d) §2 版本表按决策 ① 的结果回写实际锁定值 | 不改文档 → 我会在实现说明里逐条记录偏离原因，但 AI 后续迭代容易反复踩坑 |
| ⑥ | **音频素材未录制**（Cat/Dog/Bear/cheer/tap 共 5 条） | V1 放**本地生成的占位 ogg**（静音或极短提示音），`AudioPlayer` 接口与调用链完整实现，验收 #14 标记为「链路通过、素材待替换」，上线前由你提供真人童声慢速录音 | 你直接提供 5 条录音，我按文件名接入；或 V1 完全静音（不推荐，交互反馈会缺失） |
| ⑦ | **家长门最小面板样式**（§7.5 要求有反馈，但设计稿未给样式） | 按授权「细节由 AI 补充」补齐：长按 1.5s 后弹出**底部居中白色圆角 32dp 卡片**（遮罩 40% 黑、无新页面），含 3 项只读/开关（每日时长上限 5 分钟、音效开关、今日进度），8 秒无操作自动关闭；**不新增完整家长中心页面** | 改为全屏 Dialog / 或先做成 Toast 级最小反馈；需你确认视觉尺度 |

---

## 3. 技术方案正文

### 3.1 工程形态与目录映射

新建 `D:\英语学习\app\` 即 Gradle 根工程（单模块，`settings.gradle.kts` 只声明 `rootProject.name`），**没有 `:app` 子模块**。

| `design.md §9.1` 写法 | 实际落盘路径 | 说明 |
|---|---|---|
| `app/src/main/java/com/example/guguabc/` | `app/src/main/java/com/guguabc/kids/` | 包名建议改掉 `com.example` 占位；如需保留请明示 |
| `app/src/main/res/` | `app/src/main/res/` | 一致 |
| — | `app/gradle/libs.versions.toml` | 版本唯一锁定处 |
| — | `app/tools/` | 验收脚本（不属于 APK） |

### 3.2 版本与依赖（按决策 ① 方案 A）

| 组件 | 锁定值 | 依据 |
|---|---|---|
| Gradle | `8.11.1` | 本机 wrapper 缓存已有，可用缓存发行包执行 `gradle wrapper` 生成 jar/properties |
| AGP | `8.9.x` | 官方兼容矩阵要求 Gradle ≥ 8.11.1、JDK 17；本机 build-tools 35.0.1 齐备 |
| Kotlin | `2.1.x` + `org.jetbrains.kotlin.plugin.compose` 同版本 | KGP 2.1.x 与 AGP 8.7–8.10 兼容 |
| Compose BOM | 阶段 1 实测锁定（候选 2024.12 / 2025.01 一线，以 `assembleDebug` 通过且**不要求 compileSdk > 34** 为准） | 不预先写死，避免猜版本 |
| compile / target / minSdk | `34 / 34 / 26` | 严格取自 `design.md §2` |
| 依赖白名单 | `compose ui / foundation / ui-tooling-preview(debug) / activity-compose / core-ktx / datastore-preferences / kotlinx-coroutines`；**不用 navigation-compose**（状态机用 `sealed interface` + 状态容器，更少依赖） | 见决策 ⑤ b |
| 禁止 | 网络权限、ExoPlayer、Coil/Glide、Retrofit、Material 组件默认外观、任何第三方 SDK | `design.md §2 / §10` |

> 兼容性矩阵来自公开资料检索，最终以本机 `./gradlew assembleDebug` 实测为准；锁定值回写 `design.md §2`。
> **构建期联网下载依赖 ≠ 运行期联网**：APK 内 `uses-permission` 仍为 0 个。

### 3.3 1:1 渲染地基（最关键的一层）

```
GuguApp()
 └─ CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1f))
     └─ GuguTheme (自定义 CompositionLocal：GuguColor / GuguDimens / GuguType / GuguShadow)
         └─ GuguScreenContainer(Box.fillMaxSize, 背景色, 底部 16dp 保留区)
             └─ 当前 Screen（由 SessionState 决定）
```

- **固定设计画布**：根容器按 1280 × 800 设计单位布局；`density` 取决策 ② 的结果（默认系统值，兜底固定 1.5f）。所有 §6 的 `ABSOLUTE(x,y)` 用 `Modifier.offset(x.dp, y.dp).size(w.dp, h.dp)` 放在根 `Box` 中，**不用百分比、不用 weight、不用 fillMaxWidth 猜比例**。
- **容器与叶子分离**：容器用 `fillMaxSize / wrapContentSize / Column / Row`；只有插画、圆形底盘、贴纸、图标允许硬编码 dp，且全部取自 `GuguTokens`。
- **系统栏**：`WindowCompat.setDecorFitsSystemWindows(window, false)` + `WindowInsetsControllerCompat.hide(...)`；顶部 36dp 留白但不绘制假状态栏；底部 16dp 不占内容。
- **字体**：`LocalDensity` 锁 `fontScale=1f`；4 个 TTF 打包；阶段 2 跑 `tools/check_font_coverage.py` 用 fontTools cmap 逐字校验 `Strings.kt`，缺字判不合格。
- **Manifest**：`screenOrientation="landscape"`、`resizeableActivity="false"`、`supportsRtl="false"`、0 个权限。退出路径由 ⑤「休息啦」`finish()` 提供。

### 3.4 Token 与组件层

| 层 | 内容 | 约束 |
|---|---|---|
| `GuguTokens.kt` | 18 色 hex、圆角/间距/触控、字号字重、8 组阴影（色值/透明度/dy/blur） | 每个数值带节点 ID 注释；Composable 内禁止裸数字/裸色值 |
| `GuguTheme.kt` | 自定义 CompositionLocal | 不套 MaterialTheme 色板 |
| `components/`（7 个） | `GuguScreenContainer` / `PrimaryButton` / `RoundIconButton` / `IllustrationCircle` / `ProgressDots` / `SoundButton` / `ParentGateButton` | 全部 `testTag`、`clickable(indication = null)`、按压 `scale 0.97/100ms`、无涟漪 |
| `GuguShadow.kt` | 见决策 ③ | 先单点校准，静态缓存，1 层 |

### 3.5 五屏实现策略

| 屏 | 根布局 | 绝对定位元素 | 运行时数据 |
|---|---|---|---|
| ① 首页 | `Column` → `Row(padding 48, gap 56)` | 家长入口 `(1140,48)` 80×80 | 课程名/词表来自 `SessionState` |
| ② 认一认 | `Row(padding 96, gap 48)` | 进度点 `(595,56)`、返回 `(48,44)`、音效 `(1160,44)` | `cardIndex` 驱动词图/单词/释义/进度点 |
| ③ 听一听 | `Column(padding 96, gap 20)` | 进度点、返回、音效 | `quizIndex` 驱动目标词与 A/B 图；**图上零文字** |
| ④ 答对啦 | `Column(padding 96, gap 12)` | 左右彩纸 `(60,80)/(1040,80)`、返回、题号 `(580,58)` | 题号/副标题由 state 驱动；第三颗星保持空心 |
| ⑤ 今天真棒 | `Row(padding 96, gap 40)` | 完成提示 `(48,58)`、家长入口 `(1160,44)` | 家长卡分钟数/词表来自运行时；达上限禁用「再玩一次」 |

**静态先行**：阶段 4 五屏先接示例 state，只验收像素（#3–#11），不写业务逻辑。

### 3.6 状态、数据与行为

- `sealed interface GuguRoute` + `SessionState`（`ViewModel` + `StateFlow`），流程严格按 §7.1：HOME → CARD(0..2) → QUIZ(0..2) → CHEER → DONE。
- 切页仅 200ms 淡入淡出，**无滑动手势、无底部导航**。
- `Words.kt` 3 词、`Strings.kt` 全量文案常量（标点逐字）、UI 内零拼接零硬编码。
- `AudioPlayer.kt`（`SoundPool` 封装）：进 CARD/QUIZ 各播 1 次、QUIZ 10s 无操作重播 1 次、`AUDIOFOCUS_GAIN_TRANSIENT`、后台即停、切页释放。
- **无失败态**：选错只重播发音 + 100ms 缩放，无红叉/变色/扣分。
- 防连点 400ms（CHEER 额外 1.2s 可点）；长按家长门 ≥1500ms，短按无反应。
- `Prefs.kt`（DataStore）：音效开关、`todayDate` / `usedSeconds` 跨天清零、5 分钟上限。
- V1 禁止：账号、登录、上传、埋点、广告、外链、内购、任何设备标识符读取。

### 3.7 资源落地

| 资源 | 目标目录 | 处理 |
|---|---|---|
| 4 张插画 PNG | `res/drawable-nodpi/` | 原样，`ContentScale.Fit`，不加白底；验收期不降采样 |
| 11 个纯 path SVG | `res/drawable/*.xml` | 脚本转 VectorDrawable：`viewBox → viewportWidth/Height`，`transform=matrix(tx,ty) → <group translateX/Y>`，`pathData`/`fillColor` 原样 |
| 2 个 parent SVG | `res/drawable/*.xml` | `<circle>` 数学等价展开为 arc path |
| confetti ×2 | `res/drawable-nodpi/*.png` | 从 Ardot 节点 `scale=2` 导出（含 `<rect rx>`，按 §8 走 PNG） |
| `dots_*` / `status_icons_ref` | **不入包** | 进度点由 `ProgressDots` 运行时绘制；状态栏图标按 §1.2 不实现 |
| 字体 | `res/font/` + `OFL.txt` | 按 §1.3 子集化；`OFL.txt` 走可达的官方分发渠道，取不到则写入标准 SIL OFL 1.1 全文 + 来源声明 |
| 音频 | `res/raw/` | 占位 ogg（决策 ⑥） |

### 3.8 验收与自动化

- `tools/verify_screens.py`：`adb install/launch` → 按 §6 坐标 `adb shell input tap` 驱动 5 屏流程 → `adb exec-out screencap -p` 逐屏截图 → 统一到 1280 × 800（真机 ÷1.5、基准 `*_clean.png` ÷2）→ 输出主要元素包围盒偏差表 + 叠加图。
- 比对排除区：顶部 0–36dp、底部手势条、四角 32dp 圆角（按 §1.2 属预期差异）。
- 其余按 `design.md §11` 的 #1–#20 逐条走查，#3/#4 用 Layout Inspector 抽查。
- **当前无法做真机验收**：`adb devices` 为空、现有 AVD 都是手机。需你二选一：USB 连接荣耀 X9 开调试（**推荐，同时解密度**），或在 Android Studio Device Manager 建 1920 × 1200 / 16:10 平板 AVD。

---

## 4. 分阶段执行计划（获批后）

> 工作量用 S/M/L 相对档位表示，不是交付时限承诺；每阶段单独验收后再进入下一阶段。

| 阶段 | 产出 | 依赖 | 完成标志 | 量级 | 注意事项 |
|---|---|---|---|---|---|
| 0 · 文档对齐 | 修订 `design.md` 4 处（决策 ⑤）+ 锁定版本回写 | 你的批复 | 真值源无自相矛盾 | S | 只改配置/清单，不动任何设计数值 |
| 1 · 骨架 | 建 `app/`、Gradle wrapper、Manifest、单 Activity、横屏、隐藏系统栏、DEBUG 密度日志 | 0 | `assembleDebug` 通过；空屏 1280×800 底色正确 | M | 版本链实测在此阶段定死；JDK 21 与 AGP 8.9 兼容性实测 |
| 2 · Token + 资源 | `GuguTokens/GuguTheme`、字体下载与子集化、图标转换、插画落盘、`check_font_coverage.py` | 1 | 常量齐备且带节点 ID；字体无缺字 | M | 31 MB 字体必须裁剪；`raw.githubusercontent` 不通，OFL 换渠道 |
| 3 · 组件 | 7 个组件 + `GuguShadow` | 2 | 单组件预览尺寸/阴影真机校准通过 | M | **阴影先做 1 个**，避免 7 个一起返工 |
| 4 · 五屏静态 | 5 个 Screen（示例 state） | 3 | 通过验收 #3–#11、像素包围盒 ≤2dp | L | 此阶段不写业务逻辑；`ABSOLUTE` 坐标逐个核对 |
| 5 · 状态与音频 | 状态机、词表、音频、DataStore、防连点、家长门 | 4 | 通过 #13–#17 | L | 进度点/题号/家长卡数据不得硬编码 |
| 6 · 走查 | `verify_screens.py` + #1–#20 逐条打勾 + 偏差清单 | 5 + 真机 | 全部通过或偏差项留档 | M | 偏差不得以"看起来更好"保留 |

---

## 5. 文件级变更汇总（计划创建，非已完成）

| 路径（相对 `D:\英语学习\app\`） | 作用 | 阶段 |
|---|---|---|
| `settings.gradle.kts` / `build.gradle.kts` / `gradle.properties` | 单模块根工程配置 | 1 |
| `gradle/libs.versions.toml` / `gradle/wrapper/*` | 版本唯一锁定 + wrapper | 1 |
| `src/main/AndroidManifest.xml` | 横屏锁定、0 权限、resizeable/RTL | 1 |
| `src/main/java/com/guguabc/kids/MainActivity.kt` | 单 Activity、隐藏系统栏、DEBUG 密度日志 | 1 |
| `…/GuguApp.kt` | `LocalDensity` + Theme + 状态容器 | 1 |
| `…/design/GuguTokens.kt` / `GuguTheme.kt` / `GuguShadow.kt` | Token 真值源 + 阴影 | 2/3 |
| `…/design/components/*.kt`（7 个） | §5 全局组件 | 3 |
| `…/screens/*.kt`（5 个） | 五屏静态还原 | 4 |
| `…/state/GuguRoute.kt` / `SessionState.kt` / `GuguViewModel.kt` / `Prefs.kt` | 状态机与持久化 | 5 |
| `…/data/Words.kt` / `Strings.kt` | 词表与文案常量 | 5 |
| `…/audio/AudioPlayer.kt` | SoundPool 封装 | 5 |
| `src/main/res/drawable/*.xml`（13） / `res/drawable-nodpi/*`（4 PNG + 2 confetti + 备用图标） | 图标与插画 | 2 |
| `src/main/res/font/*`（4 TTF + OFL.txt） | 字体 | 2 |
| `src/main/res/raw/*`（5 ogg 占位） | 音频 | 5 |
| `tools/check_font_coverage.py` / `tools/svg_to_vector.py` / `tools/verify_screens.py` | 校验与转换脚本（不进 APK） | 2/6 |

> 同时修订 `D:\英语学习\design.md`（仅 §2 版本/白名单、§9.1 路径、§11 #11 四处）。

---

## 6. 关键风险与未决

| # | 风险 | 当前状态 | 处理 |
|---|---|---|---|
| R1 | 真机 density 未知 | **未决**，无设备 | 决策 ②；USB 连接后一条 logcat 即可定案 |
| R2 | 无 16:10 平板 AVD / 无真机 | **未决** | 你连接设备或建 AVD 后阶段 6 才能闭环 |
| R3 | targetSdk 34 与现行应用市场要求 | 已知事实：Google Play 自 2026-08-31 要求新应用/更新 targetSdk ≥ 36；**国内渠道要求本方案未核实、不预判** | V1 为真机原型不上架；上架前按届时官方要求单独升级 |
| R4 | 音频素材缺失 | 未决 | 决策 ⑥；占位不阻塞 1:1 视觉验收 |
| R5 | 字体体积 31 MB / 授权文件 | 已知，工具链可达 | 子集化 + OFL 随包 |
| R6 | `BlurMaskFilter` 在硬件加速下的表现 | 待实测 | 决策 ③ 的 PNG 退路 |
| R7 | Android 15/16+ 大屏会忽略 `resizeableActivity` / 方向限制 | 未来风险（当前 targetSdk 34、设备 Android 14 不受影响） | 升级 targetSdk 时需重新验证锁屏方向 |
| R8 | `Modifier.dropShadow` 等新 API 不可用 | 方案 A 下确定 | 用自建 `GuguShadow`，不依赖新 API |

---

## 7. 下一步

请回复：

1. **① 版本链**选 A 还是 B（建议 A）；
2. **② 密度兜底**是否同意"实测≠1.5 时固定设计 density"；
3. **⑤ 是否授权修订 `design.md` 4 处**（建议授权）；
4. **⑥ 音频**用占位音还是你提供录音；
5. **⑦ 家长门面板**是否接受我补的样式；
6. 是否能 **USB 连接荣耀 X9 并开启 USB 调试**（这决定密度与最终 1:1 验收能否闭环）。

确认后我从**阶段 0/1** 开始，先建 `D:\英语学习\app\` 骨架并跑通 `assembleDebug`，再逐阶段交付验收。
