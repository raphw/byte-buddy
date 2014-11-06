package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class TypeProxyObjectPropertiesTest {

    @Test
    public void testEqualsHashCode() throws Exception {
        ObjectPropertyAssertion.of(TypeProxy.class).apply();
    }

    @Test
    public void testByConstructorEqualsHashCode() throws Exception {
        ObjectPropertyAssertion.of(TypeProxy.ByConstructor.class).apply();
    }

    @Test
    public void testByReflectionFactoryEqualsHashCode() throws Exception {
        ObjectPropertyAssertion.of(TypeProxy.ByReflectionFactory.class).apply();
    }
}
