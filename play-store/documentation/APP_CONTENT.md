# App content and policy declarations

## Verified phone-app facts

- No ads, in-app purchases, subscription checkout, advertising ID, SDK analytics or app account creation.
- Notification-listener special access is central to the relay. The app presents an affirmative data-sharing disclosure before directing users to Android permission settings.
- Camera is used for QR pairing on the phone. App visibility is limited to launcher queries; QUERY_ALL_PACKAGES is not requested.
- No READ_SMS, READ_CALL_LOG, contacts, microphone, location, accessibility-service, broad-storage or all-files permission is requested.
- Financial information can be present in selected notification text. The app does not operate accounts, issue loans, initiate payments, trade assets, or give financial advice.

## Prepared Console choices

| Declaration | Prepared response |
| --- | --- |
| Ads | No |
| App access | Restricted/full function needs a receiver; provide the private reviewer instructions in APP_ACCESS.md. |
| Target audience | Suggested 18+ only, consistent with the policy's intended adult audience. Owner must confirm. |
| News app | No |
| Government affiliation | No |
| Health app | No health feature is implemented. Reassess if you market health notification processing or add health functions. |
| Financial features | Suggested “My app doesn't provide any financial features” for this notification-relay-only app. Explain the distinction below and reassess the complete Legacy product. |
| Data safety | Complete the separate worksheet; notification transmission must not be described as no collection. |
| Content rating | Complete IARC honestly in Console. No rating certificate or official age label is invented in this kit. |
| Sensitive permissions | Describe the notification listener and local QR camera use. Do not request SMS/call-log restricted-permission approvals the app does not use. |

## Financial-features rationale

The app transports notification text; it does not provide banking functionality, credit monitoring, payments, loans, insurance or advice. Reading a bank notification is not a claim to be that bank. The publisher must still disclose financial information in Data safety. If the assistant ecosystem adds money management, financial advice or other listed financial features, choose the applicable financial declaration rather than copying this relay-only recommendation.

## Content rating facts to use

There is no public user feed, user-to-user messaging service, gambling, cash prizes, purchase flow, violence, sexual content or editorial content catalog in the phone app. It can transport a user's own selected notifications to that user's assistant. Answer the actual questionnaire based on this behavior and the complete distributed service; do not guess an “Everyone” or PEGI rating before IARC assigns it.

## Other owner-only setup

Choose distribution countries, pricing, trader/organization disclosures, legal identity, address, tax and developer-verification information using your actual account records. For eligible newer personal accounts, plan the required closed test before applying for production. These steps are separate from preparing the assets and AAB.
