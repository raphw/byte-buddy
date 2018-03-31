package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodInvocationOtherTest {

    private static final String FOO = "foo";

    @Test(expected = IllegalStateException.class)
    public void testIllegal() throws Exception {
        assertThat(MethodInvocation.IllegalInvocation.INSTANCE.isValid(), is(false));
        assertThat(MethodInvocation.IllegalInvocation.INSTANCE.special(mock(TypeDescription.class)),
                is((StackManipulation) StackManipulation.Illegal.INSTANCE));
        assertThat(MethodInvocation.IllegalInvocation.INSTANCE.virtual(mock(TypeDescription.class)),
                is((StackManipulation) StackManipulation.Illegal.INSTANCE));
        assertThat(MethodInvocation.IllegalInvocation.INSTANCE.dynamic(FOO, mock(TypeDescription.class), mock(TypeList.class), mock(List.class)),
                is((StackManipulation) StackManipulation.Illegal.INSTANCE));
        assertThat(MethodInvocation.IllegalInvocation.INSTANCE.onHandle(null),
                is((StackManipulation) StackManipulation.Illegal.INSTANCE));
        MethodInvocation.IllegalInvocation.INSTANCE.apply(mock(MethodVisitor.class), mock(Implementation.Context.class));
    }
}
