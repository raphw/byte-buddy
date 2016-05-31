package net.bytebuddy.implementation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class FixedValueTest extends AbstractImplementationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private Bar bar;

    private static Object makeMethodType(Class<?> returnType, Class<?>... parameterType) throws Exception {
        return JavaType.METHOD_TYPE.load().getDeclaredMethod("methodType", Class.class, Class[].class).invoke(null, returnType, parameterType);
    }

    private static Object makeMethodHandle() throws Exception {
        Object lookup = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("publicLookup").invoke(null);
        return JavaType.METHOD_HANDLES_LOOKUP.load().getDeclaredMethod("findVirtual", Class.class, String.class, JavaType.METHOD_TYPE.load())
                .invoke(lookup, Qux.class, BAR, makeMethodType(Object.class));
    }

    @Before
    public void setUp() throws Exception {
        bar = new Bar();
    }

    @Test
    public void testTypeDescriptionConstantPool() throws Exception {
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(TypeDescription.OBJECT)).getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(qux.newInstance().bar(), is((Object) Object.class));
    }

    @Test
    public void testClassConstantPool() throws Exception {
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(Object.class)).getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(qux.newInstance().bar(), is((Object) Object.class));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeConstantPool() throws Exception {
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(JavaConstant.MethodType.of(void.class, Object.class))).getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(qux.newInstance().bar(), is(makeMethodType(void.class, Object.class)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeConstantPoolValue() throws Exception {
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(makeMethodType(void.class, Object.class))).getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(qux.newInstance().bar(), is(makeMethodType(void.class, Object.class)));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testMethodHandleConstantPool() throws Exception {
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(JavaConstant.MethodHandle.of(Qux.class.getDeclaredMethod("bar")))).getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(JavaConstant.MethodHandle.ofLoaded(qux.newInstance().bar()), is(JavaConstant.MethodHandle.ofLoaded(makeMethodHandle())));
    }

    @Test
    @JavaVersionRule.Enforce(value = 7, hotSpot = 7)
    public void testMethodHandleConstantPoolValue() throws Exception {
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(makeMethodHandle())).getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(JavaConstant.MethodHandle.ofLoaded(qux.newInstance().bar()), is(JavaConstant.MethodHandle.ofLoaded(makeMethodHandle())));
    }

    @Test
    public void testReferenceCall() throws Exception {
        assertType(implement(Foo.class, FixedValue.reference(bar)));
    }

    @Test
    public void testValueCall() throws Exception {
        assertType(implement(Foo.class, FixedValue.value(bar)));
    }

    @Test
    public void testNullValue() throws Exception {
        Class<?> type = implement(Foo.class, FixedValue.nullValue()).getLoaded();
        assertThat(type.getDeclaredFields().length, is(0));
        assertThat(type.getDeclaredMethods().length, is(1));
        assertThat(type.getDeclaredMethod(BAR).invoke(type.newInstance()), nullValue(Object.class));
    }

    @Test
    public void testOriginType() throws Exception {
        Class<?> type = implement(Baz.class, FixedValue.originType()).getLoaded();
        assertThat(type.getDeclaredFields().length, is(0));
        assertThat(type.getDeclaredMethods().length, is(1));
        assertThat(type.getDeclaredMethod(BAR).invoke(type.newInstance()), is((Object) Baz.class));
    }

    @Test
    public void testConstantPoolValue() throws Exception {
        assertThat(FixedValue.value(FOO).hashCode(), is(FixedValue.value(FOO).hashCode()));
        assertThat(FixedValue.value(FOO), is(FixedValue.value(FOO)));
        assertThat(FixedValue.value(FOO).hashCode(), not(FixedValue.value(BAR).hashCode()));
        assertThat(FixedValue.value(FOO), not(FixedValue.value(BAR)));
        assertThat(FixedValue.value(FOO).hashCode(), not(FixedValue.reference(FOO).hashCode()));
        assertThat(FixedValue.value(FOO), not(FixedValue.reference(FOO)));
    }

    @Test
    public void testReferenceValue() throws Exception {
        assertThat(FixedValue.reference(FOO).hashCode(), is(FixedValue.reference(FOO).hashCode()));
        assertThat(FixedValue.reference(FOO), is(FixedValue.reference(FOO)));
        assertThat(FixedValue.reference(FOO).hashCode(), not(FixedValue.value(FOO).hashCode()));
        assertThat(FixedValue.reference(FOO), not(FixedValue.value(FOO)));
        assertThat(FixedValue.reference(FOO).hashCode(), not(FixedValue.reference(BAR).hashCode()));
        assertThat(FixedValue.reference(FOO), not(FixedValue.reference(BAR)));
    }

    @Test
    public void testReferenceValueWithExplicitFieldName() throws Exception {
        assertThat(FixedValue.reference(FOO, QUX).hashCode(), is(FixedValue.reference(FOO, QUX).hashCode()));
        assertThat(FixedValue.reference(FOO, QUX), is(FixedValue.reference(FOO, QUX)));
        assertThat(FixedValue.reference(FOO, QUX).hashCode(), not(FixedValue.reference(BAR, QUX).hashCode()));
        assertThat(FixedValue.reference(FOO, QUX), not(FixedValue.reference(BAR, QUX)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FixedValue.ForPoolValue.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(FixedValue.ForStaticField.class).apply();
        ObjectPropertyAssertion.of(FixedValue.ForOriginType.class).apply();
        ObjectPropertyAssertion.of(FixedValue.ForOriginType.Appender.class).apply();
        ObjectPropertyAssertion.of(FixedValue.ForNullValue.class).apply();
    }

    private void assertType(DynamicType.Loaded<Foo> loaded) throws Exception {
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(instance, instanceOf(Foo.class));
        assertThat((Bar) loaded.getLoaded().getDeclaredMethod(BAR).invoke(instance), is(bar));
        instance.assertZeroCalls();
    }

    public static class Foo extends CallTraceable {

        public Bar bar() {
            register(BAR);
            return new Bar();
        }
    }

    public static class Bar {
        /* empty */
    }

    public static class Qux extends CallTraceable {

        public Object bar() {
            register(BAR);
            return null;
        }
    }

    public static class Baz {

        public Class<?> bar() {
            return null;
        }
    }
}
