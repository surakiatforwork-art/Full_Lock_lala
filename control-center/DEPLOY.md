# Full Lock Control Center

Deploy the repository on Vercel with the existing vercel.json. The dashboard is at /admin/. Vercel serves the Android check-in at /api/access. The build copies the dashboard to dist/admin and keeps server code in Vercel Functions.

1. Create a dedicated Firebase project. Enable Google in Authentication, add lalamove-helper.vercel.app under Authorized domains, and create a Firestore database. The browser uses Firebase Authentication only. Deny direct client access in Firestore rules and grant the Vercel service account the necessary Firestore and Auth read permissions.
2. Configure these Vercel Production environment variables.
   - FULL_LOCK_FIREBASE_WEB_CONFIG contains the public Firebase web-app config JSON.
   - FULL_LOCK_FIREBASE_SERVICE_ACCOUNT contains the service account JSON from that same project. Store it as a server secret.
   - FULL_LOCK_DEVICE_SECRET contains a random secret of at least 32 characters. Use a separate secret in Preview.
3. Deploy, visit /admin/, and verify that the three approved Google accounts work and an unapproved account receives HTTP 403. Verify /api/access can register a test device and can deny check-in after a device or global block.
4. Install the signed 0.1.48 build on a test device. Open Full Lock and verify that its device code appears in the dashboard. Test block, unblock, global disable and recovery with the device online. Then publish the APK and its download metadata together.

Full Lock registers after update and checks every two minutes while its process is running. A confirmed denial persists across app restarts. If a request fails, the last decision stays in effect. An offline device may continue under its last allowed decision until it reconnects. Versions without this check-in are not counted. The dashboard counts device IDs, including devices that have stopped using the app. The 24-hour count uses the latest recorded check-in. Routine updates to that timestamp are spaced at least ten minutes apart.

Dashboard shows the 200 most recently seen devices. Paste a complete device code in search to retrieve older devices. Device registration is rate-limited per client IP and counts self-reported installations. It does not prove a unique person or genuine installation. Set Firestore TTL on registrationLimits.expiresAt to expire old registration counters.
