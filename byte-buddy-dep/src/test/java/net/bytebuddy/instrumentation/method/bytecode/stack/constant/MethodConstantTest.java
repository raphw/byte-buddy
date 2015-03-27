package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodConstantTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private TypeDescription declaringType, parameterType, fieldType;

    @Mock
    private ParameterList parameterList;

    @Mock
    private TypeList typeList;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Instrumentation.Context instrumentationContext;

    @Mock
    private FieldDescription fieldDescription;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getParameters()).thenReturn(parameterList);
        when(parameterList.asTypeList()).thenReturn(typeList);
        when(declaringType.getDescriptor()).thenReturn(BAR);
        when(typeList.iterator()).thenReturn(Arrays.asList(parameterType).iterator());
        when(parameterType.getDescriptor()).thenReturn(QUX);
        when(fieldDescription.getFieldType()).thenReturn(fieldType);
        when(fieldDescription.isStatic()).thenReturn(true);
        when(fieldType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(fieldDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getInternalName()).thenReturn(BAZ);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(fieldDescription.getDescriptor()).thenReturn(QUX);
    }

    @Test
    public void testMethod() throws Exception {
        StackManipulation.Size size = MethodConstant.forMethod(methodDescription).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(6));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Class.class),
                "getDeclaredMethod",
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                false);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testMethodCached() throws Exception {
        when(instrumentationContext.cache(any(StackManipulation.class), any(TypeDescription.class))).thenReturn(fieldDescription);
        StackManipulation.Size size = MethodConstant.forMethod(methodDescription).cached().apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, BAZ, FOO, QUX);
        verifyNoMoreInteractions(methodVisitor);
        verify(instrumentationContext).cache(MethodConstant.forMethod(methodDescription), new TypeDescription.ForLoadedType(Method.class));
        verifyNoMoreInteractions(instrumentationContext);
    }

    @Test
    public void testConstructor() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        StackManipulation.Size size = MethodConstant.forMethod(methodDescription).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(5));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Class.class),
                "getDeclaredConstructor",
                "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;",
                false);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testConstructorCached() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        when(instrumentationContext.cache(any(StackManipulation.class), any(TypeDescription.class))).thenReturn(fieldDescription);
        StackManipulation.Size size = MethodConstant.forMethod(methodDescription).cached().apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, BAZ, FOO, QUX);
        verifyNoMoreInteractions(methodVisitor);
        verify(instrumentationContext).cache(MethodConstant.forMethod(methodDescription), new TypeDescription.ForLoadedType(Method.class));
        verifyNoMoreInteractions(instrumentationContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeInitializer() throws Exception {
        when(methodDescription.isTypeInitializer()).thenReturn(true);
        MethodConstant.CanCache methodConstant = MethodConstant.forMethod(methodDescription);
        assertThat(methodConstant.isValid(), is(false));
        assertThat(methodConstant.cached().isValid(), is(false));
        methodConstant.apply(methodVisitor, instrumentationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodConstant.ForMethod.class).apply();
        ObjectPropertyAssertion.of(MethodConstant.ForConstructor.class).apply();
        ObjectPropertyAssertion.of(MethodConstant.Cached.class).apply();
        ObjectPropertyAssertion.of(MethodConstant.CanCache.Illegal.class).apply();
    }
}
