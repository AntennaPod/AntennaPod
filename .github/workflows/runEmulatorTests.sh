#!/bin/bash

set -o pipefail
adb logcat -c

runTests() {
    ./gradlew connectedPlayDebugAndroidTest connectedDebugAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.notAnnotation=de.test.antennapod.IgnoreOnCi
}

# Retry tests to make them less flaky
if runTests || runTests || runTests; then
    echo "Tests succeeded"
else
    echo "Tests FAILED. Dumping logcat:"
    adb logcat -d > app/build/reports/androidTests/logcat.txt
    exit 1
fi

