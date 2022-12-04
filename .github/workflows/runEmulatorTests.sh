#!/bin/zsh

set -o pipefail

runTests() {
    ./gradlew connectedPlayDebugAndroidTest connectedDebugAndroidTest
}

# Retry tests to make them less flaky
runTests || runTests || runTests
