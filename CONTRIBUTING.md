# Contributing guidelines

## Tooling

IDE: We use Android Studio, download it here: http://developer.android.com/tools/studio/

ADB Idea: A useful plugin for working with android apps: https://github.com/pbreault/adb-idea


## Incrementing the patch number

Before each release be sure to increment the versionPatch number within build.gradle. This number should be reset to zero after a minor or major version bump (typically during a release). The patch number is reported to crashlytics and can help pinpoint what commits caused a crash, and to the beta community.
