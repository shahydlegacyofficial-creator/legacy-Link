"""Exercise the APK on an emulator with synthetic notifications and a local receiver.

Usage: python tools/emulator_smoke.py --adb /path/to/adb --apk /path/to/app-debug.apk
The specified emulator's Legacy Link data is reset. Never target a personal phone.
"""
import argparse
import hashlib
import json
import re
import shlex
import subprocess
import threading
import time
import xml.etree.ElementTree as ET
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--adb", default="adb")
parser.add_argument("--serial", default="emulator-5554")
parser.add_argument("--apk", required=True)
parser.add_argument("--output", default="design/qa")
parser.add_argument("--screens-only", action="store_true")
args = parser.parse_args()
assert args.serial.startswith("emulator-"), "This check must only run on an emulator."
out = Path(args.output)
out.mkdir(parents=True, exist_ok=True)
events = []
mode = {"online": True}
installed_apk_sha256 = hashlib.sha256(Path(args.apk).read_bytes()).hexdigest()


class Receiver(BaseHTTPRequestHandler):
    def log_message(self, *unused):
        pass

    def do_GET(self):
        self.send_response(200 if mode["online"] and self.path == "/phone-ping" else 503)
        self.end_headers()

    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        code = 503 if not mode["online"] else 200 if self.headers.get("X-Legacy-Secret") == "legacy-ui-test-only" else 401
        if code == 200:
            events.append(json.loads(body))
        self.send_response(code)
        self.end_headers()


def adb(*command, binary=False):
    if command and command[0] == "shell":
        command = ("shell", shlex.join(command[1:]))
    result = subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True, check=True)
    return result.stdout if binary else result.stdout.decode("utf-8", errors="replace").strip()


def tree():
    adb("shell", "uiautomator", "dump", "/sdcard/legacy-qa.xml")
    raw = adb("exec-out", "cat", "/sdcard/legacy-qa.xml")
    return ET.fromstring(raw[raw.index("<?xml"):])


def bounds(node):
    return list(map(int, re.findall(r"\d+", node.attrib["bounds"])))


