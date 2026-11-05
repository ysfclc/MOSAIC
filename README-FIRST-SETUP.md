# MOSAIC — first GitHub setup (mobile-friendly)

This is the bootstrap path for the ZIP-based workflow. It follows the same source-upload → validate → build → integrate → note pattern used for Memo.

## Files you receive

- `MOSAIC-Phase-1-Repository.zip` — full Phase 1 project working tree.
- `MOSAIC-GitHub-Actions-Import-and-Build.yml` — workflow to install as `.github/workflows/build-apk.yml`.

The ZIP also contains a tracked copy of the workflow at `.github/workflows/build-apk.yml`. The separate YAML is supplied so it can be installed before the first source ZIP upload.

## A. Create the remote repository once

1. In GitHub, create a new repository named **mosaic** under your account (`ysfclc/mosaic`).
2. Use **main** as the default branch. Leave the repository empty (do not add a README, license or `.gitignore`; the ZIP supplies them).
3. Open **Settings → Actions → General → Workflow permissions** and enable **Read and write permissions**. The importer needs `contents: write` to commit a verified project integration and its note.

The GitHub connector available during this build is read-only and does not expose a repository-creation operation. Therefore, create the empty remote once in GitHub; the workflow then populates its complete source tree and preserves the active workflow/input history on future imports.

## B. Install the workflow before uploading the ZIP

1. In the repository, create `.github/workflows/build-apk.yml`.
2. Paste the complete contents of `MOSAIC-GitHub-Actions-Import-and-Build.yml` into that file and commit it to `main`.
3. This workflow-file-only commit does not start a build; the trigger watches `incoming/mosaic.zip`.

## C. Upload the source snapshot

1. Create the `incoming` folder by adding `incoming/.gitkeep` (or create the folder as part of uploading the ZIP).
2. Upload `MOSAIC-Phase-1-Repository.zip` to the exact repository path **`incoming/mosaic.zip`**. Rename the uploaded file to `mosaic.zip` if GitHub keeps the local filename.
3. Commit that upload to `main`. The upload commit is retained as the immutable source snapshot.
4. Open **Actions → Build MOSAIC Android APK**. The workflow validates and extracts the archive, detects the Gradle project root, preserves `.git/`, `.github/`, `incoming/` and previous `notes/`, installs the pinned toolchain, runs verification/tests/lint, assembles the debug APK, generates and pins the Gradle wrapper if it is missing, then creates an integration commit and an upload-specific note.

## D. Download outputs

On a successful run, download:

- `MOSAIC-Android-debug-APK` — `MOSAIC-debug.apk` and checksum.
- `MOSAIC-Integrated-Repository` — fresh full working-tree ZIP and SHA-256.
- `MOSAIC-Build-Reports` — logs, Gradle reports, import note and commit details.

The workflow also commits the integrated project and `notes/imports/<upload-commit-sha>.md` to `main`. The input ZIP remains in Git history at `incoming/mosaic.zip`, but is not included in the integrated source ZIP artifact.

## E. Every next session / next phase

1. Open the same `ysfclc/mosaic` repository on `main`.
2. Download the latest `MOSAIC-Integrated-Repository.zip` artifact (or use the integrated repository working tree as the source of truth).
3. Continue from `PROJECT_STATE.md`, `docs/TASKS.md`, `docs/PHASES.md` and the latest `notes/imports/` entry.
4. Package the next complete working-tree snapshot using the **same archive filename** `MOSAIC-Phase-1-Repository.zip` locally if keeping a stable download name is useful; upload the contents to the repository path `incoming/mosaic.zip`.
5. Do not overwrite `.github/workflows/build-apk.yml` through an archive import. The active workflow is protected by the importer and must be updated separately only when intentionally changing CI.

## Upload safety rules

Never include `.git/`, `.gradle/`, `build/`, `local.properties`, `.env`, signing keys, keystores, private keys, API tokens, service-account credentials or machine-specific files. The workflow rejects unsafe archives before extraction. The uploaded ZIP itself is committed to Git history, so treat it as public-repository content.

## If a run fails

A failed ZIP validation, test, lint or debug build does not create an integration commit. The original upload commit remains in history, and the run publishes a failure/build report. Fix the source, upload a new `incoming/mosaic.zip` commit, and let the workflow validate that new revision. Re-running an already imported upload is an idempotent no-op.
