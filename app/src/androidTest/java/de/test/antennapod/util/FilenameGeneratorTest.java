package de.test.antennapod.util;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

import de.danoeh.antennapodSA.core.util.FileNameGenerator;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SmallTest
public class FilenameGeneratorTest {

    private static final String VALID1 = "abc abc";
    private static final String INVALID1 = "ab/c: <abc";
    private static final String INVALID2 = "abc abc ";

    public FilenameGeneratorTest() {
        super();
    }

    @Test
    public void testGenerateFileName() throws IOException {
        String result = FileNameGenerator.generateFileName(VALID1);
        assertEquals(result, VALID1);
        createFiles(result);
    }

    @Test
    public void testGenerateFileName1() throws IOException {
        String result = FileNameGenerator.generateFileName(INVALID1);
        assertEquals(result, VALID1);
        createFiles(result);
    }

    @Test
    public void testGenerateFileName2() throws IOException {
        String result = FileNameGenerator.generateFileName(INVALID2);
        assertEquals(result, VALID1);
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
    public void testInvalidInput() {
        String result = FileNameGenerator.generateFileName("???");
        assertFalse(TextUtils.isEmpty(result));
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

    @After
    public void tearDown() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File f = new File(context.getExternalCacheDir(), VALID1);
        f.delete();
    }

}
