"""咕咕 ABC V2 资源流水线（纯本地，无网络运行时依赖）。

两件事：
1. 发音：Piper TTS 离线把 word_packs.json 的 24 个单词合成为 WAV，
   裁静音 → 归一化 -3dBFS → 前后 120ms 静音 → libsndfile 写 OGG Vorbis。
   运行时 Android 侧只有 MediaPlayer 播 res/raw/*.ogg，不引入任何 TTS 引擎。
2. 插画：把生图模型返回的 RGB 图（浅灰棋盘格底 + 右下角水印）转成
   1024x1024 RGBA 透明底，算法沿用 V1 generated-images/make_alpha.py。

零第三方依赖：PNG 解码/编码、WAV 读写均为标准库，OGG 用 ctypes 调 libsndfile。

用法：
  python tools/prepare_v2_assets.py audio          # 全量 24 条 → tools/v2_build/audio/
  python tools/prepare_v2_assets.py audio --commit # 同上并写入 res/raw/
  python tools/prepare_v2_assets.py image SRC DST  # 单张插画透明化
  python tools/prepare_v2_assets.py verify         # 校验暂存产物

环境变量：
  GUGU_PIPER_EXE    piper.exe 路径
  GUGU_PIPER_MODEL  en_US-lessac-medium.onnx 路径
  GUGU_LIBSNDFILE   libsndfile_x64.dll 路径
"""
from __future__ import annotations

import ctypes
import json
import math
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import wave
import zlib
from collections import deque
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parent
PROJECT = ROOT.parent
PACKS = TOOLS / "word_packs.json"
BUILD = TOOLS / "v2_build"
AUDIO_OUT = BUILD / "audio"
RAW = ROOT / "src" / "main" / "res" / "raw"

SAMPLE_RATE = 22050
LEAD_MS = 120
TAIL_MS = 120
TARGET_PEAK = 10 ** (-3 / 20)     # -3 dBFS
SF_FORMAT_OGG = 0x200000
SF_FORMAT_VORBIS = 0x0060
SFM_WRITE = 0x20


# ------------------------------------------------------------------ 定位工具
def find_piper() -> Path:
    env = os.environ.get("GUGU_PIPER_EXE")
    if env and Path(env).exists():
        return Path(env)
    base = Path(os.environ.get("LOCALAPPDATA", r"C:\Users\63133\AppData\Local"))
    candidates = list(Path(sys.executable).parent.glob("Scripts/piper.exe"))
    candidates += list((base / "Temp/gugu_piper").glob("**/piper.exe"))
    if not candidates:
        raise RuntimeError("找不到 piper.exe，请设置 GUGU_PIPER_EXE")
    return candidates[0]


def find_model() -> Path:
    env = os.environ.get("GUGU_PIPER_MODEL")
    if env and Path(env).exists():
        return Path(env)
    base = Path(os.environ.get("LOCALAPPDATA", r"C:\Users\63133\AppData\Local"))
    p = base / "Temp/gugu_piper/en_US-lessac-medium.onnx"
    if not p.exists():
        raise RuntimeError(
            "找不到 Piper 语音模型。下载："
            "https://hf-mirror.com/rhasspy/piper-voices/resolve/main/en/en_US/"
            "lessac/medium/en_US-lessac-medium.onnx （+ 同名 .onnx.json）"
        )
    return p


def find_sndfile() -> Path:
    env = os.environ.get("GUGU_LIBSNDFILE")
    if env and Path(env).exists():
        return Path(env)
    base = Path(os.environ.get("LOCALAPPDATA", r"C:\Users\63133\AppData\Local"))
    hits = list((base / "Temp").glob("**/libsndfile_x64.dll"))
    if not hits:
        raise RuntimeError("找不到 libsndfile_x64.dll，请设置 GUGU_LIBSNDFILE")
    return hits[0]


# ------------------------------------------------------------------ OGG 写出
class SfInfo(ctypes.Structure):
    _fields_ = [
        ("frames", ctypes.c_int64), ("samplerate", ctypes.c_int),
        ("channels", ctypes.c_int), ("format", ctypes.c_int),
        ("sections", ctypes.c_int), ("seekable", ctypes.c_int),
    ]


