# Security policy

## Supported version

Security fixes are applied to the latest version on the `main` branch.

## Reporting a vulnerability

Do not open a public issue containing a pairing secret, private notification text, a working server address, or exploit details. Contact legacystudiosofficial@gmail.com privately and include:

- the affected version or commit;
- the Android version and device model;
- clear reproduction steps;
- the security impact;
- a suggested fix, if available.

Revoke and regenerate the pairing secret immediately if it may have been exposed.

## Deployment guidance

- Use a properly configured HTTPS receiver for the Play build. The personal build can use an encrypted private Tailscale network.
- Use a long, random, unique pairing secret.
- Never commit signing keys, `local.properties`, secrets, or real notification data.
- Private upload keys and passwords are stored outside the repository; a public upload certificate is safe to distribute.
- Review selected apps regularly; banking, authentication, and messaging notifications can be highly sensitive.
- Use a production signing key for any distributed release APK.
