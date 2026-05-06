pmcaFilesystemServer
====
[![Build Status](https://travis-ci.org/schnatterer/pmcaFilesystemServer.svg?branch=develop)](https://travis-ci.org/schnatterer/pmcaFilesystemServer)

A modern, responsive web server for Sony Cameras (PlayMemories Camera Apps) that provides access to the camera's filesystem via HTTP. 

This app is designed to work around the constraints of certain Sony cameras (like the A6000) where videos cannot be downloaded via the official WiFi apps.

# Key Features

*   **Modern Web UI:** Responsive, card-based grid layout with a clean, dark-mode friendly design.
*   **Media Gallery:** Optimized view for DCIM photos and videos.
*   **Real-time Thumbnails:** On-the-fly thumbnail extraction for JPEGs and ARW (RAW) files.
*   **Image Preview:** Built-in viewer for photos before downloading.
*   **Smart Navigation:** Breadcrumbs and a side drawer for quick access to important folders (DCIM, Videos).
*   **Bulk Download:** Download entire directories as a single ZIP file.
*   **Legacy Support:** Minimalist enough to run on the Android 4.1.2 browser found in older Sony cameras.

# Installation 

The "Modern UI" is currently in active development. You can install it via:
*   [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) (Use the "Tweak" app method).
*   Directly through `adb install`.
*   Download the latest **unsigned APK** from the [Releases](https://github.com/matheuscoletti08/pmcaFilesystemServer/releases) page.

> **Note:** For Sony cameras, signed APKs are often denied by the installation process. Prefer using the unsigned version provided in releases.

# Usage

1.  Start the app on your camera.
2.  Wait for the WiFi connection to be established.
3.  Once connected, the app will display a URL (e.g., `http://192.168.x.x:8080`).
4.  Open this URL in your phone, tablet, or PC's browser to browse and download your files.

<font color="red">⚠</font> **SECURITY WARNING:** The Web Server exposes the **entire filesystem** without authentication. Anyone on the same network can access your files. Only run this in a private network, via WiFi Direct, or using your Mobile Hotspot.

# Development

This project uses the [OpenMemories: Framework](https://github.com/ma1co/OpenMemories-Framework) and must remain compatible with **Java 7 (source 1.7)** to run on the camera's hardware.

```bash
# Connect to camera via ADB over WiFi
adb tcpip 5555
adb connect <CAMERA_IP>:5555
```

The app writes logs to the SD card at: `/storage/sdcard0/pmcaFilesystemServer/LOG.TXT`.

## Credits

Inspired by and building upon:
*   [ma1co/PMCADemo](https://github.com/ma1co/PMCADemo)
*   [LubikR/SynologyUploader](https://github.com/LubikR/SynologyUploader)
*   [Bostwickenator/STGUploader](https://github.com/Bostwickenator/STGUploader)