def write_ogg(path: Path, samples: list[float]) -> None:
    """libsndfile 在 Windows 用 fopen，写不了中文路径 → 先落 ASCII 临时目录再拷贝。"""
    lib = ctypes.CDLL(str(find_sndfile()))
    lib.sf_open.argtypes = [ctypes.c_char_p, ctypes.c_int, ctypes.POINTER(SfInfo)]
    lib.sf_open.restype = ctypes.c_void_p
    lib.sf_write_float.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_float), ctypes.c_int64]
    lib.sf_write_float.restype = ctypes.c_int64
    lib.sf_close.argtypes = [ctypes.c_void_p]
    lib.sf_close.restype = ctypes.c_int

    stage = Path(tempfile.mkdtemp(prefix="gugu_v2audio_"))
    stage_file = stage / "out.ogg"
    info = SfInfo(0, SAMPLE_RATE, 1, SF_FORMAT_OGG | SF_FORMAT_VORBIS, 0, 0)
    handle = lib.sf_open(str(stage_file).encode("ascii"), SFM_WRITE, ctypes.byref(info))
    if not handle:
        raise RuntimeError("libsndfile 无法创建 OGG")
    buf = (ctypes.c_float * len(samples))(*samples)
    written = lib.sf_write_float(handle, buf, len(samples))
    lib.sf_close(handle)
    if written != len(samples):
        raise RuntimeError(f"OGG 写入不完整：{written}/{len(samples)}")
    path.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(stage_file, path)
    shutil.rmtree(stage, ignore_errors=True)


# ------------------------------------------------------------------ WAV 后处理
def read_wav(path: Path) -> list[float]:
    with wave.open(str(path), "rb") as w:
        if w.getframerate() != SAMPLE_RATE or w.getnchannels() != 1 or w.getsampwidth() != 2:
            raise RuntimeError(f"{path.name} 格式不符：{w.getframerate()}Hz / {w.getnchannels()}ch")
        n = w.getnframes()
        raw = w.readframes(n)
    ints = struct.unpack(f"<{n}h", raw)
    return [v / 32768.0 for v in ints]


def normalize_clip(samples: list[float]) -> list[float]:
    """裁掉首尾静音 → 归一化 -3dBFS → 补 120ms 静音 → 10ms 淡入淡出防爆音。"""
    threshold = 0.01
    first = next((i for i, v in enumerate(samples) if abs(v) > threshold), len(samples))
    last = next((i for i in range(len(samples) - 1, -1, -1) if abs(samples[i]) > threshold), 0)
    if first >= last:
        raise RuntimeError("音频几乎全静音，Piper 生成可能失败")
    body = samples[first:last + 1]
    peak = max(abs(v) for v in body)
    if peak < 1e-6:
        raise RuntimeError("音频峰值过低")
    gain = TARGET_PEAK / peak
    body = [v * gain for v in body]

    fade = int(SAMPLE_RATE * 0.010)
    for i in range(min(fade, len(body))):
        body[i] *= i / fade
        body[-1 - i] *= i / fade

    lead = [0.0] * int(SAMPLE_RATE * LEAD_MS / 1000)
    tail = [0.0] * int(SAMPLE_RATE * TAIL_MS / 1000)
    return lead + body + tail


