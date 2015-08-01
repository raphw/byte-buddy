package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImplementationSpecialMethodInvocationSimpleTest extends AbstractSpecialMethodInvocationTest {

    @Override
    protected Implementation.SpecialMethodInvocation make(MethodDescription methodDescription, TypeDescription typeDescription) {
        return new Implementation.SpecialMethodInvocation.Simple(methodDescription, typeDescription, mock(StackManipulation.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Implementation.SpecialMethodInvocation.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription>() {
            @Override
            public void apply(MethodDescription mock) {
                when(mock.asToken()).thenReturn(mock(MethodDescription.Token.class));
            }
        }).applyBasic();
    }
}
