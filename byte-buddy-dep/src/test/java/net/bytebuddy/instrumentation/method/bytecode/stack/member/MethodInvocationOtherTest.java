package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodInvocationOtherTest {

    private static final String FOO = "foo";

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodInvocation.class).apply();
        ObjectPropertyAssertion.of(MethodInvocation.IllegalInvocation.class).apply();
        ObjectPropertyAssertion.of(MethodInvocation.Invocation.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription>() {
            @Override
            public void apply(MethodDescription mock) {
                when(mock.getDeclaringType()).thenReturn(mock(TypeDescription.class));
                TypeDescription returnType = mock(TypeDescription.class);
                when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
                when(mock.getReturnType()).thenReturn(returnType);
                when(mock.getInternalName()).thenReturn(FOO);
                when(mock.getParameters()).thenReturn(new ParameterList.Empty());
            }
        }).apply();
        ObjectPropertyAssertion.of(MethodInvocation.DynamicInvocation.class).apply();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegal() throws Exception {
        assertThat(MethodInvocation.IllegalInvocation.INSTANCE.isValid(), is(false));
        assertThat(MethodInvocation.IllegalInvocation.INSTANCE.special(mock(TypeDescription.class)),
                is((StackManipulation) StackManipulation.Illegal.INSTANCE));
        assertThat(MethodInvocation.IllegalInvocation.INSTANCE.virtual(mock(TypeDescription.class)),
                is((StackManipulation) StackManipulation.Illegal.INSTANCE));
        assertThat(MethodInvocation.IllegalInvocation.INSTANCE.dynamic(FOO, mock(TypeDescription.class), mock(TypeList.class), mock(List.class)),
                is((StackManipulation) StackManipulation.Illegal.INSTANCE));
        MethodInvocation.IllegalInvocation.INSTANCE.apply(mock(MethodVisitor.class), mock(Instrumentation.Context.class));
    }
}
