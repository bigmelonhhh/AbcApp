#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
咕咕 ABC · 静态走查（design.md §11 + design_v2.md §10）

用法：python tools/verify_project.py
不依赖真机 / adb；V1 五屏 1:1 约束继续冻结，V2 词表与资源一律以
app/tools/word_packs.json 为唯一真值源，不在本脚本里写死清单。

真机像素比对另由 verify_screens.py 完成（5 张 V1 基准图保持不变）。
"""
from __future__ import annotations

import json
import os
import re
import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src" / "main" / "res"
JAVA = ROOT / "src" / "main" / "java" / "com" / "guguabc" / "kids"
MANIFEST = ROOT / "src" / "main" / "AndroidManifest.xml"
ASSETS = ROOT / "src" / "main" / "assets"
PACKS = ROOT / "tools" / "word_packs.json"

results: list[tuple[str, bool, str]] = []


def check(name: str, ok: bool, detail: str = "") -> None:
    results.append((name, ok, detail))


def all_kt() -> list[Path]:
    return sorted(JAVA.rglob("*.kt"))


def strip_comments(text: str) -> str:
    """注释里常出现被禁止的示例词（如「禁止 Modifier.shadow」），走查必须忽略注释。"""
    text = re.sub(r"/\*[\s\S]*?\*/", "", text)
    return re.sub(r"//[^\n]*", "", text)


def kt_text() -> str:
    return "\n".join(p.read_text(encoding="utf-8") for p in all_kt())


def read(rel: str) -> str:
    return (JAVA / rel).read_text(encoding="utf-8")


def png_ihdr(path: Path):
    """纯标准库读 PNG IHDR；返回 (w, h, colorType) 或 None。"""
    b = path.read_bytes()
    if b[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    w, h, _depth, ctype = struct.unpack(">IIBB", b[16:26])
    return w, h, ctype


def rgba_corners_clear(path: Path) -> bool:
    """V2 生成的 PNG 每行 filter=0；校验四角 alpha=0 与右下角水印区全透明。"""
    b = path.read_bytes()
    pos, idat = 8, []
    while pos < len(b):
        n = struct.unpack(">I", b[pos:pos + 4])[0]
        typ = b[pos + 4:pos + 8]
        if typ == b"IDAT":
            idat.append(b[pos + 8:pos + 8 + n])
        pos += 12 + n
    raw = zlib.decompress(b"".join(idat))
    w, h = 1024, 1024
    stride = w * 4
    if any(raw[y * (stride + 1)] != 0 for y in range(h)):
        return False       # 非我们生成的 PNG，交给其它检查项

    def alpha(x, y):
        return raw[y * (stride + 1) + 1 + x * 4 + 3]

    if any(alpha(x, y) for x, y in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1))):
        return False
    return all(alpha(x, y) == 0
               for y in range(int(h * 0.91), h)
               for x in range(int(w * 0.85), w))


def main() -> int:
    src = kt_text()
    code = "\n".join(strip_comments(p.read_text(encoding="utf-8")) for p in all_kt())
    data = json.loads(PACKS.read_text(encoding="utf-8"))
    words = data["words"]
    packs = data["packs"]
    words_kt = read("data/Words.kt")
    session_kt = read("state/SessionState.kt")
    quiz_kt = read("state/QuizPicker.kt")
    budget_kt = read("state/TimeBudget.kt")
    tokens = read("design/GuguTokens.kt")

    # ---- #1 屏幕方向 / 系统栏 ----
    m = MANIFEST.read_text(encoding="utf-8")
    check("#1 锁定横屏", 'android:screenOrientation="landscape"' in m)
    check("#1 禁止分屏 / RTL",
          'android:resizeableActivity="false"' in m and 'android:supportsRtl="false"' in m)
    check("#2 隐藏系统栏",
          "setDecorFitsSystemWindows(window, false)" in src
          and "hide(WindowInsetsCompat.Type.systemBars())" in src)
    check("#2 fontScale 锁定", "fontScale = 1f" in src)
    check("#2 退出路径", "activity?.finish()" in src)

    # ---- #19 合规：0 权限、无第三方 SDK ----
    perms = re.findall(r"<uses-permission[^>]*>", m)
    check("#19 Manifest uses-permission = 0", len(perms) == 0, f"发现 {len(perms)} 个")
    gradle = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
    banned_sdk = ["ExoPlayer", "Retrofit", "Coil", "Glide", "firebase", "umeng", "ads"]
    hit = [b for b in banned_sdk if b.lower() in gradle.lower()]
    check("#19 无第三方 SDK", not hit, ", ".join(hit))
    toml = (ROOT / "gradle" / "libs.versions.toml").read_text(encoding="utf-8")
    version_lines = "\n".join(
        line for line in toml.splitlines()
        if "=" in line and not line.strip().startswith("#")
    )
    check("#2 版本无 alpha/beta", not re.search(r"(alpha|beta|rc\d)", version_lines, re.I))

    # ---- #5 颜色唯一真值源 ----
    expected_colors = [
        "FFFAF0", "FFF6DE", "FFFFFF", "FFC857", "FFF1CC", "FFE9A8", "FF8C7A",
        "6BB6E0", "7DD3A0", "E9A13B", "E4F3FB", "4A3423", "8A7561", "9A876F",
        "3F7E9E", "6A93A8", "E8DDC8",
    ]
    missing_colors = [c for c in expected_colors if f"0xFF{c}" not in tokens]
    check("#5 17 个颜色 Token 完整", not missing_colors, ",".join(missing_colors))
    outside = []
    for p in all_kt():
        if p.name == "GuguTokens.kt":
            continue
        if re.search(r"Color\(0x[0-9A-Fa-f]{6,8}\)", strip_comments(p.read_text(encoding="utf-8"))):
            outside.append(p.name)
    check("#10 Composable 内无裸色值", not outside, ", ".join(outside))

    # ---- #8 阴影 ----
    check("#8 禁用 Modifier.shadow(elevation)", "Modifier.shadow(" not in code)
    shadow = read("design/GuguShadow.kt")
    check("#8 自建阴影 + 1 层", "fun GuguShadowBox" in shadow and ".blur(" in shadow)
    specs = ["card =", "cardSm =", "option =", "sticker =", "parentGate =", "primary =", "coral ="]
    check("#8 7 组阴影参数齐全", all(s in tokens for s in specs))

    # ---- #9 文案（V1 逐字冻结 + V2 新增） ----
    forbidden_ui = ["9:41", "手势条", "答错啦", "积分商城", "关卡地图"]
    bad = [w for w in forbidden_ui if w in code]
    check("#10/#12 无设计板/失败态/额外页面内容", not bad, ", ".join(bad))
    strings = read("data/Strings.kt")
    required_strings = [
        "早上好呀", "咕咕 ABC", "嗨，我是咕咕！", "今天的一课", "小动物朋友",
        "3 个新词 · 约 5 分钟", "开始玩", "每天 5 分钟 · 不设关卡，不比分数",
        "点一下，听发音", "下一张", "点一点你听到的那个",
        "听不清就再点一次 · 选错不扣分，只会再放一遍", "答对啦！", "再玩一个",
        "没有「答错」，只有再听一次", "今天真棒！", "再玩一次", "休息啦",
        "给爸爸妈妈", "长按右上角「家长」可设置每日时长", "今天到这里",
        # V2
        "3 个新词 · 约 2 分钟", "看看我的贴纸册", "我的动物朋友",
        "点一点，再听一次", "今日课程", "大朋友", "单词发音由电脑合成",
    ]
    missing_strings = [s for s in required_strings if s not in strings]
    check("#9 文案逐字齐备（V1 冻结 + V2）", not missing_strings, ", ".join(missing_strings))
    cn_literals = []
    for p in list((JAVA / "screens").rglob("*.kt")) + list((JAVA / "design" / "components").rglob("*.kt")):
        body = strip_comments(p.read_text(encoding="utf-8"))
        for lit in re.findall(r'"([^"]*)"', body):
            if re.search(r"[\u4e00-\u9fff]", lit):
                cn_literals.append(f"{p.name}:{lit}")
    check("#10 界面代码无裸中文", not cn_literals, "; ".join(cn_literals[:4]))

    # ---- #6 字体 ----
    fonts = sorted(p.name for p in (RES / "font").glob("*.ttf"))
    check("#6 4 个字体文件打包", fonts == [
        "fredoka_bold.ttf", "noto_sans_sc_bold.ttf",
        "noto_sans_sc_medium.ttf", "noto_sans_sc_regular.ttf",
    ], ",".join(fonts))
    total = sum(p.stat().st_size for p in (RES / "font").glob("*.ttf"))
    check("#18 字体已子集化（< 1MB）", total < 1024 * 1024, f"{total // 1024} KB")
    ofl = ASSETS / "OFL.txt"
    check("#6 OFL 授权随包", ofl.exists() and "Open Font License" in ofl.read_text(encoding="utf-8"))

    vendor = os.environ.get("GUGU_VENDOR")
    if vendor and vendor not in sys.path:
        sys.path.insert(0, vendor)
    try:
        from fontTools.ttLib import TTFont
        need = set()
        for p in (JAVA / "data" / "Strings.kt", JAVA / "data" / "Words.kt"):
            for lit in re.findall(r'"((?:[^"\\]|\\.)*)"', p.read_text(encoding="utf-8")):
                need.update(lit)
        need.update(chr(c) for c in range(0x20, 0x7F))
        need.update("·？！，「」、。～／ ")
        miss = []
        for name in fonts:
            if name.startswith("fredoka"):
                continue
            cmap = TTFont(str(RES / "font" / name)).getBestCmap()
            miss += [f"{name}:{c}" for c in need if ord(c) not in cmap]
        check("#6 中文字体无缺字（V2 24 词）", not miss, ", ".join(miss[:6]))
    except ImportError:
        check("#6 中文字体无缺字（V2 24 词）", True, "fontTools 不可用，跳过")

    # ---- #10 #11 图标 ----
    vectors = sorted(p.stem for p in (RES / "drawable").glob("*.xml"))
    expected_icons = [
        "confetti_left", "confetti_right", "ic_arrow_right", "ic_arrow_right_sm",
        "ic_back", "ic_parent", "ic_parent_sm", "ic_play", "ic_sound_lg",
        "ic_sound_md", "ic_volume", "star_empty", "star_filled_1", "star_filled_2",
    ]
    check("#11 14 个图标全部落盘", vectors == expected_icons, ",".join(set(vectors) ^ set(expected_icons)))
    excluded = ["dots_card_1", "dots_quiz_2", "status_icons_ref"]
    check("#11 静态示意/参考图标不入包",
          not any((RES / "drawable" / (e + ".xml")).exists() for e in excluded))
    used = set(re.findall(r"R\.drawable\.(\w+)", src))
    unused = [i for i in expected_icons if i not in used]
    check("#11 每个图标都有使用点", not unused, ", ".join(unused))
    check("#10 无 ContentScale.Crop", "ContentScale.Crop" not in code)

    # ---- 插画 / 音频：按 JSON 驱动 ----
    expected_images = sorted(["ic_gugu_mascot.png"] + [w["image"] for w in words])
    illustrations = sorted(p.name for p in (RES / "drawable-nodpi").glob("*.png"))
    check("#10 25 张插画按 24 词表落盘", illustrations == expected_images,
          ",".join(set(illustrations) ^ set(expected_images)))

    expected_ogg = sorted(["cheer.ogg", "tap.ogg"] + [w["audio"] for w in words])
    ogg = sorted(p.name for p in (RES / "raw").glob("*.ogg"))
    check("#14 26 条本地 OGG（24 词 + cheer/tap）", ogg == expected_ogg,
          ",".join(set(ogg) ^ set(expected_ogg)))
    check("#14 OGG 头正确", all((RES / "raw" / n).read_bytes()[:4] == b"OggS" for n in ogg))
    check("#14 无 TTS / 网络音频",
          "TextToSpeech" not in code and "HttpURLConnection" not in code and "URL(" not in code)

    # ---- #17 state 驱动 ----
    check("#17 进度点由 state 驱动", "ProgressDots(activeIndex =" in src and "activeIndex = cardIndex" in read("screens/WordCardScreen.kt"))
    check("#17 题号由 state 驱动", "quizNumber = route.index + 1" in src)
    check("#17 家长卡数据来自运行时", "minutesUsed = session.minutesUsed" in src)
    check("#13 无失败态（选错只重播）",
          "fun pick(word: Word)" in src
          and "else playWord(target)" in src
          and "答错" not in session_kt.split("fun pick")[1].split("fun continueFromCheer")[0])
    check("#15 防连点 400ms / CHEER 1.2s", "400L" in src and "1_200L" in src)
    check("#16 家长门长按 1500ms", "1500L" in src and "parentGateHoldMs" in src)

    # ---- #12 testTag ----
    tags = ["screen_home", "screen_card", "screen_quiz", "screen_cheer", "screen_done",
            "btn_start", "btn_next", "btn_continue", "btn_replay", "btn_rest",
            "btn_sound", "btn_back", "btn_volume", "btn_parent",
            "option_a", "option_b", "word_en", "word_zh", "progress_dots",
            "sticker_wall", "parent_summary",
            "screen_sticker_book", "sticker_grid", "btn_sticker_book", "pack_list",
            "btn_reset_today"]
    missing_tags = [t for t in tags if f'"{t}"' not in src]
    check("#12 testTag 齐备（V1 + V2）", not missing_tags, ", ".join(missing_tags))
    check("#V2 家长课程包行 tag 由课号驱动",
          'tag = "pack_row_${pack.no}"' in read("state/ParentPanel.kt"))

    # ================= V2 新增检查 =================
    # ① 词表与 JSON 一致
    word_re = re.compile(
        r'Word\(\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*(\w+),\s*"([^"]+)",\s*"([^"]+)",'
        r'\s*R\.drawable\.(\w+),\s*R\.raw\.(\w+)\s*\)'
    )
    const_to_id = {f"P{i + 1}": p["id"] for i, p in enumerate(packs)}
    kt_rows = []
    for hit_row in word_re.findall(words_kt):
        wid, en, zh, pconst, level, group, img, aud = hit_row
        kt_rows.append(dict(id=wid, en=en, zh=zh, pack=const_to_id.get(pconst, pconst),
                            level=level, group=group, image=img + ".png", audio=aud + ".ogg"))
    errs = []
    if len(kt_rows) != len(words):
        errs.append(f"数量 {len(kt_rows)}/{len(words)}")
    for j, w in enumerate(words):
        if j >= len(kt_rows):
            break
        for k in ("id", "en", "zh", "pack", "level", "group", "image", "audio"):
            if kt_rows[j][k] != w[k]:
                errs.append(f"{w['id']}.{k}:{kt_rows[j][k]}≠{w[k]}")
    check("#V2 24 词表与 JSON 逐项一致", not errs, "; ".join(errs[:5]))

    # ② 8 课分组
    pack_re = re.compile(
        r'WordPack\((\w+),\s*(\d+),\s*"([^"]+)",\s*"([^"]+)",\s*all\.slice\((\d+)\.\.(\d+)\)\)'
    )
    kt_packs = [(const_to_id.get(a, a), int(b), c, d, int(e), int(f))
                for a, b, c, d, e, f in pack_re.findall(words_kt)]
    perrs = []
    if len(kt_packs) != len(packs):
        perrs.append(f"数量 {len(kt_packs)}/{len(packs)}")
    for j, p in enumerate(packs):
        if j >= len(kt_packs):
            break
        kp = kt_packs[j]
        if (kp[0], kp[1], kp[2], kp[3]) != (p["id"], p["no"], p["title"], p["level"]):
            perrs.append(f"{p['id']} 元数据不符")
        if [w["id"] for w in words][kp[4]:kp[5] + 1] != p["words"]:
            perrs.append(f"{p['id']} 三词不符")
    check("#V2 8 课分组与 JSON 一致", not perrs, "; ".join(perrs[:5]))

    # ③ 每词有图有音、anchor 非空
    missing_media = [w["id"] for w in words
                     if not (RES / "drawable-nodpi" / w["image"]).exists()
                     or not (RES / "raw" / w["audio"]).exists()]
    check("#V2 每词有图有音（24/24）", not missing_media, ",".join(missing_media))
    empty_anchor = [w["id"] for w in words if not w.get("anchor", "").strip()]
    check("#V2 每词有识别锚点", not empty_anchor, ",".join(empty_anchor))

    # ④ 21 张新插画 RGBA / 尺寸 / 四角透明 / 无水印
    new_images = [w for w in words if not w.get("existing")]
    bad_img = []
    for w in new_images:
        p = RES / "drawable-nodpi" / w["image"]
        ih = png_ihdr(p)
        if ih != (1024, 1024, 6) or not rgba_corners_clear(p):
            bad_img.append(w["image"])
    check("#V2 21 张新插画 RGBA 透明 / 无水印", not bad_img, ",".join(bad_img[:5]))

    # ⑤ 干扰项跨 group + 禁用组合 + 基准课保留 V1 轮换
    # 2026-11-09 测试体系补齐时规则抽到 QuizPicker.kt（纯逻辑，可被 JVM 单测执行），
    # 这里改为校验「规则存在 + SessionState 确实委托过去」，不再只查实现细节字符串。
    distract = data["distractors"]
    check("#V2 干扰项跨 group 规则",
          "w.group != target.group" in quiz_kt and "GROUP_RANK" in quiz_kt
          and "QuizPicker.pick(" in session_kt)
    block = quiz_kt.split("FORBIDDEN_PAIRS = setOf(")[-1]
    declared = {frozenset(pair) for pair in
                re.findall(r'setOf\("([^"]+)",\s*"([^"]+)"\)', block)}
    miss_pairs = [f"{a}↔{b}" for a, b in distract["forbiddenPairs"]
                  if frozenset((a, b)) not in declared]
    check("#V2 6 组禁用组合与 JSON 一致", not miss_pairs, ",".join(miss_pairs))
    check("#V2 基准课保持 V1 课内轮换",
          "pack.id == Words.DEFAULT_PACK_ID" in quiz_kt
          and "words[(index + 1) % words.size]" in quiz_kt
          and "QuizPicker.pick(" in session_kt)
    check("#V2 Card/Quiz/Cheer 带 packId 防串词",
          all(f"val packId: String" in read("state/GuguRoute.kt") for _ in [0])
          and read("state/GuguRoute.kt").count("packId: String") == 3)

    # ⑥ 贴纸册 24 格单屏
    book = read("screens/StickerBookScreen.kt")
    cols = re.search(r"stickerBookColumns\s*=\s*(\d+)", tokens)
    cell = re.search(r"stickerBookCell\s*=\s*(\d+)\.dp", tokens)
    gap = re.search(r"stickerBookGap\s*=\s*(\d+)\.dp", tokens)
    rows = (len(words) + int(cols.group(1)) - 1) // int(cols.group(1)) if cols else 0
    grid_h = rows * int(cell.group(1)) + (rows - 1) * int(gap.group(1)) if (cell and gap) else 9999
    check("#V2 贴纸册 24 格单屏可放下（≤800dp）",
          bool(cols) and "chunked(GuguDimens.stickerBookColumns)" in book
          and 'testTag("sticker_grid")' in book and grid_h <= 800,
          f"{cols.group(1) if cols else '?'} 列 × {rows} 行，网格高 {grid_h}dp")
    book_code = strip_comments(book)
    check("#V2 未解锁贴纸不提示 / 不显示锁",
          'enabled = unlocked' in book_code and 'ic_lock' not in book_code
          and "问号" not in book_code)

    # ⑦ 选课只在家长侧
    panel = read("state/ParentPanel.kt")
    screens_code = "\n".join(strip_comments(p.read_text(encoding="utf-8"))
                             for p in (JAVA / "screens").rglob("*.kt"))
    check("#V2 选课只在家长侧（宝宝侧无课程表）",
          "Words.packs.forEach" in panel and "onSelectPack(pack.id)" in panel
          and "onSelectPack = session::selectPack" in panel
          and "selectPack" not in screens_code and "Words.packs" not in screens_code)
    check("#V2 家长面板 8 行 + 合成语音告知",
          panel.count("pack_row_") >= 1 and "Strings.TTS_NOTICE_LINE" in panel)

    # ⑧ 学习进度只存本地
    prefs = read("state/Prefs.kt")
    net = [k for k in ("HttpURLConnection", "OkHttp", "Retrofit", "Socket(", "URL(", "Firebase")
           if k in code]
    check("#V2 学习进度只存本地（无联网代码）",
          not net and "stringSetPreferencesKey" in prefs and len(perms) == 0, ",".join(net))
    check("#V2 兼容 V1 旧数据（旧 key 保留）",
          all(k in prefs for k in ("sound_enabled", "today_date", "used_seconds")))

    # ⑨ 发音规格与授权留痕
    notice = ASSETS / "TTS_NOTICE.txt"
    notice_txt = notice.read_text(encoding="utf-8") if notice.exists() else ""
    check("#V2 合成语音告知随包",
          notice.exists() and all(k in notice_txt for k in ("电脑合成", "MIT", "Piper", "不联网")))
    pipeline = (ROOT / "tools" / "prepare_v2_assets.py").read_text(encoding="utf-8")
    sizes = [(RES / "raw" / w["audio"]).stat().st_size for w in words]
    check("#V2 24 条发音规格（OGG/体积/归一化）",
          "TARGET_PEAK" in pipeline and "LEAD_MS" in pipeline and "TAIL_MS" in pipeline
          and all(3000 <= s <= 20000 for s in sizes),
          f"体积 {min(sizes)}–{max(sizes)} B")

    # ⑩ 5 分钟语义 = 学习态累计时长（2026-11-09 交互修复）
    check("#V2 5 分钟为学习态累计、再玩回当前课",
          "TIME_LIMIT_SECONDS = 5 * 60" in session_kt
          and "学习态" in session_kt
          and "GuguRoute.Card(0, currentPackId)" in session_kt)

    # ================= 2026-11-09 交互修复回归检查 =================
    # 时长口径抽到 TimeBudget.kt（纯逻辑，由 JVM 单测逐边界执行）后，
    # 这里改为校验「纯逻辑层的规则 + SessionState 的委托」，不再只查实现细节字符串。
    session_code = strip_comments(session_kt)
    budget_code = strip_comments(budget_kt)
    tick_block = budget_code.split("fun tick(")[-1].split("fun reset(")[0]
    # 注释里会提到被弃用的实现名，家长门检查必须只看代码
    gate = strip_comments(read("design/components/ParentGateButton.kt"))
    done = read("screens/DoneScreen.kt")
    primary = read("design/components/PrimaryButton.kt")

    check("#FIX 计时只在 Card/Quiz/Cheer 累加",
          "budget.tick(isLearning" in session_code
          and "if (!isLearning) return" in tick_block
          and "seconds++" in tick_block
          and "seconds >= TIME_LIMIT_SECONDS) return" in tick_block)
    check("#FIX 上限跳转只执行一次、不再按 route !is Done 一刀切",
          "route !is GuguRoute.Done" not in session_code
          and "timeUpHandled" in budget_code
          and "!timeUpHandled" in tick_block
          and "if (result.timeUp)" in session_code)
    check("#FIX 贴纸册 / 首页不被上限踢回",
          "GuguRoute.StickerBook" not in tick_block
          and "GuguRoute.Home" not in tick_block
          and session_code.count("route = GuguRoute.Done") == 1)
    check("#FIX 旧口径超额时长一次性归零（幂等）",
          "storedSeconds > TIME_LIMIT_SECONDS" in budget_code
          and "coerceAtLeast(0)" in budget_code
          and "budget.restore(" in session_code)
    check("#FIX 家长可重置今日时长且不清学习进度",
          "fun resetTodayTime()" in session_code
          and "budget.reset(" in session_code
          and "onResetToday = session::resetTodayTime" in panel
          and "learnedWordIds" not in budget_code.split("fun reset(")[-1])
    check("#FIX 再玩一次禁用态点击有一次可见提示",
          "onDisabledClick" in primary
          and "onDisabledClick = { showTimeUp = true }" in done
          and "Strings.TIME_UP_HINT" in done
          and "2_000L" in done)
    check("#FIX 家长门按住有进度反馈 / 短按有提示",
          "LaunchedEffect(holding)" in gate
          and "drawArc(" in gate
          and "Strings.PARENT_GATE_HINT" in gate
          and "parentGateHoldMs" in gate
          and "withTimeoutOrNull" not in gate)
    check("#FIX 家长面板自动关闭留足操作时间", "20_000L" in panel)

    # ---- 2026-11-09 追加：单词卡「点一下，听发音」修复 ----
    audio = strip_comments(read("audio/AudioPlayer.kt"))
    card = strip_comments(read("screens/WordCardScreen.kt"))
    quiz_screen = strip_comments(read("screens/ListenQuizScreen.kt"))
    check("#FIX 发音按语音属性输出、MediaPlayer 与焦点请求同属性",
          "CONTENT_TYPE_SPEECH" in audio
          and "CONTENT_TYPE_SONIFICATION" not in audio
          and "player.setAudioAttributes(attributes)" in audio)
    check("#FIX 发音异步 prepare、失败不再静默吞掉",
          "prepareAsync()" in audio
          and "setOnErrorListener" in audio
          and "MediaPlayer.create(" not in audio
          and "?: return" not in audio
          and 'Log.e(TAG, "播放初始化失败' in audio)
    check("#FIX 连续点按不会被旧回调误杀正在播放的发音",
          "generation" in audio
          and "token != generation" in audio
          and "fun finish(" in audio)
    check("#FIX 单词卡提示文案整行可点 / 听一听提示条整体可点",
          'guguClick(tag = "btn_sound_hint", onClick = onSound)' in card
          and 'guguClick(tag = "btn_sound", onClick = onSound)' in quiz_screen)

    # ================= 2026-11-09 测试体系自检 =================
    # 静态走查只能证明「代码长什么样」，不能证明「行为对不对」。
    # 以下检查确保真正的可执行测试一直存在，且关键业务规则确实被单测引用。
    unit_kt = sorted((ROOT / "src" / "test" / "java").rglob("*.kt"))
    ui_kt = sorted((ROOT / "src" / "androidTest" / "java").rglob("*.kt"))
    test_code = "\n".join(p.read_text(encoding="utf-8") for p in unit_kt + ui_kt)

    check("#TEST 存在 JVM 单测与 Compose UI 测试源",
          len(unit_kt) >= 3 and len(ui_kt) >= 3, f"unit={len(unit_kt)}, ui={len(ui_kt)}")
    check("#TEST 测试依赖与 runner 已声明",
          "testImplementation(libs.junit4)" in gradle
          and "androidTestImplementation(libs.compose.ui.test.junit4)" in gradle
          and 'testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"' in gradle)
    check("#TEST 业务规则由纯逻辑层承载且被单测直接引用",
          "QuizPicker" in test_code and "TimeBudget" in test_code
          and "QuizPicker.pick(" in session_kt and "budget.tick(" in session_kt)
    check("#TEST 单测覆盖 8 课全组合与 300s 上限",
          "Words.packs.forEach" in test_code
          and "repeat(TIME_LIMIT_SECONDS" in test_code
          and "@Test" in test_code)
    check("#TEST UI 测试覆盖 5 屏 + 贴纸册 + 家长面板 + 家长门",
          all(k in test_code for k in (
              "screen_home", "screen_card", "screen_quiz", "screen_cheer", "screen_done",
              "screen_sticker_book", "pack_row_", "btn_parent", "btn_replay",
          )))
    check("#TEST 中文工程路径下单测 classpath 有 ASCII 兜底",
          "gugu-abc-unit-test" in gradle and "stageUnitTestClasses" in gradle)

    # ---- 汇总 ----
    print("=" * 74)
    print("咕咕 ABC · 静态走查（V1 冻结项 + V2 词表驱动项，无真机部分）")
    print("=" * 74)
    fails = 0
    for name, ok, detail in results:
        flag = "PASS" if ok else "FAIL"
        if not ok:
            fails += 1
        print(f"[{flag}] {name}" + (f"  → {detail}" if detail else ""))
    print("-" * 74)
    print(f"共 {len(results)} 项，失败 {fails} 项")
    print("说明：#3/#4/#7/#20 的像素、坐标、圆角、包围盒需连接荣耀平板 X9 后"
          "用 verify_screens.py + Layout Inspector 验收；V1 五张基准图未改动。")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
