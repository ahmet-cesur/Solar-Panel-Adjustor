import os
from PIL import Image, ImageOps, ImageDraw

source_image_path = "C:/Users/BC/.gemini/antigravity/brain/4d4c6db0-3c2f-4b28-b547-39fdeab6eaeb/uploaded_image_1767859157836.jpg"
res_dir = r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res"

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

def create_round_icon(img, size):
    # Resize first
    img = img.resize((size, size), Image.Resampling.LANCZOS)
    
    # Create mask
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    
    # Apply mask
    output = ImageOps.fit(img, (size, size), centering=(0.5, 0.5))
    output.putalpha(mask)
    return output

def create_square_icon(img, size):
     # Simple resize for square (adaptive icon usually handled differently but replacing pngs works for legacy)
    return img.resize((size, size), Image.Resampling.LANCZOS)

try:
    with Image.open(source_image_path) as im:
        # Convert to RGBA to support transparency
        im = im.convert("RGBA")
        
        for folder, size in sizes.items():
            target_dir = os.path.join(res_dir, folder)
            if not os.path.exists(target_dir):
                print(f"Directory not found, creating: {target_dir}")
                os.makedirs(target_dir)
            
            # Generate Square (ic_launcher.png)
            square_icon = create_square_icon(im, size)
            square_path = os.path.join(target_dir, "ic_launcher.png")
            square_icon.save(square_path, "PNG")
            print(f"Saved {square_path}")
            
            # Generate Round (ic_launcher_round.png)
            round_icon = create_round_icon(im, size)
            round_path = os.path.join(target_dir, "ic_launcher_round.png")
            round_icon.save(round_path, "PNG")
            print(f"Saved {round_path}")

    print("Icon update complete.")

except Exception as e:
    print(f"Error: {e}")
