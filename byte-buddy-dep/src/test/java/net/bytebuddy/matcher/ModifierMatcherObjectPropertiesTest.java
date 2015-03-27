package net.bytebuddy.matcher;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class ModifierMatcherObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ModifierMatcher.Mode.class).apply();
    }
}
