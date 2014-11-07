package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodInvocationObjectPropertiesTest {

    private static final String FOO = "foo";

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodInvocation.Invocation.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription>() {
            @Override
            public void apply(MethodDescription mock) {
                when(mock.getDeclaringType()).thenReturn(mock(TypeDescription.class));
                TypeDescription returnType = mock(TypeDescription.class);
                when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
                when(mock.getReturnType()).thenReturn(returnType);
                when(mock.getInternalName()).thenReturn(FOO);
                when(mock.getParameterTypes()).thenReturn(new TypeList.Empty());
            }
        }).apply();
    }
}
