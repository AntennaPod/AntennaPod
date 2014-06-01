package instrumentationTest.de.test.antennapod.ui;

import android.test.ActivityInstrumentationTestCase2;
import com.robotium.solo.Solo;
import de.danoeh.antennapod.activity.MainActivity;

/**
 * User interface tests for MainActivity
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;
    private UITestUtils uiTestUtils;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
        uiTestUtils = new UITestUtils(getInstrumentation().getTargetContext());
        uiTestUtils.setup();
    }

    @Override
    protected void tearDown() throws Exception {
        uiTestUtils.tearDown();
        super.tearDown();
    }


}
