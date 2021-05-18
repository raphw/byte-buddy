package net.bytebuddy.utility;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class JavaConstantVisitorNoOpTest {

    @Test
    public void testValue() {
        JavaConstant.Simple<?> constant = mock(JavaConstant.Simple.class);
        assertThat(JavaConstant.Visitor.NoOp.INSTANCE.onValue(constant), is((JavaConstant) constant));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testType() {
        JavaConstant.Simple<TypeDescription> constant = mock(JavaConstant.Simple.class);
        assertThat(JavaConstant.Visitor.NoOp.INSTANCE.onType(constant), is((JavaConstant) constant));
    }

    @Test
    public void testMethodType() {
        JavaConstant.MethodType constant = mock(JavaConstant.MethodType.class);
        assertThat(JavaConstant.Visitor.NoOp.INSTANCE.onMethodType(constant), is((JavaConstant) constant));
    }

    @Test
    public void testMethodHandle() {
        JavaConstant.MethodHandle constant = mock(JavaConstant.MethodHandle.class);
        assertThat(JavaConstant.Visitor.NoOp.INSTANCE.onMethodHandle(constant), is((JavaConstant) constant));
    }

    @Test
    public void testDynamic() {
        JavaConstant.Dynamic constant = mock(JavaConstant.Dynamic.class);
        assertThat(JavaConstant.Visitor.NoOp.INSTANCE.onDynamic(constant), is((JavaConstant) constant));
    }
}
