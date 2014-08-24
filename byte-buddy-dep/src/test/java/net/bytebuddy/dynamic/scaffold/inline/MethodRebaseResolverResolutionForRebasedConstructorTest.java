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
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getParameterTypes()).thenReturn(new TypeList.Explicit(Arrays.asList(parameterType)));
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getDescriptor()).thenReturn(QUX);
        when(typeDescription.getInternalName()).thenReturn(BAR);
        when(placeholderType.getDescriptor()).thenReturn(BAZ);
        when(otherPlaceHolderType.getDescriptor()).thenReturn(FOO);
    }

    @Test
    public void testPreservation() throws Exception {
        MethodRebaseResolver.Resolution resolution = new MethodRebaseResolver.Resolution.ForRebasedConstructor(methodDescription, placeholderType);
        assertThat(resolution.isRebased(), is(true));
        assertThat(resolution.getResolvedMethod().getDeclaringType(), is(typeDescription));
        assertThat(resolution.getResolvedMethod().getInternalName(), is(FOO));
        assertThat(resolution.getResolvedMethod().getModifiers(), is(MethodRebaseResolver.REBASED_METHOD_MODIFIER));
        assertThat(resolution.getResolvedMethod().getReturnType(), is(returnType));
        assertThat(resolution.getResolvedMethod().getParameterTypes(), is((TypeList) new TypeList.Explicit(Arrays.asList(parameterType, placeholderType))));
        StackManipulation.Size size = resolution.getAdditionalArguments().apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.ACONST_NULL);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new MethodRebaseResolver.Resolution.ForRebasedConstructor(methodDescription, placeholderType).hashCode(),
                is(new MethodRebaseResolver.Resolution.ForRebasedConstructor(methodDescription, placeholderType).hashCode()));
        assertThat(new MethodRebaseResolver.Resolution.ForRebasedConstructor(methodDescription, placeholderType),
                is(new MethodRebaseResolver.Resolution.ForRebasedConstructor(methodDescription, placeholderType)));
        assertThat(new MethodRebaseResolver.Resolution.ForRebasedConstructor(methodDescription, placeholderType).hashCode(),
                not(is(new MethodRebaseResolver.Resolution.ForRebasedConstructor(methodDescription, otherPlaceHolderType).hashCode())));
        assertThat(new MethodRebaseResolver.Resolution.ForRebasedConstructor(methodDescription, placeholderType),
                not(is(new MethodRebaseResolver.Resolution.ForRebasedConstructor(methodDescription, otherPlaceHolderType))));
    }
}
