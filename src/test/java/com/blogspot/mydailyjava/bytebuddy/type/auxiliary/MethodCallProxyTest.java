package com.blogspot.mydailyjava.bytebuddy.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassVersion;
import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.type.loading.ByteArrayClassLoader;
import org.junit.Test;
import org.mockito.asm.Opcodes;

public class MethodCallProxyTest {

    public static class Proxied {

        public void test() {
            System.out.println("y");
        }
    }

    public static class ProxiedSub extends Proxied {

        public void test() {
            System.out.println("z");
        }

        void test$proxy() {
            super.test();
        }
    }

    @Test
    public void testProxy() throws Exception {
        AuxiliaryClass methodCallProxy = new MethodCallProxy(new MethodDescription.ForMethod(Proxied.class.getDeclaredMethod("test")));
        AuxiliaryClass.Named named = methodCallProxy.name("com.Test", new ClassVersion(Opcodes.V1_6));
        ClassLoader classLoader = new ByteArrayClassLoader(getClass().getClassLoader(), "com.Test", named.make());
        Class<?> test = classLoader.loadClass("com.Test");
        Proxied proxied = new Proxied();
        test.getConstructor(Object.class).newInstance(proxied);
    }
}
