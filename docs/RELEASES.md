# Release and artifact policy

- Phase version starts at `0.1.0-phase1` and Android `versionCode=1`.
- CI always produces a debug APK after all tests and lint pass.
- A signed release APK is not produced until the project explicitly configures signing through GitHub Actions secrets. Never put a keystore or signing property file in the ZIP.
- Every ZIP upload remains a Git commit at `incoming/mosaic.zip`.
- Every successful integration writes `notes/imports/<upload-commit-sha>.md` and appends the note to `notes/README.md`.
- The integrated source ZIP excludes `.git/`, the uploaded ZIP, build outputs, caches and local credentials.
- Debug APK SHA-256 and integrated ZIP SHA-256 accompany artifacts.
