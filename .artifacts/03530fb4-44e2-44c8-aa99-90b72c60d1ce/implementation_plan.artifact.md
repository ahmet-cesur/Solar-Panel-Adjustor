# Enable Native Debug Symbols Generation

This plan enables the generation of the `native_debug_symbols.zip` file for release builds. Note that this file is typically only generated if your project or its dependencies contain native C/C++ libraries (`.so` files).

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///E:/01_Kodlama/AndroidStudioProjects/SolarPVtracker/app/build.gradle.kts)
- Add `ndk.debugSymbolLevel = "FULL"` inside the `release` build type block. This instructs the Android Gradle Plugin to package native debug symbols into a separate zip file.

## Verification Plan

### Automated Steps
- Run `./gradlew :app:bundleRelease` to generate the App Bundle and the symbols zip.
- Check the `app/build/outputs/native_debug_symbols/release/` directory for the `native_debug_symbols.zip` file.

## User Review Required

> [!IMPORTANT]
> Your project currently does not appear to contain any native C++ code. If no native libraries are bundled with your app, the `native_debug_symbols.zip` file may still not be created even with this setting.
>
> If you are seeing a warning in Play Store about "de-obfuscation", you likely need the **`mapping.txt`** file found at `app/build/outputs/mapping/release/mapping.txt`.
