package de.danoeh.antennapod.core.util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link DateUtils}.
 */
public class DateUtilsTest {

    @Test
    public void testParseDateWithMicroseconds() {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 13, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 963);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.963870");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseDateWithCentiseconds() {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 13, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 960);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.96");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseDateWithDeciseconds() {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 13, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 900);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.9");
        assertEquals(expected.getTime() / 1000, actual.getTime() / 1000);
        assertEquals(900, actual.getTime() % 1000);
    }

    @Test
    public void testParseDateWithMicrosecondsAndTimezone() {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 6, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 963);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.963870 +0700");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseDateWithCentisecondsAndTimezone() {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 6, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 960);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.96 +0700");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseDateWithDecisecondsAndTimezone() {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 6, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 900);
        Date actual = DateUtils.parse("2015-03-28T13:31:04.9 +0700");
        assertEquals(expected.getTime() / 1000, actual.getTime() / 1000);
        assertEquals(900, actual.getTime() % 1000);
    }

    @Test
    public void testParseDateWithTimezoneName() {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 6, 31, 4);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis());
        Date actual = DateUtils.parse("Sat, 28 Mar 2015 01:31:04 EST");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseDateWithTimezoneName2() {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 6, 31, 0);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis());
        Date actual = DateUtils.parse("Sat, 28 Mar 2015 01:31 EST");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseDateWithTimeZoneOffset() {
        GregorianCalendar exp = new GregorianCalendar(2015, 2, 28, 12, 16, 12);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis());
        Date actual = DateUtils.parse("Sat, 28 March 2015 08:16:12 -0400");
        assertEquals(expected, actual);
    }

    @Test
    public void testAsctime() {
        GregorianCalendar exp = new GregorianCalendar(2011, 4, 25, 12, 33, 0);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis());
        Date actual = DateUtils.parse("Wed, 25 May 2011 12:33:00");
        assertEquals(expected, actual);
    }

    @Test
    public void testMultipleConsecutiveSpaces() {
        GregorianCalendar exp = new GregorianCalendar(2010, 2, 23, 6, 6, 26);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis());
        Date actual = DateUtils.parse("Tue,  23 Mar   2010 01:06:26 -0500");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseDateWithNoTimezonePadding() {
        GregorianCalendar exp = new GregorianCalendar(2017, 1, 22, 22, 28, 0);
        exp.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected = new Date(exp.getTimeInMillis() + 2);
        Date actual = DateUtils.parse("2017-02-22T14:28:00.002-08:00");
        assertEquals(expected, actual);
    }

    /**
     * Requires Android platform. Root cause: {@link DateUtils} implementation makes
     * use of ISO 8601 time zone, which does not work on standard JDK.
     *
     * @see #testParseDateWithNoTimezonePadding()
     */
    @Test
    public void testParseDateWithForCest() {
        GregorianCalendar exp1 = new GregorianCalendar(2017, 0, 28, 22, 0, 0);
        exp1.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected1 = new Date(exp1.getTimeInMillis());
        Date actual1 = DateUtils.parse("Sun, 29 Jan 2017 00:00:00 CEST");
        assertEquals(expected1, actual1);

        GregorianCalendar exp2 = new GregorianCalendar(2017, 0, 28, 23, 0, 0);
        exp2.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expected2 = new Date(exp2.getTimeInMillis());
        Date actual2 = DateUtils.parse("Sun, 29 Jan 2017 00:00:00 CET");
        assertEquals(expected2, actual2);
    }

    @Test
    public void testParseDateWithIncorrectWeekday() {
        GregorianCalendar exp1 = new GregorianCalendar(2014, 9, 8, 9, 0, 0);
        exp1.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expected = new Date(exp1.getTimeInMillis());
        Date actual = DateUtils.parse("Thu, 8 Oct 2014 09:00:00 GMT"); // actually a Wednesday
        assertEquals(expected, actual);
    }

    @Test
    public void testParseDateWithBadAbbreviation() {
        GregorianCalendar exp1 = new GregorianCalendar(2014, 8, 8, 0, 0, 0);
        exp1.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expected = new Date(exp1.getTimeInMillis());
        Date actual = DateUtils.parse("Mon, 8 Sept 2014 00:00:00 GMT"); // should be Sep
        assertEquals(expected, actual);
    }

    @Test
    public void testParseDateWithTwoTimezones() {
        final GregorianCalendar exp1 = new GregorianCalendar(2015, Calendar.MARCH, 1, 1, 0, 0);
        exp1.setTimeZone(TimeZone.getTimeZone("GMT-4"));
        final Date expected = new Date(exp1.getTimeInMillis());
        final Date actual = DateUtils.parse("Sun 01 Mar 2015 01:00:00 GMT-0400 (EDT)");
        assertEquals(expected, actual);
    }
}
