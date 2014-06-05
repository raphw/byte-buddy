package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class TypeProxyEqualsHashCodeTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription first, second;
    @Mock
    private Instrumentation.Target instrumentationTarget;

    @Test
    public void testEqualsHashCode() throws Exception {
        assertThat(new TypeProxy(first, instrumentationTarget, true, false).hashCode(),
                is(new TypeProxy(first, instrumentationTarget, true, false).hashCode()));
        assertThat(new TypeProxy(first, instrumentationTarget, true, false),
                is(new TypeProxy(first, instrumentationTarget, true, false)));
        assertThat(new TypeProxy(first, instrumentationTarget, true, false).hashCode(),
                not(is(new TypeProxy(first, instrumentationTarget, false, false).hashCode())));
        assertThat(new TypeProxy(first, instrumentationTarget, true, false),
                not(is(new TypeProxy(first, instrumentationTarget, false, false))));
        assertThat(new TypeProxy(first, instrumentationTarget, true, false).hashCode(),
                not(is(new TypeProxy(second, instrumentationTarget, true, false).hashCode())));
        assertThat(new TypeProxy(first, instrumentationTarget, true, false),
                not(is(new TypeProxy(second, instrumentationTarget, true, false))));
    }

    @Test
    public void testByConstructorEqualsHashCode() throws Exception {
        assertThat(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false).hashCode(),
                is(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false).hashCode()));
        assertThat(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false),
                is(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false)));
        assertThat(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false).hashCode(),
                not(is(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), false, false).hashCode())));
        assertThat(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false),
                not(is(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), false, false))));
        assertThat(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false).hashCode(),
                not(is(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), false, false).hashCode())));
        assertThat(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false),
                not(is(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), false, false))));
        assertThat(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false).hashCode(),
                not(is(new TypeProxy.ByConstructor(second, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false).hashCode())));
        assertThat(new TypeProxy.ByConstructor(first, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false),
                not(is(new TypeProxy.ByConstructor(second, instrumentationTarget, Collections.<TypeDescription>emptyList(), true, false))));
    }

    @Test
    public void testByReflectionFactoryEqualsHashCode() throws Exception {
        assertThat(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, true, false).hashCode(),
                is(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, true, false).hashCode()));
        assertThat(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, true, false),
                is(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, true, false)));
        assertThat(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, true, false).hashCode(),
                not(is(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, false, false).hashCode())));
        assertThat(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, true, false),
                not(is(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, false, false))));
        assertThat(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, true, false).hashCode(),
                not(is(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, false, false).hashCode())));
        assertThat(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, true, false),
                not(is(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, false, false))));
        assertThat(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, true, false).hashCode(),
                not(is(new TypeProxy.ByReflectionFactory(second, instrumentationTarget, true, false).hashCode())));
        assertThat(new TypeProxy.ByReflectionFactory(first, instrumentationTarget, true, false),
                not(is(new TypeProxy.ByReflectionFactory(second, instrumentationTarget, true, false))));
    }
}
