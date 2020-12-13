package de.test.antennapod.util;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;

import java.io.File;
import java.io.IOException;

import de.danoeh.antennapod.core.util.FileNameGenerator;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@SmallTest
public class FilenameGeneratorTest {

    public FilenameGeneratorTest() {
        super();
    }

    @Test
    public void testGenerateFileName() throws IOException {
        String result = FileNameGenerator.generateFileName("abc abc");
        assertEquals(result, "abc abc");
        createFiles(result);
    }

    @Test
    public void testGenerateFileName1() throws IOException {
        String result = FileNameGenerator.generateFileName("ab/c: <abc");
        assertEquals(result, "abc abc");
        createFiles(result);
    }

    @Test
    public void testGenerateFileName2() throws IOException {
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
        assertFalse(result.isEmpty());
    }

    @Test
    public void testLongFilename() throws IOException {
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
     *
     * @throws IOException
     */
    private void createFiles(String name) throws IOException {
        File cache = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalCacheDir();
        File testFile = new File(cache, name);
        testFile.mkdir();
        assertTrue(testFile.exists());
        testFile.delete();
        assertTrue(testFile.createNewFile());
    }

}
