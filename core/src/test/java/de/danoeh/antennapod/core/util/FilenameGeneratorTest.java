package de.danoeh.antennapod.core.util;

import androidx.test.platform.app.InstrumentationRegistry;
import android.text.TextUtils;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class FilenameGeneratorTest {

    public FilenameGeneratorTest() {
        super();
    }

    @Test
    public void testGenerateFileName() throws Exception {
        String result = FileNameGenerator.generateFileName("abc abc");
        assertEquals(result, "abc abc");
        createFiles(result);
    }

    @Test
    public void testGenerateFileName1() throws Exception {
        String result = FileNameGenerator.generateFileName("ab/c: <abc");
        assertEquals(result, "abc abc");
        createFiles(result);
    }

    @Test
    public void testGenerateFileName2() throws Exception {
        String result = FileNameGenerator.generateFileName("abc abc ");
        assertEquals(result, "abc abc");
        createFiles(result);
    }

    @Test
    public void testFeedTitleContainsApostrophe() {
        String result = FileNameGenerator.generateFileName("Feed's Title ...");
        assertEquals("Feeds Title", result);
    }

    @Test
    public void testFeedTitleContainsDash() {
        String result = FileNameGenerator.generateFileName("Left - Right");
        assertEquals("Left - Right", result);
    }

    @Test
    public void testFeedTitleContainsAccents() {
        String result = FileNameGenerator.generateFileName("Äàáâãå");
        assertEquals("Aaaaaa", result);
    }

    @Test
    public void testInvalidInput() {
        String result = FileNameGenerator.generateFileName("???");
        assertFalse(TextUtils.isEmpty(result));
    }

    @Test
    public void testLongFilename() throws Exception {
        String longName = StringUtils.repeat("x", 20 + FileNameGenerator.MAX_FILENAME_LENGTH);
        String result = FileNameGenerator.generateFileName(longName);
        assertTrue(result.length() <= FileNameGenerator.MAX_FILENAME_LENGTH);
        createFiles(result);
    }

    @Test
    public void testLongFilenameNotEquals() {
        // Verify that the name is not just trimmed and different suffixes end up with the same name
        String longName = StringUtils.repeat("x", 20 + FileNameGenerator.MAX_FILENAME_LENGTH);
        String result1 = FileNameGenerator.generateFileName(longName + "a");
        String result2 = FileNameGenerator.generateFileName(longName + "b");
        assertNotEquals(result1, result2);
    }

    /**
     * Tests if files can be created.
     */
    private void createFiles(String name) throws Exception {
        File cache = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalCacheDir();
        File testFile = new File(cache, name);
        assertTrue(testFile.mkdir());
        assertTrue(testFile.exists());
        assertTrue(testFile.delete());
        assertTrue(testFile.createNewFile());
    }
}
