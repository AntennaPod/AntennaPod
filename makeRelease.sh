#!/bin/sh
set -e
DEST=$2
VERSION_NAME=$1

# need to get rid of this so that it gets regenerated
# probably a way to solve this in Gradle too...
rm -f app/src/main/assets/about.html
./gradlew clean assembleDebug assembleRelease
cp app/build/outputs/apk/app-debug.apk $DEST/AntennaPod_${VERSION_NAME}_debug.apk
cp app/build/outputs/apk/app-release.apk $DEST/AntennaPod_${VERSION_NAME}_release.apk
