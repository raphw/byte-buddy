package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;
import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.takesArguments;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BridgeMethodResolverDefaultTest {

    private static final String FOO = "foo", BAR = "bar";

    private static class Foo {

        public Object foo() {
            return null;
        }

        public Object bar(Object o) {
            return null;
        }

        public Object bar() {
            return null;
        }
    }

    private static class Bar extends Foo {

        @Override
        public String foo() {
            return null;
        }

        @Override
        public Object bar(Object o) {
            return null;
        }
    }

    private AuxiliaryType.MethodAccessorFactory.BridgeMethodResolver bridgeMethodResolver;

    @Before
    public void setUp() throws Exception {
        bridgeMethodResolver = new AuxiliaryType
                .MethodAccessorFactory.BridgeMethodResolver.Default(new TypeDescription.ForLoadedType(Bar.class)
                .getReachableMethods());
    }

    @Test
    public void testResolverRelevant() throws Exception {
        MethodDescription resolved = bridgeMethodResolver.resolveCallTo(new TypeDescription.ForLoadedType(Foo.class)
                .getReachableMethods().filter(named(FOO)).getOnly());
        assertThat(resolved.getReturnType().represents(String.class), is(true));
    }

    @Test
    public void testResolverIrrelevant() throws Exception {
        MethodDescription resolved = bridgeMethodResolver.resolveCallTo(new TypeDescription.ForLoadedType(Foo.class)
                .getReachableMethods().filter(named(BAR).and(takesArguments(1))).getOnly());
        assertThat(resolved.getReturnType().represents(Object.class), is(true));
    }

    @Test
    public void testResolverNone() throws Exception {
        MethodDescription resolved = bridgeMethodResolver.resolveCallTo(new TypeDescription.ForLoadedType(Foo.class)
                .getReachableMethods().filter(named(BAR).and(takesArguments(0))).getOnly());
        assertThat(resolved.getReturnType().represents(Object.class), is(true));
    }
}
