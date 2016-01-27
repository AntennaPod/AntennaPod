package de.danoeh.antennapod.core.tests.util;


import android.test.AndroidTestCase;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import de.danoeh.antennapod.core.util.DateUtils;

public class DateUtilsTest extends AndroidTestCase {

    public void testParseDateWithMicroseconds() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 13, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 963);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.963870");
        assertEquals(expected, actual);
    }

    public void testParseDateWithCentiseconds() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 13, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 960);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.96");
        assertEquals(expected, actual);
    }

    public void testParseDateWithDeciseconds() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 13, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 900);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.9");
        assertEquals(expected.getTime()/1000, actual.getTime()/1000);
        assertEquals(900, actual.getTime()%1000);
    }

    public void testParseDateWithMicrosecondsAndTimezone() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 6, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 963);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.963870 +0700");
        assertEquals(expected, actual);
    }

    public void testParseDateWithCentisecondsAndTimezone() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 6, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 960);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.96 +0700");
        assertEquals(expected, actual);
    }

    public void testParseDateWithDecisecondsAndTimezone() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 6, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 900);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.9 +0700");
        assertEquals(expected.getTime()/1000, actual.getTime()/1000);
        assertEquals(900, actual.getTime()%1000);
    }

    public void testParseDateWithTimezoneName() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 6, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis());
        Date actual = DateUtils.parse("Sat, 28 Mar 2015 01:31:04 EST");
        assertEquals(expected, actual);
    }

    public void testParseDateWithTimezoneName2() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 6, 31, 0);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis());
        Date actual = DateUtils.parse("Sat, 28 Mar 2015 01:31 EST");
        assertEquals(expected, actual);
    }

    public void testParseDateWithTimeZoneOffset() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 12, 16, 12);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis());
        Date actual = DateUtils.parse("Sat, 28 March 2015 08:16:12 -0400");
        assertEquals(expected, actual);
    }

    public void testAsctime() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2011, 4, 25, 12, 33, 00);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis());
        Date actual = DateUtils.parse("Wed, 25 May 2011 12:33:00");
        assertEquals(expected, actual);
    }

    public void testMultipleConsecutiveSpaces() throws Exception {
        GregorianCalendar exp = new GregorianCalendar(2010, 2, 23, 6, 6, 26);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis());
        Date actual = DateUtils.parse("Tue,  23 Mar   2010 01:06:26 -0500");
        assertEquals(expected, actual);
    }

}
