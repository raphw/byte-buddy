package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.AbstractSpecialMethodInvocationTest;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;

import static org.mockito.Mockito.mock;

public class RebaseImplementationTargetSpecialMethodInvocationTest extends AbstractSpecialMethodInvocationTest {

    @Override
    protected Implementation.SpecialMethodInvocation make(MethodDescription methodDescription, TypeDescription typeDescription) {
        return new RebaseImplementationTarget.RebasedMethodInvocation(methodDescription, typeDescription, mock(StackManipulation.class));
    }
}
