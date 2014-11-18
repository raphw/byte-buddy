package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InstrumentationSpecialMethodInvocationSimpleTest extends AbstractSpecialMethodInvocationTest {

    private static final String FOO = "foo";

    @Override
    protected Instrumentation.SpecialMethodInvocation make(String name,
                                                           TypeDescription returnType,
                                                           List<TypeDescription> parameterTypes,
                                                           TypeDescription targetType) {
        return new Instrumentation.SpecialMethodInvocation.Simple(new MethodDescription.Latent(name,
                mock(TypeDescription.class),
                returnType,
                parameterTypes,
                Opcodes.ACC_PUBLIC,
                Collections.<TypeDescription>emptyList()), targetType, mock(StackManipulation.class));
    }

    @Test
    public void testIsValid() throws Exception {
        StackManipulation stackManipulation = mock(StackManipulation.class);
        when(stackManipulation.isValid()).thenReturn(true);
        Instrumentation.SpecialMethodInvocation specialMethodInvocation =
                new Instrumentation.SpecialMethodInvocation.Simple(new MethodDescription.Latent(FOO,
                        mock(TypeDescription.class),
                        mock(TypeDescription.class),
                        new TypeList.Empty(),
                        Opcodes.ACC_PUBLIC,
                        Collections.<TypeDescription>emptyList()), mock(TypeDescription.class), stackManipulation);
        assertThat(specialMethodInvocation.isValid(), is(true));
    }

    @Test
    public void testIsInvalid() throws Exception {
        StackManipulation stackManipulation = mock(StackManipulation.class);
        when(stackManipulation.isValid()).thenReturn(false);
        Instrumentation.SpecialMethodInvocation specialMethodInvocation =
                new Instrumentation.SpecialMethodInvocation.Simple(new MethodDescription.Latent(FOO,
                        mock(TypeDescription.class),
                        mock(TypeDescription.class),
                        new TypeList.Empty(),
                        Opcodes.ACC_PUBLIC,
                        Collections.<TypeDescription>emptyList()), mock(TypeDescription.class), stackManipulation);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }
}
