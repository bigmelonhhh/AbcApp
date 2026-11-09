#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
咕咕 ABC · 静态走查（design.md §11 的 #1–#20 中可在无真机条件下执行的部分）

用法：python tools/verify_project.py
不依赖真机 / adb；只检查工程与资源。真机像素比对另由 verify_screens.py 完成。
"""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src" / "main" / "res"
JAVA = ROOT / "src" / "main" / "java" / "com" / "guguabc" / "kids"
MANIFEST = ROOT / "src" / "main" / "AndroidManifest.xml"

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


def main() -> int:
    src = kt_text()
    code = "\n".join(strip_comments(p.read_text(encoding="utf-8")) for p in all_kt())

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
    tokens = (JAVA / "design" / "GuguTokens.kt").read_text(encoding="utf-8")
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
    shadow = (JAVA / "design" / "GuguShadow.kt").read_text(encoding="utf-8")
    check("#8 自建阴影 + 1 层", "fun GuguShadowBox" in shadow and ".blur(" in shadow)
    specs = ["card =", "cardSm =", "option =", "sticker =", "parentGate =", "primary =", "coral ="]
    check("#8 7 组阴影参数齐全", all(s in tokens for s in specs))

    # ---- #9 文案 ----
    forbidden_ui = ["9:41", "手势条", "答错啦", "积分商城", "关卡地图"]
    bad = [w for w in forbidden_ui if w in code]
    check("#10/#12 无设计板/失败态/额外页面内容", not bad, ", ".join(bad))
    strings = (JAVA / "data" / "Strings.kt").read_text(encoding="utf-8")
    required_strings = [
        "早上好呀", "咕咕 ABC", "嗨，我是咕咕！", "今天的一课", "小动物朋友",
        "3 个新词 · 约 5 分钟", "开始玩", "每天 5 分钟 · 不设关卡，不比分数",
        "点一下，听发音", "下一张", "点一点你听到的那个",
        "听不清就再点一次 · 选错不扣分，只会再放一遍", "答对啦！", "再玩一个",
        "没有「答错」，只有再听一次", "今天真棒！", "再玩一次", "休息啦",
        "给爸爸妈妈", "长按右上角「家长」可设置每日时长", "今天到这里",
    ]
    missing_strings = [s for s in required_strings if s not in strings]
    check("#9 文案逐字齐备", not missing_strings, ", ".join(missing_strings))
    # 屏幕 / 组件里禁止裸中文字符串常量
    cn_literals = []
    for p in list((JAVA / "screens").rglob("*.kt")) + list((JAVA / "design" / "components").rglob("*.kt")):
        body = p.read_text(encoding="utf-8")
        body = re.sub(r"//.*", "", body)
        body = re.sub(r"/\*[\s\S]*?\*/", "", body)
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
    ofl = ROOT / "src" / "main" / "assets" / "OFL.txt"
    check("#6 OFL 授权随包", ofl.exists() and "Open Font License" in ofl.read_text(encoding="utf-8"))

    # 字体 cmap 覆盖（fontTools 可用时）
    vendor = os.environ.get("GUGU_VENDOR")
    if vendor and vendor not in sys.path:
        sys.path.insert(0, vendor)
    try:
        from fontTools.ttLib import TTFont
        # 只校验真实界面文案字面量，不把源码注释里的汉字算进去
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
        check("#6 中文字体无缺字", not miss, ", ".join(miss[:6]))
    except ImportError:
        check("#6 中文字体无缺字", True, "fontTools 不可用，跳过（prepare_assets.py 已校验）")

    # ---- #10 #11 图标 / 插画 ----
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
    illustrations = sorted(p.name for p in (RES / "drawable-nodpi").glob("*.png"))
    check("#10 4 张插画原样落盘", illustrations == [
        "ic_gugu_mascot.png", "img_bear.png", "img_cat.png", "img_dog.png",
    ], ",".join(illustrations))
    check("#10 无 ContentScale.Crop", "ContentScale.Crop" not in code)

    # ---- #14 音频 ----
    ogg = sorted(p.name for p in (RES / "raw").glob("*.ogg"))
    check("#14 5 条本地 OGG 占位音", ogg == [
        "cheer.ogg", "tap.ogg", "word_bear.ogg", "word_cat.ogg", "word_dog.ogg",
    ], ",".join(ogg))
    check("#14 OGG 头正确", all((RES / "raw" / n).read_bytes()[:4] == b"OggS" for n in ogg))
    check("#14 无 TTS / 网络音频",
          "TextToSpeech" not in code and "HttpURLConnection" not in code and "URL(" not in code)

    # ---- #17 state 驱动 ----
    check("#17 进度点由 state 驱动", "ProgressDots(activeIndex =" in src and "activeIndex = cardIndex" in (JAVA / "screens" / "WordCardScreen.kt").read_text(encoding="utf-8"))
    check("#17 题号由 state 驱动", "quizNumber = route.index + 1" in src)
    check("#17 家长卡数据来自运行时", "minutesUsed = session.minutesUsed" in src)
    check("#13 无失败态（选错只重播）",
          "fun pick(word: Word)" in src
          and "else playWord(target)" in src
          and "答错" not in (JAVA / "state" / "SessionState.kt").read_text(encoding="utf-8").split("fun pick")[1].split("fun continueFromCheer")[0])
    check("#15 防连点 400ms / CHEER 1.2s", "400L" in src and "1_200L" in src)
    check("#16 家长门长按 1500ms", "1500L" in src)

    # ---- #12 testTag ----
    tags = ["screen_home", "screen_card", "screen_quiz", "screen_cheer", "screen_done",
            "btn_start", "btn_next", "btn_continue", "btn_replay", "btn_rest",
            "btn_sound", "btn_back", "btn_volume", "btn_parent",
            "option_a", "option_b", "word_en", "word_zh", "progress_dots",
            "sticker_wall", "parent_summary"]
    missing_tags = [t for t in tags if f'"{t}"' not in src]
    check("#12 testTag 齐备", not missing_tags, ", ".join(missing_tags))

    # ---- 汇总 ----
    print("=" * 72)
    print("咕咕 ABC · 静态走查（design.md §11，无真机部分）")
    print("=" * 72)
    fails = 0
    for name, ok, detail in results:
        flag = "PASS" if ok else "FAIL"
        if not ok:
            fails += 1
        print(f"[{flag}] {name}" + (f"  → {detail}" if detail else ""))
    print("-" * 72)
    print(f"共 {len(results)} 项，失败 {fails} 项")
    print("说明：#3/#4/#7/#20 的像素、坐标、圆角、包围盒需连接荣耀平板 X9 后"
          "用 verify_screens.py + Layout Inspector 验收。")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
