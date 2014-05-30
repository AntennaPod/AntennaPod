package instrumentationTest.de.test.antennapod.ui;

import android.test.ActivityInstrumentationTestCase2;
import com.robotium.solo.Solo;
import de.danoeh.antennapod.activity.MainActivity;

/**
 * User interface tests for MainActivity
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;

    public MainActivityTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }


}
