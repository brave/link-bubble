# Link Bubble Browser

##Install Instructions

`git clone git@github.com:brave/LinkBubble.git`


##Building LinkBubble Free

Open `./Application/` in Android Studio and build.

##Building LinkBubble Pro

Open `./ProKey/` in Android Studio and build.

##Building release build

Ensure each of the following variables are added to your environment:  

    export LINK_BUBBLE_KEYSTORE_LOCATION=/path/to/linkbubble_play_keystore  
    export LINK_BUBBLE_KEYSTORE_PASSWORD='keystore-password-here'  
    export LINK_BUBBLE_KEY_ALIAS='linkbubble'  
    export LINK_BUBBLE_KEY_PASSWORD='key-password'

Navigate to one of the following folders:    

- Link Bubble:   
  `cd ./Application/`

- Link Bubble Pro:  
  `cd ./ProKey/`

Run the following command:

`./gradlew assemblePlaystoreRelease`
 
The build process should take less than 1 minute.  Installing the app on a device can be done with:
- Link Bubble:  
  `adb install -r ./LinkBubble/build/outputs/apk/LinkBubble-playstore-release.apk`

- Link Bubble Pro:  
  `adb install -r ./LinkBubblePro/build/outputs/apk/LinkBubblePro-playstore-release.apk`

If you get an error about similar to:

> Failure [INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES]

Try uninstalling the application which already exists on your plugged in device.

##ADB

If you don't have `adb` in your path add it to your `~/.bash_profile` or similar file:

`export PATH=/Users/<your-username>/Library/Android/sdk/platform-tools:$PATH`

- **Getting a list of devices:**
  `adb devices`
