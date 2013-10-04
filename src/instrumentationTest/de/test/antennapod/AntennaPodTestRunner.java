package instrumentationTest.de.test.antennapod;

import android.test.InstrumentationTestRunner;
import android.test.suitebuilder.TestSuiteBuilder;
import android.util.Log;

import instrumentationTest.de.test.antennapod.service.download.HttpDownloaderTest;
import instrumentationTest.de.test.antennapod.util.FilenameGeneratorTest;
import junit.framework.TestSuite;

public class AntennaPodTestRunner extends InstrumentationTestRunner {

    @Override
    public TestSuite getAllTests() {
        return new TestSuiteBuilder(AntennaPodTestRunner.class).includeAllPackagesUnderHere()
        //        .excludePackages("instrumentationTest.de.test.antennapod.syndication.handler")
        //        .excludePackages("instrumentationTest.de.test.antennapod.gpodnet")
                .build();
    }
}
