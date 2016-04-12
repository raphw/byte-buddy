package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class TargetMethodAnnotationDriverBinderParameterBinderForFixedValueOfConstantTest {

    private static final String FOO = "foo";

    private static final byte NUMERIC_VALUE = 42;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true},
                {NUMERIC_VALUE},
                {(short) NUMERIC_VALUE},
                {(char) NUMERIC_VALUE},
                {(int) NUMERIC_VALUE},
                {(long) NUMERIC_VALUE},
                {(float) NUMERIC_VALUE},
                {(double) NUMERIC_VALUE},
                {FOO},
                {Object.class}
        });
    }

    private final Object value;

    public TargetMethodAnnotationDriverBinderParameterBinderForFixedValueOfConstantTest(Object value) {
        this.value = value;
    }

    @Test
    public void testConstant() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(MethodDelegation.to(Foo.class)
                        .appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.of(Bar.class, value)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .foo(), is(value));
    }

    public static class Foo {

        public static Object intercept(@Bar Object value) {
            return value;
        }

        public Object foo() {
            throw new AssertionError();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Bar {
        /* empty */
    }
}