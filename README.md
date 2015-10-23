# Link Bubble Browser

##Install instructions and setup

`git clone git@github.com:brave/LinkBubble.git`

Either install the [Crashlytics/Fabric Android Studio plugin](http://try.crashlytics.com/sdk-android/) or copy `Application/LinkBubble/fabric.properties.template` to `Application/LinkBubble/fabric.properties` and fill in the apiSecret.

##Building Link Bubble Free

Open `./Application/` in Android Studio and build.  You'll need the NDK installed if you don't already have it, instructions below.

##Building release build

Copy `build-release.sh.template` to `build-release.sh`.

Modify each of these exported environment variables: `LINK_BUBBLE_KEYSTORE_LOCATION`, `LINK_BUBBLE_KEYSTORE_PASSWORD`, and `LINK_BUBBLE_KEY_PASSWORD`.

If you get an error about similar to:

> Failure [INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES]

Try uninstalling the application which already exists on your plugged in device.

##Installing the NDK

The following describes steps for installing the NDK on your system. It is specific to OS X, if you use another operating system the steps will be slightly different.

1. Download the NDK from: http://developer.android.com/ndk/downloads/index.html and select the appropriate package for your operating system
2. cd `~/Downloads`
3. Add execute permissions: `chmod a+x ./android-ndk-r10e-darwin-x86_64.bin`
4. Execute the binary: `./android-ndk-r10e-darwin-x86_64.bin`
5. This will extract a folder named something like `android-ndk-r10e/`
6. Move this folder to `~/Library/Android/ndk`
7. Edit your `local.properties` file in Android studio to point to the NDK location. An example `local.properties` looks like this:

```
sdk.dir=/Users/bbondy/Library/Android/sdk
ndk.dir=/Users/bbondy/Library/Android/ndk
```

##Telling getlocalization.com about new strings

1. Periodically upload the file `./Application/LinkBubble/src/main/res/values/strings.xml` to [getlocalization.com](https://www.getlocalization.com/LinkBubble/files/).  getlocalization.com will determine which strings are new
2. When prompted on getlocalization.com, press the mark for retranslation (or keep existing) for changed strings.

##Getting new translated strings from getlocalization.com

1. Install npm dependencies with `npm install`.
2. Run `npm run translate <username> <password>` to pull down the translated xml files.
3. Commit and push your change.

Remember to uncomment `checkStrings` from `MainApplication` and call it in `onCreate` to make sure the pulled files don't cause crashes with format specifiers.

##ADB

If you don't have `adb` in your path add it to your `~/.bash_profile` or similar file:

`export PATH=/Users/<your-username>/Library/Android/sdk/platform-tools:$PATH`

- **Installing an apk onto your device:**  
  `adb install -r ./LinkBubble/build/outputs/apk/LinkBubble-playstore-release.apk`
- **Getting a list of devices:**
  `adb devices`
