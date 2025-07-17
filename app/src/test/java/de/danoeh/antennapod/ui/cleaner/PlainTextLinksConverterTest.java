package de.danoeh.antennapod.ui.cleaner;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


import org.junit.Test;
import org.junit.experimental.theories.Theory;

import java.util.List;

public class PlainTextLinksConverterTest {


    @Test
    public void testConvertPlainTextLinksToHtml() {
        final String link1 = "https://url.to/link";
        final String textWithLink = "text  " + link1;
        assertEquals("text " + makeLinkHtml(link1), PlainTextLinksConverter.convertLinksToHtml(textWithLink));

        final String link2 = "https://t.me/link";
        final String textWithLink2 = "text " + link2;
        assertEquals("text " + makeLinkHtml(link2), PlainTextLinksConverter.convertLinksToHtml(textWithLink2));

        final String text = "artist here:  www.example.com";
        final String expected = "artist here: <a href=\"https://www.example.com\">www.example.com</a>";
        assertEquals(expected, PlainTextLinksConverter.convertLinksToHtml(text));

        final String textWithTwoLinks = "text " + link1 + " and " + link2;
        final String expectedTwoLinks = "text " + makeLinkHtml(link1) + " and " + makeLinkHtml(link2);
        assertEquals(expectedTwoLinks, PlainTextLinksConverter.convertLinksToHtml(textWithTwoLinks));

        final String textWithMixturePlainTextAndHtml = "text " + link1 + " and " + makeLinkHtml(link2);
        final String expectedMixture = "text " + makeLinkHtml(link1) + " and " + makeLinkHtml(link2);
        assertEquals(expectedMixture, PlainTextLinksConverter.convertLinksToHtml(textWithMixturePlainTextAndHtml));

        final String textWithSpecialChars = "text'" + link1 + " and=" + link2;
        final String expectedWithSpecialChars = "text'" + makeLinkHtml(link1) + " and=" + makeLinkHtml(link2);
        assertEquals(expectedWithSpecialChars, PlainTextLinksConverter.convertLinksToHtml(textWithSpecialChars));

        final String linkWithParams = "http://t.me/link#mark?param1=1&param2=true;param3=true";
        final String textWithParams = "text " + linkWithParams + " after-text";
        final String expectedWithParams = "text " + makeLinkHtml(linkWithParams) + " after-text";
        assertEquals(expectedWithParams, PlainTextLinksConverter.convertLinksToHtml(textWithParams));

        final String linkWithComma = "https://example.org/%D0%%86_(%D1%%BC,_2020)";
        final String textWithComma = "text " + linkWithComma;
        assertEquals("text " + makeLinkHtml(linkWithComma), PlainTextLinksConverter.convertLinksToHtml(textWithComma));

        final String linkWithDot = "https://www.ietf.org/rfc/rfc3986.txt";
        final String textWithDot = "text " + linkWithDot;
        assertEquals("text " + makeLinkHtml(linkWithDot), PlainTextLinksConverter.convertLinksToHtml(textWithDot));

        final String linkWithTilda = "https://www.example.org/valid/-~.,/url/";
        final String textWithTilda = "text " + linkWithTilda;
        assertEquals("text " + makeLinkHtml(linkWithTilda), PlainTextLinksConverter.convertLinksToHtml(textWithTilda));
    }

    @Test
    @Theory
    public void testExistingLinksArePreserved() {
        var links = List.of(
                "Click <a alt=\"abc\" href=\"http://url.to/link\">http://url.to/link, this link</a>",
                "<a href=\"http://domain.org/link\">domain.org</a>",
                "you can find it on <a href=\"http://xy.org\">our new website http://xy.org</a>",
                "you can find it on <a href=\"http://xy.org/newlanding\">our new website http://xy.org</a>",
                "<p><img src=\"https://url.to/i.jpg\" alt=\"https://url.to/i.jpg\"></p>",
                "text \n<audio src=\"https://url.to/i.mp3\" alt=\"https://url.to/i.mp3\">\n  text \n</audio>",
                "<a href=\"https://example.com/p/ai-fakers?utm_source=example&amp;utm_medium=email\">AI interview</a> - <em>01:57:01</em>",
                "sign up for our premium feed here! <a href=\"https://www.example.com/url?q=https://example.com/join&amp;source=gmail-imap&amp;ust=123&amp;usg=AOvVaw123gzEv9s9\"><strong>https://example.com/join</strong></a>",
                "you can do so here:<a href=\"https://www.example.com/url?q=https://example.com/button&amp;source=gmail-imap&amp;ust=123&amp;usg=AOvV123jw--CX123tATY\"><strong>https://example.com/button</strong></a>",
                "LINKS:<a href=\"https://www.example.com/url?q=https://example.org/&amp;source=gmail-imap&amp;ust=123&amp;usg=AOvVa123GJxenALD\"><strong>Example</strong></a>",
                "<a href=\"https://www.example.com/url?q=https://example.org/buttons/ask-me-chili-cheese-fries&amp;source=gmail-imap&amp;ust=123&amp;usg=AOvVaw2oFNwzuvrfrokwHf6zq1P4\"><strong>Example</strong></a>",
                "<p><a href=\"https://example.com/media/FN_123zV2i?format=png&amp;name=900x900\">A picture of the photo in question</a></p>",
                "<a href=\"https://www.example.com/redirect?event=video_description&amp;redir_token=123l&amp;q=https%3A%2F%2Fexample.com%2Fshop%2Fbook%2F&amp;v=4iOzkYTrjzg\">https://example.com/shop/book/</a>",
                "<a href=\"https://www.example.com/redirect?event=video_description&amp;redir_token=123Ws&amp;q=https%3A%2F%2Fexample.me%2FyH6x%2Fgx5ywe7g&amp;v=yIbY7x5zQO8\">https://example.me/yH6x/gx5ywe7g</a>",
                ""

        );
        links.forEach(link -> assertEquals(link, PlainTextLinksConverter.convertLinksToHtml(link)));
    }

    @Test
    public void testConvertToHtmlWhenNoLinksAreDetected() {
        assertNull(PlainTextLinksConverter.convertLinksToHtml((String) null));
        assertEquals("", PlainTextLinksConverter.convertLinksToHtml(""));

        final String text = "plain text";
        assertEquals(text, PlainTextLinksConverter.convertLinksToHtml(text));

        final String specialCharacters = "text with ' special \" characters !@#$%^&*()<>?123";
        var expected = specialCharacters.replace("&", "&amp;");
        expected = expected.replace("<", "&lt;");
        expected = expected.replace(">", "&gt;");
        assertEquals(expected, PlainTextLinksConverter.convertLinksToHtml(specialCharacters));

        final String textWithDots = "\"Text With...Dots Works\"";
        assertEquals(textWithDots, PlainTextLinksConverter.convertLinksToHtml(textWithDots));
    }

    /**
     * Adds {@code <a href>..</a>} around provided string
     */
    private static String makeLinkHtml(String plain) {
        if (plain == null || plain.isEmpty()) {
            return "";
        }
        String encodedPlain = plain.replace("&", "&amp;");
        return "<a href=\"" + encodedPlain + "\">" + encodedPlain + "</a>";
    }
}