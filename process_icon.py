from PIL import Image
import os

input_path = r"C:\Users\Tharu\.gemini\antigravity\brain\ac3bbe81-e859-480f-a7cc-e23b1caaee4d\scanfix_logo_blue_1772203827962.png"
res_dir = r"c:\Users\Tharu\Projects\ScanFix\app\src\main\res"

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

def process_image():
    img = Image.open(input_path).convert("RGBA")
    data = img.getdata()
    
    newData = []
    # Make white and near-white pixels transparent
    for item in data:
        if item[0] > 240 and item[1] > 240 and item[2] > 240:
            newData.append((255, 255, 255, 0))
        else:
            newData.append(item)
            
    img.putdata(newData)
    
    # Crop to the visible area
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
        
    width, height = img.size
    max_dim = max(width, height)
    
    # Pad by 20% to leave some breathing room around the logo inside the icon frame
    padded_size = int(max_dim * 1.2)
    padded_img = Image.new("RGBA", (padded_size, padded_size), (255, 255, 255, 0))
    
    offset = ((padded_size - width) // 2, (padded_size - height) // 2)
    padded_img.paste(img, offset)
    
    for folder, size in sizes.items():
        out_dir = os.path.join(res_dir, folder)
        os.makedirs(out_dir, exist_ok=True)
        
        try:
            resampling_filter = Image.Resampling.LANCZOS
        except AttributeError:
            resampling_filter = Image.LANCZOS

        resized = padded_img.resize((size, size), resampling_filter)
        
        # Save standard icon
        resized.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")
        
        # Save round icon
        resized.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")

    print(f"Icons generated successfully in {len(sizes)} resolutions!")

if __name__ == "__main__":
    process_image()
