# 咕咕 ABC · V2 增量设计规格（24 词动物认知）

> **文档定位**：`design.md` 是 **V1 五屏 1:1 还原的唯一真值源**，其像素基准、文案、验收表
> 尚未在真机闭环，**V2 不得改写 V1 正文**，否则历史验收失去基准。本文是 V2 的增量规格，
> 与 `design.md` 配套使用；凡本文未提及的视觉、Token、组件、交互，一律沿用 V1 原值。
>
> 版本：V2.0 · 2026-11-09 · 状态：已确认（词量 24 / 选课权归家长 / 发音走离线预生成 A 方案）

---

## 1. 目标与边界

| 项 | V2 决议 |
|---|---|
| 词量 | **24 个常见动物**，8 课 × 3 词；前 6 课 L1（3–4 岁）、后 2 课 L2（4–5 岁） |
| 单课词数 | **恒为 3**，V1 的 5 张像素基准图、`ProgressDots(count=3)`、CHEER 三颗星、DONE 三格贴纸墙均不废 |
| 选课权 | **家长侧**：长按家长门内选择/解锁课程包；宝宝侧**不出现**课程表、关卡地图、底部导航（沿用 `design.md §7.1` 禁令） |
| 复习 | 新增「贴纸册」，宝宝可点已学动物听音复习，是 V2 的第二条主线（间隔重复优先于堆词量） |
| 无失败态 | 不变：选错只重播发音，不扣分、不锁按钮、不记录错误 |
| 每日时长 | `TIME_LIMIT_SECONDS = 5 * 60` 语义由「一课」改为「**每日会话总时长**」；一课约 1.5–2 分钟，到点强制 DONE |
| 合规底线 | 沿用 `§7.5`：**无账号、无上传、无埋点、无广告、无外链、无内购**；新增的学习进度**只存本地 DataStore** |
| 明确不做 | 中文语音、句子、拼写、字母、测验分数、成就体系、连续打卡、联网同步 |

---

## 2. 词库（唯一真值源：`app/tools/word_packs.json`）

> `group` 用于 3 的干扰项规则；`anchor` 是插画必须保留的物种识别特征，用于 9.2 的剪影抽查。
> `en` 首字母大写为卡片显示形式；音频文本用全小写单词，`Piper` 不接受驼峰。

| # | id | en | zh | 课 | 主题 | level | group | anchor |
|---|---|---|---|---|---|---|---|---|
| 1 | cat | Cat | 小猫 | 1 | 身边的小伙伴 | L1 | feline | 竖耳 + 胡须 + 长尾 |
| 2 | dog | Dog | 小狗 | 1 | 身边的小伙伴 | L1 | canine | 垂耳 + 吐舌 |
| 3 | bear | Bear | 小熊 | 1 | 身边的小伙伴 | L1 | chunky | 圆耳 + 厚实身形 |
| 4 | pig | Pig | 小猪 | 2 | 农场里 | L1 | chunky | 猪鼻 + 卷尾 |
| 5 | cow | Cow | 奶牛 | 2 | 农场里 | L1 | chunky | 黑白斑 + 小角 |
| 6 | duck | Duck | 小鸭 | 2 | 农场里 | L1 | bird | 扁黄嘴 + 蹼 |
| 7 | rabbit | Rabbit | 兔子 | 3 | 草地上 | L1 | tiny | 长耳 + 短尾 |
| 8 | sheep | Sheep | 绵羊 | 3 | 草地上 | L1 | chunky | 蓬松卷毛 |
| 9 | horse | Horse | 马 | 3 | 草地上 | L1 | slender | 长脸 + 鬃毛 |
| 10 | fish | Fish | 小鱼 | 4 | 池塘边 | L1 | aquatic | 尾鳍 + 流线 |
| 11 | frog | Frog | 青蛙 | 4 | 池塘边 | L1 | aquatic | 大眼 + 蹲姿 |
| 12 | bird | Bird | 小鸟 | 4 | 池塘边 | L1 | bird | 尖喙 + 展翅 |
| 13 | fox | Fox | 狐狸 | 5 | 树林里 | L1 | canine | 尖脸 + 大尾 |
| 14 | monkey | Monkey | 猴子 | 5 | 树林里 | L1 | primate | 长尾 + 圆耳 |
| 15 | deer | Deer | 鹿 | 5 | 树林里 | L1 | slender | 鹿角 |
| 16 | lion | Lion | 狮子 | 6 | 动物园 | L1 | feline | 鬃毛 |
| 17 | tiger | Tiger | 老虎 | 6 | 动物园 | L1 | feline | 橙底黑条纹 |
| 18 | panda | Panda | 熊猫 | 6 | 动物园 | L1 | chunky | 黑白眼圈 |
| 19 | elephant | Elephant | 大象 | 7 | 大个子朋友 | L2 | giant | 长鼻 + 大耳 |
| 20 | giraffe | Giraffe | 长颈鹿 | 7 | 大个子朋友 | L2 | slender | 长脖 + 斑块 |
| 21 | zebra | Zebra | 斑马 | 7 | 大个子朋友 | L2 | slender | 黑白条纹 |
| 22 | owl | Owl | 猫头鹰 | 8 | 特别的朋友 | L2 | bird | 大眼 + 面盘 |
| 23 | turtle | Turtle | 乌龟 | 8 | 特别的朋友 | L2 | aquatic | 圆壳 |
| 24 | butterfly | Butterfly | 蝴蝶 | 8 | 特别的朋友 | L2 | tiny | 对称大翅 |

