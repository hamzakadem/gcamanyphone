# Troubleshooting & Common Issues Guide

## 1. Installation & Permission Issues

### 1.1 "App Not Installed" or "Install Blocked"
- **Cause**: Android Play Protect or security settings blocking apps from unknown sources.
- **Solution**:
  1. Open your phone's **Settings** > **Apps** > **Special App Access** > **Install Unknown Apps**.
  2. Enable permission for your browser or file manager.
  3. If Google Play Protect displays a warning dialog (*"Unrecognized app"*), tap **More details** and select **Install anyway**.

### 1.2 Black Screen on Launch
- **Cause**: Camera permission was denied, or another application is holding an exclusive lock on the camera sensor.
- **Solution**:
  1. Close any running camera applications (Instagram, Snapchat, WhatsApp, system camera).
  2. Open phone **Settings** > **Apps** > **GCam Lite** > **Permissions** and ensure **Camera** is set to **Allow**.
  3. Reopen GCam Lite.

---

## 2. Hardware Quirks & Performance

### 2.1 OutOfMemoryError (OOM) on 1GB or 2GB Devices
- **Cause**: Device has a very small per-app heap limit (e.g. 128MB).
- **Solution**:
  1. Switch the processing mode to **Fast (Single)** by tapping the mode chip at the top.
  2. Open **Settings** (gear icon) and select **Fast (720p / Low RAM)** capture resolution.
  3. Close background apps before shooting.

### 2.2 Flash Mode Does Not Fire
- **Cause**: Some budget devices have LED torches that cannot be triggered in strobe mode when the battery is below 15%.
- **Solution**:
  1. Ensure battery is above 20%.
  2. Tap the flash icon in the top left to cycle from **Auto** to **On**.

### 2.3 Photos Are Not Showing in the System Gallery
- **Cause**: On Android 9 and older, the system media scanner may take a few seconds to index new files.
- **Solution**:
  1. Tap the review thumbnail in the bottom left of GCam Lite to open the photo directly.
  2. Alternatively, open a file manager and navigate to `/sdcard/DCIM/GCamLite/`.

---

## 3. GitHub Actions Build Troubleshooting

### 3.1 GitHub Actions Is Not Running Automatically
- **Cause**: Actions may be disabled by default on newly created repositories.
- **Solution**:
  1. Open the repository on GitHub and click the **Actions** tab.
  2. Click the green button **"I understand my workflows, go ahead and enable them"**.
  3. Re-run the workflow via the **Run workflow** button.

### 3.2 "Resource Not Accessible by Integration" (Release Job)
- **Cause**: Default repository workflow permissions are set to "Read repository contents".
- **Solution**:
  1. Open repository **Settings** > **Actions** > **General**.
  2. Scroll down to **Workflow permissions**.
  3. Select **Read and write permissions** and click **Save**.
