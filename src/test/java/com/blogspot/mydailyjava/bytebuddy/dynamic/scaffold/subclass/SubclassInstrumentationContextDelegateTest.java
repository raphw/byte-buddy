package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.ClassLoadingStrategy;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.TypeWriter;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.*;

public class SubclassInstrumentationContextDelegateTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "bar", TO_STRING = "toString";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;
    @Mock
    private MethodDescription firstMethod, secondMethod;
    @Mock
    private TypeDescription firstMethodReturnType, secondMethodReturnType, superType;
    @Mock
    private TypeList firstMethodParameters, secondMethodParameters;
    @Mock
    private MethodList methodList;

    private SubclassInstrumentationContextDelegate delegate;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(firstMethod.getReturnType()).thenReturn(firstMethodReturnType);
        when(firstMethod.getParameterTypes()).thenReturn(firstMethodParameters);
        when(firstMethod.getDeclaringType()).thenReturn(superType);
        when(firstMethod.getInternalName()).thenReturn(QUX);
        when(firstMethodReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondMethod.getReturnType()).thenReturn(secondMethodReturnType);
        when(secondMethod.getParameterTypes()).thenReturn(secondMethodParameters);
        when(secondMethod.getDeclaringType()).thenReturn(superType);
        when(secondMethod.getInternalName()).thenReturn(BAZ);
        when(superType.isAssignableFrom(superType)).thenReturn(true);
        when(secondMethodReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(instrumentedType.detach()).thenReturn(instrumentedType);
        when(instrumentedType.getReachableMethods()).thenReturn(methodList);
        when(instrumentedType.getSupertype()).thenReturn(superType);
        when(methodList.filter(any(MethodMatcher.class))).thenReturn(methodList);
        when(methodList.iterator()).thenReturn(Arrays.<MethodDescription>asList().iterator());
        delegate = new SubclassInstrumentationContextDelegate(instrumentedType,
                BridgeMethodResolver.Simple.Factory.FAIL_FAST,
                FOO);
    }

    @Test
    public void testIteratorIsEmpty() throws Exception {
        assertThat(delegate.getProxiedMethods().iterator().hasNext(), is(false));
    }

    @Test
    public void testProxyMethodRegistration() throws Exception {
        MethodDescription firstProxyMethod = delegate.requireAccessorMethodFor(firstMethod);
        assertThat(firstProxyMethod.isStatic(), is(false));
        assertThat(firstProxyMethod, not(is(firstMethod)));
        MethodDescription secondProxyMethod = delegate.requireAccessorMethodFor(secondMethod);
        assertThat(secondProxyMethod, not(is(secondMethod)));
        assertThat(delegate.requireAccessorMethodFor(firstMethod), is(firstProxyMethod));
        Iterator<MethodDescription> iterator = delegate.getProxiedMethods().iterator();
        assertThat(iterator.hasNext(), is(true));
        MethodDescription next = iterator.next();
        assertThat(next, is(firstProxyMethod));
        assertThat(delegate.target(next), notNullValue());
        assertThat(delegate.target(next).getAttributeAppender(), notNullValue());
        assertThat(delegate.target(next).getByteCodeAppender(), notNullValue());
        assertThat(delegate.target(next).isDefineMethod(), is(true));
        assertThat(iterator.hasNext(), is(true));
        next = iterator.next();
        assertThat(next, is(secondProxyMethod));
        assertThat(delegate.target(next), notNullValue());
        assertThat(delegate.target(next).getAttributeAppender(), notNullValue());
        assertThat(delegate.target(next).getByteCodeAppender(), notNullValue());
        assertThat(delegate.target(next).isDefineMethod(), is(true));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testProxyMethodCreation() throws Exception {
        TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
        when(instrumentedType.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        TypeInitializer typeInitializer = mock(TypeInitializer.class);
        when(instrumentedType.getTypeInitializer()).thenReturn(typeInitializer);
        when(instrumentedType.getName()).thenReturn(BAR);
        when(instrumentedType.getInternalName()).thenReturn(BAR);
        when(instrumentedType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(instrumentedType.getSupertype()).thenReturn(objectType);
        TypeList interfaceTypes = mock(TypeList.class);
        when(instrumentedType.getInterfaces()).thenReturn(interfaceTypes);
        Instrumentation.Context instrumentationContext = mock(Instrumentation.Context.class);
        MethodDescription proxyMethod = delegate.requireAccessorMethodFor(objectType.getDeclaredMethods()
                .filter(named(TO_STRING)).getOnly());
        TypeWriter.InGeneralPhase<?> typeWriter = new TypeWriter.Builder<Object>(instrumentedType,
                instrumentationContext,
                ClassFormatVersion.forCurrentJavaVersion())
                .build(new ClassVisitorWrapper.Chain());
        Class<?> loaded = typeWriter
                .methods()
                .write(delegate.getProxiedMethods(), delegate)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(loaded.getName(), is(BAR));
        assertThat(loaded.getDeclaredFields().length, is(0));
        assertThat(loaded.getDeclaredMethods().length, is(1));
        assertThat(loaded.getDeclaredConstructors().length, is(0));
        Constructor<?> constructor = ReflectionFactory.getReflectionFactory()
                .newConstructorForSerialization(loaded, Object.class.getDeclaredConstructor());
        Object instance = constructor.newInstance();
        Method loadedProxyMethod = loaded.getDeclaredMethods()[0];
        loadedProxyMethod.setAccessible(true);
        assertThat((String) loadedProxyMethod.invoke(instance), is(instance.toString()));
        assertThat(loadedProxyMethod.getName(), is(proxyMethod.getName()));
        assertThat(loadedProxyMethod.getModifiers(), is(proxyMethod.getModifiers()));
        verify(instrumentationContext).getRegisteredAuxiliaryTypes();
        verifyNoMoreInteractions(instrumentationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsExceptionForUnknownMethod() throws Exception {
        delegate.target(firstMethod);
    }
}
