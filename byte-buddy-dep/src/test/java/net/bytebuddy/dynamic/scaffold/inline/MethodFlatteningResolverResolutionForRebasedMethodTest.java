package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MethodFlatteningResolverResolutionForRebasedMethodTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;
    @Mock
    private MethodFlatteningResolver.MethodNameTransformer methodNameTransformer, otherMethodNameTransformer;
    @Mock
    private StackManipulation stackManipulation;
    @Mock
    private TypeDescription typeDescription, returnType, parameterType;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getParameterTypes()).thenReturn(new TypeList.Explicit(Arrays.asList(parameterType)));
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        when(typeDescription.getInternalName()).thenReturn(BAR);
        when(typeDescription.getDescriptor()).thenReturn(BAR);
        when(methodNameTransformer.transform(FOO)).thenReturn(QUX);
        when(otherMethodNameTransformer.transform(FOO)).thenReturn(FOO + BAR);
    }

    @Test
    public void testPreservation() throws Exception {
        MethodFlatteningResolver.Resolution resolution = new MethodFlatteningResolver.Resolution.ForRebasedMethod(methodDescription, methodNameTransformer);
        assertThat(resolution.isRebased(), is(true));
        assertThat(resolution.getResolvedMethod().getDeclaringType(), is(typeDescription));
        assertThat(resolution.getResolvedMethod().getInternalName(), is(QUX));
        assertThat(resolution.getResolvedMethod().getModifiers(), is(MethodFlatteningResolver.REBASED_METHOD_MODIFIER));
        assertThat(resolution.getResolvedMethod().getReturnType(), is(returnType));
        assertThat(resolution.getResolvedMethod().getParameterTypes(), is((TypeList) new TypeList.Explicit(Arrays.asList(parameterType))));
        StackManipulation.Size size = resolution.getAdditionalArguments().apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new MethodFlatteningResolver.Resolution.ForRebasedMethod(methodDescription, methodNameTransformer).hashCode(),
                is(new MethodFlatteningResolver.Resolution.ForRebasedMethod(methodDescription, methodNameTransformer).hashCode()));
        assertThat(new MethodFlatteningResolver.Resolution.ForRebasedMethod(methodDescription, methodNameTransformer),
                is(new MethodFlatteningResolver.Resolution.ForRebasedMethod(methodDescription, methodNameTransformer)));
        assertThat(new MethodFlatteningResolver.Resolution.ForRebasedMethod(methodDescription, methodNameTransformer).hashCode(),
                not(is(new MethodFlatteningResolver.Resolution.ForRebasedMethod(methodDescription, otherMethodNameTransformer).hashCode())));
        assertThat(new MethodFlatteningResolver.Resolution.ForRebasedMethod(methodDescription, methodNameTransformer),
                not(is(new MethodFlatteningResolver.Resolution.ForRebasedMethod(methodDescription, otherMethodNameTransformer))));
    }
}
