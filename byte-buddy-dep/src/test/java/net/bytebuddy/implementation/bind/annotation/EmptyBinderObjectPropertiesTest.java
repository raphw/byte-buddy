package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class EmptyBinderObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Empty.Binder.class).apply();
    }
}
