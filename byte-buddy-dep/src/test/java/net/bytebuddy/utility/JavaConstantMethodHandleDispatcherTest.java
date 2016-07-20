package net.bytebuddy.utility;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.Mockito.mock;

public class JavaConstantMethodHandleDispatcherTest {

    @Test(expected = IllegalStateException.class)
    public void testLegacyVmInitialization() throws Exception {
        JavaConstant.MethodHandle.Dispatcher.ForLegacyVm.INSTANCE.initialize();
    }

    @Test(expected = IllegalStateException.class)
    public void testLegacyVmPunlicLookup() throws Exception {
        JavaConstant.MethodHandle.Dispatcher.ForLegacyVm.INSTANCE.publicLookup();
    }

    @Test(expected = IllegalStateException.class)
    public void testLegacyVmLookupType() throws Exception {
        JavaConstant.MethodHandle.Dispatcher.ForLegacyVm.INSTANCE.lookupType(mock(Object.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Method> methods1 = Arrays.asList(Foo.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(JavaConstant.MethodHandle.Dispatcher.ForJava8CapableVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods1.next();
            }
        }).apply();
        final Iterator<Method> methods2 = Arrays.asList(Foo.class.getDeclaredMethods()).iterator();
        final Iterator<Constructor<?>> constructors2 = Arrays.<Constructor<?>>asList(Foo.class.getDeclaredConstructors()).iterator();
        ObjectPropertyAssertion.of(JavaConstant.MethodHandle.Dispatcher.ForJava7CapableVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods2.next();
            }
        }).create(new ObjectPropertyAssertion.Creator<Constructor<?>>() {
            @Override
            public Constructor<?> create() {
                return constructors2.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(JavaConstant.MethodHandle.Dispatcher.ForLegacyVm.class).apply();
    }

    @SuppressWarnings("unused")
    private abstract class Foo {

        private Foo(Void v) {
            /* empty */
        }

        private Foo(String s) {
            /* empty */
        }

        abstract void a1();

        abstract void a2();

        abstract void a3();

        abstract void a4();

        abstract void a5();

        abstract void a6();

        abstract void a7();

        abstract void a8();

        abstract void a9();

        abstract void a10();

        abstract void a11();

        abstract void a12();

        abstract void a13();

        abstract void a14();

        abstract void a15();

        abstract void a16();

        abstract void a17();

        abstract void a18();

        abstract void a19();

        abstract void a20();

        abstract void a21();

        abstract void a22();

        abstract void a23();
    }
}