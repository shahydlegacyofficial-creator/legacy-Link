# Data safety worksheet — owner review required

This worksheet applies to the HTTPS-only Google Play flavor. It is grounded in `Prefs`, `Sender`, `LegacyNotificationListener`, `AppPickerActivity` and the merged manifest. The phone app sends data to a user-configured receiver; a “no data collected” blanket answer would not accurately describe that transmission. The receiver and any AI integrations must also be reviewed by the publisher.

## Prepared form answers

| Question | Proposed answer and basis |
| --- | --- |
| Collects or shares user data | Yes: selected notification text and source metadata leave the phone. |
| All collected data encrypted in transit | Yes for the Play flavor: HTTPS is required in validation, sender guards and the Android manifest. Confirm receiver integrations and all future SDKs remain consistent. |
| User account creation | No: there is no Legacy Studios registration/login flow. Pairing authenticates a receiver. |
| Data deletion mechanism | Yes: Settings → Clear local data, plus the public deletion page and support email. Explain that independent receiver copies require the receiver operator's controls. |
| Independent security review / MASA | No such assessment has been performed. Do not claim certification. |
| Ads, advertising ID, analytics or crash-reporting SDK | None in this source. Do not declare advertising or analytics purposes for this app. |
| Ephemeral-only processing | No: undelivered notification events can persist in the local retry queue, and receiver retention is not guaranteed ephemeral. |

## Data-type mapping

The app forwards unstructured text from any source a user selects. Use the baseline rows below and assess the conditional rows against the supported sources and the complete receiver product. “Collected” here uses Play's off-device definition, even when Legacy Studios does not operate the receiver.

| Form category / data type | Source and handling | Proposed purpose / requirement |
| --- | --- | --- |
| App activity / Installed apps | Source app name and package ID accompany each forwarded event. The full installed-app inventory and search query remain local. | App functionality; required for the relay's source identity. |
| Financial info / Purchase history | Banking notifications can describe payments and transactions. | App functionality; optional by source selection. |
| Financial info / Other financial info | Banking alerts can include balances or other financial text. | App functionality; optional by source selection. |
| Financial info / User payment info | Include when supported notifications contain account or payment-account references. | App functionality; optional by source selection. |
| Messages / Other in-app messages | Chat or other message notifications can contain message text and sender labels. | App functionality; optional by source selection. |
| Messages / SMS or MMS; Emails | Include when SMS or email notification sources are supported/selected, even though no READ_SMS or email-account permission is requested. | App functionality; optional by source selection. |
| Personal info / Name, User IDs, Email address, Phone number | Notification text can contain these identifiers; include the categories exposed by your supported sources. | App functionality; optional by source selection. |
| App activity / Other user-generated content | Other forwarded notification text that does not fit a more specific category. | App functionality; optional by source selection. |
| Calendar / Calendar events; Health and fitness / Health info; Location; Files and docs; other text categories | Assess if you allow such sources or the receiver extracts these details. The source picker permits arbitrary apps, so do not assert these are impossible without limiting supported sources. | Include applicable text categories after reviewing the complete service. |

The app does not extract camera images, photos, video, audio, address-book records, GPS readings, ad identifiers or a full SMS database. Do not confuse lack of those permissions with absence of personal information in selected notification text. A receiver that infers location, profiles users, logs IP data, uses analytics, or sends events onward may require additional declarations.

## Sharing treatment

A conservative answer is to disclose transfer of the applicable categories to the configured assistant as sharing for app functionality. Google describes exceptions for user-directed transfers and qualifying prominent disclosure/consent. Apply an exception only after confirming that every deployed recipient and downstream use meets its conditions; accepting a disclosure does not authorize unrelated advertising or undisclosed AI processing. Do not silently choose “no sharing” for the complete ecosystem based solely on this phone app.

## Before you certify

Record the actual receiver operator, downstream AI providers, purposes, retention/deletion controls and regions in your private release record. Review the proposed categories and form wording in the current Console. Do not upload real notification data or secrets to this repository. This is a decision worksheet, not a fabricated official CSV import or a pre-certified declaration.

Official form guidance: https://support.google.com/googleplay/android-developer/answer/10787469
