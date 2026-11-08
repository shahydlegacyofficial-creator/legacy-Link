# Reviewer access instructions

Legacy Link's complete function requires a compatible assistant receiver. Provide Google Play reviewers a stable, internet-reachable HTTPS test receiver with a valid publicly trusted certificate. It must be available throughout review and must not require access to your private LAN or Tailscale account. Do not put production or reviewer secrets in public GitHub files.

## Paste into Play Console after filling the private fields

App access: Some functionality is restricted because an assistant receiver must be paired.

1. Open Legacy Link → Settings.
2. Under manual connection, enter the HTTPS test receiver address supplied privately below and its test pairing secret. Choose Save & test connection.
3. Read and agree to the notification-sharing disclosure. In Android settings, enable notification access for Legacy Link.
4. Open Apps and select an installed test source. No source is selected by default.
5. Post a harmless test notification from that source. The assistant receiver accepts POST /phone-notify using X-Legacy-Secret and can confirm delivery.
6. Use Overview to pause/resume forwarding and inspect the waiting count. Settings → Clear local data removes the phone's pairing, selections and queue.

Private fields to supply in Play Console only:

- Reviewer receiver: YOUR STABLE HTTPS TEST ADDRESS
- Test pairing secret: YOUR PRIVATE TEST SECRET
- Test-event confirmation method: YOUR REVIEWER-ACCESSIBLE METHOD
- Any receiver sign-in steps: STATE NONE OR PROVIDE VALID TEST ACCESS
- Review contact: legacystudiosofficial@gmail.com

The phone app itself has no account registration, paid feature gate or two-factor login. A receiver failure prevents the pairing test; a permission refusal prevents notification capture. Provide access without geographical restrictions, expiring codes or personal staff approval.

## Receiver contract

GET /phone-ping returns 2xx. POST /phone-notify verifies X-Legacy-Secret and accepts JSON fields app, appLabel, title, text and at. The pairing test uses app mv.legacy.link and title Pairing test. Details are in SERVER_API.md.

Use a receiver dedicated to synthetic review data. The example receiver in `tools/reviewer_receiver.py` can be placed behind an HTTPS reverse proxy you control. The template does not create or host a public receiver, and its local HTTP address is not a Play-review endpoint.
