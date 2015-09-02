package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.MockitoRule;
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
import static org.mockito.Mockito.*;

public class TypeProxyCreationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private TypeProxy.InvocationFactory invocationFactory;

    @Mock
    private AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Mock
    private MethodDescription.InDefinedShape proxyMethod;

    private TypeDescription foo;

    private MethodList<?> fooMethods;

    private int modifiers;

    @Before
    public void setUp() throws Exception {
        for (ModifierContributor modifierContributor : AuxiliaryType.DEFAULT_TYPE_MODIFIER) {
            modifiers = modifiers | modifierContributor.getMask();
        }
        foo = new TypeDescription.ForLoadedType(Foo.class);
        fooMethods = MethodGraph.Compiler.DEFAULT.compile(foo)
                .listNodes()
                .asMethodList()
                .filter(isVirtual().and(not(isFinal())).and(not(isDefaultFinalizer())));
        when(proxyMethod.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(proxyMethod, Arrays.asList(foo, foo, foo)));
        when(proxyMethod.getDeclaringType()).thenReturn(foo);
        when(proxyMethod.getInternalName()).thenReturn(FOO);
        when(proxyMethod.getDescriptor()).thenReturn(FOO);
        when(proxyMethod.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        when(proxyMethod.asDefined()).thenReturn(proxyMethod);
    }

    @Test
    public void testAllIllegal() throws Exception {
        when(implementationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(implementationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        TypeDescription dynamicType = new TypeProxy(foo,
                implementationTarget,
                invocationFactory,
                true,
                false)
                .make(BAR, ClassFileVersion.forCurrentJavaVersion(), methodAccessorFactory)
                .getTypeDescription();
        assertThat(dynamicType.getModifiers(), is(modifiers));
        assertThat(dynamicType.getSuperType(), is((GenericTypeDescription) foo));
        assertThat(dynamicType.getInterfaces(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(dynamicType.getName(), is(BAR));
        assertThat(dynamicType.getDeclaredMethods().size(), is(2));
        assertThat(dynamicType.isAssignableTo(Serializable.class), is(false));
        verifyZeroInteractions(methodAccessorFactory);
        for (MethodDescription methodDescription : fooMethods) {
            verify(invocationFactory).invoke(implementationTarget, foo, methodDescription);
        }
        verifyNoMoreInteractions(invocationFactory);
        verify(specialMethodInvocation, times(fooMethods.size())).isValid();
        verifyNoMoreInteractions(specialMethodInvocation);
    }

    @Test
    public void testAllLegal() throws Exception {
        when(implementationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(implementationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        TypeDescription dynamicType = new TypeProxy(foo,
                implementationTarget,
                invocationFactory,
                true,
                false)
                .make(BAR, ClassFileVersion.forCurrentJavaVersion(), methodAccessorFactory)
                .getTypeDescription();
        assertThat(dynamicType.getModifiers(), is(modifiers));
        assertThat(dynamicType.getSuperType(), is((GenericTypeDescription) foo));
        assertThat(dynamicType.getInterfaces(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(dynamicType.getName(), is(BAR));
        assertThat(dynamicType.getDeclaredMethods().size(), is(2));
        assertThat(dynamicType.isAssignableTo(Serializable.class), is(false));
        verify(methodAccessorFactory, times(fooMethods.size())).registerAccessorFor(specialMethodInvocation);
        for (MethodDescription methodDescription : fooMethods) {
            verify(invocationFactory).invoke(implementationTarget, foo, methodDescription);
        }
        verifyNoMoreInteractions(invocationFactory);
        verify(specialMethodInvocation, times(fooMethods.size())).isValid();
        verifyNoMoreInteractions(specialMethodInvocation);
    }

    @Test
    public void testAllLegalSerializable() throws Exception {
        when(implementationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(implementationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        TypeDescription dynamicType = new TypeProxy(foo,
                implementationTarget,
                invocationFactory,
                true,
                true)
                .make(BAR, ClassFileVersion.forCurrentJavaVersion(), methodAccessorFactory)
                .getTypeDescription();
        assertThat(dynamicType.getModifiers(), is(modifiers));
        assertThat(dynamicType.getSuperType(), is((GenericTypeDescription) foo));
        assertThat(dynamicType.getInterfaces(), is((GenericTypeList) new GenericTypeList.ForLoadedType(Serializable.class)));
        assertThat(dynamicType.getName(), is(BAR));
        assertThat(dynamicType.getDeclaredMethods().size(), is(2));
        assertThat(dynamicType.isAssignableTo(Serializable.class), is(true));
        verify(methodAccessorFactory, times(fooMethods.size())).registerAccessorFor(specialMethodInvocation);
        for (MethodDescription methodDescription : fooMethods) {
            verify(invocationFactory).invoke(implementationTarget, foo, methodDescription);
        }
        verifyNoMoreInteractions(invocationFactory);
        verify(specialMethodInvocation, times(fooMethods.size())).isValid();
        verifyNoMoreInteractions(specialMethodInvocation);
    }

    @Test
    public void testAllLegalNotIgnoreFinalizer() throws Exception {
        when(implementationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(implementationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        TypeDescription dynamicType = new TypeProxy(foo,
                implementationTarget,
                invocationFactory,
                false,
                false)
                .make(BAR, ClassFileVersion.forCurrentJavaVersion(), methodAccessorFactory)
                .getTypeDescription();
        assertThat(dynamicType.getModifiers(), is(modifiers));
        assertThat(dynamicType.getSuperType(), is((GenericTypeDescription) foo));
        assertThat(dynamicType.getInterfaces(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(dynamicType.getName(), is(BAR));
        assertThat(dynamicType.getDeclaredMethods().size(), is(2));
        assertThat(dynamicType.isAssignableTo(Serializable.class), is(false));
        verify(methodAccessorFactory, times(fooMethods.size() + 1)).registerAccessorFor(specialMethodInvocation);
        for (MethodDescription methodDescription : fooMethods) {
            verify(invocationFactory).invoke(implementationTarget, foo, methodDescription);
        }
        verify(invocationFactory).invoke(implementationTarget, foo,
                new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod("finalize")));
        verifyNoMoreInteractions(invocationFactory);
        verify(specialMethodInvocation, times(fooMethods.size() + 1)).isValid();
        verifyNoMoreInteractions(specialMethodInvocation);
    }

    @Test
    public void testForConstructorConstruction() throws Exception {
        when(implementationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(implementationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        StackManipulation stackManipulation = new TypeProxy.ForSuperMethodByConstructor(foo,
                implementationTarget,
                Collections.<TypeDescription>singletonList(new TypeDescription.ForLoadedType(Void.class)),
                true,
                false);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        when(implementationContext.register(any(AuxiliaryType.class))).thenReturn(foo);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(3));
        verify(implementationContext).register(any(AuxiliaryType.class));
        verifyNoMoreInteractions(implementationContext);
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
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testForDefaultMethodConstruction() throws Exception {
        when(implementationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(implementationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        StackManipulation stackManipulation = new TypeProxy.ForDefaultMethod(foo,
                implementationTarget,
                false);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        when(implementationContext.register(any(AuxiliaryType.class))).thenReturn(foo);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(2));
        verify(implementationContext).register(any(AuxiliaryType.class));
        verifyNoMoreInteractions(implementationContext);
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
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testForReflectionFactoryConstruction() throws Exception {
        when(implementationTarget.getTypeDescription()).thenReturn(foo);
        when(invocationFactory.invoke(eq(implementationTarget), eq(foo), any(MethodDescription.class)))
                .thenReturn(specialMethodInvocation);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        when(specialMethodInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(methodAccessorFactory.registerAccessorFor(specialMethodInvocation)).thenReturn(proxyMethod);
        StackManipulation stackManipulation = new TypeProxy.ForSuperMethodByReflectionFactory(foo,
                implementationTarget,
                true,
                false);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        when(implementationContext.register(any(AuxiliaryType.class)))
                .thenReturn(new TypeDescription.ForLoadedType(FooProxyMake.class));
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(3));
        verify(implementationContext).register(any(AuxiliaryType.class));
        verifyNoMoreInteractions(implementationContext);
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
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testImplementationIsValid() throws Exception {
        assertThat(TypeProxy.AbstractMethodErrorThrow.INSTANCE.isValid(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAccessorIsValid() throws Exception {
        TypeProxy typeProxy = new TypeProxy(mock(TypeDescription.class),
                mock(Implementation.Target.class),
                mock(TypeProxy.InvocationFactory.class),
                false,
                false);
        TypeProxy.MethodCall methodCall = typeProxy.new MethodCall(mock(AuxiliaryType.MethodAccessorFactory.class));
        TypeDescription instrumentedType = mock(TypeDescription.class);
        FieldList<FieldDescription.InDefinedShape> fieldList = mock(FieldList.class);
        when(fieldList.filter(any(ElementMatcher.class))).thenReturn(fieldList);
        when(fieldList.getOnly()).thenReturn(mock(FieldDescription.InDefinedShape.class));
        when(instrumentedType.getDeclaredFields()).thenReturn(fieldList);
        TypeProxy.MethodCall.Appender appender = methodCall.new Appender(instrumentedType);
        Implementation.SpecialMethodInvocation specialMethodInvocation = mock(Implementation.SpecialMethodInvocation.class);
        when(specialMethodInvocation.isValid()).thenReturn(true);
        StackManipulation stackManipulation = appender.new AccessorMethodInvocation(mock(MethodDescription.class), specialMethodInvocation);
        assertThat(stackManipulation.isValid(), is(true));
        verify(specialMethodInvocation).isValid();
        verifyNoMoreInteractions(specialMethodInvocation);
    }

    @SuppressWarnings("unchecked")
    public static class Foo {

        private Void target;

        public Foo(Void argument) {
        }
    }

    @SuppressWarnings("unchecked")
    public static class FooProxyMake {

        private Void target;

        public static FooProxyMake make() {
            return null;
        }
    }
}
