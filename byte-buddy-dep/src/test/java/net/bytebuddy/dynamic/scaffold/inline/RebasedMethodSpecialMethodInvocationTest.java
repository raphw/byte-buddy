package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class RebasedMethodSpecialMethodInvocationTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodFlatteningResolver.Resolution resolution;
    @Mock
    private MethodDescription methodDescription;
    @Mock
    private StackManipulation stackManipulation;
    @Mock
    private TypeDescription typeDescription, otherTypeDescription;

    @Before
    public void setUp() throws Exception {
        when(resolution.getResolvedMethod()).thenReturn(methodDescription);
        when(resolution.getAdditionalArguments()).thenReturn(stackManipulation);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(methodDescription.getReturnType()).thenReturn(typeDescription);
        when(methodDescription.getParameterTypes()).thenReturn(new TypeList.Explicit(Arrays.asList(typeDescription)));
        when(typeDescription.getStackSize()).thenReturn(StackSize.ZERO);
        when(methodDescription.getInternalName()).thenReturn(FOO);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation.of(resolution, typeDescription).hashCode(),
                is(RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation.of(resolution, typeDescription).hashCode()));
        assertThat(RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation.of(resolution, typeDescription),
                is(RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation.of(resolution, typeDescription)));
        assertThat(RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation.of(resolution, typeDescription).hashCode(),
                not(is(RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation.of(resolution, otherTypeDescription).hashCode())));
        assertThat(RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation.of(resolution, typeDescription),
                not(is(RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation.of(resolution, otherTypeDescription))));

    }
}
