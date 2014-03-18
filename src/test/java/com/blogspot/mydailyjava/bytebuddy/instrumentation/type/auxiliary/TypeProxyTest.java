package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import org.junit.Test;

public class TypeProxyTest {

    private static final String QUX = "qux";

    public static class Foo extends Bar {

        public void foo() {
            /* do nothing */
        }
    }

    public static class Bar {

        public void bar() {

        }
    }

    @Test
    public void testClass() throws Exception {
        String targetName = getClass().getName() + "$" + QUX;
//        new TypeProxy(new TypeDescription.ForLoadedType(Foo.class), new TypeDescription.ForLoadedType(Bar.class), true)
//                .make(targetName, ClassFormatVersion.forCurrentJavaVersion(), );

    }
}