def scroll(up=False):
    current = tree()
    nodes = [n for n in current.iter("node") if n.attrib.get("scrollable") == "true"]
    assert nodes, "No scrollable content found"
    x1, y1, x2, y2 = bounds(nodes[0])
    start, end = (.25, .82) if up else (.82, .25)
    adb("shell", "input", "swipe", str((x1+x2)//2), str(y1+int((y2-y1)*start)), str((x1+x2)//2), str(y1+int((y2-y1)*end)), "260")


def find(text=None, desc=None, cls=None, allow_scroll=True):
    for attempt in range(9 if allow_scroll else 1):
        for node in tree().iter("node"):
            a = node.attrib
            if ((text is None or a.get("text", "").casefold() == text.casefold()) and (desc is None or a.get("content-desc") == desc)
                    and (cls is None or a.get("class") == cls)):
                x1, y1, x2, y2 = bounds(node)
                if x2 > x1 and y2 > y1:
                    return node
        if allow_scroll:
            scroll(up=attempt >= 3)
    raise AssertionError(f"Missing UI node: text={text}, desc={desc}, class={cls}")


def tap(**selectors):
    x1, y1, x2, y2 = bounds(find(**selectors))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def fill(desc, value):
    tap(desc=desc)
    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
    adb("shell", "input", "keyevent", "--longpress", "KEYCODE_DEL")
    # Tests only enter fresh fields; no clipboard or real credentials are used.
    adb("shell", "input", "text", value)
    adb("shell", "input", "keyevent", "4")


def capture(name):
    (out / f"{name}.xml").write_bytes(ET.tostring(tree(), encoding="utf-8"))
    time.sleep(.3)
    (out / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p", binary=True))


def wait_for(predicate, message, seconds=15):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if predicate():
            return
        time.sleep(.5)
    raise AssertionError(message)


def notify(tag, body):
    adb("shell", "cmd", "notification", "post", "-t", "Synthetic banking notification", tag, body)


server = ThreadingHTTPServer(("127.0.0.1", 18765), Receiver)
threading.Thread(target=server.serve_forever, daemon=True).start()
try:
    print("Installing and opening clean APK", flush=True)
    adb("install", "-r", args.apk)
    adb("shell", "pm", "clear", "mv.legacy.link")
    adb("reverse", "tcp:18765", "tcp:18765")
    adb("logcat", "-c")
    adb("shell", "am", "start", "-n", "mv.legacy.link/.MainActivity")
    find(text="Connect your Legacy assistant")
    capture("01-overview")
    tap(desc="Apps")
    find(text="0 apps selected")
    capture("02-apps")
    fill("Search apps", "camera")
    find(text="Camera")
    capture("03-search")
    tap(desc="Settings")
    capture("04-settings")
    if args.screens_only:
        print("PASS: clean overview, sources, search and connection screenshots", flush=True)
        raise SystemExit(0)
    assert find(desc="Pairing secret").attrib.get("password") == "true", "Secret must be masked"
    tap(desc="Scan pairing QR")
    current = tree()
    for node in current.iter("node"):
        if node.attrib.get("text") in ("While using the app", "Only this time"):
            x1, y1, x2, y2 = bounds(node)
            adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
            break
    capture("05-qr-scanner")
    adb("shell", "input", "keyevent", "4")
    find(desc="Assistant address")
    fill("Assistant address", "http://127.0.0.1:18765")
    fill("Pairing secret", "legacy-ui-test-only")
    tap(desc="Save & test connection")
    wait_for(lambda: any(e.get("title") == "Pairing test" for e in events), "Pairing test was not delivered")
    time.sleep(1)
    capture("06-pairing-verified")
    tap(desc="Overview")
    tap(text="Allow notification access")
    find(text="Share selected notifications with Legacy")
    capture("06a-disclosure")
    tap(text="Not now")
    initial_prefs = adb("shell", "run-as", "mv.legacy.link", "cat", "shared_prefs/legacy_link.xml")
    assert 'name="privacy_consent_v1" value="true"' not in initial_prefs, "Declining must not record consent"
    tap(text="Allow notification access")
    tap(text="I agree")
    adb("shell", "input", "keyevent", "4")
    print("Pairing and scanner passed; testing source filtering", flush=True)
    adb("shell", "cmd", "notification", "allow_listener", "mv.legacy.link/mv.legacy.link.LegacyNotificationListener")
    time.sleep(1)
    notify("legacy-unselected", "Unselected synthetic notification")
    time.sleep(1)
    assert not any(e.get("text") == "Unselected synthetic notification" for e in events), "An unselected app was forwarded"
    tap(desc="Apps")
    fill("App package name", "com.android.shell")
    tap(desc="Add app")
    tap(desc="Selected")
    find(text="Shell")
    capture("07-selected-source")
    tap(desc="Overview")
    notify("legacy-selected", "Selected synthetic notification")
    wait_for(lambda: any(e.get("text") == "Selected synthetic notification" for e in events), "Selected notification was not forwarded")
    capture("08-ready")
    print("Selected notification delivered; testing offline queue and pause", flush=True)
    mode["online"] = False
    notify("legacy-offline", "Queued synthetic notification")
    time.sleep(2)
    tap(desc="Apps")
    tap(desc="Overview")
    # Read only synthetic test state from this debug package on this emulator.
    def prefs():
        return adb("shell", "run-as", "mv.legacy.link", "cat", "shared_prefs/legacy_link.xml")
    wait_for(lambda: "Queued synthetic notification" in prefs(), "Offline notification was not queued")
    tap(desc="Notification forwarding", cls="android.widget.Switch")
    mode["online"] = True
    tap(desc="Apps")
    tap(desc="Overview")
    time.sleep(1)
    assert not any(e.get("text") == "Queued synthetic notification" for e in events), "Paused queue was delivered"
    find(text="Your link is paused")
    capture("09-paused")
    tap(desc="Notification forwarding", cls="android.widget.Switch")
    wait_for(lambda: any(e.get("text") == "Queued synthetic notification" for e in events), "Queue did not flush after resuming")
    wait_for(lambda: "Queued synthetic notification" not in prefs(), "Delivered notification remained in queue")
    capture("10-resumed")
    tap(desc="Settings")
    tap(desc="Notification access blocked?")
    find(text="Notification access blocked?")
    capture("11-restricted-settings-help")
    tap(text="Open App info")
    current_activity = adb("shell", "dumpsys", "activity", "activities")
    assert "com.android.settings" in current_activity, "App info shortcut did not open Settings"
    adb("shell", "input", "keyevent", "4")
    tap(desc="Clear local data")
    find(text="Clear local data?")
    tap(text="Clear local data")
    wait_for(lambda: "legacy-ui-test-only" not in prefs() and "com.android.shell" not in prefs(), "Local data was not cleared")
    delivered_before_reset = len(events)
    notify("legacy-after-reset", "Notification after local reset")
    time.sleep(1)
    assert len(events) == delivered_before_reset, "Notification was forwarded after local data reset"
    capture("12-local-data-cleared")
    crashes = adb("logcat", "-b", "crash", "-d")
    (out / "crashes.txt").write_text(crashes, encoding="utf-8")
    assert "FATAL EXCEPTION" not in crashes, crashes
    (out / "result.json").write_text(json.dumps({"status": "passed", "apk_sha256": installed_apk_sha256, "checks": ["fresh install", "private-by-default sources", "app search", "masked secret", "QR scanner launch", "manual pairing", "affirmative disclosure and decline", "unselected source blocked", "selected source delivered", "offline queue", "pause blocks queued delivery", "resume drains queue", "restricted-settings helper and App info", "local data reset stops forwarding", "no runtime crashes"], "synthetic_events_received": len(events)}, indent=2), encoding="utf-8")
    print("PASS: installation, UI, pairing, filtering, queue, pause/resume, crash check", flush=True)
finally:
    server.shutdown()
    adb("reverse", "--remove", "tcp:18765")
