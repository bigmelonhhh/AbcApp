#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
咕咕 ABC · 资源准备流水线（design.md §8 的可复现实现）

输入：../gugu-abc-android-assets/{icons,illustrations}、Google Fonts 源字体
输出：src/main/res/{drawable,drawable-nodpi,font,raw,mipmap-*}、src/main/assets/OFL.txt

规则：
- 图标：10 个纯 <path> + 2 个含 <circle> 的图标统一转 VectorDrawable；
        confetti 的 <circle>/<rect rx> 按 design.md §8 允许的「路径展开工具转换」
        做数学等价展开（圆弧 A 指令 / 圆角矩形路径），不是手画近似。
        dots_* / status_icons_ref 不入包：进度点由 ProgressDots 运行时绘制，状态栏图标不实现。
- 字体：按 Strings.kt / Words.kt 的全部字符做子集化，并逐字校验 cmap 覆盖。
- 音频：本地合成占位 OGG Vorbis（libsndfile），无外部素材、无网络依赖。
- 启动图标：纯 #FFC857 占位色块，非设计内容。

依赖（可选，缺失时降级）：
  GUGU_VENDOR      fontTools 所在目录（wheel 解包目录）
  GUGU_LIBSNDFILE  libsndfile_x64.dll 路径（缺失时降级为 WAV）
"""
from __future__ import annotations

import ctypes
import math
import os
import re
import shutil
import struct
import sys
import wave
import xml.etree.ElementTree as ET
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT.parent / "gugu-abc-android-assets"
ICONS = ASSETS / "icons"
ILLUSTRATIONS = ASSETS / "illustrations"
RES = ROOT / "src" / "main" / "res"
JAVA = ROOT / "src" / "main" / "java" / "com" / "guguabc" / "kids"
FONT_SRC = Path(os.environ.get("GUGU_FONT_SRC", ROOT / "tools" / "fontsrc"))
OFL_SRC = ROOT / "tools" / "OFL.txt"

# design.md §8：这两个是静态示意 / 参考，不进 APK
EXCLUDED = {"dots_card_1", "dots_quiz_2", "status_icons_ref"}

FONT_URLS = {
    "fredoka_bold.ttf": "https://fonts.gstatic.com/s/fredoka/v17/X7nP4b87HvSqjb_WIi2yDCRwoQ_k7367_B-i2yQag0-mac3OFiXMFg.ttf",
    "noto_sans_sc_regular.ttf": "https://fonts.gstatic.com/s/notosanssc/v41/k3kCo84MPvpLmixcA63oeAL7Iqp5IZJF9bmaG9_FnYw.ttf",
    "noto_sans_sc_medium.ttf": "https://fonts.gstatic.com/s/notosanssc/v41/k3kCo84MPvpLmixcA63oeAL7Iqp5IZJF9bmaG-3FnYw.ttf",
    "noto_sans_sc_bold.ttf": "https://fonts.gstatic.com/s/notosanssc/v41/k3kCo84MPvpLmixcA63oeAL7Iqp5IZJF9bmaGzjCnYw.ttf",
}

FONT_HEADER = """咕咕 ABC · 字体授权与来源
================================
本 APK 打包以下字体，均以 SIL Open Font License 1.1 发布。
字体文件按 design.md §3.5 用 pyftsubset 子集化，仅保留界面文案所需字形，
未修改字体名称、未混淆来源。

  fredoka_bold.ttf          Fredoka Bold (700)
  noto_sans_sc_regular.ttf  Noto Sans SC Regular (400)
  noto_sans_sc_medium.ttf   Noto Sans SC Medium (500)
  noto_sans_sc_bold.ttf     Noto Sans SC Bold (700)

来源：Google Fonts (fonts.google.com / fonts.gstatic.com)
授权：SIL Open Font License, Version 1.1
       https://openfontlicense.org/
下列为 OFL 1.1 授权全文（模板中的 <dates> / <Copyright Holder>
由各字体上游 METADATA.pb 中的 copyright 字段定义，此处保留官方模板原文）。