**课包 id**：`p1_friends` / `p2_farm` / `p3_meadow` / `p4_pond` / `p5_forest` / `p6_zoo` / `p7_big` / `p8_special`。
第 1 课**保持 V1 原样**（Cat / Dog / Bear），是像素基准课，图片、音频重制但视觉不得变。

**资源命名**：插画 `img_<id>.png`，发音 `word_<id>.ogg`，全部沿用 V1 前缀规则，禁止拼音/无语义名。

### 2.1 未收录动物与理由

| 未收录 | 理由 |
|---|---|
| Chick / Hen / Goose | 与 Duck、Bird 同为禽类；中文「小鸡 / 小鸟 / 小鸭」对 3 岁易混，轮廓近似 |
| Wolf | 与 Dog 轮廓太近，且非 3–5 岁生活常见 |
| Snake / Crocodile / Hippo / Rhino / Squirrel | 或音节过长，或超出生活经验，或易引发恐惧 |
| Penguin / Whale / Dolphin / Koala | 留作 V3「远方动物」包 |
| Bee | 与 Butterfly 同为小型带翅，二选一辨识度差 |

> 若后续要补「小鸡」，用 `Chick` **替换**课 8 的 `Butterfly`（同为 3 音节，替换成本最低），不要扩成 4 词一课。

---

## 3. 干扰项（二选一）规则

V1 的 `quizFor(index)` 用课内 `Words.all[(index+1) % size]` 取干扰项。按语义分组后，
课 6 会出现 `Lion vs Tiger`（同为猫科、同为四足卧姿），对 3 岁变成猜谜，故 V2 改为：

1. 干扰项从**已学词**（含本课与历史课）中取，**禁止与 target 同 `group`**。
2. 优先取体型/姿态差异最大者；同一 target 连续两次不得使用同一干扰项。
3. 显式禁用组合：`lion↔tiger`、`cat↔lion`、`dog↔fox`、`duck↔bird`、`horse↔zebra`、`turtle↔frog`。
4. 目标词左右轮换（沿用 V1 的 `index % 2`），**选项卡上不出现任何文字**。
5. 首课（第 1 课）保持 V1 的课内轮换结果不变，确保像素基准与验收可复现。

> 副作用是正向的：新旧词混合进二选一，天然形成间隔重复。

---

## 4. 信息架构

```
HOME ──开始玩──> CARD(0..2) ──> QUIZ(0..2) ──> CHEER ──> DONE
  │                                                        │
  │（长按 1.5s 家长门）                                     ├──> 贴纸册（新页，可返回 DONE）
  └──> 家长设置面板 ──> 课程包列表（新页，可返回）            ├──> 再玩一次（当日未超时）
                                                            └──> 休息啦 → finish()
```

