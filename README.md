# GPS Location App (Android)

An Android app that shows the device's current **latitude and longitude**, read directly from the **GPS receiver** (`LocationManager.GPS_PROVIDER`). It also shows accuracy and the time of the fix, and updates live every 2 seconds / 1 metre.

## Permissions

**Declared** in `app/src/main/AndroidManifest.xml`:

- `ACCESS_FINE_LOCATION` – precise location from GPS
- `ACCESS_COARSE_LOCATION` – required alongside FINE on Android 12+
- `<uses-feature android:name="android.hardware.location.gps">`

**Requested at runtime** in `MainActivity.kt`:

1. Checks `ACCESS_FINE_LOCATION` with `ContextCompat.checkSelfPermission`.
2. If missing, requests it with the `ActivityResultContracts.RequestMultiplePermissions` launcher.
3. If denied, shows a rationale dialog with **Try again**.
4. If permanently denied (or only approximate location granted), offers to open the app's settings page.
5. If GPS is switched off, offers to open the device's Location settings.
6. Location updates stop in `onPause()` and resume in `onResume()` to save battery.

## Tech

Kotlin · Android Gradle Plugin 8.5.2 · Gradle 8.9 · minSdk 26 · targetSdk 34 · View Binding · Material 3

## Run it

1. Open the folder in Android Studio (**File → Open**) and let Gradle sync.
2. Run on a phone (best: real GPS) or an emulator.
3. **Emulator:** open the emulator's **⋯ Extended controls → Location**, pick a point, and press **Set location**. The coordinates appear in the app.
4. Tap **Allow → While using the app** (choose **Precise**) when prompted.
