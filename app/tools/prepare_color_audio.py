"""Generate offline color audio using the existing Piper/OGG pipeline.

Set GUGU_PIPER_EXE, GUGU_PIPER_MODEL (Lessac), GUGU_COLOR_ZH_MODEL (Huayan),
and GUGU_LIBSNDFILE. Both voice models must have neighboring .onnx.json files.
"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
from tempfile import TemporaryDirectory

from prepare_v2_assets import (
    RAW, SAMPLE_RATE, find_model, find_piper, normalize_clip, read_wav, write_ogg,
)

COLORS = {
    "red": "红色", "yellow": "黄色", "blue": "蓝色",
    "green": "绿色", "orange": "橙色", "purple": "紫色",
}
MATCH_PROMPTS = {"zh": "找一个颜色一样的。", "en": "Find the same color."}


def synthesize(model: Path, phrase: str, wav: Path) -> list[float]:
    wav.unlink(missing_ok=True)
    subprocess.run(
        [str(find_piper()), "-m", str(model), "-f", str(wav), "--length-scale", "1.15"],
        input=(phrase + "\n").encode("utf-8"), check=True, timeout=90,
    )
    return normalize_clip(read_wav(wav))


def generate(commit: bool) -> None:
    english = find_model()
    mandarin = Path(os.environ["GUGU_COLOR_ZH_MODEL"])
    output = RAW if commit else RAW.parents[3] / "build" / "color-audio"
    with TemporaryDirectory(prefix="gugu_colors_") as temp:
        stage = Path(temp)
        wav = stage / "clip.wav"
        for color, zh in COLORS.items():
            en_clip = synthesize(english, color.capitalize(), wav)
            zh_clip = synthesize(mandarin, zh, wav)
            write_ogg(stage / f"color_{color}_en.ogg", en_clip)
            write_ogg(stage / f"color_{color}_bilingual.ogg", zh_clip + [0.0] * (SAMPLE_RATE // 10) + en_clip)
        for lang, phrase in MATCH_PROMPTS.items():
            clip = synthesize(mandarin if lang == "zh" else english, phrase, wav)
            write_ogg(stage / f"color_match_{lang}.ogg", clip)
        output.mkdir(parents=True, exist_ok=True)
        for ogg in sorted(stage.glob("*.ogg")):
            shutil.copyfile(ogg, output / ogg.name)
            print(ogg.name, ogg.stat().st_size)


if __name__ == "__main__":
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--commit", action="store_true", help="Write the complete set into res/raw")
    generate(cli.parse_args().commit)