- 家长面板内新增「今日课程」一行，点击展开 **8 课列表**；选课后回首页，首页课程卡内容随课包变化。
- 宝宝侧流程仍是单一闭环，**不新增任何导航元素**。
- 学习进度 `learnedWordIds` 只由"完成某课 QUIZ"写入，不因点贴纸册改变。

---

## 5. 新增页规格（沿用 V1 Token，基准 1280 × 800 dp）

### 5.1 贴纸册 `StickerBookScreen`

| 元素 | 规格 |
|---|---|
| 根容器 | `GuguScreenContainer`，底色 `#FFFAF0`，`testTag = screen_sticker_book` |
| 顶部 | 返回圆钮（复用 `RoundIconButton` + `ic_back`，48dp，绝对定位 x48/y48）；标题「我的动物朋友」40sp Bold `#4A3423`，居中 |
| 副标题 | 「点一点，再听一次」22sp Regular `#9A876F` |
| 网格 | 4 列 × 6 行 = 24 格，格 160 × 160、圆角 36、`#FFFFFF`、gap 16，居中；总宽 `4×160+3×16 = 688dp` |
| 贴纸 | 复用 V1 `Sticker()`：132 × 132 插画 `ContentScale.Fit`，`shadowSticker` |
| 未解锁态 | 灰阶 `alpha=0.28` + `#E8DDC8` 底，**不显示问号、不显示锁图标**（避免制造"未完成"焦虑） |
| 交互 | 点已解锁贴纸 → 播该词发音 + 100ms 缩放反馈；点未解锁 → 无反应（不提示、不弹窗） |
| 禁止 | 不显示数量、不显示百分比、不排序、不分组标签、不出现任何"集齐/奖励"文案 |

### 5.2 家长课程包列表 `ParentPackListScreen`

| 元素 | 规格 |
|---|---|
| 承载 | **不新建全屏页**：沿用 V1 家长门底部卡片（`§7.5` / 附录 C ④），高度由 200 扩到 420dp，宽 720dp，圆角 32，`#FFFFFF`，40% 黑遮罩 |
| 标题 | 「今日课程」20sp Bold `#4A3423`；右上角关闭圆钮 |
| 列表 | 8 行，每行高 56dp、圆角 16；选中行底 `#FFF1CC` + 1dp `#FFC857` 描边；未选中 `#FFFFFF` |
| 行内容 | 课号 `第 N 课` 18sp `#9A876F` + 主题名 20sp `#4A3423` + 3 个 32dp 小头像（当前课包三词） |
| L2 标识 | 第 7/8 课右侧加 18sp `#6A93A8` 文字「大朋友」，**不做锁定态、不做解锁条件** |
| 交互 | 点行 → 写入 `currentPackId` → 关闭面板 → 回首页；8s 无操作自动关闭（沿用 V1） |
| 禁止 | 不出现进度条、星星数、完成率、付费/解锁入口 |

---

## 6. 现有页改动清单

| 页 | 改动 | 不变 |
|---|---|---|
| HomeScreen | `LESSON_TITLE` 按课包动态；`LESSON_META` 改为 `"3 个新词 · 约 2 分钟"`；三词头像取 `currentPack.words`；`btn_start` 启动当前课 | 双栏布局、吉祥物、家长门位置、`testTag` |
| WordCardScreen | 无 | 全部（1:1 基准） |
| ListenQuizScreen | 干扰项来源改 3 的规则 | 两张 420×370 卡、72dp 间距、无文字、10s 重播 |
| CheerScreen | 无 | 三颗星（2 实 1 空）、1.2s 延迟、题号格式 |
| DoneScreen | 贴纸墙仍显示**本轮 3 词**；其下新增文字入口「看看我的贴纸册」24sp `#3F7E9E`；家长小结词表取本轮实学 | 左右分栏、家长卡 200 高、徽章/家长门位置 |
| ParentPanel | 增加「今日课程」行与 5.2 列表 | 时长上限、音效开关、今日进度、8s 自动关闭 |

全部中文文案进 `Strings.kt`，Composable 内禁止裸中文；新增 `testTag`：
`screen_sticker_book` / `sticker_grid` / `btn_sticker_book` / `pack_row_1..8`。

