package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.TextConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationChainedTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo";

    @Test
    public void testChainingVoid() throws Exception {
        VoidInterceptor voidInterceptor = new VoidInterceptor();
        DynamicType.Loaded<Foo> dynamicType = instrument(Foo.class, MethodDelegation.to(voidInterceptor)
                .filter(isDeclaredBy(VoidInterceptor.class))
                .andThen(new Instrumentation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE)));
        assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        assertThat(voidInterceptor.intercepted, is(true));
    }

    @Test
    public void testChainingNonVoid() throws Exception {
        NonVoidInterceptor nonVoidInterceptor = new NonVoidInterceptor();
        DynamicType.Loaded<Foo> dynamicType = instrument(Foo.class, MethodDelegation.to(nonVoidInterceptor)
                .filter(isDeclaredBy(NonVoidInterceptor.class))
                .andThen(new Instrumentation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE)));
        assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        assertThat(nonVoidInterceptor.intercepted, is(true));
    }

    public static class Foo {

        public String foo() {
            return null;
        }
    }

    public class VoidInterceptor {

        private boolean intercepted = false;

        public void intercept() {
            intercepted = true;
        }
    }

    public class NonVoidInterceptor {

        private boolean intercepted = false;

        public Integer intercept() {
            intercepted = true;
            return 0;
        }
    }
}
