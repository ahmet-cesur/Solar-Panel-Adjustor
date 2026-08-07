# Walkthrough - Debug Symbols Generation

I have updated the build configuration to generate debug symbols.

## Changes Made

### Build Configuration
- **Native Debug Symbols:** Added `ndk.debugSymbolLevel = "FULL"` to the `release` build type in [app/build.gradle.kts](file:///E:/01_Kodlama/AndroidStudioProjects/SolarPVtracker/app/build.gradle.kts).

## Results and Findings

### File Locations
- **Mapping File (Kotlin/Java Symbols):**
    - `E:/01_Kodlama/AndroidStudioProjects/SolarPVtracker/app/build/outputs/mapping/release/mapping.txt`
    - Bu dosya, Google Play Console'da "App Bundle Explorer" -> "Downloads" -> "Assets" kısmındaki **"ReTrace mapping file"** alanına yüklenir.

- **Native Debug Symbols (.zip):**
    - Projenizde C++ (yerel kod) bulunmadığı için sistem otomatik olarak bir `.zip` dosyası oluşturmamaktadır. Google Play Store'da eğer "Native Debug Symbols" uyarısı alıyorsanız ve projenizde C++ yoksa, bu uyarıyı dikkate almanıza gerek yoktur veya sadece `mapping.txt` dosyasını yüklemeniz yeterlidir.

> [!IMPORTANT]
> Google Play Console sizden bir sembol dosyası istiyorsa, bu %99 ihtimalle **`mapping.txt`** dosyasıdır. Play Console artık bu dosyayı doğrudan `.txt` olarak kabul etmektedir. Eğer mutlaka `.zip` istiyorsa, `mapping.txt` dosyasını sağ tıklayıp "Arşive ekle" (zip) yaparak yükleyebilirsiniz.
