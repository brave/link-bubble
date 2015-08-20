# Link Bubble Browser

##Install instructions and setup

`git clone git@github.com:brave/LinkBubble.git`

Either install the [Crashlytics/Fabric Android Studio plugin](http://try.crashlytics.com/sdk-android/) or copy `Application/LinkBubble/fabric.properties.template` to `Application/LinkBubble/fabric.properties` and fill in the apiSecret.

##Building Link Bubble Free

Open `./Application/` in Android Studio and build.

##Building release build

Copy `build-release.sh.template` to `build-release.sh`.

Modify each of these exported environment variables: `LINK_BUBBLE_KEYSTORE_LOCATION`, `LINK_BUBBLE_KEYSTORE_PASSWORD`, and `LINK_BUBBLE_KEY_PASSWORD`.

If you get an error about similar to:

> Failure [INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES]

Try uninstalling the application which already exists on your plugged in device.

##Telling getlocalization.com about new strings

1. Periodically upload the file `./Application/LinkBubble/src/main/res/values/strings.xml` to [getlocalization.com](https://www.getlocalization.com/LinkBubble/files/).  getlocalization.com will determine which strings are new
2. When prompted on getlocalization.com, press the mark for retranslation (or keep existing) for changed strings.

##Getting new translated strings from getlocalization.com

1. Install npm dependencies with `npm install`.
2. Run `npm run translate <username> <password>` to pull down the translated xml files.
3. Commit and push your change.


##ADB

If you don't have `adb` in your path add it to your `~/.bash_profile` or similar file:

`export PATH=/Users/<your-username>/Library/Android/sdk/platform-tools:$PATH`

- **Installing an apk onto your device:**  
  `adb install -r ./LinkBubble/build/outputs/apk/LinkBubble-playstore-release.apk`
- **Getting a list of devices:**
  `adb devices`