---

## 7. 数据层

```kotlin
data class Word(
    val id: String, val en: String, val zh: String,
    val packId: String, val level: String, val group: String,
    @DrawableRes val imageRes: Int, @RawRes val audioRes: Int,
)
data class WordPack(val id: String, val title: String, val level: String, val words: List<Word>)
```

- `Words.all` 保留为 24 词扁平表（兼容旧调用），新增 `Words.packs` 与 `Words.of(packId)`。
- `GuguRoute.Card / Quiz / Cheer` 增加 `packId`（或统一包一层 `LessonContext`），**禁止跨课串词**。
- `GuguNavHost` 三处 `Words.all[r.index]` 自动播报改为 `session.currentLesson.words[r.index]`；
  `DoneScreen.learnedWords` 由 `Words.all` 改为本轮 `currentPack.words`。
- `Prefs` 新增 `currentPackId: String`、`learnedWordIds: Set<String>`；
  V1 旧数据缺字段时默认 `p1_friends` + 空集合，**不得清掉**已有 `soundEnabled / todayDate / usedSeconds`。
- `quizFor()` 按 3 重写；`replay()` 回当前课 `Card(0)`，不是第 1 课。

---

## 8. 发音规格（A 方案：离线预生成）

| 项 | 规格 |
|---|---|
| 生成方式 | **Piper TTS 本地一次性生成**，语音模型 MIT（可商用、可分发、无需署名）；引擎 GPL-3.0 只在生成端使用，**不进 APK** |
| 运行时 | **只有 `MediaPlayer` 播 `res/raw/*.ogg`**；无 TTS 引擎依赖、无网络请求 —— 不违反 `design.md §7.4` |
| 音色 | 单一女声（`en_US-lessac-medium` 或同等），24 词全程同一音色，避免听感跳变 |
| 语速 | `length_scale ≈ 1.3`（慢速），单词单发，不做句子、不加冠词 |
| 音频 | 单声道 22050Hz、OGG Vorbis；前后各留 120ms 静音；峰值归一化到 -3dBFS；单条 ≤ 2s |
| 命名 | `word_<id>.ogg`，24 条；V1 的 `word_cat/dog/bear.ogg` **一并重制**（原为合成占位音）；`cheer.ogg` / `tap.ogg` 保留 |
| 听审 | 逐条人工听：单词可辨、无吞音、无机械断字、音量一致；`Giraffe` / `Zebra` / `Butterfly` 重点复核 |
| 授权留痕 | 随 APK 附 `assets/TTS_NOTICE.txt`：说明音频为本地神经语音合成、非真人录音、合成引擎与模型许可；儿童产品需向家长明示合成语音 |
| 中文路径 | `libsndfile` 在 Windows 上无法写中文路径 → 先在纯 ASCII 临时目录生成再拷贝（沿用 `prepare_assets.py` 既有做法） |

> 备选 B（Azure 付费层）仅在需要更接近真人音质时启用：微软 Product Terms 的商业输出权**仅授予付费 S0 层**，
> 免费 F0 不含，且需披露合成语音。**不使用 `edge-tts`**（非微软产品，未声明商用权）。

---

## 9. 插画规格

### 9.1 统一规格

- 1024 × 1024 **RGBA 透明底**、PNG、`ContentScale.Fit`、放 `res/drawable-nodpi/` 与
  `gugu-abc-android-assets/illustrations/` 各一份。
- 风格锚：以 V1 `img_cat.png` 为参考做图生图，沿用奶油黄绘本风（`#FFFAF0` / `#FFC857` / `#FF8C7A` / `#6BB6E0` / `#7DD3A0`）。
- 统一：同一描边粗细、同一眼睛画法、同一柔和阴影、主体居中占比 ≈ 70%、无文字、无背景场景、无水印。
- 生成后逐张校验：alpha 通道存在且四角全透明、1024×1024、无右下角水印、无浅灰棋盘格底。
  V1 已踩过这三个坑，校验逻辑复用 `generated-images/make_alpha.py` 的思路。

