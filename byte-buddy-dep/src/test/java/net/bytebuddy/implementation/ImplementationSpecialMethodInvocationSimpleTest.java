package net.bytebuddy.implementation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImplementationSpecialMethodInvocationSimpleTest extends AbstractSpecialMethodInvocationTest {

    private static final String FOO = "foo";

    @Override
    protected Implementation.SpecialMethodInvocation make(String name,
                                                          TypeDescription returnType,
                                                          List<TypeDescription> parameterTypes,
                                                          TypeDescription targetType) {
        List<ParameterDescription.Token> tokens = new ArrayList<ParameterDescription.Token>(parameterTypes.size());
        for (TypeDescription parameterType : parameterTypes) {
            tokens.add(new ParameterDescription.Token(parameterType));
        }
        return new Implementation.SpecialMethodInvocation.Simple(new MethodDescription.Latent(name,
                mock(TypeDescription.class),
                returnType,
                tokens,
                Opcodes.ACC_PUBLIC,
                Collections.<TypeDescription>emptyList(),
                Collections.<AnnotationDescription>emptyList()), targetType, mock(StackManipulation.class));
    }

    @Test
    public void testIsValid() throws Exception {
        StackManipulation stackManipulation = mock(StackManipulation.class);
        when(stackManipulation.isValid()).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation =
                new Implementation.SpecialMethodInvocation.Simple(new MethodDescription.Latent(FOO,
                        mock(TypeDescription.class),
                        mock(TypeDescription.class),
                        Collections.<ParameterDescription.Token>emptyList(),
                        Opcodes.ACC_PUBLIC,
                        Collections.<GenericTypeDescription>emptyList(),
                        Collections.<AnnotationDescription>emptyList()), mock(TypeDescription.class), stackManipulation);
        assertThat(specialMethodInvocation.isValid(), is(true));
    }

    @Test
    public void testIsInvalid() throws Exception {
        StackManipulation stackManipulation = mock(StackManipulation.class);
        when(stackManipulation.isValid()).thenReturn(false);
        Implementation.SpecialMethodInvocation specialMethodInvocation =
                new Implementation.SpecialMethodInvocation.Simple(new MethodDescription.Latent(FOO,
                        mock(TypeDescription.class),
                        mock(TypeDescription.class),
                        Collections.<ParameterDescription.Token>emptyList(),
                        Opcodes.ACC_PUBLIC,
                        Collections.<TypeDescription>emptyList(),
                        Collections.<AnnotationDescription>emptyList()), mock(TypeDescription.class), stackManipulation);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }
}
