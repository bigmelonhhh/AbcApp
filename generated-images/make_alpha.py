"""Turn the generated RGB storybook illustrations into transparent PNGs.

The image model returned RGB (no alpha) with a faint off-white paper grid as
background. We flood-fill from the canvas edges over neutral near-white pixels
so the characters stay intact, then drop a small watermark area in the corner.
Pure stdlib, no Pillow needed.
"""
import glob
import os
import struct
import zlib
from collections import deque

DIR = os.path.dirname(os.path.abspath(__file__))
NAMES = ["gugu-mascot", "gugu-cat", "gugu-dog", "gugu-bear"]


def read_chunks(path):
    data = open(path, "rb").read()
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
            assert depth == 8 and ctype == 2
        elif typ == b"IDAT":
            idat.append(payload)
    raw = zlib.decompress(b"".join(idat))
    stride, bpp = w * 3, 3
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


def is_bg(r, g, b):
    # The generator baked a faint white/grey checkerboard into the RGB canvas.
    # Keep the threshold loose so the darker checker tiles don't wall off the
    # lighter ones; connectivity from the canvas edge still protects enclosed
    # light areas (eggshell hat, eye whites, cream muzzles).
    return min(r, g, b) >= 205 and max(r, g, b) - min(r, g, b) <= 42


def chunk(typ, payload):
    return (struct.pack(">I", len(payload)) + typ + payload
            + struct.pack(">I", zlib.crc32(typ + payload) & 0xFFFFFFFF))


def write_rgba(path, w, h, rgba):
    raw = bytearray()
    stride = w * 4
    for y in range(h):
        raw.append(0)
        raw.extend(rgba[y * stride:(y + 1) * stride])
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
           + chunk(b"IEND", b""))
    open(path, "wb").write(png)


def process(src, dst):
    w, h, rows = decode_rgb(read_chunks(src))
    rgba = bytearray(w * h * 4)
    for y in range(h):
        line = rows[y]
        for x in range(w):
            i = (y * w + x) * 4
            rgba[i:i + 3] = line[x * 3:x * 3 + 3]
            rgba[i + 3] = 255

    q = deque()
    seen = bytearray(w * h)

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
        if x > 0:
            mark(x - 1, y)
        if x < w - 1:
            mark(x + 1, y)
        if y > 0:
            mark(x, y - 1)
        if y < h - 1:
            mark(x, y + 1)

    for idx in range(w * h):
        if seen[idx]:
            rgba[idx * 4 + 3] = 0

    # Model watermark sits in the bottom-right corner, on empty background.
    for y in range(int(h * 0.91), h):
        for x in range(int(w * 0.85), w):
            rgba[(y * w + x) * 4 + 3] = 0

    write_rgba(dst, w, h, rgba)
    print(os.path.basename(dst), w, h, "transparent pixels:", sum(seen))


if __name__ == "__main__":
    srcs = sorted(glob.glob(os.path.join(DIR, "Flat_hand_drawn_*.png")))
    for name, src in zip(NAMES, srcs):
        process(src, os.path.join(DIR, name + ".png"))
