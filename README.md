# Link Bubble Browser

##Install instructions and setup

`git clone git@github.com:brave/LinkBubble.git`

Either install the [Crashlytics/Fabric Android Studio plugin](http://try.crashlytics.com/sdk-android/) or copy `Application/LinkBubble/fabric.properties.template` to `Application/LinkBubble/fabric.properties` and fill in the apiSecret.

##Building Link Bubble Free

Open `./Application/` in Android Studio and build.

##Building Link Bubble Pro

Open `./ProKey/` in Android Studio and build.

##Building release build

Copy `build-release.sh.template` to `build-release.sh`.

Modify each of these exported environment variables: `LINK_BUBBLE_KEYSTORE_LOCATION`, `LINK_BUBBLE_KEYSTORE_PASSWORD`, and `LINK_BUBBLE_KEY_PASSWORD`.

If you get an error about similar to:

> Failure [INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES]

Try uninstalling the application which already exists on your plugged in device.

##ADB

If you don't have `adb` in your path add it to your `~/.bash_profile` or similar file:

`export PATH=/Users/<your-username>/Library/Android/sdk/platform-tools:$PATH`

- **Getting a list of devices:**
  `adb devices`
