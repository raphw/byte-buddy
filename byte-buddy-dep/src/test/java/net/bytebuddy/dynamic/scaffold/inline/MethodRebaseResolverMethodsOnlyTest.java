package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class MethodRebaseResolverMethodsOnlyTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription method, constructor, other;

    @Mock
    private TypeDescription returnType, parameterType, declaringType;

    @Mock
    private MethodRebaseResolver.MethodNameTransformer methodNameTransformer;

    private MethodRebaseResolver methodRebaseResolver;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(parameterType.getStackSize()).thenReturn(StackSize.ZERO);
        Set<MethodDescription> methodDescriptions = new HashSet<MethodDescription>();
        methodDescriptions.add(method);
        methodDescriptions.add(constructor);
        methodRebaseResolver = new MethodRebaseResolver.MethodsOnly(methodDescriptions, methodNameTransformer);
        when(method.getInternalName()).thenReturn(FOO);
        when(method.getReturnType()).thenReturn(returnType);
        when(method.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(method, Collections.singletonList(parameterType)));
        when(constructor.isConstructor()).thenReturn(true);
        when(methodNameTransformer.transform(method)).thenReturn(BAR);
        when(returnType.asRawType()).thenReturn(returnType);
        when(parameterType.asRawType()).thenReturn(parameterType);
        when(parameterType.accept(Mockito.any(GenericTypeDescription.Visitor.class))).thenReturn(parameterType);
        when(method.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.asRawType()).thenReturn(declaringType);
    }

    @Test
    public void testResolutionPreservesNonInstrumentedMethod() throws Exception {
        MethodRebaseResolver.Resolution resolution = methodRebaseResolver.resolve(other);
        assertThat(resolution.isRebased(), is(false));
        assertThat(resolution.getAdditionalArguments(), is((StackManipulation) StackManipulation.LegalTrivial.INSTANCE));
        assertThat(resolution.getResolvedMethod(), is(other));
    }

    @Test
    public void testResolutionInstrumentedMethod() throws Exception {
        MethodRebaseResolver.Resolution resolution = methodRebaseResolver.resolve(method);
        assertThat(resolution.isRebased(), is(true));
        assertThat(resolution.getAdditionalArguments(), is((StackManipulation) StackManipulation.LegalTrivial.INSTANCE));
        assertThat(resolution.getResolvedMethod().getInternalName(), is(BAR));
        assertThat(resolution.getResolvedMethod().getReturnType(), is((GenericTypeDescription) returnType));
        assertThat(resolution.getResolvedMethod().getParameters().asTypeList(),
                is((GenericTypeList) new GenericTypeList.Explicit(Collections.singletonList(parameterType))));
        assertThat(resolution.getResolvedMethod().isSynthetic(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolutionInstrumentedConstructor() throws Exception {
        methodRebaseResolver.resolve(constructor);
    }

    @Test
    public void testNoAuxiliaryTypes() throws Exception {
        assertThat(methodRebaseResolver.getAuxiliaryTypes().size(), is(0));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRebaseResolver.MethodsOnly.class).apply();
    }
}