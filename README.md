<<<<<<< HEAD
# ScanFix
=======
# Kotlin TFLite Object Detection App

This is a native Android application written in Kotlin that uses TensorFlow Lite for real-time object detection.

## Setup Instructions

1.  **Open in Android Studio**:
    Open this folder (`d:\projects\ai-kotlin-app`) in Android Studio. It should automatically sync the Gradle project.

2.  **Download the Model**:
    The application requires a TFLite model with metadata.
    Download the **MobileNet V1 (quantized)** model from the official TensorFlow Lite Task Library examples or use this link:
    [Download lite-model_ssd_mobilenet_v1_1_metadata_2.tflite](https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/object_detection/android/lite-model_ssd_mobilenet_v1_1_metadata_2.tflite)

3.  **Rename and Place the Model**:
    Rename the downloaded file to `mobilenet_v1_1_0_224_quant.tflite` (or update the filename in `ObjectDetectorHelper.kt`).
    Place the file in: `app/src/main/assets/`
    (You may need to create the `assets` folder if it doesn't exist: right-click `app/src/main` -> New -> Folder -> Assets Folder).

## Features
- Real-time object detection using CameraX.
- Bounding box overlay on detected objects.
- Uses TensorFlow Lite Task Vision library.
- Runs on CPU (default), GPU, or NNAPI.

## Requirements
- Android Device with Camera (API 21+).
- Internet connection (to download dependencies).
>>>>>>> 6d7b4c5 (Initial commit: ScanFix AI inspection system)
