# NEXSHOP Bank Notification Receiver — API contract

The Android notifier sends each selected Android notification to the configured HTTPS endpoint.

## Request

`POST /api/bank-notifications`

Headers:
- `Content-Type: application/json`
- `Authorization: Bearer <NEXSHOP_DEVICE_TOKEN>`

JSON body:
```json
{
  "event_id": "sha256...",
  "source_package": "tj.dc.next1",
  "source_app": "DC Next",
  "title": "DC Next",
  "body": "УСПЕШНО ЗАЧИСЛЕНО! ... на сумму 5,00",
  "posted_at_unix_ms": 1794950123000
}
```

Return HTTP 2xx only after the event has been durably stored. Return 401 for an invalid token.

## Server-side payment matching rules

This payload is an Android notification relay, NOT a bank API and NOT cryptographic proof that a transfer settled. The server must:
1. Authenticate the device token over HTTPS.
2. Accept only the configured DC Next source package.
3. Parse the amount and transaction/account reference from the notification body.
4. Match the exact received amount to one unexpired pending top-up.
5. Require an unambiguous one-to-one match; otherwise flag for manual review.
6. Store `event_id` with a UNIQUE constraint and reject duplicates.
7. Credit only the requested wallet amount, never the surcharge/differential amount.
8. Use a database transaction so a notification cannot credit twice.
9. Keep an audit record and alert an administrator on ambiguous or unmatched notices.

Do not credit funds merely because a user pressed “I paid” or because a notification text contains “successful”. Android notification access can be disabled, delayed, duplicated, or spoofed on a compromised device. For high-assurance settlement, prefer an official Dushanbe City merchant API/webhook or reconcile against the merchant statement.

## Example configuration

- Endpoint: `https://YOUR_DOMAIN/api/bank-notifications`
- Device token: a long random secret generated on the server and stored privately in the Android app.