# ------------------------------------------------------------------ 发音全量
def build_audio(commit: bool = False) -> None:
    data = json.loads(PACKS.read_text(encoding="utf-8"))
    words = data["words"]
    piper, model = find_piper(), find_model()
    stage = Path(tempfile.mkdtemp(prefix="gugu_v2wav_"))
    AUDIO_OUT.mkdir(parents=True, exist_ok=True)
    print(f"Piper : {piper}\nModel : {model}\n音色   : {data['audio']['voice']}  length_scale={data['audio']['lengthScale']}")
    ok = 0
    for w in words:
        wav = stage / f"{w['id']}.wav"
        cmd = [str(piper), "-m", str(model), "-f", str(wav),
               "--length-scale", str(data["audio"]["lengthScale"])]
        r = subprocess.run(cmd, input=w["ttsText"] + "\n", text=True,
                           capture_output=True, timeout=120)
        if r.returncode != 0 or not wav.exists():
            print(f"  [FAIL] {w['id']}: {r.stderr[-200:]}")
            continue
        samples = normalize_clip(read_wav(wav))
        ogg = AUDIO_OUT / w["audio"]
        write_ogg(ogg, samples)
        dur = (len(samples) - (LEAD_MS + TAIL_MS) * SAMPLE_RATE // 1000) / SAMPLE_RATE
        print(f"  {w['id']:<10} {w['ttsText']:<10} {dur:.2f}s  {ogg.stat().st_size:>7} B")
        ok += 1
    shutil.rmtree(stage, ignore_errors=True)
    print(f"完成 {ok}/{len(words)} 条 → {AUDIO_OUT}")
    if commit and ok == len(words):
        RAW.mkdir(parents=True, exist_ok=True)
        for w in words:
            shutil.copy2(AUDIO_OUT / w["audio"], RAW / w["audio"])
        print(f"已写入 {RAW}（V1 的 word_cat/dog/bear.ogg 占位音被重制覆盖）")
    elif commit:
        print("存在失败条目，未写入 res/raw/")


# ------------------------------------------------------------------ PNG 透明化
def read_chunks(path: Path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    pos, out = 8, []
    while pos < len(data):
        n = struct.unpack(">I", data[pos:pos + 4])[0]
        typ = data[pos + 4:pos + 8]
        out.append((typ, data[pos + 8:pos + 8 + n]))
        pos += 12 + n
    return out


def paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    return a if pa <= pb and pa <= pc else (b if pb <= pc else c)


def decode_rgb(chunks):
    w = h = None
    idat = []
    for typ, payload in chunks:
        if typ == b"IHDR":
            w, h, depth, ctype = struct.unpack(">IIBB", payload[:10])
            assert depth == 8 and ctype in (2, 6), f"不支持的 PNG 类型 {ctype}"
        elif typ == b"IDAT":
            idat.append(payload)
    raw = zlib.decompress(b"".join(idat))
    bpp = 3
    stride = w * bpp
    rows, prev, pos = [], bytearray(stride), 0
    for _ in range(h):
        ft = raw[pos]
        pos += 1
        line = bytearray(raw[pos:pos + stride])
        pos += stride
        if ft:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x - bpp] if x >= bpp else 0
                if ft == 1:
                    line[x] = (line[x] + a) & 255
                elif ft == 2:
                    line[x] = (line[x] + b) & 255
                elif ft == 3:
                    line[x] = (line[x] + ((a + b) // 2)) & 255
                elif ft == 4:
                    line[x] = (line[x] + paeth(a, b, c)) & 255
        rows.append(line)
        prev = line
    return w, h, rows


def is_bg(r, g, b) -> bool:
    # 生图模型把浅灰棋盘格 + 白底烘焙进 RGB；从画布边缘 flood-fill 只清连通背景，
    # 主体内部的眼白 / 奶油色肚子因不连通而保留。
    return min(r, g, b) >= 205 and max(r, g, b) - min(r, g, b) <= 42


def chunk(typ, payload):
    return (struct.pack(">I", len(payload)) + typ + payload
            + struct.pack(">I", zlib.crc32(typ + payload) & 0xFFFFFFFF))


def write_rgba(path: Path, w: int, h: int, rgba: bytearray) -> None:
    raw = bytearray()
    stride = w * 4
    for y in range(h):
        raw.append(0)
        raw.extend(rgba[y * stride:(y + 1) * stride])
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
           + chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def process_image(src: Path, dst: Path) -> None:
    w, h, rows = decode_rgb(read_chunks(src))
    assert (w, h) == (1024, 1024), f"尺寸不符 {(w, h)}"
    rgba = bytearray(w * h * 4)
    for y in range(h):
        line = rows[y]
        for x in range(w):
            i = (y * w + x) * 4
            rgba[i:i + 3] = line[x * 3:x * 3 + 3]
            rgba[i + 3] = 255

    q, seen = deque(), bytearray(w * h)

    def mark(x, y):
        idx = y * w + x
        if seen[idx]:
            return
        i = idx * 4
        if not is_bg(rgba[i], rgba[i + 1], rgba[i + 2]):
            return
        seen[idx] = 1
        q.append((x, y))

    for x in range(w):
        mark(x, 0)
        mark(x, h - 1)
    for y in range(h):
        mark(0, y)
        mark(w - 1, y)
    while q:
        x, y = q.popleft()
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if 0 <= nx < w and 0 <= ny < h:
                mark(nx, ny)
    for idx in range(w * h):
        if seen[idx]:
            rgba[idx * 4 + 3] = 0

    # 右下角水印区域（生图模型固定位置），仅在空白背景上，不触碰主体
    for y in range(int(h * 0.91), h):
        for x in range(int(w * 0.85), w):
            rgba[(y * w + x) * 4 + 3] = 0

    write_rgba(dst, w, h, rgba)
    transparent = sum(1 for i in range(w * h) if rgba[i * 4 + 3] == 0)
    print(f"{dst.name}: {w}x{h} RGBA, 透明像素 {transparent} ({transparent / (w * h) * 100:.1f}%)")


# ------------------------------------------------------------------ 校验
def verify() -> None:
    data = json.loads(PACKS.read_text(encoding="utf-8"))
    missing = [w["audio"] for w in data["words"] if not (AUDIO_OUT / w["audio"]).exists()]
    bad_header = [w["audio"] for w in data["words"]
                  if (AUDIO_OUT / w["audio"]).exists()
                  and (AUDIO_OUT / w["audio"]).read_bytes()[:4] != b"OggS"]
    print(f"音频 {len(data['words']) - len(missing)}/{len(data['words'])} 条")
    print("缺失:", missing or "无")
    print("OGG 头异常:", bad_header or "无")
    if missing or bad_header:
        sys.exit(1)


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "verify"
    if cmd == "audio":
        build_audio(commit="--commit" in sys.argv)
    elif cmd == "image":
        process_image(Path(sys.argv[2]), Path(sys.argv[3]))
    elif cmd == "verify":
        verify()
    else:
        print(__doc__)
        sys.exit(2)
