package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.MoreOpcodes;
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

public class MethodVariableAccessDescriptionTest {

    private static final int PARAMETER_STACK_SIZE = 2;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private TypeDescription declaringType, firstParameterType, secondParameterType;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(firstParameterType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(secondParameterType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(firstParameterType.asRawType()).thenReturn(firstParameterType); // REFACTOR
        when(secondParameterType.asRawType()).thenReturn(secondParameterType); // REFACTOR
        ParameterList parameterList = ParameterList.Explicit.latent(methodDescription, Arrays.asList(firstParameterType, secondParameterType));
        when(methodDescription.getParameters()).thenReturn(parameterList);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testStaticMethod() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        StackManipulation stackManipulation = MethodVariableAccess.loadThisReferenceAndArguments(methodDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(PARAMETER_STACK_SIZE));
        assertThat(size.getMaximalSize(), is(PARAMETER_STACK_SIZE));
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_1);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testNonStaticMethod() throws Exception {
        StackManipulation stackManipulation = MethodVariableAccess.loadThisReferenceAndArguments(methodDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(PARAMETER_STACK_SIZE + 1));
        assertThat(size.getMaximalSize(), is(PARAMETER_STACK_SIZE + 1));
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_1);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_2);
        verifyNoMoreInteractions(methodVisitor);
    }
}
