package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Field;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationFieldTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testExplicitFieldAccess() throws Exception {
        DynamicType.Loaded<Explicit> loaded = instrument(Explicit.class, MethodDelegation.to(Swap.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        Explicit explicit = loaded.getLoaded().newInstance();
        assertThat(explicit.foo, is(FOO));
        explicit.swap();
        assertThat(explicit.foo, is(FOO + BAR));
    }

    @Test
    public void testImplicitFieldGetterAccess() throws Exception {
        DynamicType.Loaded<ImplicitGetter> loaded = instrument(ImplicitGetter.class, MethodDelegation.to(GetInterceptor.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        ImplicitGetter implicitGetter = loaded.getLoaded().newInstance();
        assertThat(implicitGetter.foo, is(FOO));
        assertThat(implicitGetter.getFoo(), is(FOO + BAR));
        assertThat(implicitGetter.foo, is(FOO + BAR));
    }

    @Test
    public void testImplicitFieldSetterAccess() throws Exception {
        DynamicType.Loaded<ImplicitSetter> loaded = instrument(ImplicitSetter.class, MethodDelegation.to(SetInterceptor.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        ImplicitSetter implicitSetter = loaded.getLoaded().newInstance();
        assertThat(implicitSetter.foo, is(FOO));
        implicitSetter.setFoo(BAR);
        assertThat(implicitSetter.foo, is(FOO + BAR));
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

    public static class Explicit {

        protected String foo = FOO;

        public void swap() {
            /* do nothing */
        }
    }

    public static class ImplicitGetter {

        protected String foo = FOO;

        public String getFoo() {
            return null;
        }
    }

    public static class GetInterceptor {

        public static String get(@Field Get<String> getter, @Field Set<String> setter) {
            setter.set(getter.get() + BAR);
            return getter.get();
        }
    }

    public static class ImplicitSetter {

        protected String foo = FOO;

        public void setFoo(String value) {
            /* do nothing */
        }
    }

    public static class SetInterceptor {

        public static void set(@Argument(0) String value, @Field Get<String> getter, @Field Set<String> setter) {
            setter.set(getter.get() + value);
        }
    }
}
