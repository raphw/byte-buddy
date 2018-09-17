package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceRepeatOnTest {

    private static final String FOO = "foo";

    @Test
    public void testInstanceOfRepeat() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(InstanceOfRepeat.class)
                .visit(Advice.to(InstanceOfRepeat.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) 2));
    }

    @Test
    public void testInstanceOfNoRepeat() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(InstanceOfNoRepeat.class)
                .visit(Advice.to(InstanceOfNoRepeat.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) 1));
    }

    @Test(expected = IllegalStateException.class)
    public void testInstanceOfRepeatOnConstructor() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(InstanceOfRepeat.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testValueRepeatOnConstructor() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(DefaultValueIllegalPrimitiveRepeat.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testInstanceOfPrimitiveRepeat() throws Exception {
        Advice.to(InstanceOfIllegalPrimitiveRepeat.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInstanceOfPrimitiveInstanceOfRepeat() throws Exception {
        Advice.to(InstanceOfIllegalPrimitiveInstanceOfRepeat.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultValuePrimitiveRepeat() throws Exception {
        Advice.to(DefaultValueIllegalPrimitiveRepeat.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonDefaultValuePrimitiveRepeat() throws Exception {
        Advice.to(NonDefaultValueIllegalPrimitiveRepeat.class);
    }

    public static class Sample {

        public String foo() {
            return FOO;
        }
    }

    public static class InstanceOfRepeat {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = InstanceOfRepeat.class)
        private static Object enter(@Advice.Exit Object exit) {
            return exit == null ? new InstanceOfRepeat() : null;
        }
    }

    public static class InstanceOfNoRepeat {

        private int count;

        public int foo() {
            return ++count;
        }

        @Advice.OnMethodExit(repeatOn = InstanceOfRepeat.class)
        private static Object exit() {
            return null;
        }
    }

    public static class InstanceOfIllegalPrimitiveRepeat {

        @Advice.OnMethodExit(repeatOn = InstanceOfRepeat.class)
        private static void exit() {
            /* empty */
        }
    }

    public static class DefaultValueIllegalPrimitiveRepeat {

        @Advice.OnMethodExit(repeatOn = Advice.OnDefaultValue.class)
        private static void exit() {
            /* empty */
        }
    }

    public static class NonDefaultValueIllegalPrimitiveRepeat {

        @Advice.OnMethodExit(repeatOn = Advice.OnNonDefaultValue.class)
        private static void exit() {
            /* empty */
        }
    }

    public static class InstanceOfIllegalPrimitiveInstanceOfRepeat {

        @Advice.OnMethodExit(repeatOn = int.class)
        private static void exit() {
            /* empty */
        }
    }

}
