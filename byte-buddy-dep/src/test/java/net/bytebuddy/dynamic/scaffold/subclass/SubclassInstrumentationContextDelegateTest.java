package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.SuperMethodCall;
import net.bytebuddy.instrumentation.TypeInitializer;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.any;
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
    @Mock
    private MethodLookupEngine methodLookupEngine;

    private SubclassInstrumentationContextDelegate delegate;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(firstMethod.getReturnType()).thenReturn(firstMethodReturnType);
        when(firstMethod.getParameterTypes()).thenReturn(firstMethodParameters);
        when(firstMethod.getDeclaringType()).thenReturn(superType);
        when(firstMethod.getInternalName()).thenReturn(QUX);
        when(firstMethod.getUniqueSignature()).thenReturn(FOO);
        when(firstMethod.isSpecializableFor(superType)).thenReturn(true);
        when(firstMethodReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondMethod.getReturnType()).thenReturn(secondMethodReturnType);
        when(secondMethod.getParameterTypes()).thenReturn(secondMethodParameters);
        when(secondMethod.getDeclaringType()).thenReturn(superType);
        when(secondMethod.getInternalName()).thenReturn(BAZ);
        when(secondMethod.getUniqueSignature()).thenReturn(BAZ);
        when(secondMethod.isSpecializableFor(superType)).thenReturn(true);
        MethodDescription toStringMethod = new TypeDescription.ForLoadedType(Object.class)
                .getDeclaredMethods().filter(named(TO_STRING)).getOnly();
        when(superType.isAssignableFrom(superType)).thenReturn(true);
        when(secondMethodReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(instrumentedType.detach()).thenReturn(instrumentedType);
        when(methodLookupEngine.getReachableMethods(instrumentedType)).thenReturn(methodList);
        when(instrumentedType.getSupertype()).thenReturn(superType);
        when(methodList.filter(isBridge())).thenReturn(new MethodList.Empty());
        when(methodList.iterator()).thenReturn(Arrays.asList(firstMethod, secondMethod, toStringMethod).iterator());
        delegate = new SubclassInstrumentationContextDelegate(instrumentedType,
                methodLookupEngine,
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
                ClassFileVersion.forCurrentJavaVersion())
                .build(new ClassVisitorWrapper.Chain());
        TypeWriter.MethodPool constructorPool = mock(TypeWriter.MethodPool.class);
        TypeWriter.MethodPool.Entry entry = mock(TypeWriter.MethodPool.Entry.class);
        when(entry.isDefineMethod()).thenReturn(true);
        when(entry.getAttributeAppender()).thenReturn(MethodAttributeAppender.NoOp.INSTANCE);
        ByteCodeAppender superMethodCallAppender = SuperMethodCall.INSTANCE.appender(instrumentedType);
        when(entry.getByteCodeAppender()).thenReturn(superMethodCallAppender);
        when(constructorPool.target(any(MethodDescription.class))).thenReturn(entry);
        Class<?> loaded = typeWriter
                .methods()
                .write(new TypeDescription.ForLoadedType(Object.class).getDeclaredMethods().filter(isConstructor()), constructorPool)
                .write(delegate.getProxiedMethods(), delegate)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(loaded.getName(), is(BAR));
        assertThat(loaded.getDeclaredFields().length, is(0));
        assertThat(loaded.getDeclaredMethods().length, is(1));
        assertThat(loaded.getDeclaredConstructors().length, is(1));
        Object instance = loaded.newInstance();
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
