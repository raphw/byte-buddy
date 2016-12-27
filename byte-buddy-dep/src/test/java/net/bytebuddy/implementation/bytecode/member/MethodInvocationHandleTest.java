package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MethodInvocationHandleTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.InDefinedShape methodDescription;

    @Mock
    private TypeDescription.Generic returnType;

    @Mock
    private TypeDescription declaringType, firstType, secondType;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.asDefined()).thenReturn(methodDescription);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstType.getDescriptor()).thenReturn(FOO);
        when(secondType.getDescriptor()).thenReturn(BAR);
        when(secondType.getStackSize()).thenReturn(StackSize.ZERO);
        when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(methodDescription.getInternalName()).thenReturn(QUX);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        when(declaringType.getDescriptor()).thenReturn(BAR);
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(methodDescription, firstType, secondType));
    }

    @Test
    public void testExactHandleStatic() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).onHandle(MethodInvocation.HandleType.EXACT);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", BAZ, false);
    }

    @Test
    public void testExactHandleConstructor() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).onHandle(MethodInvocation.HandleType.EXACT);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", BAZ, false);
    }

    @Test
    public void testExactHandleNonStatic() throws Exception {
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription).onHandle(MethodInvocation.HandleType.EXACT);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "(" + BAR + BAZ.substring(1), false);
    }

    @Test
    public void testMethodNames() throws Exception {
        assertThat(MethodInvocation.HandleType.EXACT.getMethodName(), is("invokeExact"));
        assertThat(MethodInvocation.HandleType.REGULAR.getMethodName(), is("invoke"));
    }
}
