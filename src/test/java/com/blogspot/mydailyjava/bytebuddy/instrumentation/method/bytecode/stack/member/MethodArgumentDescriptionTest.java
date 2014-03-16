package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import com.blogspot.mydailyjava.bytebuddy.utility.MoreOpcodes;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodArgumentDescriptionTest {

    private static final int PARAMETER_STACK_SIZE = 2;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;
    @Mock
    private TypeDescription declaringType, firstParameter, secondParameter;
    @Mock
    private TypeList parameterTypes;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(methodDescription.getParameterTypes()).thenReturn(parameterTypes);
        when(parameterTypes.size()).thenReturn(PARAMETER_STACK_SIZE);
        when(parameterTypes.get(0)).thenReturn(firstParameter);
        when(parameterTypes.get(1)).thenReturn(secondParameter);
        when(parameterTypes.iterator()).thenReturn(Arrays.asList(firstParameter, secondParameter).iterator());
        when(declaringType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(firstParameter.getStackSize()).thenReturn(StackSize.SINGLE);
        when(secondParameter.getStackSize()).thenReturn(StackSize.SINGLE);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testStaticMethod() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        StackManipulation stackManipulation = MethodVariableAccess.loadThisAndArguments(methodDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(PARAMETER_STACK_SIZE));
        assertThat(size.getMaximalSize(), is(PARAMETER_STACK_SIZE));
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_1);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testNonStaticMethod() throws Exception {
        StackManipulation stackManipulation = MethodVariableAccess.loadThisAndArguments(methodDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(PARAMETER_STACK_SIZE + 1));
        assertThat(size.getMaximalSize(), is(PARAMETER_STACK_SIZE + 1));
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_1);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_2);
        verifyNoMoreInteractions(methodVisitor);
    }
}
