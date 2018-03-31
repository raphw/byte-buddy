package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
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
public class MethodRebaseResolverResolutionForRebasedMethodTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {false, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE},
                {true, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC}
        });
    }

    private final boolean interfaceType;

    private final int rebasedMethodModifiers;

    public MethodRebaseResolverResolutionForRebasedMethodTest(boolean interfaceType, int rebasedMethodModifiers) {
        this.interfaceType = interfaceType;
        this.rebasedMethodModifiers = rebasedMethodModifiers;
    }

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.InDefinedShape methodDescription;

    @Mock
    private MethodNameTransformer methodNameTransformer, otherMethodNameTransformer;

    @Mock
    private TypeDescription instrumentedType, typeDescription, returnType, parameterType;

    @Mock
    private TypeDescription.Generic genericReturnType, genericParameterType;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(methodDescription.getReturnType()).thenReturn(genericReturnType);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        when(typeDescription.getInternalName()).thenReturn(BAR);
        when(typeDescription.getDescriptor()).thenReturn(BAR);
        when(instrumentedType.isInterface()).thenReturn(interfaceType);
        when(methodNameTransformer.transform(methodDescription)).thenReturn(QUX);
        when(otherMethodNameTransformer.transform(methodDescription)).thenReturn(FOO + BAR);
        when(parameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(methodDescription, genericParameterType));
        when(genericReturnType.asErasure()).thenReturn(returnType);
        when(genericReturnType.asRawType()).thenReturn(genericReturnType);
        when(genericReturnType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(genericReturnType);
        when(genericParameterType.asErasure()).thenReturn(parameterType);
        when(genericParameterType.asGenericType()).thenReturn(genericParameterType);
        when(parameterType.asGenericType()).thenReturn(genericParameterType);
        when(genericParameterType.asRawType()).thenReturn(genericParameterType);
        when(genericParameterType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(genericParameterType);
    }

    @Test
    public void testPreservation() throws Exception {
        MethodRebaseResolver.Resolution resolution = MethodRebaseResolver.Resolution.ForRebasedMethod.of(instrumentedType,
                methodDescription,
                methodNameTransformer);
        assertThat(resolution.isRebased(), is(true));
        assertThat(resolution.getResolvedMethod().getDeclaringType(), is(typeDescription));
        assertThat(resolution.getResolvedMethod().getInternalName(), is(QUX));
        assertThat(resolution.getResolvedMethod().getModifiers(), is(rebasedMethodModifiers));
        assertThat(resolution.getResolvedMethod().getReturnType(), is(genericReturnType));
        assertThat(resolution.getResolvedMethod().getParameters(), is((ParameterList<ParameterDescription.InDefinedShape>) new ParameterList.Explicit
                .ForTypes(resolution.getResolvedMethod(), parameterType)));
        StackManipulation.Size size = resolution.getAdditionalArguments().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }
}
