# ScanFix 🔍

**ScanFix** is a professional-grade Android application for real-time object detection and defect analysis. Built with Kotlin and TensorFlow Lite, it brings powerful edge AI capabilities directly to your mobile device, enabling instant identification and inspection without the need for an internet connection.

---

## 🚀 Features

-   **Real-Time Object Detection**: Identify multiple objects instantly using the device camera.
-   **On-Device Inference**: Powered by TensorFlow Lite for fast, offline performance.
-   **Smart Dashboard**: Visualize detection metrics, including total counts and average confidence scores.
-   **Dynamic Charts**: Real-time confidence trend analysis using interactive line charts.
-   **Dark Mode UI**: A sleek, modern "Dark Mint" aesthetic optimized for low-light environments.
-   **Customizable Settings**: Adjust detection confidence thresholds on the fly.

## 📱 Tech Stack

-   **Language**: Kotlin
-   **UI Framework**: Android Views (XML), Material Components
-   **Machine Learning**: TensorFlow Lite (Task Vision Library)
-   **Camera**: CameraX
-   **Navigation**: Jetpack Navigation Component
-   **Architecture**: MVVM (Model-View-ViewModel)
-   **Charts**: MPAndroidChart

---

## 🛠️ Setup & Installation

### Prerequisites
-   Android Studio Iguana or newer.
-   Android Device with Camera (API Level 24+ recommended).
-   Basic knowledge of Android development.

### Steps

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/Hackb07/ScanFix.git
    cd ScanFix
    ```

2.  **Open in Android Studio**
    -   Launch Android Studio -> `File` -> `Open` -> Select the `ScanFix` folder.
    -   Wait for Gradle synchronization to complete.

3.  **Download the ML Model**
    -   The app requires a TensorFlow Lite model with metadata.
    -   Download the **MobileNet V1 (quantized)** model [here](https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/object_detection/android/lite-model_ssd_mobilenet_v1_1_metadata_2.tflite).
    -   Rename the file to `mobilenet_v1_1_0_224_quant.tflite`.
    -   Place it in the `app/src/main/assets/` directory.

4.  **Run the App**
    -   Connect your Android device via USB.
    -   Click the **Run** button (▶️) in Android Studio.

---

## 🤝 Contribution Guidelines

We welcome contributions from the community! Whether you want to fix a bug, improve documentation, or add a new feature, here's how you can help:

### Reporting Issues
-   Check the [Issues](https://github.com/Hackb07/ScanFix/issues) tab to see if your problem has already been reported.
-   If not, create a new issue with a descriptive title and detailed steps to reproduce.

### Submitting Changes
1.  **Fork the Project**: Create your own copy of the repository.
2.  **Create a Branch**:
    ```bash
    git checkout -b feature/AmazingFeature
    ```
3.  **Commit Your Changes**:
    ```bash
    git commit -m 'Add some AmazingFeature'
    ```
4.  **Push to the Branch**:
    ```bash
    git push origin feature/AmazingFeature
    ```
5.  **Open a Pull Request**: Go to the original repository and open a PR with a clear description of your changes.

### Code Style
-   Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
-   Ensure XML layouts are well-organized and use appropriate naming conventions (e.g., `fragment_scan.xml`, `ic_scan.xml`).

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📧 Contact

Project Link: [https://github.com/Hackb07/ScanFix](https://github.com/Hackb07/ScanFix)
