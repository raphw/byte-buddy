package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.AbstractSpecialMethodInvocationTest;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RebaseInstrumentationTargetSpecialMethodInvocationTest extends AbstractSpecialMethodInvocationTest {

    private static final String FOO = "foo";

    @Override
    protected Instrumentation.SpecialMethodInvocation make(String name,
                                                           TypeDescription returnType,
                                                           List<TypeDescription> parameterTypes,
                                                           TypeDescription targetType) {
        MethodRebaseResolver.Resolution resolution = mock(MethodRebaseResolver.Resolution.class);
        when(resolution.getAdditionalArguments()).thenReturn(StackManipulation.LegalTrivial.INSTANCE);
        when(resolution.getResolvedMethod()).thenReturn(new MethodDescription.Latent(
                name, mock(TypeDescription.class), returnType, parameterTypes, Opcodes.ACC_PUBLIC, new TypeList.Empty()));
        return new RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation(resolution, targetType);
    }

    @Test
    public void testIsValid() throws Exception {
        MethodRebaseResolver.Resolution resolution = mock(MethodRebaseResolver.Resolution.class);
        when(resolution.getAdditionalArguments()).thenReturn(StackManipulation.LegalTrivial.INSTANCE);
        when(resolution.getResolvedMethod()).thenReturn(new MethodDescription.Latent(
                FOO, mock(TypeDescription.class), mock(TypeDescription.class), new TypeList.Empty(), Opcodes.ACC_STATIC, new TypeList.Empty()));
        Instrumentation.SpecialMethodInvocation specialMethodInvocation =
                new RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation(resolution, mock(TypeDescription.class));
        assertThat(specialMethodInvocation.isValid(), is(true));
    }

    @Test
    public void testIsInvalid() throws Exception {
        MethodRebaseResolver.Resolution resolution = mock(MethodRebaseResolver.Resolution.class);
        when(resolution.getAdditionalArguments()).thenReturn(StackManipulation.Illegal.INSTANCE);
        when(resolution.getResolvedMethod()).thenReturn(new MethodDescription.Latent(
                FOO, mock(TypeDescription.class), mock(TypeDescription.class), new TypeList.Empty(), Opcodes.ACC_PUBLIC, new TypeList.Empty()));
        Instrumentation.SpecialMethodInvocation specialMethodInvocation =
                new RebaseInstrumentationTarget.RebasedMethodSpecialMethodInvocation(resolution, mock(TypeDescription.class));
        assertThat(specialMethodInvocation.isValid(), is(false));
    }
}