### 9.2 「清晰认知」验收（V2 特有）

每张必须保留 §2 表的 `anchor` 特征；批量出图后做**剪影抽查**：把图转纯黑剪影，
若无法在 3 秒内认出物种即重画。重点盯 `Lion / Tiger`（鬃毛 vs 条纹）、
`Horse / Zebra / Giraffe`（长脸三兄弟）、`Rabbit / Sheep`（都是圆毛团）。

> 风险提示：AI 越往"可爱"画越容易丢物种特征，宁可略微写实也不要 24 团黄色毛球。

### 9.3 字体子集

重跑中文 cmap 子集化：24 个中文名新增「颈、蝶、龟、猫头鹰、狮、狐、鹿、斑马」等字；
英文子集需覆盖 24 个词的大小写。不重跑会静默 fallback 到系统字体，破坏 1:1。

---

## 10. 资源与验收清单

| 类别 | 数量 | 说明 |
|---|---|---|
| 新插画 | **21 张** | 已有 3 张动物图 + 吉祥物复用；24 词全覆盖 |
| 重制发音 | **24 条** | 含 V1 三词重制；`cheer/tap` 不变 |
| 新增页面 | 2 | 贴纸册（全屏）、家长课程包列表（面板扩展） |
| 改动页面 | 4 | Home / ListenQuiz / Done / ParentPanel；WordCard / Cheer **零改动** |

**验收脚本改造**：`tools/verify_project.py` 现有 35 项里以下为 V1 写死清单，须参数化后再跑：
`#2` 文案常量、`#11` 14 个图标、`#14` 5 条 OGG、4 张插画清单、`LESSON_META` 字面量。
新增 V2 检查项：24 词表与 JSON 一致、每词有图有音、`anchor` 非空、干扰项不同 group、
`learnedWordIds` 无联网写出、贴纸册 24 格、家长面板 8 行、OGG 峰值与静音边界。
`tools/verify_screens.py` 的 5 张 V1 基准图**保持不变**，两页新页另出设计稿基准。

**打包验收**：改动完成后仍执行 `./gradlew assembleDebug assembleRelease`，
静态走查 35+ 项全 PASS；真机 1:1 像素验收仍由用户连接荣耀平板 X9 后执行，agent 不主动做 adb 真机操作。

---

## 11. 待确认项与风险

| # | 事项 | 影响 | 建议 |
|---|---|---|---|
| ① | Piper 语音模型下载与本机推理环境 | 装不上则 24 条发音无法产出 | 已验证 `hf-mirror.com` 可达；失败时降级为 Windows SAPI（零安装，但机械音，产品体验下降） |
| ② | 插画批量生成的风格一致性 | 21 张若跑偏，全部返工 | 先用 1 张样本定风格锚，确认后再批量 |
| ③ | 合成语音的家长告知 | 儿童产品透明度要求 | 上线前在家长面板加一行「单词发音由电脑合成」，不放在宝宝侧 |
| ④ | `densityDpi` 未实测（V1 遗留） | 新页同受影响 | 沿用 V1 固定设计 density 兜底，真机 logcat 后统一回写 |
| ⑤ | 24 词中文/英文授权与分发 | 上架合规 | 上架前复核 Piper 模型 MIT 条款与字体 OFL 随包，本文不作法律结论 |

---

## 附录 · V1 / V2 关系

- `design.md`：**V1 五屏**，1:1 像素基准，正文冻结。
- `design_v2.md`（本文）：**V2 增量**，词库 / 两页 / 数据层 / 资源规格。
- `app/tools/word_packs.json`：24 词结构化清单，供 `prepare_assets.py`、词表生成与验收脚本共同消费。
- 三者冲突时：**视觉基底以 `design.md` 为准，词与流程以本文为准，机器可读数据以 JSON 为准**。

---

## 附录 · 2026-11-09 链路实测决议（覆盖 §11 ① 的"待确认"）

流水线脚本：`app/tools/prepare_v2_assets.py`（纯标准库 + ctypes，零第三方运行时依赖）。
词表：`app/tools/word_packs.json`。

