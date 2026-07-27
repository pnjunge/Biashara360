# Production build

The archive must be created on macOS with Xcode, an Apple Developer team, and
an App Store provisioning profile for `com.app.biashara`.

1. Run `pod install` from this directory.
2. Open `iosApp.xcworkspace`, select the `iosApp` target, and choose the Apple
   Developer team under Signing & Capabilities.
3. Increment `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION`.
4. Archive with the generic iOS device destination:

   ```sh
   xcodebuild \
     -workspace iosApp.xcworkspace \
     -scheme iosApp \
     -configuration Release \
     -destination 'generic/platform=iOS' \
     -archivePath build/Biashara360.xcarchive \
     clean archive
   ```

5. Copy `ExportOptions.example.plist` to an untracked signing-specific file,
   add any required team settings, then export:

   ```sh
   xcodebuild \
     -exportArchive \
     -archivePath build/Biashara360.xcarchive \
     -exportOptionsPlist ExportOptions.plist \
     -exportPath build/export
   ```
