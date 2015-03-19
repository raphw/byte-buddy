package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.ModifierContributor;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.MoreOpcodes;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TypeProxyCreationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation.Target instrumentationTarget;

    @Mock
    private TypeProxy.InvocationFactory invocationFactory;

    @Mock
    private AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

    @Mock
    private Instrumentation.SpecialMethodInvocation specialMethodInvocation;

    @Mock
    private MethodDescription proxyMethod;

    private TypeDescription foo;

    private MethodList fooMethods;

    private int modifiers;

    @Before
    public void setUp() throws Exception {
        for (ModifierContributor modifierContributor : AuxiliaryType.DEFAULT_TYPE_MODIFIER) {
            modifiers = modifiers | modifierContributor.getMask();
        }
        foo = new TypeDescription.ForLoadedType(Foo.class);
        MethodLookupEngine methodLookupEngine = MethodLookupEngine.Default.Factory.INSTANCE.make(true);
        fooMethods = methodLookupEngine.process(foo)
                .getInvokableMethods()
                .filter(isOverridable().and(not(isDefaultFinalizer())));
        ParameterList parameterList = ParameterList.Explicit.latent(proxyMethod, Arrays.asList(foo, foo, foo));
        when(proxyMethod.getParameters()).thenReturn(parameterList);
        when(proxyMethod.getDeclaringType()).thenReturn(foo);
        when(proxyMethod.getInternalName()).thenReturn(FOO);
        when(proxyMethod.getDescriptor()).thenReturn(FOO);
        when(proxyMethod.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
    }

    @Test
    public void testAllIllegal() throws Exception {
        when(instrumentationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(instrumentationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        TypeDescription dynamicType = new TypeProxy(foo,
                instrumentationTarget,
                invocationFactory,
                true,
                false)
                .make(BAR, ClassFileVersion.forCurrentJavaVersion(), methodAccessorFactory)
                .getTypeDescription();
        assertThat(dynamicType.getModifiers(), is(modifiers));
        assertThat(dynamicType.getSupertype(), is(foo));
        assertThat(dynamicType.getInterfaces(), is((TypeList) new TypeList.Empty()));
        assertThat(dynamicType.getName(), is(BAR));
        assertThat(dynamicType.getDeclaredMethods().size(), is(2));
        assertThat(dynamicType.isAssignableTo(Serializable.class), is(false));
        verifyZeroInteractions(methodAccessorFactory);
        for (MethodDescription methodDescription : fooMethods) {
            verify(invocationFactory).invoke(instrumentationTarget, foo, methodDescription);
        }
        verifyNoMoreInteractions(invocationFactory);
        verify(specialMethodInvocation, times(fooMethods.size())).isValid();
        verifyNoMoreInteractions(specialMethodInvocation);
    }

    @Test
    public void testAllLegal() throws Exception {
        when(instrumentationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(instrumentationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        TypeDescription dynamicType = new TypeProxy(foo,
                instrumentationTarget,
                invocationFactory,
                true,
                false)
                .make(BAR, ClassFileVersion.forCurrentJavaVersion(), methodAccessorFactory)
                .getTypeDescription();
        assertThat(dynamicType.getModifiers(), is(modifiers));
        assertThat(dynamicType.getSupertype(), is(foo));
        assertThat(dynamicType.getInterfaces(), is((TypeList) new TypeList.Empty()));
        assertThat(dynamicType.getName(), is(BAR));
        assertThat(dynamicType.getDeclaredMethods().size(), is(2));
        assertThat(dynamicType.isAssignableTo(Serializable.class), is(false));
        verify(methodAccessorFactory, times(fooMethods.size())).registerAccessorFor(specialMethodInvocation);
        for (MethodDescription methodDescription : fooMethods) {
            verify(invocationFactory).invoke(instrumentationTarget, foo, methodDescription);
        }
        verifyNoMoreInteractions(invocationFactory);
        verify(specialMethodInvocation, times(fooMethods.size())).isValid();
        verifyNoMoreInteractions(specialMethodInvocation);
    }

    @Test
    public void testAllLegalSerializable() throws Exception {
        when(instrumentationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(instrumentationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        TypeDescription dynamicType = new TypeProxy(foo,
                instrumentationTarget,
                invocationFactory,
                true,
                true)
                .make(BAR, ClassFileVersion.forCurrentJavaVersion(), methodAccessorFactory)
                .getTypeDescription();
        assertThat(dynamicType.getModifiers(), is(modifiers));
        assertThat(dynamicType.getSupertype(), is(foo));
        assertThat(dynamicType.getInterfaces(), is((TypeList) new TypeList.ForLoadedType(Serializable.class)));
        assertThat(dynamicType.getName(), is(BAR));
        assertThat(dynamicType.getDeclaredMethods().size(), is(2));
        assertThat(dynamicType.isAssignableTo(Serializable.class), is(true));
        verify(methodAccessorFactory, times(fooMethods.size())).registerAccessorFor(specialMethodInvocation);
        for (MethodDescription methodDescription : fooMethods) {
            verify(invocationFactory).invoke(instrumentationTarget, foo, methodDescription);
        }
        verifyNoMoreInteractions(invocationFactory);
        verify(specialMethodInvocation, times(fooMethods.size())).isValid();
        verifyNoMoreInteractions(specialMethodInvocation);
    }

    @Test
    public void testAllLegalNotIgnoreFinalizer() throws Exception {
        when(instrumentationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(instrumentationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        TypeDescription dynamicType = new TypeProxy(foo,
                instrumentationTarget,
                invocationFactory,
                false,
                false)
                .make(BAR, ClassFileVersion.forCurrentJavaVersion(), methodAccessorFactory)
                .getTypeDescription();
        assertThat(dynamicType.getModifiers(), is(modifiers));
        assertThat(dynamicType.getSupertype(), is(foo));
        assertThat(dynamicType.getInterfaces(), is((TypeList) new TypeList.Empty()));
        assertThat(dynamicType.getName(), is(BAR));
        assertThat(dynamicType.getDeclaredMethods().size(), is(2));
        assertThat(dynamicType.isAssignableTo(Serializable.class), is(false));
        verify(methodAccessorFactory, times(fooMethods.size() + 1)).registerAccessorFor(specialMethodInvocation);
        for (MethodDescription methodDescription : fooMethods) {
            verify(invocationFactory).invoke(instrumentationTarget, foo, methodDescription);
        }
        verify(invocationFactory).invoke(instrumentationTarget, foo,
                new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("finalize")));
        verifyNoMoreInteractions(invocationFactory);
        verify(specialMethodInvocation, times(fooMethods.size() + 1)).isValid();
        verifyNoMoreInteractions(specialMethodInvocation);
    }

    @Test
    public void testForConstructorConstruction() throws Exception {
        when(instrumentationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(instrumentationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        StackManipulation stackManipulation = new TypeProxy.ForSuperMethodByConstructor(foo,
                instrumentationTarget,
                Collections.<TypeDescription>singletonList(new TypeDescription.ForLoadedType(Void.class)),
                true,
                false);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Instrumentation.Context instrumentationContext = mock(Instrumentation.Context.class);
        when(instrumentationContext.register(any(AuxiliaryType.class))).thenReturn(foo);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(3));
        verify(instrumentationContext).register(any(AuxiliaryType.class));
        verifyNoMoreInteractions(instrumentationContext);
        verify(methodVisitor).visitTypeInsn(Opcodes.NEW, Type.getInternalName(Foo.class));
        verify(methodVisitor, times(2)).visitInsn(Opcodes.DUP);
        verify(methodVisitor).visitInsn(Opcodes.ACONST_NULL);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL,
                foo.getInternalName(),
                MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                foo.getDeclaredMethods().filter(isConstructor()).getOnly().getDescriptor(),
                false);
        verify(methodVisitor).visitFieldInsn(Opcodes.PUTFIELD,
                foo.getInternalName(),
                TypeProxy.INSTANCE_FIELD,
                Type.getDescriptor(Void.class));
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testForDefaultMethodConstruction() throws Exception {
        when(instrumentationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(instrumentationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        StackManipulation stackManipulation = new TypeProxy.ForDefaultMethod(foo,
                instrumentationTarget,
                false);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Instrumentation.Context instrumentationContext = mock(Instrumentation.Context.class);
        when(instrumentationContext.register(any(AuxiliaryType.class))).thenReturn(foo);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(2));
        verify(instrumentationContext).register(any(AuxiliaryType.class));
        verifyNoMoreInteractions(instrumentationContext);
        verify(methodVisitor).visitTypeInsn(Opcodes.NEW, Type.getInternalName(Foo.class));
        verify(methodVisitor, times(2)).visitInsn(Opcodes.DUP);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL,
                foo.getInternalName(),
                MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                foo.getDeclaredMethods().filter(isConstructor()).getOnly().getDescriptor(),
                false);
        verify(methodVisitor).visitFieldInsn(Opcodes.PUTFIELD,
                foo.getInternalName(),
                TypeProxy.INSTANCE_FIELD,
                Type.getDescriptor(Void.class));
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testForReflectionFactoryConstruction() throws Exception {
        when(instrumentationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(instrumentationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        StackManipulation stackManipulation = new TypeProxy.ForSuperMethodByReflectionFactory(foo,
                instrumentationTarget,
                true,
                false);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Instrumentation.Context instrumentationContext = mock(Instrumentation.Context.class);
        when(instrumentationContext.register(any(AuxiliaryType.class)))
                .thenReturn(new TypeDescription.ForLoadedType(FooProxyMake.class));
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(3));
        verify(instrumentationContext).register(any(AuxiliaryType.class));
        verifyNoMoreInteractions(instrumentationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC,
                Type.getInternalName(FooProxyMake.class),
                TypeProxy.REFLECTION_METHOD,
                Type.getMethodDescriptor(FooProxyMake.class.getDeclaredMethod("make")),
                false);
        verify(methodVisitor).visitInsn(Opcodes.DUP);
        verify(methodVisitor).visitFieldInsn(Opcodes.PUTFIELD,
                Type.getInternalName(FooProxyMake.class),
                TypeProxy.INSTANCE_FIELD,
                Type.getDescriptor(Void.class));
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testInstrumentationIsValid() throws Exception {
        assertThat(TypeProxy.AbstractMethodErrorThrow.INSTANCE.isValid(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAccessorIsValid() throws Exception {
        TypeProxy typeProxy = new TypeProxy(mock(TypeDescription.class),
                mock(Instrumentation.Target.class),
                mock(TypeProxy.InvocationFactory.class),
                false,
                false);
        TypeProxy.MethodCall methodCall = typeProxy.new MethodCall(mock(AuxiliaryType.MethodAccessorFactory.class));
        TypeDescription instrumentedType = mock(TypeDescription.class);
        FieldList fieldList = mock(FieldList.class);
        when(fieldList.filter(any(ElementMatcher.class))).thenReturn(fieldList);
        when(fieldList.getOnly()).thenReturn(mock(FieldDescription.class));
        when(instrumentedType.getDeclaredFields()).thenReturn(fieldList);
        TypeProxy.MethodCall.Appender appender = methodCall.new Appender(instrumentedType);
        Instrumentation.SpecialMethodInvocation specialMethodInvocation = mock(Instrumentation.SpecialMethodInvocation.class);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        StackManipulation stackManipulation = appender.new AccessorMethodInvocation(mock(MethodDescription.class), specialMethodInvocation);
        assertThat(stackManipulation.isValid(), is(true));
        verify(specialMethodInvocation).isValid();
        verifyNoMoreInteractions(specialMethodInvocation);
    }

    public static class Foo {

        private Void target;

        public Foo(Void argument) {
        }
    }

    public static class FooProxyMake {

        private Void target;

        public static FooProxyMake make() {
            return null;
        }
    }
}