| # | 事项 | 实测结果 / 决议 |
|---|---|---|
| ① | Piper 安装 | PyPI 官方源在本机不通；**阿里云 PyPI 镜像**装成 `piper-tts 1.8.0` + `onnxruntime 1.31.1` + `numpy 2.5.4`（约 3m19s）。语音模型从 `hf-mirror.com` 下载 `en_US-lessac-medium.onnx` **63,201,294 B**（5s，MIT）+ 同名 `.onnx.json`。模型放本机临时目录，**不进版本库** |
| ② | 发音产出 | **24/24 条成功**，22050Hz 单声道 16bit，词长 0.32–0.67s，OGG 单条 5.3–6.9KB、合计约 196KB；音色全程 `en_US-lessac-medium`、`length_scale=1.3` |
| ③ | 音频后处理 | 原始 WAV 峰值 0dBFS、首尾静音不一致（cat 前 4ms / giraffe 前 258ms）→ 脚本统一：裁静音 → 归一化 **-3dBFS** → 前后各 **120ms** 静音 → 10ms 淡入淡出 → libsndfile 写 OGG Vorbis。中文路径先在 ASCII 临时目录生成再拷贝（沿用 V1 做法） |
| ④ | 生图三坑复现 | 生图模型返回的仍是 **RGB（colorType=2，无 alpha）+ 浅灰棋盘格底 + 右下角「生成 / WORKBUDDY」水印**，与 V1 完全一样。`prepare_v2_assets.py image` 复用 V1 `make_alpha.py` 的边缘 flood-fill 算法处理样本：输出 **1024×1024 RGBA**，透明像素 61.5%，主体（眼白、米色肚子、爪子）完整无挖空，水印与棋盘格清除干净 |
| ⑤ | 风格一致性 | 用 V1 `img_cat.png` 做 `image1` + `input_fidelity=high` 后，老虎样本的描边、水彩颗粒、大眼、腮红、柔和阴影与 V1 猫高度一致 → **风格锁得住** |
| ⑥ | **新发现的问题** | 一致性过头：老虎与 V1 猫几乎是**同一坐姿 / 卷尾 / 同一张脸换皮**。若 21 张都这样，会退化成「同一只猫换装」，削弱物种识别，正是 §9.2 担心的风险的变体 |
| ⑦ | 批量姿态策略 | 按 `group` 给每类指定不同姿态，prompt 强制写死：四足站立（chunky/slender/giant）、趴卧（pig/sheep）、游动（aquatic）、展翅（bird/tiny-insect）、坐姿（feline/primate）；同时把该词 `anchor` 作为首要特征描述。`input_fidelity` 由 high 降为 **medium**，只锁画风不锁构图 |
| ⑧ | 资源落位 | 24 条 OGG 先产在 `app/tools/v2_build/audio/` **暂存**，未写入 `res/raw/`：V1 的 `verify_project.py #14` 校验"正好 5 条 OGG"，提前写入会让 V1 静态走查失败。待样本听审确认 + V2 代码改造时，用 `prepare_v2_assets.py audio --commit` 一次性写入（会重制覆盖 V1 的 3 条合成占位音） |
| ⑨ | 运行时 | Android 侧**零改动**：仍只有 `MediaPlayer` 播 `res/raw/*.ogg`，无 TTS 引擎、无网络请求，`design.md §7.4` 未被违反 |

**样本交付**（供确认后再批量 21 张）：
- 插画：`generated-images/v2_samples/img_tiger_clean.png`（原始生成图同目录保留，便于对比）
- 发音：`app/tools/v2_build/audio/word_cat.ogg` / `word_giraffe.ogg` 等 24 条
- 两页版面：`gugu-abc-previews/V2新页预览.html`

**批量开跑前需确认 2 件事**：① 试听样本音色是否接受（不接受可换 `en_US-amy-medium` 等其它 MIT 音色重跑，成本仅本地计算）；
② 是否按 ⑦ 的姿态策略出 21 张（生成 21 张会额外消耗约 105–210 credits）。

> 2026-11-09 用户已确认「继续开发吧，都同意」，两项均按建议执行，见下一附录。

---

## 附录 · 2026-11-09 V2 实现决议（覆盖 §5.1 / §6 的待定项）

