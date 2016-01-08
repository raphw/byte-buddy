package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
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
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodVariableAccessOfMethodArgumentsTest {

    private static final String FOO = "foo";

    private static final int PARAMETER_STACK_SIZE = 2;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.InDefinedShape methodDescription, bridgeMethod;

    @Mock
    private TypeDescription declaringType, firstRawParameterType, secondRawParameterType;

    @Mock
    private TypeDescription.Generic firstParameterType, secondParameterType;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(firstParameterType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(firstParameterType.asErasure()).thenReturn(firstRawParameterType);
        when(firstParameterType.asGenericType()).thenReturn(firstParameterType);
        when(secondParameterType.asErasure()).thenReturn(secondRawParameterType);
        when(secondParameterType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(secondParameterType.asGenericType()).thenReturn(secondParameterType);
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(methodDescription, firstParameterType, secondParameterType));
        when(bridgeMethod.getDeclaringType()).thenReturn(declaringType);
        when(secondRawParameterType.getInternalName()).thenReturn(FOO);
        when(firstParameterType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(firstParameterType);
        when(secondParameterType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(secondParameterType);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testStaticMethod() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        StackManipulation stackManipulation = MethodVariableAccess.allArgumentsOf(methodDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(PARAMETER_STACK_SIZE));
        assertThat(size.getMaximalSize(), is(PARAMETER_STACK_SIZE));
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 1);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testNonStaticMethod() throws Exception {
        StackManipulation stackManipulation = MethodVariableAccess.allArgumentsOf(methodDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(PARAMETER_STACK_SIZE));
        assertThat(size.getMaximalSize(), is(PARAMETER_STACK_SIZE));
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 1);
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 2);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testStaticMethodWithPrepending() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        StackManipulation stackManipulation = MethodVariableAccess.allArgumentsOf(methodDescription).prependThisReference();
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(PARAMETER_STACK_SIZE));
        assertThat(size.getMaximalSize(), is(PARAMETER_STACK_SIZE));
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 1);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testNonStaticMethodWithPrepending() throws Exception {
        StackManipulation stackManipulation = MethodVariableAccess.allArgumentsOf(methodDescription).prependThisReference();
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(PARAMETER_STACK_SIZE + 1));
        assertThat(size.getMaximalSize(), is(PARAMETER_STACK_SIZE + 1));
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 1);
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 2);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testBridgeMethodWithoutCasting() throws Exception {
        when(bridgeMethod.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(bridgeMethod,
                Arrays.asList(firstParameterType, secondParameterType)));
        StackManipulation stackManipulation = MethodVariableAccess.allArgumentsOf(methodDescription).asBridgeOf(bridgeMethod);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(PARAMETER_STACK_SIZE));
        assertThat(size.getMaximalSize(), is(PARAMETER_STACK_SIZE));
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 1);
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 2);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testBridgeMethodWithCasting() throws Exception {
        when(secondRawParameterType.asErasure()).thenReturn(secondRawParameterType);
        when(bridgeMethod.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(bridgeMethod, secondParameterType, secondParameterType));
        StackManipulation stackManipulation = MethodVariableAccess.allArgumentsOf(methodDescription).asBridgeOf(bridgeMethod);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(PARAMETER_STACK_SIZE));
        assertThat(size.getMaximalSize(), is(PARAMETER_STACK_SIZE));
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 1);
        verify(methodVisitor).visitTypeInsn(Opcodes.CHECKCAST, FOO);
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 2);
        verifyNoMoreInteractions(methodVisitor);
    }
}
