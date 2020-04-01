runTests() {
    ./gradlew connectedPlayDebugAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.notAnnotation=de.test.antennapod.IgnoreOnCi \
        | grep -v "V/InstrumentationResultParser: INSTRUMENTATION_STATUS"
}

# Retry tests to make them less flaky
runTests || runTests || runTests
