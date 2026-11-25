"""Generate original synthetic test audio. Requires an ffmpeg executable, no Python packages."""

import argparse
from pathlib import Path
import struct
import subprocess
import zlib


def create_cover(path):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    pixels = b"".join(b"\x00" + bytes((60, 120, 180)) * 128 for _ in range(128))
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", 128, 128, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(pixels))
        + chunk(b"IEND", b"")
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ffmpeg", required=True, help="Local ffmpeg executable")
    parser.add_argument("--output", default="app/build/b1-test-music")
    args = parser.parse_args()
    output = Path(args.output).resolve()
    if "onedrive" in str(output).lower():
        parser.error("Use a local output path outside OneDrive")
    output.mkdir(parents=True, exist_ok=True)
    cover = output / "synthetic-cover.png"
    create_cover(cover)
    cases = [
        ("Music/VinylB1/AlbumA", "Đường về", "Đặng B1", 31, "embedded"),
        ("Music/VinylB1/AlbumB", "Không bìa nhúng", "Nghệ sĩ B1", 31, "sibling"),
        ("Music/VinylB1/AlbumC", "Bìa hỏng", "Nghệ sĩ B1", 31, "broken"),
        ("Music/VinylB1/Short", "Short - excluded", "Test", 10, "none"),
        ("Ringtones/VinylB1", "Ringtone - excluded", "Test", 31, "none"),
        ("Notifications/VinylB1", "Notification - excluded", "Test", 31, "none"),
        ("Alarms/VinylB1", "Alarm - excluded", "Test", 31, "none"),
    ]
    for folder, album, artist, seconds, artwork in cases:
        destination = output / folder
        destination.mkdir(parents=True, exist_ok=True)
        for track in range(1, 3 if seconds == 31 and folder.startswith("Music/") else 2):
            song = destination / f"track-{track}.mp3"
            command = [args.ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-f", "lavfi",
                       "-i", f"sine=frequency={330 + track * 110}:duration={seconds}"]
            if artwork in ("embedded", "broken"):
                command += ["-i", str(cover), "-map", "0:a", "-map", "1:v", "-c:v", "copy", "-disposition:v", "attached_pic"]
            command += ["-c:a", "libmp3lame", "-q:a", "7", "-id3v2_version", "3",
                        "-metadata", f"title={album} {track}", "-metadata", f"album={album}",
                        "-metadata", f"artist={artist}", "-metadata", f"track={track}", str(song)]
            subprocess.run(command, check=True)
            if artwork == "broken":
                data = song.read_bytes()
                assert b"\x89PNG\r\n\x1a\n" in data, "Missing embedded PNG"
                song.write_bytes(data.replace(b"\x89PNG\r\n\x1a\n", b"BROKEN!!", 1))
        if artwork == "sibling":
            subprocess.run([args.ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", str(cover),
                            "-frames:v", "1", str(destination / "cover.jpg")], check=True)
    print(f"Generated 10 original synthetic audio files in {output}")
    print("Expected library: 6 songs / 3 albums; short + ringtone + notification + alarm excluded.")


if __name__ == "__main__":
    main()
