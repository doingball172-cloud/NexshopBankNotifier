NEXSHOP BANK NOTIFIER v2 — Android source project

This version preserves Telegram forwarding and adds a second delivery path to the NEXSHOP server over HTTPS.

New settings:
- NEXSHOP HTTPS endpoint URL
- NEXSHOP Bearer device token
- Test server button

Every selected app notification is sent independently to Telegram (if configured) and to the NEXSHOP HTTPS endpoint (if configured). The server payload contains a stable SHA-256 event_id, source package, app label, title, body and Android notification post time.

Setup:
1. Open this folder in Android Studio.
2. Set Telegram Bot Token and Chat ID.
3. Set NEXSHOP HTTPS URL, e.g. https://YOUR_DOMAIN/api/bank-notifications.
4. Set the device Bearer token generated privately by your server.
5. Select DC Next, save settings and enable notification access.
6. Test Telegram and test server buttons.

Build:
Build > Build Bundle(s) / APK(s) > Build APK(s)
APK: app/build/outputs/apk/debug/app-debug.apk

Important:
- This project is source code; no APK has been built or tested on a device here.
- Use HTTPS only.
- Never share bot tokens, device tokens, passwords, PINs or OTP codes in chat.
- This app relays Android notifications; it is not a bank API. Server must deduplicate events and match exact expected top-ups before crediting any balance. Prefer official bank merchant API/webhooks for final settlement confirmation.
- Android may stop background delivery if battery optimization restricts the app.
