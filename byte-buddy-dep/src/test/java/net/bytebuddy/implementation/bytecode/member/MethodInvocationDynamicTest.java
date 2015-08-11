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
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MethodInvocationDynamicTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.InDefinedShape methodDescription;

    @Mock
    private TypeDescription returnType, declaringType, firstType, secondType;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Object argument;

    @Before
    public void setUp() throws Exception {
        when(declaringType.asErasure()).thenReturn(declaringType);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(firstType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstType.getDescriptor()).thenReturn(FOO);
        when(secondType.getDescriptor()).thenReturn(BAR);
        when(secondType.getStackSize()).thenReturn(StackSize.ZERO);
        when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(returnType.getDescriptor()).thenReturn(QUX);
        when(methodDescription.getInternalName()).thenReturn(QUX);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        when(declaringType.getInternalName()).thenReturn(BAR);
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(methodDescription, Arrays.asList(firstType, secondType)));
    }

    @Test
    public void testDynamicStaticBootstrap() throws Exception {
        when(methodDescription.isBootstrap()).thenReturn(true);
        when(methodDescription.isStatic()).thenReturn(true);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription)
                .dynamic(FOO, returnType, Arrays.asList(firstType, secondType), Collections.singletonList(argument));
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getSizeImpact(), is(0));
        verify(methodVisitor).visitInvokeDynamicInsn(FOO, "(" + FOO + BAR + ")" + QUX, new Handle(Opcodes.H_INVOKESTATIC, BAR, QUX, BAZ), argument);
    }

    @Test
    public void testDynamicConstructorBootstrap() throws Exception {
        when(methodDescription.isBootstrap()).thenReturn(true);
        when(methodDescription.isConstructor()).thenReturn(true);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription)
                .dynamic(FOO, returnType, Arrays.asList(firstType, secondType), Collections.singletonList(argument));
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getSizeImpact(), is(0));
        verify(methodVisitor).visitInvokeDynamicInsn(FOO, "(" + FOO + BAR + ")" + QUX, new Handle(Opcodes.H_NEWINVOKESPECIAL, BAR, QUX, BAZ), argument);
    }

    @Test
    public void testIllegalBootstrap() throws Exception {
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription)
                .dynamic(FOO, returnType, Arrays.asList(firstType, secondType), Collections.singletonList(argument));
        assertThat(stackManipulation.isValid(), is(false));
    }
}
