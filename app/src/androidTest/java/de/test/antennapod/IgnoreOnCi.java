package de.test.antennapod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tests with this annotation are ignored on CI. This could be reasonable
 * if the performance of the CI server is not enough to provide a reliable result.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IgnoreOnCi {
}
