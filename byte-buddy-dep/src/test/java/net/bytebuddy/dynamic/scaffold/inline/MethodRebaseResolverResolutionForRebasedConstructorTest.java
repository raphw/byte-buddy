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
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodRebaseResolverResolutionForRebasedConstructorTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private TypeDescription typeDescription, returnType, parameterType, placeholderType, otherPlaceHolderType;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(parameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(placeholderType.getStackSize()).thenReturn(StackSize.ZERO);
        ParameterList parameterList = ParameterList.Explicit.latent(methodDescription, Collections.singletonList(parameterType));
        when(methodDescription.getParameters()).thenReturn(parameterList);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getDescriptor()).thenReturn(QUX);
        when(typeDescription.getInternalName()).thenReturn(BAR);
        when(placeholderType.getDescriptor()).thenReturn(BAZ);
        when(otherPlaceHolderType.getDescriptor()).thenReturn(FOO);
        when(returnType.asRawType()).thenReturn(returnType); // REFACTOR
        when(parameterType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(parameterType);
        when(parameterType.asRawType()).thenReturn(parameterType); // REFACTOR
    }

    @Test
    public void testPreservation() throws Exception {
        MethodRebaseResolver.Resolution resolution = MethodRebaseResolver.Resolution.ForRebasedConstructor.of(methodDescription, placeholderType);
        assertThat(resolution.isRebased(), is(true));
        assertThat(resolution.getResolvedMethod().getDeclaringType(), is(typeDescription));
        assertThat(resolution.getResolvedMethod().getInternalName(), is(FOO));
        assertThat(resolution.getResolvedMethod().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertThat(resolution.getResolvedMethod().getReturnType(), is((GenericTypeDescription) returnType));
        assertThat(resolution.getResolvedMethod().getParameters(), is(ParameterList.Explicit.latent(resolution.getResolvedMethod(),
                Arrays.asList(parameterType, placeholderType))));
        StackManipulation.Size size = resolution.getAdditionalArguments().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.ACONST_NULL);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRebaseResolver.Resolution.ForRebasedConstructor.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription>() {
            @Override
            public void apply(MethodDescription mock) {
                when(mock.getParameters()).thenReturn(new ParameterList.Empty());
                when(mock.getExceptionTypes()).thenReturn(new GenericTypeList.Empty());
                when(mock.getDeclaringType()).thenReturn(mock(TypeDescription.class));
                TypeDescription returnType = mock(TypeDescription.class);
                when(returnType.asRawType()).thenReturn(returnType); // REFACTOR
                when(mock.getReturnType()).thenReturn(returnType);
                when(mock.getInternalName()).thenReturn(FOO + System.identityHashCode(mock));
            }
        }).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getDescriptor()).thenReturn(FOO + System.identityHashCode(mock));
                when(mock.asRawType()).thenReturn(mock); // REFACTOR
                when(mock.getStackSize()).thenReturn(StackSize.ZERO);
            }
        }).apply();
    }
}
