# Google and Facebook merchant signup

Web business owners can choose Google or Facebook on `/register`, complete their
phone number and business details, and continue to subscription checkout. Returning
owners can use the same provider; existing OTP and account access checks still apply.
Email/password signup remains available. Provider tokens are held only in component
memory and verified again when registration is submitted.

Run the backend normally to apply Flyway migration `V030__social_identities.sql`.
Set these backend environment variables and restart:

- `GOOGLE_CLIENT_ID`: Google OAuth **Web application** client ID. Configure the
  web app's exact origins in Authorized JavaScript origins (including localhost
  for development). The Google Identity Services popup returns an ID token;
  there is no backend redirect callback or Google client secret in this flow.
- `FACEBOOK_LOGIN_APP_ID` and `FACEBOOK_LOGIN_APP_SECRET`: dedicated Facebook
  Login app credentials. Enable the JavaScript SDK login flow and allow the
  website domain/origins in the Meta app dashboard. Use HTTPS in production.
- `FACEBOOK_LOGIN_GRAPH_VERSION`: optional, defaults to `v23.0`; select a version
  supported by your Meta app. These credentials are separate from the app's
  WhatsApp/Instagram business onboarding credentials.

Publish the consent/app configuration as required by each provider, including
privacy policy and data deletion settings, and grant the email/profile permissions
needed for real users. Apps in testing/development mode can restrict sign-in to
configured testers. Secrets must stay on the backend. The providers endpoint
exposes only public IDs and the Graph version. Unconfigured buttons are hidden.

An existing email/password account is never automatically linked by email. It must
continue using its existing sign-in method. A social account is identified by
provider plus provider subject ID. Missing email permission causes signup to fail
with an email-signup fallback. Social signup generates an unknown random password;
owners can use the existing password-reset flow to set a password if needed.

Verification:

1. Sign up with each provider using a new email, finish business details, and
   verify subscription checkout appears.
2. Sign out and sign back in using that provider; verify the same business opens.
3. Enable OTP and verify social sign-in still requests the code.
4. Cancel consent, deny email, use an existing email/password account, and submit
   an expired token; verify no account/session is created.
5. Verify deactivated users/businesses cannot sign in.

Provider setup references:
- https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid
- https://developers.google.com/identity/gsi/web/guides/verify-google-id-token
- https://developers.facebook.com/docs/facebook-login/web/
