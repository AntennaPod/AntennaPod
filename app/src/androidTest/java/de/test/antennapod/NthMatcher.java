package de.test.antennapod;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.concurrent.atomic.AtomicInteger;

public class NthMatcher {
    public static <T> Matcher<T> first(final Matcher<T> matcher) {
        return nth(matcher, 1);
    }

    public static <T> Matcher<T> nth(final Matcher<T> matcher, final int index) {
        return new BaseMatcher<T>() {
            AtomicInteger count = new AtomicInteger(0);

            @Override
            public boolean matches(final Object item) {
                if (matcher.matches(item)) {
                    return count.incrementAndGet() == index;
                }
                return false;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Item #" + index + " ");
                description.appendDescriptionOf(matcher);
            }
        };
    }
}
