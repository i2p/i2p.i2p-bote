# I2P-Bote for Android

Bote is an Android port of I2P-Bote.

## Build process

### Dependencies:

- Java SDK (preferably Oracle/Sun or OpenJDK) 1.6.0 or higher
- Apache Ant 1.8.0 or higher
- [I2P source](https://github.com/i2p/i2p.i2p)
- [I2P-Bote source](https://github.com/i2p/i2p.i2p-bote)
- Android SDK 21
- Android Build Tools 21.1.1
- Android Support Repository
- Gradle 2.2.1

### Gradle

The build system is based on Gradle. There are several methods for setting Gradle up:

* It can be downloaded from [the Gradle website](http://www.gradle.org/downloads).

* Most distributions will have Gradle packages. Be careful to check the provided version; Debian and Ubuntu have old versions in their main repositories. There is a [PPA](https://launchpad.net/~cwchien/+archive/gradle) for Ubuntu with the latest version of Gradle.

* A Gradle wrapper is provided in the codebase. It takes all the same commands as the regular `gradle` command. The first time that any command is run, it will automatically download, cache and use the correct version of Gradle. This is the simplest way to get started with the codebase. To use it, replace `gradle` with `./gradlew` (or `./gradlew.bat` on Windows) in the commands below.

Gradle will pull dependencies over the clearnet by default. To use Tor, create a `gradle.properties` file in `i2p.android.base` containing:

    ```
    systemProp.socksProxyHost=localhost
    systemProp.socksProxyPort=9150
    ```

### Preparation

1. Install I2P. You need the installed libraries to build against.

2. Download the Android SDK. The simplest method is to download Android Studio.

3. Check out the `i2p.i2p-bote` and `i2p.i2p-bote.android` repositories.

4. Create a `local.properties` file in `i2p.i2p-bote.android/botejars` containing:

    ```
    i2pbase=/path/to/installed/i2p
    botesrc=/path/to/i2p.i2p-bote
    ```

5. If you want to use a local copy of the I2P Android client library, install it in your local Maven repository with:

    ```
    cd path/to/i2p.android.base
    ./gradlew client:installArchives
    ```

### Building from the command line

1. Create a `local.properties` file in `i2p.i2p-bote.android` containing:

    ```
    sdk.dir=/path/to/android-studio/sdk
    ```

2. `gradle assembleDebug`

3. The APK will be placed in `i2p.i2p-bote.android/app/build/apk`.

### Building with Android Studio

1. Import `i2p.i2p-bote.android` into Android Studio. (This creates the `local.properties` file automatically).

2. Build and run the app (`Shift+F10`).

### Signing release builds

1. Create a `signing.properties` file in `i2p.i2p-bote.android` containing:

    ```
    STORE_FILE=/path/to/android.keystore
    STORE_PASSWORD=store.password
    KEY_ALIAS=key.alias
    KEY_PASSWORD=key.password
    ```

2. `gradle assembleRelease`
