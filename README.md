# I2P-Bote for Android

Bote is an Android port of I2P-Bote.

## Build process

### Preparation

1. Install I2P. You need the installed libraries to build against.

2. Download the Android SDK. The simplest method is to download Android Studio.

3. Check out the `i2p.i2p-bote` and `i2p.i2p-bote.android` repositories.

4. Create a `local.properties` file in `i2p.i2p-bote.android/app` containing:

    i2pbase=/path/to/installed/i2p
    botesrc=/path/to/i2p.i2p-bote

5. Gradle will pull dependencies over the clearnet by default. To use Tor, create a `gradle.properties` file in `i2p.i2p-bote.android` containing:

    systemProp.socksProxyHost=localhost
    systemProp.socksProxyPort=9150

### Building from the command line

1. Create a `local.properties` file in `i2p.i2p-bote.android` containing:

    sdk.dir=/path/to/android-studio/sdk

2. `./gradlew assembleDebug`

3. The APK will be placed in `i2p.i2p-bote.android/app/build/apk`.

### Building with Android Studio

1. Import `i2p.i2p-bote.android` into Android Studio. (This creates the `local.properties` file automatically).

2. Build and run the app (Shift+F10).
