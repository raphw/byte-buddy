package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Field;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationFieldTest extends AbstractInstrumentationTest{

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testFieldAccess() throws Exception{
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(Swap.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        Foo foo = loaded.getLoaded().newInstance();
        assertThat(foo.foo, is(FOO));
        foo.swap();
        assertThat(foo.foo, is(FOO + BAR));
    }

    public static interface Get<T> {

        T get();
    }

    public static interface Set<T> {

        void set(T value);
    }

    public static class Swap {

        public static void swap(@Field(FOO) Get<String> getter, @Field(FOO) Set<String> setter) {
            setter.set(getter.get() + BAR);
        }
    }

    public static class Foo {

        protected String foo = FOO;

        public void swap() {
            /* do nothing */
        }
    }
}
