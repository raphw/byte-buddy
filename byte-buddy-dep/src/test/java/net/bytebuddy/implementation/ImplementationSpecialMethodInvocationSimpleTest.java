package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImplementationSpecialMethodInvocationSimpleTest extends AbstractSpecialMethodInvocationTest {

    @Override
    protected Implementation.SpecialMethodInvocation make(MethodDescription methodDescription, TypeDescription typeDescription) {
        return new Implementation.SpecialMethodInvocation.Simple(methodDescription, typeDescription, mock(StackManipulation.class));
    }

    @Test
    public void testHashCode() throws Exception {
        MethodDescription firstMethod = mock(MethodDescription.class), secondMethod = mock(MethodDescription.class);
        MethodDescription.SignatureToken firstToken = mock(MethodDescription.SignatureToken.class), secondToken = mock(MethodDescription.SignatureToken.class);
        when(firstMethod.asSignatureToken()).thenReturn(firstToken);
        when(secondMethod.asSignatureToken()).thenReturn(secondToken);
        TypeDescription firstType = mock(TypeDescription.class), secondType = mock(TypeDescription.class);
        assertThat(new Implementation.SpecialMethodInvocation.Simple(firstMethod, firstType, mock(StackManipulation.class)).hashCode(),
                is(new Implementation.SpecialMethodInvocation.Simple(firstMethod, firstType, mock(StackManipulation.class)).hashCode()));
        assertThat(new Implementation.SpecialMethodInvocation.Simple(firstMethod, firstType, mock(StackManipulation.class)).hashCode(),
                not(new Implementation.SpecialMethodInvocation.Simple(secondMethod, firstType, mock(StackManipulation.class)).hashCode()));
        assertThat(new Implementation.SpecialMethodInvocation.Simple(firstMethod, firstType, mock(StackManipulation.class)).hashCode(),
                not(new Implementation.SpecialMethodInvocation.Simple(firstMethod, secondType, mock(StackManipulation.class)).hashCode()));
    }

    @Test
    public void testEquality() throws Exception {
        MethodDescription firstMethod = mock(MethodDescription.class), secondMethod = mock(MethodDescription.class);
        MethodDescription.SignatureToken firstToken = mock(MethodDescription.SignatureToken.class), secondToken = mock(MethodDescription.SignatureToken.class);
        when(firstMethod.asSignatureToken()).thenReturn(firstToken);
        when(secondMethod.asSignatureToken()).thenReturn(secondToken);
        TypeDescription firstType = mock(TypeDescription.class), secondType = mock(TypeDescription.class);
        assertThat(new Implementation.SpecialMethodInvocation.Simple(firstMethod, firstType, mock(StackManipulation.class)),
                is(new Implementation.SpecialMethodInvocation.Simple(firstMethod, firstType, mock(StackManipulation.class))));
        assertThat(new Implementation.SpecialMethodInvocation.Simple(firstMethod, firstType, mock(StackManipulation.class)),
                not(new Implementation.SpecialMethodInvocation.Simple(secondMethod, firstType, mock(StackManipulation.class))));
        assertThat(new Implementation.SpecialMethodInvocation.Simple(firstMethod, firstType, mock(StackManipulation.class)),
                not(new Implementation.SpecialMethodInvocation.Simple(firstMethod, secondType, mock(StackManipulation.class))));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Implementation.SpecialMethodInvocation.Simple.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription>() {
            @Override
            public void apply(MethodDescription mock) {
                when(mock.asSignatureToken()).thenReturn(Mockito.mock(MethodDescription.SignatureToken.class));
            }
        }).applyBasic();
    }
}
