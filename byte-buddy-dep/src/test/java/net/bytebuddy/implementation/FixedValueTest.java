package net.bytebuddy.implementation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaInstance;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class FixedValueTest extends AbstractImplementationTest {

    private static final String BAR = "bar";

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
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(new TypeDescription.ForLoadedType(Object.class))).getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertEquals(Object.class, qux.newInstance().bar());
    }

    @Test
    public void testClassConstantPool() throws Exception {
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(Object.class)).getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertEquals(Object.class, qux.newInstance().bar());
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodTypeConstantPool() throws Exception {
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(JavaInstance.MethodType.of(void.class, Object.class))).getLoaded();
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
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleConstantPool() throws Exception {
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(JavaInstance.MethodHandle.of(Qux.class.getDeclaredMethod("bar")))).getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(JavaInstance.MethodHandle.of(qux.newInstance().bar()), is(JavaInstance.MethodHandle.of(makeMethodHandle())));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testMethodHandleConstantPoolValue() throws Exception {
        Class<? extends Qux> qux = implement(Qux.class, FixedValue.value(makeMethodHandle())).getLoaded();
        assertThat(qux.getDeclaredFields().length, is(0));
        assertThat(JavaInstance.MethodHandle.of(qux.newInstance().bar()), is(JavaInstance.MethodHandle.of(makeMethodHandle())));
    }

    @Test
    public void testReferenceCall() throws Exception {
        assertType(implement(Foo.class, FixedValue.reference(bar)));
    }

    @Test
    public void testValueCall() throws Exception {
        assertType(implement(Foo.class, FixedValue.value(bar)));
    }

    private void assertType(DynamicType.Loaded<Foo> loaded) throws Exception {
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().newInstance();
        assertNotEquals(Foo.class, instance.getClass());
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
}
