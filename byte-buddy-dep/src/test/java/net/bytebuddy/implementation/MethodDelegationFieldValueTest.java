package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationFieldValueTest extends AbstractImplementationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testLegalFieldAccess() throws Exception {
        DynamicType.Loaded<SimpleField> loaded = implement(SimpleField.class, MethodDelegation.to(SimpleInterceptor.class));
        SimpleField instance = loaded.getLoaded().newInstance();
        instance.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        instance.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test
    public void testLegalFieldAccessStatic() throws Exception {
        DynamicType.Loaded<SimpleStaticField> loaded = implement(SimpleStaticField.class, MethodDelegation.to(SimpleInterceptor.class));
        SimpleStaticField instance = loaded.getLoaded().newInstance();
        SimpleStaticField.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        SimpleStaticField.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonAssignableFieldAccess() throws Exception {
        implement(SimpleField.class, MethodDelegation.to(NonAssignableInterceptor.class));
    }

    @Test
    public void testLegalFieldAccessDynamicTyping() throws Exception {
        DynamicType.Loaded<SimpleStaticField> loaded = implement(SimpleStaticField.class, MethodDelegation.to(DynamicInterceptor.class));
        SimpleStaticField instance = loaded.getLoaded().newInstance();
        SimpleStaticField.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        SimpleStaticField.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test
    public void testExtendedFieldMostSpecific() throws Exception {
        DynamicType.Loaded<ExtendedField> loaded = implement(ExtendedField.class, MethodDelegation.to(SimpleInterceptor.class));
        ExtendedField instance = loaded.getLoaded().newInstance();
        instance.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        instance.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test
    public void testExtendedFieldSkipsNonVisible() throws Exception {
        DynamicType.Loaded<ExtendedPrivateField> loaded = implement(ExtendedPrivateField.class, MethodDelegation.to(SimpleInterceptor.class));
        SimpleField instance = loaded.getLoaded().newInstance();
        instance.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        instance.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
    }

    @Test
    public void testExtendedFieldExplicitType() throws Exception {
        DynamicType.Loaded<ExtendedField> loaded = implement(ExtendedField.class, MethodDelegation.to(ExplicitInterceptor.class));
        SimpleField instance = loaded.getLoaded().newInstance();
        instance.foo = FOO;
        assertThat(instance.foo(), is((Object) FOO));
        instance.foo = BAR;
        assertThat(instance.foo(), is((Object) BAR));
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

        @Override
        public Object foo() {
            return null;
        }
    }

    public static class ExtendedPrivateField extends SimpleField {

        private Object foo;

        @Override
        public Object foo() {
            return null;
        }
    }

    public static class ExplicitInterceptor {

        public static Object intercept(@FieldValue(value = FOO, definingType = SimpleField.class) Object value) {
            return value;
        }
    }
}
