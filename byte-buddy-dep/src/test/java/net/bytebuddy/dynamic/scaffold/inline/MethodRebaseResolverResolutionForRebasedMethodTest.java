package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodRebaseResolverResolutionForRebasedMethodTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodRebaseResolver.MethodNameTransformer methodNameTransformer, otherMethodNameTransformer;

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private TypeDescription typeDescription, returnType, parameterType;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        when(typeDescription.getInternalName()).thenReturn(BAR);
        when(typeDescription.getDescriptor()).thenReturn(BAR);
        when(methodNameTransformer.transform(methodDescription)).thenReturn(QUX);
        when(otherMethodNameTransformer.transform(methodDescription)).thenReturn(FOO + BAR);
        when(parameterType.getStackSize()).thenReturn(StackSize.ZERO);
        ParameterList parameterList = ParameterList.Explicit.latent(methodDescription, Collections.singletonList(parameterType));
        when(methodDescription.getParameters()).thenReturn(parameterList);
        when(returnType.asRawType()).thenReturn(returnType); // REFACTOR
        when(parameterType.asRawType()).thenReturn(parameterType); // REFACTOR
    }

    @Test
    public void testPreservation() throws Exception {
        MethodRebaseResolver.Resolution resolution = MethodRebaseResolver.Resolution.ForRebasedMethod.of(methodDescription, methodNameTransformer);
        assertThat(resolution.isRebased(), is(true));
        assertThat(resolution.getResolvedMethod().getDeclaringType(), is(typeDescription));
        assertThat(resolution.getResolvedMethod().getInternalName(), is(QUX));
        assertThat(resolution.getResolvedMethod().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertThat(resolution.getResolvedMethod().getReturnType(), is((GenericTypeDescription) returnType));
        assertThat(resolution.getResolvedMethod().getParameters(), is(ParameterList.Explicit.latent(resolution.getResolvedMethod(), Collections.singletonList(parameterType))));
        StackManipulation.Size size = resolution.getAdditionalArguments().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRebaseResolver.Resolution.ForRebasedMethod.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription>() {
            @Override
            public void apply(MethodDescription mock) {
                when(mock.getParameters()).thenReturn(new ParameterList.Empty());
                when(mock.getExceptionTypes()).thenReturn(new GenericTypeList.Empty());
                when(mock.getDeclaringType()).thenReturn(mock(TypeDescription.class));
                TypeDescription returnType = mock(TypeDescription.class);
                when(returnType.asRawType()).thenReturn(returnType); // REFACTOR
                when(mock.getReturnType()).thenReturn(returnType);
            }
        }).refine(new ObjectPropertyAssertion.Refinement<MethodRebaseResolver.MethodNameTransformer>() {
            @Override
            public void apply(MethodRebaseResolver.MethodNameTransformer mock) {
                when(mock.transform(any(MethodDescription.class))).thenReturn(FOO + System.identityHashCode(mock));
            }
        }).apply();
    }
}
