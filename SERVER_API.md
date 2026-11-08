# Legacy/JARVIS server integration

Legacy Link sends notifications directly to a small HTTP API on the computer running the assistant.

## Pairing QR payload

The assistant can display a QR code containing this JSON object:

```json
{
  "host": "http://100.100.100.100:3000",
  "secret": "replace-with-a-long-random-secret"
}
```

The Google Play flavor requires an `https://` base URL with a valid certificate. The personal flavor also supports `http://` for encrypted private-network setups. Do not add a trailing route. The app adds `/phone-ping` and `/phone-notify` itself. For the Play QR, encode the HTTPS receiver address rather than the HTTP example above.

Use at least 32 random bytes for `secret`. Do not hard-code a real secret in the app, source repository, screenshots, or logs.

## Required routes

### `GET /phone-ping`

This is a reachability check and does not include the secret.

Successful response:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"ok":true}
```

### `POST /phone-notify`

Headers:

```http
Content-Type: application/json; charset=utf-8
X-Legacy-Secret: your-pairing-secret
```

Body:

```json
{
  "app": "com.whatsapp",
  "appLabel": "WhatsApp",
  "title": "Contact name",
  "text": "Message preview",
  "at": 1787000000000
}
```

| Field | Type | Meaning |
| --- | --- | --- |
| `app` | string | Android package name |
| `appLabel` | string | Human-readable application name |
| `title` | string | Notification title |
| `text` | string | Notification text or expanded text |
| `at` | integer | Phone timestamp in Unix milliseconds |

Return a `2xx` status only after the event has been accepted. Return `401` when the secret is wrong. Other `4xx` and `5xx` responses are treated as delivery failures and the phone retains the event for retry.

## Minimal Node.js example

This example uses only built-in Node.js modules. Replace `handleNotification` with the handoff to your Legacy/JARVIS event bus.

```js
const crypto = require("node:crypto");
const http = require("node:http");

const port = Number(process.env.LEGACY_PHONE_PORT || 3000);
const secret = process.env.LEGACY_PHONE_SECRET;

if (!secret || secret.length < 32) {
  throw new Error("Set LEGACY_PHONE_SECRET to a long random value.");
}

function secretMatches(candidate = "") {
  const expected = Buffer.from(secret);
  const received = Buffer.from(candidate);
  return expected.length === received.length &&
    crypto.timingSafeEqual(expected, received);
}

function sendJson(response, status, body) {
  response.writeHead(status, { "Content-Type": "application/json" });
  response.end(JSON.stringify(body));
}

function handleNotification(event) {
  console.log(`[phone] ${event.appLabel}: ${event.title}`);
  // Forward `event` to your assistant here.
}

const server = http.createServer((request, response) => {
  if (request.method === "GET" && request.url === "/phone-ping") {
    return sendJson(response, 200, { ok: true });
  }

  if (request.method !== "POST" || request.url !== "/phone-notify") {
    return sendJson(response, 404, { ok: false });
  }

  if (!secretMatches(request.headers["x-legacy-secret"])) {
    return sendJson(response, 401, { ok: false, error: "unauthorized" });
  }

  let raw = "";
  request.setEncoding("utf8");
  request.on("data", chunk => {
    raw += chunk;
    if (raw.length > 65536) request.destroy();
  });
  request.on("end", () => {
    try {
      const event = JSON.parse(raw);
      if (typeof event.app !== "string" || typeof event.text !== "string") {
        return sendJson(response, 400, { ok: false, error: "invalid_event" });
      }
      handleNotification(event);
      return sendJson(response, 202, { ok: true });
    } catch {
      return sendJson(response, 400, { ok: false, error: "invalid_json" });
    }
  });
});

server.listen(port, "0.0.0.0", () => {
  console.log(`Legacy phone link listening on port ${port}`);
});
```

Start it with the secret supplied outside source control:

```bash
LEGACY_PHONE_SECRET="your-long-random-secret" node phone-link-server.js
```

On Windows PowerShell:

```powershell
$env:LEGACY_PHONE_SECRET = "your-long-random-secret"
node .\phone-link-server.js
```

## Network recommendations

- Prefer a Tailscale IP (`100.x.x.x`) or MagicDNS hostname.
- Allow the Node.js process through Windows Firewall on private networks only.
- Do not forward port 3000 from the public internet.
- If you must use a public network, terminate TLS, add rate limiting, and restrict source access.
