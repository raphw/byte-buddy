package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class MemberRemovalTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testFieldRemoval() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new MemberRemoval().stripFields(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredField(FOO);
            fail();
        } catch (NoSuchFieldException ignored) {
        }
        assertThat(type.getDeclaredField(BAR), notNullValue(Field.class));
    }

    @Test
    public void testMethodRemoval() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new MemberRemoval().stripMethods(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO);
            fail();
        } catch (NoSuchMethodException ignored) {
        }
        assertThat(type.getDeclaredMethod(BAR), notNullValue(Method.class));
    }

    @Test
    public void testConstructorRemoval() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new MemberRemoval().stripConstructors(takesArguments(0)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredConstructor();
            fail();
        } catch (NoSuchMethodException ignored) {
        }
        assertThat(type.getDeclaredConstructor(Void.class), notNullValue(Constructor.class));
    }

    private static class Sample {

        Void foo;

        Void bar;

        Sample() {
            /* empty */
        }

        Sample(Void ignored) {
            /* empty */
        }

        void foo() {
            /* empty*/
        }

        void bar() {
            /* empty*/
        }
    }
}