------------------------------------------------------------
"""


def log(tag: str, msg: str) -> None:
    print(f"[{tag}] {msg}")


# ---------------------------------------------------------------- SVG → VectorDrawable
def parse_matrix(value: str) -> tuple[float, float]:
    m = re.search(r"matrix\(\s*1\s+0\s+0\s+1\s+([-\d.]+)\s+([-\d.]+)\s*\)", value or "")
    return (float(m.group(1)), float(m.group(2))) if m else (0.0, 0.0)


def num(v: float) -> str:
    return f"{v:.6f}".rstrip("0").rstrip(".")


def circle_path(cx: float, cy: float, r: float) -> str:
    # 与 SVG <circle> 完全等价的两段圆弧（Android PathParser 支持 a/A 指令）
    return (
        f"M {num(cx - r)} {num(cy)} "
        f"a {num(r)} {num(r)} 0 1 0 {num(2 * r)} 0 "
        f"a {num(r)} {num(r)} 0 1 0 {num(-2 * r)} 0 Z"
    )


def rect_path(w: float, h: float, rx: float, ry: float) -> str:
    if rx <= 0 or ry <= 0:
        return f"M 0 0 L {num(w)} 0 L {num(w)} {num(h)} L 0 {num(h)} Z"
    return (
        f"M {num(rx)} 0 L {num(w - rx)} 0 "
        f"A {num(rx)} {num(ry)} 0 0 1 {num(w)} {num(ry)} "
        f"L {num(w)} {num(h - ry)} "
        f"A {num(rx)} {num(ry)} 0 0 1 {num(w - rx)} {num(h)} "
        f"L {num(rx)} {num(h)} "
        f"A {num(rx)} {num(ry)} 0 0 1 0 {num(h - ry)} "
        f"L 0 {num(ry)} "
        f"A {num(rx)} {num(ry)} 0 0 1 {num(rx)} 0 Z"
    )


def svg_to_vector(src: Path, dst: Path) -> None:
    root = ET.parse(src).getroot()
    vb = (root.attrib.get("viewBox") or "0 0 36 36").split()
    w, h = float(vb[2]), float(vb[3])
    groups: list[str] = []
    for el in list(root):
        tag = el.tag.split("}")[-1]
        fill = el.attrib.get("fill", "#4A3423")
        # SVG 允许 #RGB 简写；aapt2 的 VectorDrawable 只保险接受 #RRGGBB
        if re.fullmatch(r"#[0-9A-Fa-f]{3}", fill):
            fill = "#" + "".join(c * 2 for c in fill[1:]).upper()
        if fill.lower() in ("none", ""):
            continue  # 仅 status_icons_ref（不实现）存在 stroke-only 元素
        tx, ty = parse_matrix(el.attrib.get("transform", ""))
        if tag == "path":
            d = el.attrib.get("d", "")
        elif tag == "circle":
            cx = float(el.attrib.get("cx", 0))
            cy = float(el.attrib.get("cy", 0))
            r = float(el.attrib.get("r", 0))
            d = circle_path(cx, cy, r)
        elif tag == "rect":
            rw = float(el.attrib.get("width", 0))
            rh = float(el.attrib.get("height", 0))
            rx = float(el.attrib.get("rx", 0))
            ry = float(el.attrib.get("ry", rx))
            d = rect_path(rw, rh, rx, ry)
        else:
            continue
        path = (
            f'        <path android:fillColor="{fill}"\n'
            f'              android:pathData="{d}" />'
        )
        if tx or ty:
            groups.append(
                f'    <group android:translateX="{num(tx)}" android:translateY="{num(ty)}">\n'
                f"{path}\n    </group>"
            )
        else:
            groups.append(path)
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{num(w)}dp" android:height="{num(h)}dp"\n'
        f'    android:viewportWidth="{num(w)}" android:viewportHeight="{num(h)}">\n'
        + "\n".join(groups)
        + "\n</vector>\n"
    )
    dst.write_text(xml, encoding="utf-8")


def build_icons() -> int:
    out = RES / "drawable"
    out.mkdir(parents=True, exist_ok=True)
    count = 0
    for src in sorted(ICONS.glob("*.svg")):
        if src.stem in EXCLUDED:
            log("icon", f"skip (静态示意/参考) {src.name}")
            continue
        dst = out / (src.stem + ".xml")
        svg_to_vector(src, dst)
        count += 1
    log("icon", f"生成 {count} 个 VectorDrawable")
    return count


# ---------------------------------------------------------------- 字体
def collect_text() -> str:
    chars: set[str] = set()
    for p in (JAVA / "data" / "Strings.kt", JAVA / "data" / "Words.kt"):
        if not p.exists():
            continue
        for lit in re.findall(r'"((?:[^"\\]|\\.)*)"', p.read_text(encoding="utf-8")):
            chars.update(lit)
    # 保底：ASCII 可打印字符 + 常见中文标点（子集化只减不增，不影响画面）
    chars.update(chr(c) for c in range(0x20, 0x7F))
    chars.update("·？！，「」、。～／ ")
    return "".join(sorted(chars))


def download_fonts() -> None:
    """源字体缺失时按 FONT_URLS 重新下载到 tools/fontsrc（不入 APK）。"""
    FONT_SRC.mkdir(parents=True, exist_ok=True)
    for name, url in FONT_URLS.items():
        dst = FONT_SRC / name
        if dst.exists() and dst.stat().st_size > 10_000:
            continue
        log("font", f"下载源字体 {name}")
        import subprocess
        subprocess.run(["curl", "-sL", "--max-time", "180", "-o", str(dst), url], check=True)


def subset_fonts(text: str) -> None:
    download_fonts()
    vendor = os.environ.get("GUGU_VENDOR")
    if vendor and vendor not in sys.path:
        sys.path.insert(0, vendor)
    try:
        from fontTools import subset
        from fontTools.ttLib import TTFont
    except ImportError:
        log("font", "fontTools 不可用 → 直接复制源字体（体积较大但可打包）")
        for name in FONT_URLS:
            shutil.copy2(FONT_SRC / name, RES / "font" / name)
        return

    (RES / "font").mkdir(parents=True, exist_ok=True)
    for name in FONT_URLS:
        src = FONT_SRC / name
        dst = RES / "font" / name
        font = TTFont(str(src))
        opts = subset.Options()
        opts.name_IDs = ["*"]
        opts.name_legacy = True
        opts.name_languages = ["*"]
        opts.layout_features = ["*"]
        opts.notdef_outline = True
        opts.recalc_bounds = True
        s = subset.Subsetter(options=opts)
        s.populate(text=text)
        s.subset(font)
        font.save(str(dst))
        log("font", f"{name}: {src.stat().st_size // 1024} KB → {dst.stat().st_size // 1024} KB")

    # 逐字校验（design.md §3.5：缺字触发 fallback 判不合格）
    latin = "".join(chr(c) for c in range(0x20, 0x7F)) + "CatDogBear"
    for name in FONT_URLS:
        required = latin if name.startswith("fredoka") else text
        cmap = TTFont(str(RES / "font" / name)).getBestCmap()
        missing = sorted({c for c in required if ord(c) not in cmap})
        if missing:
            raise RuntimeError(f"{name} 缺字 {''.join(missing)!r}")
    log("font", "全部字体 cmap 覆盖检查通过")


def build_ofl() -> None:
    assets = ROOT / "src" / "main" / "assets"
    assets.mkdir(parents=True, exist_ok=True)
    canonical = OFL_SRC.read_text(encoding="utf-8") if OFL_SRC.exists() else "(OFL 1.1 全文见 https://openfontlicense.org/documents/OFL.txt)"
    (assets / "OFL.txt").write_text(FONT_HEADER + canonical, encoding="utf-8")
    log("ofl", "src/main/assets/OFL.txt 已生成")


# ---------------------------------------------------------------- 占位音频
class SfInfo(ctypes.Structure):
    _fields_ = [
        ("frames", ctypes.c_int64),
        ("samplerate", ctypes.c_int),
        ("channels", ctypes.c_int),
        ("format", ctypes.c_int),
        ("sections", ctypes.c_int),
        ("seekable", ctypes.c_int),
    ]


def make_libsndfile():
    dll = os.environ.get("GUGU_LIBSNDFILE")
    if not dll or not Path(dll).exists():
        return None
    lib = ctypes.CDLL(dll)
    lib.sf_open.argtypes = [ctypes.c_char_p, ctypes.c_int, ctypes.POINTER(SfInfo)]
    lib.sf_open.restype = ctypes.c_void_p
    lib.sf_write_float.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_float), ctypes.c_int64]
    lib.sf_write_float.restype = ctypes.c_int64
    lib.sf_close.argtypes = [ctypes.c_void_p]
    lib.sf_close.restype = ctypes.c_int
    return lib


def tone(freq: float, seconds: float, rate: int = 44100, amp: float = 0.12) -> list[float]:
    n = int(rate * seconds)
    fade = max(1, int(rate * 0.02))
    out = []
    for i in range(n):
        env = min(1.0, i / fade, (n - i) / fade)
        out.append(amp * env * (0.75 * math.sin(2 * math.pi * freq * i / rate)
                               + 0.25 * math.sin(2 * math.pi * freq * 2 * i / rate)))
    return out


def build_audio() -> None:
    raw = RES / "raw"
    raw.mkdir(parents=True, exist_ok=True)
    lib = make_libsndfile()
    # libsndfile 在 Windows 上用 fopen，无法写含中文的路径 → 先在纯 ASCII 临时目录生成再拷贝
    import tempfile
    stage = Path(tempfile.mkdtemp(prefix="gugu_audio_"))
    specs = {
        "word_cat": tone(523.25, 0.65),
        "word_dog": tone(587.33, 0.65),
        "word_bear": tone(659.25, 0.65),
        "cheer": tone(659.25, 0.28) + tone(783.99, 0.34),
        "tap": tone(880.0, 0.08, amp=0.07),
    }
    if lib:
        for name, samples in specs.items():
            info = SfInfo(0, 44100, 1, 0x200000 | 0x0060, 0, 0)  # OGG | VORBIS
            stage_file = str(stage / f"{name}.ogg")
            handle = lib.sf_open(stage_file.encode("ascii"), 0x20, ctypes.byref(info))  # SFM_WRITE
            if not handle:
                raise RuntimeError(f"libsndfile 无法创建 {stage_file}")
            buf = (ctypes.c_float * len(samples))(*samples)
            lib.sf_write_float(handle, buf, len(samples))
            lib.sf_close(handle)
            shutil.copy2(stage_file, raw / f"{name}.ogg")
        shutil.rmtree(stage, ignore_errors=True)
        log("audio", "5 个占位 OGG Vorbis 已生成")
    else:
        log("audio", "libsndfile 不可用 → 降级为 WAV（R.raw 资源名不变）")
        for name, samples in specs.items():
            with wave.open(str(raw / f"{name}.wav"), "wb") as w:
                w.setnchannels(1)
                w.setsampwidth(2)
                w.setframerate(44100)
                w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32767)) for s in samples))
        shutil.rmtree(stage, ignore_errors=True)


# ---------------------------------------------------------------- 插画与启动图标
def build_illustrations() -> None:
    dst_dir = RES / "drawable-nodpi"
    dst_dir.mkdir(parents=True, exist_ok=True)
    for src in sorted(ILLUSTRATIONS.glob("*.png")):
        shutil.copy2(src, dst_dir / src.name)
        log("art", src.name)


def write_png(path: Path, w: int, h: int, pixel) -> None:
    rows = bytearray()
    for y in range(h):
        rows.append(0)
        for x in range(w):
            rows.extend(pixel(x, y, w, h))
    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)  # RGBA
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", ihdr)
        + chunk(b"IDAT", zlib.compress(bytes(rows), 9))
        + chunk(b"IEND", b"")
    )


def build_launcher() -> None:
    # design.md §2：应用图标 V1 用默认占位，不自行设计 → 纯 #FFC857 色块
    for folder, size in [("mipmap-mdpi", 48), ("mipmap-hdpi", 72), ("mipmap-xhdpi", 96),
                         ("mipmap-xxhdpi", 144), ("mipmap-xxxhdpi", 192)]:
        d = RES / folder
        d.mkdir(parents=True, exist_ok=True)
        write_png(d / "ic_launcher.png", size, size, lambda x, y, w, h: (255, 200, 87, 255))
    log("launcher", "5 个密度占位图标已生成")


def main() -> None:
    log("path", f"工程根 {ROOT}")
    build_icons()
    build_illustrations()
    subset_fonts(collect_text())
    build_ofl()
    build_audio()
    build_launcher()
    log("done", "资源准备完成")


if __name__ == "__main__":
    main()
