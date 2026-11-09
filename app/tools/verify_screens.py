#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
咕咕 ABC · 真机像素比对（design.md §11 #20）

前置：
  1) 荣耀平板 X9 USB 连接并开启 USB 调试，adb devices 能看到设备
  2) adb install -r build/outputs/apk/debug/GuguABC-debug.apk
  3) 本机 adb 在 PATH 中（已验证 35.0.2 可用）

用法：
  python tools/verify_screens.py capture   # 按状态机走通 5 屏并逐屏截图
  python tools/verify_screens.py compare   # 与 *_clean.png 统一到 1280×800 后比对
  python tools/verify_screens.py all       # 两者都做

比对方法（design.md §11）：
  基准 *_clean.png 2560×1600 ÷2 → 1280×800
  真机截图 1920×1200 ÷1.5 → 1280×800
  排除区：顶部 0–36dp（假状态栏）、底部 16dp（手势条）、四角 32dp（画布外框圆角）
不依赖 PIL / numpy，纯标准库解析 PNG（支持 palette / RGB / RGBA、8bit、非隔行）。
"""
from __future__ import annotations

import re
import struct
import subprocess
import sys
import time
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASELINE = ROOT.parent / "gugu-abc-android-assets" / "screens"
OUT = ROOT / "tools" / "screenshots"
PKG = "com.guguabc.kids"

# 状态机路径（design.md §7.1）。每组：先截图，再点击的 testTag。
# ③ 目标词左右轮换：quiz0 选 option_a，quiz1 选 option_b，quiz2 选 option_a。
STEPS = [
    ("screen_01_home_clean.png", "btn_start"),
    ("screen_02_card_clean.png", "btn_next"),          # Cat → Dog
    (None, "btn_next"),                                 # Dog → Bear
    (None, "btn_next"),                                 # Bear → Quiz(0)
    ("screen_03_quiz_clean.png", "option_a"),           # Quiz(0) 正确 = Cat
    ("screen_04_cheer_clean.png", "btn_continue"),
    (None, "option_b"),                                 # Quiz(1) 正确 = Dog
    (None, "btn_continue"),
    (None, "option_a"),                                 # Quiz(2) 正确 = Bear
    (None, "btn_continue"),                             # → DONE
    ("screen_05_done_clean.png", None),
]

SCREEN_W, SCREEN_H = 1280, 800


def adb(*args: str, capture: bool = False):
    cmd = ["adb", *args]
    if capture:
        return subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def device_size() -> tuple[int, int] | None:
    r = adb("shell", "wm", "size", capture=True)
    m = re.search(r"(\d+)x(\d+)", r.stdout.decode("utf-8", "ignore"))
    return (int(m.group(1)), int(m.group(2))) if m else None


def node_center(tag: str) -> tuple[int, int] | None:
    """Compose Modifier.testTag → UIAutomator 的 resource-id。"""
    adb("shell", "uiautomator", "dump", "/sdcard/gugu_dump.xml")
    r = adb("exec-out", "cat", "/sdcard/gugu_dump.xml", capture=True)
    xml = r.stdout.decode("utf-8", "ignore")
    m = re.search(r'resource-id="[^"]*' + re.escape(tag) + r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        # 属性顺序可能不同
        for node in re.findall(r"<node[^>]+>", xml):
            if re.search(r'resource-id="[^"]*' + re.escape(tag) + r'"', node):
                b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
                if b:
                    x1, y1, x2, y2 = map(int, b.groups())
                    return ((x1 + x2) // 2, (y1 + y2) // 2)
        return None
    x1, y1, x2, y2 = map(int, m.groups())
    return ((x1 + x2) // 2, (y1 + y2) // 2)


def tap(tag: str) -> bool:
    c = node_center(tag)
    if not c:
        print(f"  !! 未找到 testTag={tag}，请确认 App 在前台且系统栏已隐藏")
        return False
    adb("shell", "input", "tap", str(c[0]), str(c[1]))
    return True


def capture() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    devices = adb("devices", capture=True).stdout.decode("utf-8", "ignore")
    if not re.search(r"\n[^\n]+\tdevice\r?\n?", devices):
        print("没有检测到已连接的设备。请先 USB 连接荣耀平板 X9 并开启 USB 调试。")
        return 1
    size = device_size()
    print(f"设备分辨率：{size}（期望 1920x1200 横屏）")

    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-a", "android.intent.action.MAIN",
        "-c", "android.intent.category.LAUNCHER", "-n", f"{PKG}/.MainActivity")
    time.sleep(2.0)

    for i, (baseline, tag) in enumerate(STEPS):
        if baseline:
            path = OUT / baseline.replace("_clean", "")
            r = adb("exec-out", "screencap", "-p", capture=True)
            if not r.stdout.startswith(b"\x89PNG"):
                err = r.stderr.decode("utf-8", "ignore").strip()
                print(f"  !! 截图失败：{err[:200] or '未返回 PNG 数据'}")
                return 1
            path.write_bytes(r.stdout)
            print(f"  截图 {path.name}  ({len(r.stdout)} bytes)")
        if tag:
            # CHEER 的按钮有 1.2s 延迟可点
            if tag == "btn_continue":
                time.sleep(1.5)
            if not tap(tag):
                return 1
            time.sleep(0.9)
    print("5 屏截图完成 →", OUT)
    return 0


# ------------------------------------------------------------------ PNG 解码
def paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    return a if pa <= pb and pa <= pc else (b if pb <= pc else c)


def read_png(path: Path):
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"不是 PNG: {path}")
    pos, idat, plte, w, h, ct = 8, bytearray(), b"", 0, 0, 0
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if tag == b"IHDR":
            w, h, bd, ct, comp, filt, inter = struct.unpack(">IIBBBBB", chunk)
            if bd != 8 or inter != 0 or ct not in (2, 3, 6):
                raise ValueError(f"不支持的 PNG 格式: bitdepth={bd} colortype={ct} interlace={inter}")
        elif tag == b"PLTE":
            plte = chunk
        elif tag == b"IDAT":
            idat += chunk
        elif tag == b"IEND":
            break
    bpp = {2: 3, 3: 1, 6: 4}[ct]
    stride = w * bpp
    raw = zlib.decompress(bytes(idat))
    rows: list[bytearray] = []
    prev = bytearray(stride)
    i = 0
    for _ in range(h):
        f = raw[i]; i += 1
        line = bytearray(raw[i:i + stride]); i += stride
        if f == 1:
            for x in range(bpp, stride):
                line[x] = (line[x] + line[x - bpp]) & 255
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + ((a + prev[x]) // 2)) & 255
        elif f == 4:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                c = prev[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + paeth(a, prev[x], c)) & 255
        elif f != 0:
            raise ValueError(f"未知 PNG filter {f}")
        rows.append(line); prev = line

    # 保留原始扫描线（bytearray），避免把 2560×1600 展开成数百万个 Python tuple
    return w, h, ct, plte, rows


def pixel(line: bytearray, x: int, ct: int, plte: bytes) -> tuple[int, int, int]:
    if ct == 3:
        i = line[x]
        return plte[i * 3], plte[i * 3 + 1], plte[i * 3 + 2]
    if ct == 2:
        return line[x * 3], line[x * 3 + 1], line[x * 3 + 2]
    return line[x * 4], line[x * 4 + 1], line[x * 4 + 2]


def excluded(x: int, y: int) -> bool:
    # 顶部假状态栏 36dp、底部手势条 16dp、四角 32dp 圆角
    if y < 36 or y >= SCREEN_H - 16:
        return True
    if x < 32 and y < 32:
        return True
    if x > SCREEN_W - 32 and y < 32:
        return True
    if x < 32 and y > SCREEN_H - 32:
        return True
    if x > SCREEN_W - 32 and y > SCREEN_H - 32:
        return True
    return False


def compare() -> int:
    lines = ["咕咕 ABC · 真机像素比对报告", "=" * 64]
    failures = 0
    for baseline, _ in STEPS:
        if not baseline:
            continue
        shot = OUT / baseline.replace("_clean", "")
        ref = BASELINE / baseline
        if not shot.exists():
            lines.append(f"[SKIP] {baseline}：缺少截图 {shot}")
            continue
        if not ref.exists():
            lines.append(f"[SKIP] {baseline}：缺少基准图 {ref}")
            continue
        rw, rh, rct, rplte, rrows = read_png(ref)
        dw, dh, dct, dplte, drows = read_png(shot)
        total = diff_sum = bad = 0
        minx, miny, maxx, maxy = SCREEN_W, SCREEN_H, 0, 0
        for y in range(SCREEN_H):
            if y < 36 or y >= SCREEN_H - 16:
                continue
            rline = rrows[min(rh - 1, y * rh // SCREEN_H)]
            dline = drows[min(dh - 1, y * dh // SCREEN_H)]
            for x in range(SCREEN_W):
                if excluded(x, y):
                    continue
                a = pixel(rline, min(rw - 1, x * rw // SCREEN_W), rct, rplte)
                b = pixel(dline, min(dw - 1, x * dw // SCREEN_W), dct, dplte)
                d = abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])
                total += 1
                diff_sum += d
                if d > 24:
                    bad += 1
                    minx, miny = min(minx, x), min(miny, y)
                    maxx, maxy = max(maxx, x), max(maxy, y)
        mean = diff_sum / max(1, total) / 3
        ratio = bad / max(1, total)
        lines.append(
            f"{baseline}\n"
            f"  基准 {rw}x{rh}  |  真机 {dw}x{dh}  |  统一采样到 {SCREEN_W}x{SCREEN_H}\n"
            f"  平均通道差 {mean:.2f}/255  |  明显差异像素 {ratio * 100:.2f}%"
            + (f"  |  差异包围盒 ({minx},{miny})–({maxx},{maxy})" if bad else "")
        )
        if ratio > 0.15:
            failures += 1
            lines.append("  → 差异偏大，请按 design.md §11 #3/#4/#7 逐项检查")
    lines.append("-" * 64)
    lines.append(f"共比对 5 屏，明显偏差屏数 {failures}")
    lines.append("提示：阴影模糊、字体行高、PNG 插画缩放会产生正常小差异；"
                 "重点看主要元素包围盒与文字基线。")
    report = "\n".join(lines)
    print(report)
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "compare_report.txt").write_text(report, encoding="utf-8")
    return 1 if failures else 0


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "all"
    rc = 0
    if mode in ("capture", "all"):
        rc |= capture()
    if mode in ("compare", "all"):
        rc |= compare()
    sys.exit(rc)
