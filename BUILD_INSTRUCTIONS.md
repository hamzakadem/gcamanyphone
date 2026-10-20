# Cloud Build & APK Delivery Guide (No Local Android Studio / JDK Required)

This guide explains how to build the APK entirely in the cloud using **GitHub Actions**.

You **do not need** to install Android Studio, Android SDK, Java (JDK), or Gradle on your computer. All compilation, automated tests, and APK packaging are performed automatically by GitHub's cloud runners.

---

## 1. Initial Setup: Create a GitHub Repository

1. Open your web browser and navigate to [https://github.com](https://github.com).
2. Log in to your GitHub account (or sign up for a free account if you do not have one).
3. In the top-right corner of the page, click the **`+`** icon and select **New repository**.
4. Configure your repository:
   - **Repository name**: `GCamCompatibleCamera`
   - **Visibility**: Choose **Public** (recommended for free unlimited GitHub Actions minutes) or **Private**.
   - **Initialize with**: Leave all checkboxes unchecked (do **not** check "Add a README", .gitignore, or license, as these are already provided in the project).
5. Click **Create repository**.

---

## 2. Upload the Project Code to GitHub

You can upload the project using Git or directly via the GitHub website interface.

### Option A: Using the Command Line with Git (Recommended)

Open a terminal or PowerShell in this project folder (`c:\Users\admin\Desktop\gcam for me`) and run:

```bash
# Initialize git if not already initialized
git init -b main

# Stage all project files (the reference APK is excluded automatically by .gitignore)
git add .

# Create the initial commit
git commit -m "Initial commit: Standalone GCam Lite camera app with GitHub Actions CI/CD"

# Link to your new GitHub repository (replace USERNAME with your GitHub username)
git remote add origin https://github.com/USERNAME/GCamCompatibleCamera.git

# Push to GitHub
git push -u origin main
```

*(If prompted, authenticate using your GitHub credentials or a Personal Access Token).*

### Option B: Uploading via the Web Browser

1. In your newly created empty repository on GitHub, click the link that says **"uploading an existing file"**.
2. Drag and drop all files and folders from this folder **except** the `reference` folder (which contains the 326MB APK).
3. Ensure that the hidden `.github` folder (`.github/workflows/android-build.yml`) and `gradle` folder (`gradle/wrapper/*`) are included.
4. Type a commit message (e.g. `Initial commit`) and click **Commit changes**.

---

## 3. Trigger and Monitor the GitHub Actions Build

1. On your GitHub repository page, click the **Actions** tab in the top navigation bar.
2. If prompted with *"Workflows aren't run on this repository"*, click the green button **"I understand my workflows, go ahead and enable them"**.
3. Under **All workflows** in the left sidebar, click **Android Build & APK Delivery**.
4. If a build was automatically triggered by your commit, click on the running workflow run.
5. If no run started automatically, click the **Run workflow** dropdown button on the right, ensure the `main` branch is selected, and click **Run workflow**.
6. The workflow will execute the following cloud steps:
   - Set up JDK 17 (Temurin).
   - Set up Android SDK (Platform 34, Build-Tools 34.0.0).
   - Validate the Gradle wrapper.
   - Run unit tests (`./gradlew testDebugUnitTest`).
   - Run Android lint static checks (`./gradlew lintDebug`).
   - Compile and package the debug APK (`./gradlew assembleDebug`).
   - Verify the APK integrity and compute SHA-256 checksums.
   - Package the source code into a clean ZIP archive.
   - Upload the artifacts.

---

## 4. Download Your Generated APK

1. Once the workflow run finishes (indicated by a green checkmark `✓`), click on the run title to open its summary page.
2. Scroll down to the **Artifacts** section at the bottom of the page.
3. You will see three downloadable items:
   - **`GCamCompatibleCamera-debug-apk`**: Contains the compiled APK (`GCamCompatibleCamera-debug.apk`) and `SHA256SUMS.txt`.
   - **`GCamCompatibleCamera-source`**: Contains the complete source code archive (`GCamCompatibleCamera-Source.zip`).
   - **`GCamCompatibleCamera-test-reports`**: Contains test and lint HTML reports.
4. Click on **`GCamCompatibleCamera-debug-apk`** to download it.
5. GitHub downloads artifacts as a `.zip` file (e.g., `GCamCompatibleCamera-debug-apk.zip`).
6. Extract the downloaded ZIP file on your computer or phone to obtain the actual `.apk` file:
   ```
   GCamCompatibleCamera-debug.apk
   ```

---

## 5. Installing the APK on Your Android Device

1. Transfer `GCamCompatibleCamera-debug.apk` to your phone via USB cable, Google Drive, WhatsApp, or email.
2. Open the file on your device.
3. If prompted with *"Install unknown apps"*, enable permission for the browser or file manager you are using.
4. Tap **Install**.
5. Open **GCam Lite** from your launcher and grant Camera & Storage permissions when prompted.

---

## 6. Optional: Publishing a Release (Tag-Triggered Build)

If you want a public downloadable release with direct asset links:

1. Create a version tag on GitHub:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. The `release` job in `.github/workflows/android-build.yml` will automatically trigger, build the assets, and publish a new GitHub Release with `GCamCompatibleCamera-debug.apk` and `GCamCompatibleCamera-Source.zip` directly attached!
