package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationFieldValueTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testLegalFieldAccess() throws Exception {
        DynamicType.Loaded<SimpleField> loaded = new ByteBuddy()
                .subclass(SimpleField.class)
                .method(isDeclaredBy(SimpleField.class))
                .intercept(MethodDelegation.to(SimpleInterceptor.class))
                .make()
                .load(SimpleField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        SimpleField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        instance.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test
    public void testLegalFieldAccessStatic() throws Exception {
        DynamicType.Loaded<SimpleStaticField> loaded = new ByteBuddy()
                .subclass(SimpleStaticField.class)
                .method(isDeclaredBy(SimpleStaticField.class))
                .intercept(MethodDelegation.to(SimpleInterceptor.class))
                .make()
                .load(SimpleStaticField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        SimpleStaticField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        SimpleStaticField.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        SimpleStaticField.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonAssignableFieldAccess() throws Exception {
        new ByteBuddy()
                .subclass(SimpleField.class)
                .method(isDeclaredBy(SimpleField.class))
                .intercept(MethodDelegation.to(NonAssignableInterceptor.class))
                .make();
    }

    @Test
    public void testLegalFieldAccessDynamicTyping() throws Exception {
        DynamicType.Loaded<SimpleStaticField> loaded = new ByteBuddy()
                .subclass(SimpleStaticField.class)
                .method(isDeclaredBy(SimpleStaticField.class))
                .intercept(MethodDelegation.to(DynamicInterceptor.class))
                .make()
                .load(SimpleStaticField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        SimpleStaticField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        SimpleStaticField.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        SimpleStaticField.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test
    public void testExtendedFieldMostSpecific() throws Exception {
        DynamicType.Loaded<ExtendedField> loaded = new ByteBuddy()
                .subclass(ExtendedField.class)
                .method(isDeclaredBy(ExtendedField.class))
                .intercept(MethodDelegation.to(SimpleInterceptor.class))
                .make()
                .load(ExtendedField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        ExtendedField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        instance.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test
    public void testExtendedFieldSkipsNonVisible() throws Exception {
        DynamicType.Loaded<ExtendedPrivateField> loaded = new ByteBuddy()
                .subclass(ExtendedPrivateField.class)
                .method(isDeclaredBy(ExtendedPrivateField.class))
                .intercept(MethodDelegation.to(SimpleInterceptor.class))
                .make()
                .load(ExtendedPrivateField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        SimpleField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        instance.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test
    public void testExtendedFieldExplicitType() throws Exception {
        DynamicType.Loaded<ExtendedField> loaded = new ByteBuddy()
                .subclass(ExtendedField.class)
                .method(isDeclaredBy(ExtendedField.class))
                .intercept(MethodDelegation.to(ExplicitInterceptor.class))
                .make()
                .load(ExtendedField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        SimpleField instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        instance.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test
    public void testAccessor() throws Exception {
        DynamicType.Loaded<SimpleFieldAccessor> loaded = new ByteBuddy()
                .subclass(SimpleFieldAccessor.class)
                .method(isDeclaredBy(SimpleFieldAccessor.class))
                .intercept(MethodDelegation.to(SimpleAccessorInterceptor.class))
                .make()
                .load(SimpleFieldAccessor.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        SimpleFieldAccessor instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo = FOO;
        assertThat(instance.getFoo(), is((Object) FOO));
        instance.foo = BAR;
        instance.setFoo(FOO);
        assertThat(instance.foo, is((Object) BAR));
    }

    public static class SimpleField {

        public Object foo;

        public Object foo() {
            return null;
        }
    }

    public static class SimpleStaticField {

        public static Object foo;

        public Object foo() {
            return null;
        }
    }

    public static class SimpleInterceptor {

        public static Object intercept(@FieldValue(FOO) Object value) {
            return value;
        }
    }

    public static class NonAssignableInterceptor {

        public static Object intercept(@FieldValue(FOO) String value) {
            return value;
        }
    }

    public static class DynamicInterceptor {

        public static Object intercept(@RuntimeType @FieldValue(FOO) String value) {
            return value;
        }
    }

    public static class ExtendedField extends SimpleField {

        public Object foo;

        public Object foo() {
            return null;
        }
    }

    public static class ExtendedPrivateField extends SimpleField {

        private Object foo;

        public Object foo() {
            return null;
        }
    }

    public static class ExplicitInterceptor {

        public static Object intercept(@FieldValue(value = FOO, declaringType = SimpleField.class) Object value) {
            return value;
        }
    }

    public static class SimpleFieldAccessor {

        public Object foo;

        public Object getFoo() {
            return foo;
        }

        public void setFoo(Object foo) {
            this.foo = foo;
        }
    }

    public static class SimpleAccessorInterceptor {

        public static Object intercept(@FieldValue Object value) {
            return value;
        }
    }
}
