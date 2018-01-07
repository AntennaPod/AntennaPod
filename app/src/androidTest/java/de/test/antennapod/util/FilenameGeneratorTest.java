package de.test.antennapod.util;

import android.test.AndroidTestCase;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

import de.danoeh.antennapod.core.util.FileNameGenerator;

public class FilenameGeneratorTest extends AndroidTestCase {

	private static final String VALID1 = "abc abc";
	private static final String INVALID1 = "ab/c: <abc";
	private static final String INVALID2 = "abc abc ";

    public FilenameGeneratorTest() {
        super();
    }

	public void testGenerateFileName() throws IOException {
		String result = FileNameGenerator.generateFileName(VALID1);
		assertEquals(result, VALID1);
		createFiles(result);
	}

	public void testGenerateFileName1() throws IOException {
		String result = FileNameGenerator.generateFileName(INVALID1);
		assertEquals(result, VALID1);
		createFiles(result);
	}
	
	public void testGenerateFileName2() throws IOException {
		String result = FileNameGenerator.generateFileName(INVALID2);
		assertEquals(result, VALID1);
		createFiles(result);
	}

	public void testFeedTitleContainsApostrophe() {
		String result = FileNameGenerator.generateFileName("Feed's Title ...");
		assertEquals("Feeds Title", result);
	}

	public void testFeedTitleContainsDash() {
		String result = FileNameGenerator.generateFileName("Left - Right");
		assertEquals("Left - Right", result);
	}

	public void testInvalidInput() {
		String result = FileNameGenerator.generateFileName("???");
		assertTrue(!TextUtils.isEmpty(result));
	}

	/**
	 * Tests if files can be created.
	 * 
	 * @throws IOException
	 */
	private void createFiles(String name) throws IOException {
		File cache = getContext().getExternalCacheDir();
		File testFile = new File(cache, name);
		testFile.mkdir();
		assertTrue(testFile.exists());
		testFile.delete();
		assertTrue(testFile.createNewFile());

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		File f = new File(getContext().getExternalCacheDir(), VALID1);
		f.delete();
	}

}
