package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class MethodInvocationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final int ARGUMENT_STACK_SIZE = 1;

    private final StackSize stackSize;

    private final int expectedSize;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.InDefinedShape methodDescription;

    @Mock
    private TypeDescription.Generic returnType, otherType;

    @Mock
    private TypeDescription declaringType, rawOtherType;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private MethodVisitor methodVisitor;

    public MethodInvocationTest(StackSize stackSize) {
        this.stackSize = stackSize;
        this.expectedSize = stackSize.getSize() - ARGUMENT_STACK_SIZE;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {StackSize.ZERO},
                {StackSize.SINGLE},
                {StackSize.DOUBLE}
        });
    }

    @Before
    public void setUp() throws Exception {
        when(declaringType.asErasure()).thenReturn(declaringType);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(methodDescription.getStackSize()).thenReturn(ARGUMENT_STACK_SIZE);
        when(declaringType.getInternalName()).thenReturn(FOO);
        when(otherType.asErasure()).thenReturn(rawOtherType);
        when(rawOtherType.getInternalName()).thenReturn(BAZ);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(QUX);
        when(returnType.getStackSize()).thenReturn(stackSize);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testTypeInitializerInvocation() throws Exception {
        when(methodDescription.isTypeInitializer()).thenReturn(true);
        assertThat(MethodInvocation.invoke(methodDescription).isValid(), is(false));
    }

    @Test
    public void testStaticMethodInvocation() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription), Opcodes.INVOKESTATIC, FOO, false);
    }

    @Test
    public void testStaticPrivateMethodInvocation() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        when(methodDescription.isPrivate()).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription), Opcodes.INVOKESTATIC, FOO, false);
    }

    @Test
    public void testPrivateMethodInvocation() throws Exception {
        when(methodDescription.isPrivate()).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription), Opcodes.INVOKESPECIAL, FOO, false);
    }

    @Test
    public void testConstructorMethodInvocation() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription), Opcodes.INVOKESPECIAL, FOO, false);
    }

    @Test
    public void testPublicMethodInvocation() throws Exception {
        assertInvocation(MethodInvocation.invoke(methodDescription), Opcodes.INVOKEVIRTUAL, FOO, false);
    }

    @Test
    public void testInterfaceMethodInvocation() throws Exception {
        when(declaringType.isInterface()).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription), Opcodes.INVOKEINTERFACE, FOO, true);
    }

    @Test
    public void testStaticInterfaceMethodInvocation() throws Exception {
        when(declaringType.isInterface()).thenReturn(true);
        when(methodDescription.isStatic()).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription), Opcodes.INVOKESTATIC, FOO, true);
    }

    @Test
    public void testDefaultInterfaceMethodInvocation() throws Exception {
        when(methodDescription.isDefaultMethod()).thenReturn(true);
        when(declaringType.isInterface()).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription), Opcodes.INVOKEINTERFACE, FOO, true);
    }

    @Test
    public void testExplicitlySpecialDefaultInterfaceMethodInvocation() throws Exception {
        when(methodDescription.isDefaultMethod()).thenReturn(true);
        when(methodDescription.isSpecializableFor(declaringType)).thenReturn(true);
        when(declaringType.isInterface()).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription).special(declaringType), Opcodes.INVOKESPECIAL, FOO, true);
    }

    @Test
    public void testExplicitlySpecialDefaultInterfaceMethodInvocationOnOther() throws Exception {
        when(methodDescription.isDefaultMethod()).thenReturn(true);
        when(methodDescription.isSpecializableFor(rawOtherType)).thenReturn(false);
        assertThat(MethodInvocation.invoke(methodDescription).special(rawOtherType).isValid(), is(false));
    }

    @Test
    public void testExplicitlySpecialMethodInvocation() throws Exception {
        when(methodDescription.isSpecializableFor(rawOtherType)).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription).special(rawOtherType), Opcodes.INVOKESPECIAL, BAZ, false);
    }

    @Test
    public void testIllegalSpecialMethodInvocation() throws Exception {
        assertThat(MethodInvocation.invoke(methodDescription).special(rawOtherType).isValid(), is(false));
    }

    @Test
    public void testExplicitlyVirtualMethodInvocation() throws Exception {
        when(declaringType.isAssignableFrom(rawOtherType)).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription).virtual(rawOtherType), Opcodes.INVOKEVIRTUAL, BAZ, false);
    }

    @Test
    public void testExplicitlyVirtualMethodInvocationOfInterface() throws Exception {
        when(declaringType.isAssignableFrom(rawOtherType)).thenReturn(true);
        when(rawOtherType.isInterface()).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription).virtual(rawOtherType), Opcodes.INVOKEINTERFACE, BAZ, true);
    }

    @Test
    public void testExplicitlyVirtualMethodInvocationOfInterfaceOfObjectMethod() throws Exception {
        when(declaringType.isAssignableFrom(rawOtherType)).thenReturn(true);
        when(rawOtherType.isInterface()).thenReturn(true);
        when(declaringType.represents(Object.class)).thenReturn(true);
        assertInvocation(MethodInvocation.invoke(methodDescription).virtual(rawOtherType), Opcodes.INVOKEVIRTUAL, FOO, false);
    }

    @Test
    public void testStaticVirtualInvocation() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        assertThat(MethodInvocation.invoke(methodDescription).virtual(rawOtherType).isValid(), is(false));
    }

    @Test
    public void testPrivateVirtualInvocation() throws Exception {
        when(methodDescription.isPrivate()).thenReturn(true);
        assertThat(MethodInvocation.invoke(methodDescription).virtual(rawOtherType).isValid(), is(false));
    }

    @Test
    public void testConstructorVirtualInvocation() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        assertThat(MethodInvocation.invoke(methodDescription).virtual(rawOtherType).isValid(), is(false));
    }

    private void assertInvocation(StackManipulation stackManipulation,
                                  int opcode,
                                  String typeName,
                                  boolean interfaceInvocation) {
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(expectedSize));
        assertThat(size.getMaximalSize(), is(Math.max(0, expectedSize)));
        verify(methodVisitor).visitMethodInsn(opcode, typeName, BAR, QUX, interfaceInvocation);
        verifyNoMoreInteractions(methodVisitor);
    }
}
