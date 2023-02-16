package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.constant.JavaConstantValue;
import net.bytebuddy.utility.JavaConstant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
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
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodDescription.InDefinedShape methodDescription;

    @Mock
    private TypeDescription returnType, declaringType, firstType, secondType;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private JavaConstant argument;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private Object provided;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.asDefined()).thenReturn(methodDescription);
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
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(methodDescription, firstType, secondType));
        when(argument.accept(JavaConstantValue.Visitor.INSTANCE)).thenReturn(provided);
        when(argument.getTypeDescription()).thenReturn(typeDescription);
    }

    @Test
    public void testDynamicStaticBootstrap() throws Exception {
        when(methodDescription.isInvokeBootstrap(Collections.singletonList(typeDescription))).thenReturn(true);
        when(methodDescription.isStatic()).thenReturn(true);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription)
                .dynamic(FOO, returnType, Arrays.asList(firstType, secondType), Collections.singletonList(argument));
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInvokeDynamicInsn(FOO, "(" + FOO + BAR + ")" + QUX, new Handle(Opcodes.H_INVOKESTATIC, BAR, QUX, BAZ, false), provided);
    }

    @Test
    public void testDynamicConstructorBootstrap() throws Exception {
        when(methodDescription.isInvokeBootstrap(Collections.singletonList(typeDescription))).thenReturn(true);
        when(methodDescription.isConstructor()).thenReturn(true);
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription)
                .dynamic(FOO, returnType, Arrays.asList(firstType, secondType), Collections.singletonList(argument));
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInvokeDynamicInsn(FOO, "(" + FOO + BAR + ")" + QUX, new Handle(Opcodes.H_NEWINVOKESPECIAL, BAR, QUX, BAZ, false), provided);
    }

    @Test
    public void testIllegalBootstrap() throws Exception {
        StackManipulation stackManipulation = MethodInvocation.invoke(methodDescription)
                .dynamic(FOO, returnType, Arrays.asList(firstType, secondType), Collections.singletonList(argument));
        assertThat(stackManipulation.isValid(), is(false));
    }
}