| # | 事项 | 决议 |
|---|---|---|
| ① | **贴纸册网格修正** | 原文 4 列 × 6 行 × 160dp 需 `6×160+5×16 = 1040dp` 高，**超出 800dp 画布**。改为 **6 列 × 4 行 × 128dp / gap 16**，总 848×560dp，标题区 160dp，单屏铺满、幼儿无需滚动；贴纸缩为 104×104、圆角 28 |
| ② | DONE 页入口 | 「看看我的贴纸册」用**绝对定位**叠加在 `(96, 690)`，不参与 Column 布局 → V1 五屏所有元素坐标零改动，5 张像素基准图继续可复现 |
| ③ | 首页文案 | 基准课 `p1_friends` 保留 V1 逐字「小动物朋友 / 3 个新词 · 约 5 分钟」；其余 7 课用课包名 + 「3 个新词 · 约 2 分钟」。既满足 §6 动态化，又不废 V1 基准 |
| ④ | 家长面板 | 8 行单列 + 卡片整体 `verticalScroll`（420dp 装不下约 900dp 内容），保留 40% 遮罩、时长 / 音效 / 今日进度；新增合成语音告知行（只在家长侧）。**8s 自动关闭已被下方「交互修复决议」⑥ 改为 20s** |
| ⑤ | 干扰项确定性 | `quizFor()` 保持纯函数：候选按 `|groupRank 差|` 降序 + id 排序，用 `(index + pack.no + sessionSeed) % size` 取值，**不在重组中写状态**。`sessionSeed` 只在 `start / replay / selectPack` 时 +1，保证可复现与验收 |
| ⑥ | 字体子集 | `tools/fontsrc` 曾被清理 → 从 `fonts.gstatic.com` 重新拉 4 个源字体（各约 10MB），`fonttools 4.68.0` 按 252 字符重跑，4 个 TTF 共 **187KB**，cmap 逐字校验通过 |
| ⑦ | 发音落位 | `prepare_v2_assets.py audio --commit` 写入 24 条 OGG（覆盖 V1 三条占位音），`res/raw` 共 26 条；新增 `assets/TTS_NOTICE.txt` 授权与家长告知 |
| ⑧ | 插画 | 21 张按姿态策略生成（`input_fidelity=medium` + 每词 `anchor` 强制写进 prompt），全部经 flood-fill 转 RGBA，逐张校验 1024×1024 / 四角透明 / 右下水印区全透明；`tiger` 按四足行走姿态**重画**，不再与 V1 猫同姿 |
| ⑨ | 验收脚本 | `verify_project.py` 由 35 项扩到 **54 项**，V1 写死的 OGG / 插画 / 文案清单全部改为按 `word_packs.json` 驱动；`verify_screens.py` 的 5 张 V1 基准图不变 |
| ⑩ | APK 体积 | debug **35.3MB** / release **23.4MB**，其中 25 张 1024×1024 PNG 约 27MB。若需瘦身，可做无损 PNG 行滤波重压或降采样到 512×512；当前为保「清晰认知」不动 |

**产物**：`app/build/outputs/apk/debug/GuguABC-debug.apk`（可直接 adb install）、
`app/build/outputs/apk/release/GuguABC-release-unsigned.apk`（未签名）。
最终 `./gradlew assembleDebug assembleRelease` **BUILD SUCCESSFUL**，静态走查 **54 PASS / 0 FAIL**。

**仍未闭环**：`densityDpi` 实测、真机 1:1 像素验收（含 V2 两页新基准图）、release 签名、
24 条合成语音逐条人工听审。真机验证按约定由用户连接荣耀平板 X9 后执行。

---

## 附录 · 2026-11-09 交互修复决议（覆盖 V1 附录 C ④ 部分条款与本文时长口径）

> 输入：真机 DONE 页反馈 ①「再玩一次」点击无反应 ② 右上角人像点击无反应 ③ 贴纸册打开后立即自动关闭。
> 诊断详见 `V2交互问题诊断与修复方案.md`。**`design.md` 正文与 5 张 V1 像素基准图继续冻结**，
> 本附录所有改动只落在状态机与交互反馈层，V1 五屏坐标 / 文案 / 色值零改动。

