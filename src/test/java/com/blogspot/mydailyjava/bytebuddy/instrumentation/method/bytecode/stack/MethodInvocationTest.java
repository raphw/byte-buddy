package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.test.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class MethodInvocationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {2, StackSize.SINGLE},
                {0, StackSize.ZERO},
        });
    }

    private final int methodStackSize;
    private final StackSize returnTypeSize;

    public MethodInvocationTest(int methodStackSize, StackSize returnTypeSize) {
        this.methodStackSize = methodStackSize;
        this.returnTypeSize = returnTypeSize;
    }

    @Mock
    private MethodDescription methodDescription;
    @Mock
    private TypeDescription declaringType, declaringTypeSubType, returnType;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    private int expectedSizeChange;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(QUX);
        when(declaringType.isAssignableFrom(declaringTypeSubType)).thenReturn(true);
        when(declaringTypeSubType.getInternalName()).thenReturn(BAZ);
        expectedSizeChange = configureSizeChange(methodStackSize, returnTypeSize);
    }

    private int configureSizeChange(int methodStackSize, StackSize returnTypeSize) {
        when(methodDescription.getStackSize()).thenReturn(methodStackSize);
        when(returnType.getStackSize()).thenReturn(returnTypeSize);
        return returnTypeSize.getSize() - methodStackSize;
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testStaticMethodImplicitInvocation() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        evaluateImplicitlyTypedInvocation(Opcodes.INVOKESTATIC);
        verify(methodDescription, atLeast(1)).isStatic();
    }

    @Test
    public void testVirtualMethodImplicitInvocation() throws Exception {
        evaluateImplicitlyTypedInvocation(Opcodes.INVOKEVIRTUAL);
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).isConstructor();
        verify(methodDescription, atLeast(1)).isPrivate();
    }

    @Test
    public void testPrivateMethodImplicitInvocation() throws Exception {
        when(methodDescription.isPrivate()).thenReturn(true);
        evaluateImplicitlyTypedInvocation(Opcodes.INVOKESPECIAL);
        verify(methodDescription, atLeast(1)).isStatic();
        verify(methodDescription, atLeast(1)).isPrivate();
    }

    @Test
    public void testPrivateStaticMethodImplicitInvocation() throws Exception {
        when(methodDescription.isPrivate()).thenReturn(true);
        when(methodDescription.isStatic()).thenReturn(true);
        evaluateImplicitlyTypedInvocation(Opcodes.INVOKESTATIC);
        verify(methodDescription, atLeast(1)).isStatic();
    }

    @Test
    public void testConstructorImplicitInvocation() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        evaluateImplicitlyTypedInvocation(Opcodes.INVOKESPECIAL);
        verify(methodDescription, atLeast(1)).isConstructor();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticMethodExplicitVirtualInvocation() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        TypeDescription explicitType = mock(TypeDescription.class);
        MethodInvocation.invoke(methodDescription).virtual(explicitType);
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticMethodExplicitSpecialInvocation() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        TypeDescription explicitType = mock(TypeDescription.class);
        MethodInvocation.invoke(methodDescription).special(explicitType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorExplicitVirtualInvocation() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        TypeDescription explicitType = mock(TypeDescription.class);
        MethodInvocation.invoke(methodDescription).virtual(explicitType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorExplicitSpecialInvocationNonDeclaringType() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        TypeDescription explicitType = mock(TypeDescription.class);
        MethodInvocation.invoke(methodDescription).special(explicitType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorExplicitSpecialInvocationDeclaringType() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        MethodInvocation.invoke(methodDescription).special(declaringType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrivateMethodExplicitVirtualInvocation() throws Exception {
        when(methodDescription.isPrivate()).thenReturn(true);
        TypeDescription explicitType = mock(TypeDescription.class);
        MethodInvocation.invoke(methodDescription).virtual(explicitType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrivateMethodExplicitSpecialInvocationNonDeclaringType() throws Exception {
        when(methodDescription.isPrivate()).thenReturn(true);
        TypeDescription explicitType = mock(TypeDescription.class);
        MethodInvocation.invoke(methodDescription).special(explicitType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrivateMethodExplicitSpecialInvocationDeclaringType() throws Exception {
        when(methodDescription.isPrivate()).thenReturn(true);
        MethodInvocation.invoke(methodDescription).special(declaringType);
    }

    @Test
    public void testVirtualMethodExplicitVirtualInvocationSubType() throws Exception {
        evaluateInvocation(Opcodes.INVOKEVIRTUAL, BAZ, MethodInvocation.invoke(methodDescription).virtual(declaringTypeSubType));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVirtualMethodExplicitVirtualInvocationNonSubType() throws Exception {
        MethodInvocation.invoke(methodDescription).virtual(returnType);
    }

    @Test
    public void testVirtualMethodExplicitSpecialInvocationSubType() throws Exception {
        evaluateInvocation(Opcodes.INVOKESPECIAL, BAZ, MethodInvocation.invoke(methodDescription).special(declaringTypeSubType));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVirtualMethodExplicitSpecialInvocationNonSubType() throws Exception {
        MethodInvocation.invoke(methodDescription).special(returnType);
    }

    private void evaluateImplicitlyTypedInvocation(int opcode) {
        evaluateInvocation(opcode, FOO, MethodInvocation.invoke(methodDescription));
    }

    private void evaluateInvocation(int opcode, String internalTypeName, StackManipulation invocation) {
        assertThat(invocation.isValid(), is(true));
        StackManipulation.Size size = invocation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(expectedSizeChange));
        assertThat(size.getMaximalSize(), is(Math.max(0, expectedSizeChange)));
        verify(methodVisitor).visitMethodInsn(opcode, internalTypeName, BAR, QUX);
        verifyNoMoreInteractions(methodVisitor);
    }
}
