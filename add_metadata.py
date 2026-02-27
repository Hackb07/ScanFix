"""
This script injects the required Object Detection Metadata into a raw `.tflite` model
(like one exported from YOLOv8) so it can be loaded successfully by the ScanFix app!

Run this on your computer:
1. pip install tflite-support
2. Change the `model_path` to your raw model
3. Change `label_path` to a .txt file containing your model's classes (e.g., labels.txt)
4. Run python add_metadata.py
"""

import os
from tflite_support.metadata_writers import object_detector
from tflite_support.metadata_writers import writer_utils

def inject_metadata():
    # 1. Provide the paths to your local raw model and a labels.txt file
    model_path = "./your_raw_model.tflite"
    label_path = "./labels.txt"
    export_model_path = "./scanfix_ready_model.tflite"

    # Make sure you have a labels file where each line is a class (e.g. line 1: person, line 2: car)
    if not os.path.exists(label_path):
        with open(label_path, 'w') as f:
            f.write("person\nbicycle\ncar\nmotorcycle") # Default dummy labels
            print("Created a dummy labels.txt file! Edit it with your actual classes.")

    if not os.path.exists(model_path):
        print(f"Error: Could not find raw model at {model_path}. Please place your TFLite model here!")
        return

    # 2. Inject Object Detection Metadata
    # You might need to adjust the input/output normalization depending on your exact model architecture
    writer = object_detector.MetadataWriter.create_for_inference(
        writer_utils.load_file(model_path), 
        [127.5], [127.5], # Normalization Mean and Std (common for YOLO/MobileNet)
        [label_path]
    )

    # 3. Save the new Android-ready TFLite file!
    writer_utils.save_file(writer.populate(), export_model_path)
    print(f"Success! You can now upload {export_model_path} into the ScanFix App.")

if __name__ == "__main__":
    inject_metadata()
