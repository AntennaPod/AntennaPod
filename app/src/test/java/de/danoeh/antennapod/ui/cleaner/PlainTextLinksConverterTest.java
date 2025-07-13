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

        final String text = "artist here:  www.sorayaperry.com";
        final String expected = "artist here: <a href=\"http://www.sorayaperry.com\">www.sorayaperry.com</a>";
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

        final String linkWithComma = "https://wikipedia.org/wiki/%D0%9E%D1%82%D0%B5%D1%86_(%D1%84%D0%B8%D0%BB%D1%8C%D0%BC,_2020)";
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
                "<a href=\"https://newsletter.pragmaticengineer.com/p/ai-fakers?utm_source=substack&amp;utm_medium=email\">AI interview</a> - <em>01:57:01</em>",
                "sign up for our premium feed here! <a href=\"https://www.google.com/url?q=https://hyperfixedpod.com/join&amp;source=gmail-imap&amp;ust=1733759237000000&amp;usg=AOvVaw1wVGwumKqSOTeWJgzEv9s9\"><strong>https://hyperfixedpod.com/join</strong></a>",
                "you can do so here:<a href=\"https://www.google.com/url?q=https://hyperfixedpod.com/button&amp;source=gmail-imap&amp;ust=1733759237000000&amp;usg=AOvVaw0l32E4Vjw--CXD5HqitATY\"><strong>https://hyperfixedpod.com/button</strong></a>",
                "LINKS:<a href=\"https://www.google.com/url?q=https://buttonmuseum.org/&amp;source=gmail-imap&amp;ust=1733759237000000&amp;usg=AOvVaw3JwxOdV3y8OmyqGJxenALD\"><strong>Busy Beaver Button Museum</strong></a>",
                "<a href=\"https://www.google.com/url?q=https://buttonmuseum.org/buttons/ask-me-chili-cheese-fries&amp;source=gmail-imap&amp;ust=1733759237000000&amp;usg=AOvVaw2oFNwzuvrfrokwHf6zq1P4\"><strong>The Button in Question</strong></a>",
                "<p><a href=\"https://pbs.twimg.com/media/FN_lcifXIAIzV2i?format=png&amp;name=900x900\">A picture of the photo in question</a></p>",
                "<a href=\"https://www.youtube.com/redirect?event=video_description&amp;redir_token=QUFFLUhqa3JDNFl&amp;q=https%3A%2F%2Fexample.com%2Fshop%2Fbook%2F&amp;v=4iOzkYTrjzg\">https://example.com/shop/book/</a>",
                "<a href=\"https://www.youtube.com/redirect?event=video_description&amp;redir_token=QUFFLUhqbnpVQWs&amp;q=https%3A%2F%2Fexample.onelink.me%2FyH6x%2Fgx5ywe7g&amp;v=yIbY7x5zQO8\">https://example.onelink.me/yH6x/gx5ywe7g</a>",
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

        final String textWithDots = "\"Once Upon a Time...in Hollywood\"";
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