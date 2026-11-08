"""Synthetic-data review receiver. Run behind a valid HTTPS reverse proxy.

Set LEGACY_REVIEW_SECRET privately. No events are written to disk. This example
does not deploy hosting, supply a certificate, or replace your production server.
"""
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from collections import deque

secret = os.environ.get("LEGACY_REVIEW_SECRET", "")
if len(secret) < 20:
    raise SystemExit("Set a private LEGACY_REVIEW_SECRET of at least 20 characters.")
events = deque(maxlen=20)
page = b'''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Legacy Link review receiver</title>
<style>body{background:#0b0e13;color:#f1f4f8;font:18px system-ui;max-width:760px;margin:60px auto;padding:24px}input,button{font:inherit;padding:12px;margin:8px 0}pre{white-space:pre-wrap;overflow-wrap:anywhere}</style>
<h1>Legacy Link test receiver</h1><p>Use synthetic notifications only. Enter the private test secret to inspect the last 20 accepted events held in memory.</p>
<input id="secret" type="password" placeholder="Private test secret" autocomplete="off"><button id="load">Load events</button><pre id="events"></pre>
<script>document.getElementById('load').onclick=async()=>{const r=await fetch('/review-events',{headers:{'X-Legacy-Secret':document.getElementById('secret').value}});document.getElementById('events').textContent=r.ok?JSON.stringify(await r.json(),null,2):'Access rejected';};</script></html>'''


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *unused):
        pass

    def reply(self, code, body=b"", content_type="application/json; charset=utf-8"):
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def authorized(self):
        return hmac.compare_digest(self.headers.get("X-Legacy-Secret", ""), secret)

    def do_GET(self):
        if self.path == "/phone-ping":
            self.reply(200, b'{"ok":true}')
        elif self.path == "/":
            self.reply(200, page, "text/html; charset=utf-8")
        elif self.path == "/review-events":
            self.reply(200, json.dumps(list(events)).encode()) if self.authorized() else self.reply(401)
        else:
            self.reply(404)

    def do_POST(self):
        if self.path != "/phone-notify":
            self.reply(404)
            return
        if not self.authorized():
            self.reply(401)
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 < length <= 65536:
                self.reply(413)
                return
            event = json.loads(self.rfile.read(length))
            if not isinstance(event, dict) or not all(key in event for key in ("app", "appLabel", "title", "text", "at")):
                self.reply(400)
                return
            events.append(event)
            self.reply(200, b'{"ok":true}')
        except (ValueError, UnicodeDecodeError):
            self.reply(400)


if __name__ == "__main__":
    print("Local review receiver listening on 127.0.0.1:3000. Use an HTTPS reverse proxy for review.")
    ThreadingHTTPServer(("127.0.0.1", 3000), Handler).serve_forever()
