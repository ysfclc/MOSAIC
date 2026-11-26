# Vercel APK build

MOSAIC remains a native Android/Gradle project. Vercel is used as the build-and-delivery host, not as a web wrapper around the app.

## Flow

1. The private GitHub repository `ysfclc/MOSAIC` is linked to the Vercel project `mosaic-apk` in the `Yusuf's projects` Hobby team.
2. A push to a non-production branch creates a Vercel Preview deployment. The project runs `scripts/vercel-build-apk.sh`.
3. The build installs Java 17 and Android SDK packages `platforms;android-37.2` and `build-tools;36.0.0`, then runs source checks, Python contract tests, Android unit tests, lint and `:app:assembleDebug`.
4. The script verifies the persistent MOSAIC development signing certificate, places the APK and checksum in `dist/`, and Vercel serves them as static artifacts.
5. Only a reviewed merge to `main` updates the production APK download page.

## Output

- `/MOSAIC-debug.apk` — installable development APK.
- `/MOSAIC-debug.apk.sha256.txt` — SHA-256 checksum.
- `/build-info.txt` — source commit, build time, version code and signing certificate fingerprint.

The APK uses the existing persistent development signing identity so it can update prior installs made with that same certificate. It is not a production release key. The build script fails if the keystore is absent or the certificate fingerprint differs.

## Build constraints

- No GitHub Actions runner is needed for the Vercel build.
- No Vercel environment variables or GitHub tokens are embedded in the script.
- The repository is private; Vercel's linked Git integration checks out the source.
- Android SDK and JDK downloads are cached under the Vercel build user's home cache where supported.
- The APK is built from the exact source revision associated with the Vercel deployment.
- Vercel Preview deployments are for testing. Production is updated only by a merge to the configured production branch.

## Current setup status

The Vercel project is linked to the private GitHub repository. Preview deployments are explicitly enabled in project settings. The only deployment record so far was canceled before build because its metadata reported a production target for the continuity branch. A new branch commit is being used to test the linked Git Preview trigger; do not describe the pipeline as verified until Vercel confirms target=preview, build logs show all checks passing, and the deployed APK download is validated. Production remains on main.
