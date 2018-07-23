package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodConstantTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.InDefinedShape methodDescription, auxiliaryConstructor;

    @Mock
    private TypeDescription declaringType, parameterType, fieldType, instrumentedType, auxiliaryType;

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private TypeDescription.Generic genericFieldType;

    @Mock
    private ParameterList<?> parameterList;

    @Mock
    private TypeList.Generic typeList;

    @Mock
    private TypeList rawTypeList;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private FieldDescription.InDefinedShape fieldDescription;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(declaringType.asErasure()).thenReturn(declaringType);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getParameters()).thenReturn((ParameterList) parameterList);
        when(parameterList.asTypeList()).thenReturn(typeList);
        when(declaringType.getDescriptor()).thenReturn(BAR);
        when(typeList.asErasures()).thenReturn(rawTypeList);
        when(rawTypeList.iterator()).thenReturn(Collections.singletonList(parameterType).iterator());
        when(parameterType.getDescriptor()).thenReturn(QUX);
        when(fieldDescription.getType()).thenReturn(genericFieldType);
        when(fieldDescription.isStatic()).thenReturn(true);
        when(genericFieldType.asErasure()).thenReturn(fieldType);
        when(genericFieldType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(fieldDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getInternalName()).thenReturn(BAZ);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(fieldDescription.getDescriptor()).thenReturn(QUX);
        when(fieldDescription.asDefined()).thenReturn(fieldDescription);
        when(implementationContext.getClassFileVersion()).thenReturn(classFileVersion);
        when(implementationContext.getInstrumentedType()).thenReturn(instrumentedType);
        when(auxiliaryConstructor.isConstructor()).thenReturn(true);
        when(auxiliaryConstructor.getDeclaringType()).thenReturn(auxiliaryType);
        when(auxiliaryConstructor.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(auxiliaryConstructor.getDescriptor()).thenReturn(FOO);
        when(auxiliaryConstructor.getInternalName()).thenReturn(BAR);
        when(auxiliaryType.getInternalName()).thenReturn(QUX);
    }

    @Test
    public void testMethod() throws Exception {
        StackManipulation.Size size = MethodConstant.of(methodDescription).apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(6));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Class.class),
                "getDeclaredMethod",
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                false);
    }

    @Test
    public void testMethodPublic() throws Exception {
        when(methodDescription.isPublic()).thenReturn(true);
        StackManipulation.Size size = MethodConstant.of(methodDescription).apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(6));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Class.class),
                "getMethod",
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                false);
    }

    @Test
    public void testMethodCached() throws Exception {
        when(implementationContext.cache(any(StackManipulation.class), any(TypeDescription.class))).thenReturn(fieldDescription);
        StackManipulation.Size size = MethodConstant.of(methodDescription).cached().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, BAZ, FOO, QUX);
        verifyNoMoreInteractions(methodVisitor);
        verify(implementationContext).cache(MethodConstant.of(methodDescription), TypeDescription.ForLoadedType.of(Method.class));
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testMethodPrivileged() throws Exception {
        when(methodDescription.isMethod()).thenReturn(true);
        when(implementationContext.register(any(AuxiliaryType.class))).thenReturn(auxiliaryType);
        when(auxiliaryType.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(auxiliaryConstructor));
        StackManipulation.Size size = MethodConstant.ofPrivileged(methodDescription).apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(5));
        assertThat(size.getMaximalSize(), is(8));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL,
                QUX,
                BAR,
                FOO,
                false);
    }

    @Test
    public void testMethodPrivilegedCached() throws Exception {
        when(implementationContext.cache(any(StackManipulation.class), any(TypeDescription.class))).thenReturn(fieldDescription);
        StackManipulation.Size size = MethodConstant.ofPrivileged(methodDescription).cached().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, BAZ, FOO, QUX);
        verifyNoMoreInteractions(methodVisitor);
        verify(implementationContext).cache(MethodConstant.ofPrivileged(methodDescription), TypeDescription.ForLoadedType.of(Method.class));
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testConstructor() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        StackManipulation.Size size = MethodConstant.of(methodDescription).apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(5));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Class.class),
                "getDeclaredConstructor",
                "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;",
                false);
    }

    @Test
    public void testConstructorPublic() {
        when(methodDescription.isConstructor()).thenReturn(true);
        when(methodDescription.isPublic()).thenReturn(true);
        StackManipulation.Size size = MethodConstant.of(methodDescription).apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(5));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Class.class),
                "getConstructor",
                "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;",
                false);
    }

    @Test
    public void testConstructorCached() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        when(implementationContext.cache(any(StackManipulation.class), any(TypeDescription.class))).thenReturn(fieldDescription);
        StackManipulation.Size size = MethodConstant.of(methodDescription).cached().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, BAZ, FOO, QUX);
        verifyNoMoreInteractions(methodVisitor);
        verify(implementationContext).cache(MethodConstant.of(methodDescription), TypeDescription.ForLoadedType.of(Constructor.class));
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testConstructorPrivileged() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        when(implementationContext.register(any(AuxiliaryType.class))).thenReturn(auxiliaryType);
        when(auxiliaryType.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(auxiliaryConstructor));
        StackManipulation.Size size = MethodConstant.ofPrivileged(methodDescription).apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(4));
        assertThat(size.getMaximalSize(), is(7));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL,
                QUX,
                BAR,
                FOO,
                false);
    }

    @Test
    public void testConstructorPrivilegedCached() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        when(implementationContext.cache(any(StackManipulation.class), any(TypeDescription.class))).thenReturn(fieldDescription);
        StackManipulation.Size size = MethodConstant.ofPrivileged(methodDescription).cached().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, BAZ, FOO, QUX);
        verifyNoMoreInteractions(methodVisitor);
        verify(implementationContext).cache(MethodConstant.ofPrivileged(methodDescription), TypeDescription.ForLoadedType.of(Constructor.class));
        verifyNoMoreInteractions(implementationContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeInitializer() throws Exception {
        when(methodDescription.isTypeInitializer()).thenReturn(true);
        MethodConstant.CanCache methodConstant = MethodConstant.of(methodDescription);
        assertThat(methodConstant.isValid(), is(false));
        assertThat(methodConstant.cached().isValid(), is(false));
        methodConstant.apply(methodVisitor, implementationContext);
    }
}
