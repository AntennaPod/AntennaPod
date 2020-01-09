runTests() {
    ./gradlew connectedPlayDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notAnnotation=de.test.antennapod.IgnoreOnCi 
}

# Retry tests to make them less flaky
runTests || runTests || runTests
