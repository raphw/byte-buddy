package net.bytebuddy.matcher;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class StringMatcherObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(StringMatcher.Mode.class).apply();
    }
}
