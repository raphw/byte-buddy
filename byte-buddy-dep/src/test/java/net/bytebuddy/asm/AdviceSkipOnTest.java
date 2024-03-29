package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceSkipOnTest {

    private static final String FOO = "foo";

    @Test
    public void testInstanceOfSkip() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(InstanceOfSkip.class)
                .visit(Advice.to(InstanceOfSkip.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testInstanceOfNoSkip() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(InstanceOfNoSkip.class)
                .visit(Advice.to(InstanceOfNoSkip.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testInstanceOfSkipArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(InstanceOfSkipArray.class)
                .visit(Advice.to(InstanceOfSkipArray.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testInstanceOfNoSkipArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(InstanceOfNoSkipArray.class)
                .visit(Advice.to(InstanceOfNoSkipArray.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testInstanceOfNoSkipNullArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(InstanceOfNoSkipNullArray.class)
                .visit(Advice.to(InstanceOfNoSkipNullArray.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testInstanceOfSkipOnConstructor() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(InstanceOfSkip.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testValueSkipOnConstructor() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(DefaultValueIllegalPrimitiveSkip.class).on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testInstanceOfPrimitiveSkip() throws Exception {
        Advice.to(InstanceOfIllegalPrimitiveSkip.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInstanceOfPrimitiveInstanceOfSkip() throws Exception {
        Advice.to(InstanceOfIllegalPrimitiveInstanceOfSkip.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultValuePrimitiveSkip() throws Exception {
        Advice.to(DefaultValueIllegalPrimitiveSkip.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonDefaultValuePrimitiveSkip() throws Exception {
        Advice.to(NonDefaultValueIllegalPrimitiveSkip.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testRepeatOnNonArrayType() throws Exception {
        Advice.to(NoArrayRepeatOnIndex.class);
    }

    public static class Sample {

        public String foo() {
            return FOO;
        }
    }

    public static class InstanceOfSkip {

        public String foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = InstanceOfSkip.class)
        private static Object enter() {
            return new InstanceOfSkip();
        }
    }

    public static class InstanceOfNoSkip {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = InstanceOfNoSkip.class)
        private static Object enter() {
            return null;
        }
    }

    public static class InstanceOfSkipArray {

        public String foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter(skipOn = InstanceOfSkipArray.class, skipOnIndex = 0)
        private static Object[] enter() {
            return new Object[]{new InstanceOfSkipArray()};
        }
    }

    public static class InstanceOfNoSkipArray {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = InstanceOfNoSkipArray.class, skipOnIndex = 0)
        private static Object[] enter() {
            return new Object[1];
        }
    }

    public static class InstanceOfNoSkipNullArray {

        public String foo() {
            return FOO;
        }

        @Advice.OnMethodEnter(skipOn = InstanceOfSkip.class, skipOnIndex = 0)
        private static Object[] enter() {
            return null;
        }
    }

    public static class InstanceOfIllegalPrimitiveSkip {

        @Advice.OnMethodEnter(skipOn = InstanceOfSkip.class)
        private static void enter() {
            /* empty */
        }
    }

    public static class DefaultValueIllegalPrimitiveSkip {

        @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
        private static void enter() {
            /* empty */
        }
    }

    public static class NonDefaultValueIllegalPrimitiveSkip {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static void enter() {
            /* empty */
        }
    }

    public static class InstanceOfIllegalPrimitiveInstanceOfSkip {

        @Advice.OnMethodEnter(skipOn = int.class)
        private static void enter() {
            /* empty */
        }
    }

    public static class NoArrayRepeatOnIndex {

        @Advice.OnMethodEnter(skipOn = Object.class, skipOnIndex = 0)
        private static Object enter() {
            return null;
        }
    }
}
