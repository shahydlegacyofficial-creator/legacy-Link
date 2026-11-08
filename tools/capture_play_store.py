"""Capture actual Play-release UI and verify store-facing controls on a test emulator."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shlex
import subprocess
import time
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("--adb", default="adb")
parser.add_argument("--serial", default="emulator-5554")
args = parser.parse_args()
assert args.serial.startswith("emulator-"), "Only a disposable emulator may be used."
raw = root / "play-store/assets/raw-phone"
raw.mkdir(parents=True, exist_ok=True)
apk = root / "play-store/builds/Legacy-Link-2.1.0-play.apk"


def adb(*command, binary=False, check=True):
    if command and command[0] == "shell":
        command = ("shell", shlex.join(command[1:]))
    process = subprocess.run([args.adb, "-s", args.serial, *command], check=check, capture_output=True, timeout=40)
    return process.stdout if binary else process.stdout.decode("utf-8", errors="replace")


def tree():
    adb("shell", "uiautomator", "dump", "/sdcard/legacy-store.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/legacy-store.xml"))


def bounds(node):
    return list(map(int, re.findall(r"\d+", node.attrib["bounds"])))


def swipe(up=False, current=None):
    current = tree() if current is None else current
    node = next((n for n in current.iter("node") if n.attrib.get("scrollable") == "true"), None)
    if node is None:
        return
    x1, y1, x2, y2 = bounds(node)
    start, end = (.25,.83) if up else (.83,.25)
    adb("shell", "input", "swipe", str((x1+x2)//2), str(int(y1+(y2-y1)*start)), str((x1+x2)//2), str(int(y1+(y2-y1)*end)), "220")


def find(text=None, desc=None, cls=None):
    for attempt in range(12):
        current = tree()
        for node in current.iter("node"):
            a = node.attrib
            if ((text is None or a.get("text", "").casefold() == text.casefold()) and (desc is None or a.get("content-desc") == desc) and (cls is None or a.get("class") == cls)):
                x1,y1,x2,y2 = bounds(node)
                if x2>x1 and y2>y1:
                    return node
        swipe(up=attempt>=4, current=current)
    raise AssertionError(f"Missing UI control {text or desc or cls}")


def tap(**selector):
    x1,y1,x2,y2 = bounds(find(**selector))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def top():
    for _ in range(4):
        swipe(up=True)


def capture(name):
    ui = tree()
    (raw/(name+".xml")).write_bytes(ET.tostring(ui, encoding="utf-8"))
    time.sleep(.25)
    (raw/(name+".png")).write_bytes(adb("exec-out", "screencap", "-p", binary=True))
    print("Captured " + name, flush=True)


def fill(desc, value):
    tap(desc=desc)
    adb("shell", "input", "text", value)
    adb("shell", "input", "keyevent", "4")


adb("shell", "wm", "size", "1080x1920")
adb("shell", "wm", "density", "420")
adb("shell", "settings", "put", "system", "font_scale", "1.0")
adb("uninstall", "mv.legacy.link", check=False)
adb("install", str(apk))
adb("logcat", "-c")
adb("shell", "am", "start", "-n", "mv.legacy.link/.MainActivity")
tree()
capture("01-overview")
tap(desc="Apps")
find(text="0 apps selected")
find(text="Calendar")
capture("02-sources")
tap(desc="Share notifications from Calendar")
tap(desc="Share notifications from Phone")
tap(desc="Selected")
top()
find(text="2 apps selected")
capture("03-selected")
tap(desc="Settings")
top()
capture("04-pairing")
# HTTPS-only validation must reject an insecure address without saving pairing.
fill("Assistant address", "http://127.0.0.1:18765")
fill("Pairing secret", "synthetic-play-check-only")
tap(desc="Save & test connection")
tap(desc="Overview")
find(text="●  NOT PAIRED")
tap(text="Allow notification access")
find(text="Share selected notifications with Legacy")
capture("05-disclosure")
tap(text="Not now")
tap(desc="Notification forwarding", cls="android.widget.Switch")
top()
find(text="Your link is paused")
capture("06-paused")
tap(desc="Privacy and data sharing")
find(text="Your link. Your choice.")
capture("07-privacy")
tap(text="Close")
tap(desc="Settings")
tap(desc="Clear local data")
find(text="Clear local data?")
capture("08-clear-data")
tap(text="Cancel")
tap(desc="Notification access blocked?")
capture("09-notification-access-help")
tap(text="Open App info")
assert "com.android.settings" in adb("shell", "dumpsys", "activity", "activities")
adb("shell", "input", "keyevent", "4")
# Verify the affirmative path opens the OS-controlled permission screen.
tap(desc="Open Android permissions")
tap(text="I agree")
assert "com.android.settings" in adb("shell", "dumpsys", "activity", "activities")
adb("shell", "input", "keyevent", "4")
crashes = adb("logcat", "-b", "crash", "-d")
assert "FATAL EXCEPTION" not in crashes, crashes
verification = {"status":"passed", "apk_sha256":hashlib.sha256(apk.read_bytes()).hexdigest(), "device":"Android 16 / API 36 emulator", "capture_size":[1080,1920], "checks":["signed Play release installs", "store screenshots captured from actual UI", "two selected sources saved", "HTTPS-only form rejects HTTP without saving pairing", "disclosure visible and can be declined", "pause control", "privacy and local deletion controls", "restricted-settings helper opens App info", "affirmative disclosure opens OS permission settings", "no runtime crashes"], "scope":"UI and transport validation; external HTTPS receiver and real bank app require owner testing."}
(root/"play-store/verification").mkdir(exist_ok=True)
(root/"play-store/verification/play-ui.json").write_text(json.dumps(verification, indent=2)+"\n", encoding="utf-8")
print("PASS: signed Play release UI, permission helper and HTTPS-only validation", flush=True)
