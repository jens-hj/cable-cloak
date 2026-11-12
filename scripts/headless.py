#!/usr/bin/env python3
"""Run a command (e.g. ./gradlew runClientGameTest) on an isolated Xvfb display with software OpenGL.

Usage: nix develop -c python3 scripts/headless.py ./gradlew runClientGameTest -PshaderPack=/path/pack.zip
"""
import os
import shutil
import subprocess
import sys

xvfb = shutil.which("Xvfb")
if xvfb is None:
    raise SystemExit("Xvfb not found; run inside nix develop")
if len(sys.argv) < 2:
    raise SystemExit(__doc__)
display = subprocess.Popen([xvfb, "-displayfd", "1", "-screen", "0", "1920x1080x24", "-nolisten", "tcp"],
                           stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True)
try:
    number = display.stdout.readline().strip()
    if not number.isdigit():
        raise SystemExit("could not start Xvfb")
    env = os.environ.copy()
    env["DISPLAY"] = ":" + number
    env.pop("WAYLAND_DISPLAY", None)
    # Keep the Gradle daemon out of it so the game inherits this display.
    sys.exit(subprocess.run(sys.argv[1:] + ["--no-daemon"], env=env).returncode)
finally:
    display.terminate()
    display.wait(timeout=10)
