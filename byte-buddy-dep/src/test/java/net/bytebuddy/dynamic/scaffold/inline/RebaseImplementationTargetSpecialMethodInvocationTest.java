package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.implementation.AbstractSpecialMethodInvocationTest;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RebaseImplementationTargetSpecialMethodInvocationTest extends AbstractSpecialMethodInvocationTest {

    private static final String FOO = "foo";

    @Mock
    private TypeDescription returnType;

    @Override
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        when(returnType.asRawType()).thenReturn(returnType);
        when(returnType.accept(Mockito.any(GenericTypeDescription.Visitor.class))).thenReturn(returnType);
    }

    @Override
    protected Implementation.SpecialMethodInvocation make(String name,
                                                          TypeDescription returnType,
                                                          List<TypeDescription> parameterTypes,
                                                          TypeDescription targetType) {
        MethodRebaseResolver.Resolution resolution = mock(MethodRebaseResolver.Resolution.class);
        when(resolution.getAdditionalArguments()).thenReturn(StackManipulation.LegalTrivial.INSTANCE);
        MethodDescription.InDeclaredForm methodDescription = mock(MethodDescription.InDeclaredForm.class);
        when(methodDescription.getInternalName()).thenReturn(name);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(methodDescription, parameterTypes));
        TypeDescription declaringType = mock(TypeDescription.class);
        when(declaringType.asRawType()).thenReturn(declaringType);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(methodDescription.asDeclared()).thenReturn(methodDescription);
        when(resolution.getResolvedMethod()).thenReturn(methodDescription);
        return new RebaseImplementationTarget.RebasedMethodSpecialMethodInvocation(resolution, targetType);
    }

    @Test
    public void testIsValid() throws Exception {
        MethodRebaseResolver.Resolution resolution = mock(MethodRebaseResolver.Resolution.class);
        when(resolution.getAdditionalArguments()).thenReturn(StackManipulation.LegalTrivial.INSTANCE);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.asRawType()).thenReturn(typeDescription);
        when(resolution.getResolvedMethod()).thenReturn(new MethodDescription.Latent(typeDescription,
                FOO,
                Opcodes.ACC_STATIC,
                Collections.<GenericTypeDescription>emptyList(),
                returnType,
                Collections.<ParameterDescription.Token>emptyList(),
                Collections.<GenericTypeDescription>emptyList(),
                Collections.<AnnotationDescription>emptyList(),
                MethodDescription.NO_DEFAULT_VALUE));
        Implementation.SpecialMethodInvocation specialMethodInvocation = new RebaseImplementationTarget.RebasedMethodSpecialMethodInvocation(resolution,
                mock(TypeDescription.class));
        assertThat(specialMethodInvocation.isValid(), is(true));
    }

    @Test
    public void testIsInvalid() throws Exception {
        MethodRebaseResolver.Resolution resolution = mock(MethodRebaseResolver.Resolution.class);
        when(resolution.getAdditionalArguments()).thenReturn(StackManipulation.Illegal.INSTANCE);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.asRawType()).thenReturn(typeDescription);
        when(resolution.getResolvedMethod()).thenReturn(new MethodDescription.Latent(typeDescription,
                FOO,
                Opcodes.ACC_PUBLIC,
                Collections.<GenericTypeDescription>emptyList(),
                returnType,
                Collections.<ParameterDescription.Token>emptyList(),
                Collections.<GenericTypeDescription>emptyList(),
                Collections.<AnnotationDescription>emptyList(),
                MethodDescription.NO_DEFAULT_VALUE));
        Implementation.SpecialMethodInvocation specialMethodInvocation = new RebaseImplementationTarget.RebasedMethodSpecialMethodInvocation(resolution,
                mock(TypeDescription.class));
        assertThat(specialMethodInvocation.isValid(), is(false));
    }
}
