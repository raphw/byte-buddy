package net.bytebuddy.utility;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class PropertyDispatcherObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PropertyDispatcher.class).apply();
    }
}
