package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.CallTraceable;
import net.bytebuddy.utility.MockitoRule;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class TypeProxyTest {

    private static final String BAR = "bar", TO_STRING = "toString", EQUALS = "equals", HASH_CODE = "hashCode", CLONE = "clone";
    private static final String QUX = "qux", ACCESSOR = "accessor";
    private static final boolean EQUALS_RESULT = true;
    private static final int HASH_CODE_RESULT = 41;
    private final String methodName;
    private final Class<?>[] parameterTypes;
    private final Object[] methodArguments;
    private final Matcher<?> matcher;
    @Rule
    public TestRule mockitoRule = new MockitoRule(this);
    @Mock
    private AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

    public TypeProxyTest(String methodName, Class<?>[] parameterTypes, Object[] methodArguments, Matcher<?> matcher) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.methodArguments = methodArguments;
        this.matcher = matcher;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BAR, new Class<?>[0], new Object[0], nullValue()},
                {TO_STRING, new Class<?>[0], new Object[0], is(TO_STRING)},
                {EQUALS, new Class<?>[]{Object.class}, new Object[]{EQUALS}, is(EQUALS_RESULT)},
                {HASH_CODE, new Class<?>[0], new Object[0], is(HASH_CODE_RESULT)},
                {CLONE, new Class<?>[0], new Object[0], is(CLONE)}
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethodOfTypeProxy() throws Exception {
        Class<Foo> proxy = makeProxyType(Foo.class, Bar.class);
        Field field = proxy.getDeclaredField(TypeProxy.INSTANCE_FIELD);
        field.setAccessible(true);
        Bar proxyTarget = new Bar();
        Constructor<Foo> proxyConstructor = proxy.getDeclaredConstructor();
        proxyConstructor.setAccessible(true);
        Foo proxyInstance = proxyConstructor.newInstance();
        field.set(proxyInstance, proxyTarget);
        Method method = proxy.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        assertThat(method.invoke(proxyInstance, methodArguments), is((Matcher) matcher));
        proxyTarget.callTraceable.assertOnlyCall(methodName, methodArguments);
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> makeProxyType(Class<T> proxyType, Class<?> instrumentedType) {
        when(methodAccessorFactory.requireAccessorMethodFor(any(MethodDescription.class),
                eq(AuxiliaryType.MethodAccessorFactory.LookupMode.Default.BY_SIGNATURE))).then(new SameSignatureAnswer());
        String auxiliaryTypeName = instrumentedType.getName() + "$" + QUX;
        DynamicType dynamicType = new TypeProxy(
                new TypeDescription.ForLoadedType(proxyType),
                new TypeDescription.ForLoadedType(instrumentedType),
                true).make(auxiliaryTypeName, ClassFileVersion.forCurrentJavaVersion(), methodAccessorFactory);
        DynamicType.Unloaded<?> unloaded = (DynamicType.Unloaded<?>) dynamicType;
        Class<?> auxiliaryType = unloaded.load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION).getLoaded();
        assertThat(auxiliaryType.getName(), is(auxiliaryTypeName));
        assertThat(auxiliaryType.getModifiers(), is(Opcodes.ACC_SYNTHETIC));
        assertThat(auxiliaryType.getDeclaredConstructors().length, is(1));
        assertThat(auxiliaryType.getDeclaredFields().length, is(1));
        return (Class<T>) auxiliaryType;
    }

    public static class Bar extends Foo {

        public CallTraceable callTraceable = new CallTraceable();

        void foo() {
            /* do nothing */
        }

        void accessorBar() {
            callTraceable.register(BAR);
        }

        String accessorToString() {
            callTraceable.register(TO_STRING);
            return TO_STRING;
        }

        boolean accessorEquals(Object o) {
            callTraceable.register(EQUALS, o);
            return EQUALS_RESULT;
        }

        int accessorHashCode() {
            callTraceable.register(HASH_CODE);
            return HASH_CODE_RESULT;
        }

        Object accessorClone() {
            callTraceable.register(CLONE);
            return CLONE;
        }
    }

    public static class Foo {

        public void bar() {
            /* do nothing */
        }
    }

    private static class SameSignatureAnswer implements Answer<MethodDescription> {

        @Override
        public MethodDescription answer(InvocationOnMock invocation) throws Throwable {
            MethodDescription source = ((MethodDescription) invocation.getArguments()[0]);
            for (Method method : Bar.class.getDeclaredMethods()) {
                if (method.getName().equals(ACCESSOR
                        + source.getName().substring(0, 1).toUpperCase()
                        + source.getName().substring(1))) {
                    return new MethodDescription.ForLoadedMethod(method);
                }
            }
            throw new AssertionError("Cannot find delegate for " + source);
        }
    }
}
