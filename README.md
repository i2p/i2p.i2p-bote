# I2P-Bote

[![Build Status](https://travis-ci.org/i2p/i2p.i2p-bote.svg?branch=master)](https://travis-ci.org/i2p/i2p.i2p-bote)

I2P-Bote is a plugin for I2P that allows users to send and receive emails while preserving privacy. It does not need a mail server because emails are stored in a distributed hash table. They are automatically encrypted and digitally signed, which ensures no one but the intended recipient can read the email, and third parties cannot forge them.

## Features

- Themeable webmail interface
- User interface translated in many languages
- One-click creation of email accounts (called email identities)
- Emails can be sent under a sender identity, or anonymously
- ElGamal, Elliptic Curve, and NTRU Encryption
- Encryption and signing is transparent, without the need to know about PGP
- Delivery confirmation
- Basic support for short recipient names
- IMAP / SMTP

### Planned Features

- Custom folders
- Sending and receiving via relays, similar to Mixmaster
- Lots of small improvements

## Build process

### Dependencies:

- Java SDK (preferably Oracle/Sun or OpenJDK) 1.7.0 or higher
- Apache Ant 1.8.0 or higher
- Gradle 2.14.1

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

### Building the I2P plugin

```
gradle :webapp:plugin
```

The plugin will be placed in `i2p.i2p-bote/webapp/build/plugin`.

### Building the standalone WAR

```
gradle :webapp:war
```

The WAR will be placed in `i2p.i2p-bote/webapp/build/libs`.

### Running the standalone WAR

Ensure you have an I2P router running locally with an I2CP server port open (on port 7654). Then run:

```
gradle :webapp:tomcatRunWar
```

This will build and run the WAR. (Jetty currently does not work.)

The data directory will be placed in `i2p.i2p-bote/webapp/i2pbote`; logs will be in `i2p.i2p-bote/webapp/logs`.

## Android build process

### Additional dependencies:

- [I2P source](https://github.com/i2p/i2p.i2p)
- Android SDK 25
- Android Build Tools 25.0.2
- Android Support Repository

### Preparation

1. Download the Android SDK. The simplest method is to download Android Studio.

2. Create a `local.properties` file in `i2p.i2p-bote/android` containing:

    ```
    i2psrc=/path/to/i2p.i2p
    ```

3. If you want to use a local copy of the I2P Android client library, install it in your local Maven repository with:

    ```
    cd path/to/i2p.android.base
    ./gradlew client:installArchives
    ```

### Building from the command line

1. Create a `local.properties` file in `i2p.i2p-bote` containing:

    ```
    sdk.dir=/path/to/android-studio/sdk
    ```

2. `gradle :android:assembleDebug`

3. The APK will be placed in `i2p.i2p-bote/android/build/apk`.

### Building with Android Studio

1. Import `i2p.i2p-bote` into Android Studio. (This creates the `local.properties` file automatically).

2. Build and run the app (`Shift+F10`).

### Signing release builds

1. Create a `signing.properties` file in `i2p.i2p-bote` containing:

    ```
    STORE_FILE=/path/to/android.keystore
    STORE_PASSWORD=store.password
    KEY_ALIAS=key.alias
    KEY_PASSWORD=key.password
    ```

2. `gradle assembleRelease`

## More information

The links below only work within I2P, i.e., make sure you are running I2P and your browser is using the proxy at localhost:4444.

- http://i2pbote.i2p I2P-Bote homepage
- http://forum.i2p/viewforum.php?f=35 I2P-Bote forum