| # | 事项 | 决议 |
|---|---|---|
| ① | **时长口径** | 5 分钟由「每日会话总时长」改为 **学习态累计时长**：仅 `Card / Quiz / Cheer` 累加；首页 / DONE / 贴纸册 / 家长面板停留不计时。`minutesUsed` 上限因此恒为 5，家长小结不再出现「今天 71 分钟」 |
| ② | **上限跳转** | 到 300s 且处于学习态时**只跳一次** `DONE`（`timeUpHandled` 保护），之后不再累加、不再重复跳转；删除原 `route !is Done` 的一刀切判断 |
| ③ | **旧数据迁移** | V1 口径把页面停留算进 `usedSeconds`（真机实测已达约 4260s）。`GuguSession.init` 中 `stored.usedSeconds > 300 → 0`；新口径下该值恒 ≤300，故归零**幂等、只在升级后首次启动生效**，不新增 DataStore key，也不会让冷启动变成绕过上限的后门 |
| ④ | **贴纸册 / 首页豁免** | `tick()` 内无任何针对 `StickerBook` / `Home` 的判断；贴纸册可停留、可返回 DONE；家长选课后能正常停在首页 |
| ⑤ | **家长可重置** | 家长面板「每日时长上限 · 5 分钟」行改为可点「重置今日时长」，点击后 2 秒显示「已重置」。`resetTodayTime()` **只清 `usedSeconds`**，不动 `learnedWordIds` / `currentPackId` / `soundEnabled`；跨天清零保持原样 |
| ⑥ | **面板自动关闭** | `design.md` 附录 C ④ / 本文实现决议 ④ 的 8 秒延长到 **20 秒**：面板含 8 行课包 + 滚动，8 秒不足以完成选课与重置。仍可点关闭按钮 / 点遮罩立即关闭 |
| ⑦ | **再玩一次反馈** | 禁用态不再用 `clickable(enabled=false)` 吞点击；`PrimaryButton` 新增 `onDisabledClick` 分流。额度耗尽时点击，复用家长小结卡第三行文字位临时显示 2 秒「今天的 5 分钟用完啦，家长可重置」，**零新增元素、零坐标变化**；禁用 alpha 0.45→0.32 且文字同步降透明（基准图拍的是启用态，不受影响） |
| ⑧ | **家长门反馈** | 保留「长按 ≥1500ms 才打开面板、短按不打开」的防误入约束；改为独立 `LaunchedEffect` 计时（不再用 `onPress + withTimeoutOrNull` 阻塞手势循环），按住期间画 1.5s 进度环，松手即取消；短按给一次性文字提示「长按右上角「家长」2 秒可打开设置」，落实 §7.5「必须有反馈，禁止做成死按钮」。进度环与提示只在交互态出现，静态基准图零影响 |
| ⑨ | **验收脚本** | `verify_project.py` 由 54 项扩到 **62 项**，新增 8 项交互修复回归检查（计时口径、单次跳转、贴纸册豁免、旧值迁移、重置不清进度、禁用态提示、家长门反馈、面板时长） |

**边界说明**：`design.md §6.5 / §7.5 / 附录 C ④` 中「8 秒自动关闭 / 短按无任何反应」两句，由本附录 ⑥⑧ 以 V2 增量方式覆盖；`design.md` 正文不修改，待 V1 真机 1:1 验收闭环后再统一回写。

**真机复测清单**（用户连接荣耀平板 X9 后）：
1. 长按人像 2 秒 → 出现进度环并弹出面板，20 秒内不闪退；短按 → 只出现一次性文字提示、不打开面板。
2. 面板点「重置今日时长」→ 显示「已重置」，今日已玩归 0，已学贴纸与课程选择不变。
3. 选任一课 → 回首页可正常点「开始玩」，不再被踢回 DONE。
4. 完成一课 → 点「看看我的贴纸册」可停留 ≥10 秒、返回正常。
5. 连续玩满 5 分钟 → 自动进 DONE；点「再玩一次」→ 家长卡第三行出现 2 秒提示。
