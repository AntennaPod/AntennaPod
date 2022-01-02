package de.danoeh.antennapod.core.sync;

import junit.framework.TestCase;

public class GuidValidatorTest extends TestCase {

    public void testIsValidGuid() {
        assertTrue(GuidValidator.isValidGuid("skfjsdvgsd"));
    }

    public void testIsInvalidGuid() {
        assertFalse(GuidValidator.isValidGuid(""));
        assertFalse(GuidValidator.isValidGuid(" "));
        assertFalse(GuidValidator.isValidGuid("\n"));
        assertFalse(GuidValidator.isValidGuid(" \n"));
        assertFalse(GuidValidator.isValidGuid(null));
        assertFalse(GuidValidator.isValidGuid("null"));
    }
}