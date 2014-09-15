package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.HashCodeEqualsTester;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public class TypeProxyEqualsHashCodeTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription first, second;
    @Mock
    private Instrumentation.Target instrumentationTarget;

    @Test
    public void testEqualsHashCode() throws Exception {
        HashCodeEqualsTester.of(TypeProxy.class).apply();
    }

    @Test
    public void testByConstructorEqualsHashCode() throws Exception {
        HashCodeEqualsTester.of(TypeProxy.ByConstructor.class).apply();
    }

    @Test
    public void testByReflectionFactoryEqualsHashCode() throws Exception {
        HashCodeEqualsTester.of(TypeProxy.ByReflectionFactory.class).apply();
    }
}
