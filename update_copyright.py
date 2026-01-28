import os

files = [
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-ar\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-bg\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-de\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-es\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-fr\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-hi\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-nl\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-pt\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-ru\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-tr\strings.xml",
    r"c:\Users\BC\AndroidStudioProjects\SolarPVtracker\app\src\main\res\values-zh-rCN\strings.xml"
]

for file_path in files:
    try:
        if os.path.exists(file_path):
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if '2025' in content:
                print(f"Updating {file_path}")
                new_content = content.replace('2025', '2026')
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
            else:
                print(f"Skipping {file_path} (2025 not found)")
        else:
            print(f"File not found: {file_path}")
    except Exception as e:
        print(f"Error processing {file_path}: {e}")

print("Copyright update complete.")
