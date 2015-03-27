package net.bytebuddy.matcher;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class MethodSortMatcherObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodSortMatcher.Sort.class).apply();
    }
}
