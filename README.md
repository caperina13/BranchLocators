# Branch Locator (Android Studio)

Kotlin + Jetpack Compose starter project for a searchable branch locator.

## Features
- Search branches
- Tap a branch to expand its map links
- Each map link opens Google Maps (or a web fallback)
- Add, edit, and delete branches behind an administrator password
- Add multiple map links per branch
- Dark/light mode
- Saves branch edits and administrator password on this device

## Initial administrator password
**Admin@1234**

Change this inside the app using the administrator menu after opening the app. This starter stores its password locally and the initial password is in source code. It is suitable for basic editing protection on a personal/company device, not strong security against someone who can inspect or modify the APK. For stronger security, use server-side authentication.

## Replace the sample branches
The app starts with sample locations. You can replace these in the `defaultBranches()` function in:
`app/src/main/java/com/example/branchlocator/MainActivity.kt`

Or install the app and sign in as administrator to add/edit branches from the UI.

For each map link, enter:
- A friendly label such as `Main branch`, `Warehouse`, or `Parking entrance`
- A full Google Maps URL, exact street address, or latitude/longitude such as `14.6760, 120.5360`

Exact addresses or coordinates are recommended to avoid selecting the wrong place.

## Open and run
1. Install a recent version of Android Studio with JDK 17 support.
2. Extract the ZIP and open the `BranchLocatorAndroid` folder in Android Studio.
3. Allow Gradle sync and dependency downloads to finish.
4. Connect an Android phone with USB debugging enabled, or start an emulator.
5. Press **Run** to install and test.

## Build an APK
1. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. When the build finishes, use the notification's **locate** / **Show in Explorer** action.
3. Typical debug APK path: `app/build/outputs/apk/debug/app-debug.apk`
4. For distribution, use **Build > Generate Signed Bundle / APK** and follow the signing wizard.

No location permission is required because this app opens a saved destination in Maps and does not track the user's current location.
