package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.CallTraceable;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class FieldAccessorTest<T extends CallTraceable,
        S extends CallTraceable,
        U extends CallTraceable,
        V extends CallTraceable,
        X extends CallTraceable,
        Y extends CallTraceable> {

    private static final String FOO = "foo";

    private static final String GET = "get", SET = "set";

    private static final Object STATIC_FIELD = null;

    private static final String STRING_VALUE = "qux";

    private static final boolean BOOLEAN_VALUE = true;

    private static final byte BYTE_VALUE = 42;

    private static final short SHORT_VALUE = 42;

    private static final char CHAR_VALUE = '@';

    private static final int INT_VALUE = 42;

    private static final long LONG_VALUE = 42L;

    private static final float FLOAT_VALUE = 42f;

    private static final double DOUBLE_VALUE = 42d;

    private static final String STRING_DEFAULT_VALUE = "baz";

    private static final boolean BOOLEAN_DEFAULT_VALUE = false;

    private static final byte BYTE_DEFAULT_VALUE = 0;

    private static final short SHORT_DEFAULT_VALUE = 0;

    private static final char CHAR_DEFAULT_VALUE = 0;

    private static final int INT_DEFAULT_VALUE = 0;

    private static final long LONG_DEFAULT_VALUE = 0L;

    private static final float FLOAT_DEFAULT_VALUE = 0f;

    private static final double DOUBLE_DEFAULT_VALUE = 0d;

    private final Object value;

    private final Class<T> instanceGetter;

    private final Class<S> instanceSetter;

    private final Class<U> staticGetter;

    private final Class<V> staticSetter;

    private final Class<?> propertyType;

    public FieldAccessorTest(Object value,
                             Class<T> instanceGetter,
                             Class<S> instanceSetter,
                             Class<U> staticGetter,
                             Class<V> staticSetter,
                             Class<?> propertyType) {
        this.value = value;
        this.instanceGetter = instanceGetter;
        this.instanceSetter = instanceSetter;
        this.staticGetter = staticGetter;
        this.staticSetter = staticSetter;
        this.propertyType = propertyType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BOOLEAN_VALUE,
                        BooleanInstanceGetter.class, BooleanInstanceSetter.class,
                        BooleanClassGetter.class, BooleanClassSetter.class,
                        boolean.class},
                {BYTE_VALUE,
                        ByteInstanceGetter.class, ByteInstanceSetter.class,
                        ByteClassGetter.class, ByteClassSetter.class,
                        byte.class},
                {SHORT_VALUE,
                        ShortInstanceGetter.class, ShortInstanceSetter.class,
                        ShortClassGetter.class, ShortClassSetter.class,
                        short.class},
                {CHAR_VALUE,
                        CharacterInstanceGetter.class, CharacterInstanceSetter.class,
                        CharacterClassGetter.class, CharacterClassSetter.class,
                        char.class},
                {INT_VALUE,
                        IntegerInstanceGetter.class, IntegerInstanceSetter.class,
                        IntegerClassGetter.class, IntegerClassSetter.class,
                        int.class},
                {LONG_VALUE,
                        LongInstanceGetter.class, LongInstanceSetter.class,
                        LongClassGetter.class, LongClassSetter.class,
                        long.class},
                {FLOAT_VALUE,
                        FloatInstanceGetter.class, FloatInstanceSetter.class,
                        FloatClassGetter.class, FloatClassSetter.class,
                        float.class},
                {DOUBLE_VALUE,
                        DoubleInstanceGetter.class, DoubleInstanceSetter.class,
                        DoubleClassGetter.class, DoubleClassSetter.class,
                        double.class},
                {STRING_VALUE,
                        ObjectInstanceGetter.class, ObjectInstanceSetter.class,
                        ObjectClassGetter.class, ObjectClassSetter.class,
                        Object.class}
        });
    }

    @Test
    public void testInstanceGetterBeanProperty() throws Exception {
        testGetter(instanceGetter, FieldAccessor.ofBeanProperty());
    }

    @Test
    public void testStaticGetterBeanProperty() throws Exception {
        testGetter(staticGetter, FieldAccessor.ofBeanProperty());
    }

    @Test
    public void testInstanceGetterExplicit() throws Exception {
        testGetter(instanceGetter, FieldAccessor.ofField(FOO));
    }

    @Test
    public void testStaticGetterExplicit() throws Exception {
        testGetter(staticGetter, FieldAccessor.ofField(FOO));
    }

    @Test
    public void testInstanceGetterField() throws Exception {
        testGetter(instanceGetter, FieldAccessor.of(instanceGetter.getDeclaredField(FOO)));
    }

    @Test
    public void testStaticGetterField() throws Exception {
        testGetter(staticGetter, FieldAccessor.of(staticGetter.getDeclaredField(FOO)));
    }

    @Test
    public void testInstanceSetterBeanProperty() throws Exception {
        testSetter(instanceSetter, FieldAccessor.ofBeanProperty());
    }

    @Test
    public void testStaticSetterBeanProperty() throws Exception {
        testSetter(staticSetter, FieldAccessor.ofBeanProperty());
    }

    @Test
    public void testInstanceSetterExplicit() throws Exception {
        testSetter(instanceSetter, FieldAccessor.ofField(FOO));
    }

    @Test
    public void testStaticSetterExplicit() throws Exception {
        testSetter(staticSetter, FieldAccessor.ofField(FOO));
    }

    @Test
    public void testStaticSetterField() throws Exception {
        testSetter(staticSetter, FieldAccessor.of(staticSetter.getDeclaredField(FOO)));
    }

    @Test
    public void testInstanceSetterField() throws Exception {
        testSetter(instanceSetter, FieldAccessor.of(instanceSetter.getDeclaredField(FOO)));
    }

    @SuppressWarnings("unchecked")
    private <Z extends CallTraceable> void testGetter(Class<Z> target, Implementation implementation) throws Exception {
        DynamicType.Loaded<Z> loaded = new ByteBuddy()
                .subclass(target)
                .method(isDeclaredBy(target))
                .intercept(implementation)
                .make()
                .load(target.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Z instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(target)));
        assertThat(instance, instanceOf(target));
        Method getter = loaded.getLoaded()
                .getDeclaredMethod(GET + Character.toUpperCase(FOO.charAt(0)) + FOO.substring(1));
        assertThat(getter.invoke(instance), is(value));
        instance.assertZeroCalls();
        assertFieldValue(target, instance);
    }

    @SuppressWarnings("unchecked")
    private <Z extends CallTraceable> void testSetter(Class<Z> target, Implementation implementation) throws Exception {
        DynamicType.Loaded<Z> loaded = new ByteBuddy()
                .subclass(target)
                .method(isDeclaredBy(target))
                .intercept(implementation)
                .make()
                .load(target.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Z instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(target)));
        assertThat(instance, instanceOf(target));
        Method setter = loaded.getLoaded()
                .getDeclaredMethod(SET + Character.toUpperCase(FOO.charAt(0)) + FOO.substring(1), propertyType);
        assertThat(setter.invoke(instance, value), nullValue());
        instance.assertZeroCalls();
        assertFieldValue(target, instance);
    }

    private void assertFieldValue(Class<?> fieldHolder, Object instance) throws Exception {
        Field field = fieldHolder.getDeclaredField(FOO);
        boolean isStatic = (Modifier.STATIC & field.getModifiers()) != 0;
        Object fieldValue = isStatic ? field.get(STATIC_FIELD) : field.get(instance);
        assertThat(fieldValue, is(value));
    }

    public static class BooleanInstanceGetter extends CallTraceable {

        protected boolean foo = BOOLEAN_VALUE;

        public boolean getFoo() {
            register(FOO);
            return BOOLEAN_DEFAULT_VALUE;
        }
    }

    public static class BooleanInstanceSetter extends CallTraceable {

        protected boolean foo = BOOLEAN_DEFAULT_VALUE;

        public void setFoo(boolean foo) {
            register(FOO, foo);
        }
    }

    public static class BooleanClassGetter extends CallTraceable {

        protected static boolean foo = BOOLEAN_VALUE;

        public boolean getFoo() {
            register(FOO);
            return BOOLEAN_DEFAULT_VALUE;
        }
    }

    public static class BooleanClassSetter extends CallTraceable {

        protected static boolean foo = BOOLEAN_DEFAULT_VALUE;

        public void setFoo(boolean foo) {
            register(FOO, foo);
        }
    }

    public static class ByteInstanceGetter extends CallTraceable {

        protected byte foo = BYTE_VALUE;

        public byte getFoo() {
            register(FOO);
            return BYTE_DEFAULT_VALUE;
        }
    }

    public static class ByteInstanceSetter extends CallTraceable {

        protected byte foo = BYTE_DEFAULT_VALUE;

        public void setFoo(byte foo) {
            register(FOO, foo);
        }
    }

    public static class ByteClassGetter extends CallTraceable {

        protected static byte foo = BYTE_VALUE;

        public byte getFoo() {
            register(FOO);
            return BYTE_DEFAULT_VALUE;
        }
    }

    public static class ByteClassSetter extends CallTraceable {

        protected static byte foo = BYTE_DEFAULT_VALUE;

        public void setFoo(byte foo) {
            register(FOO, foo);
        }
    }

    public static class ShortInstanceGetter extends CallTraceable {

        protected short foo = SHORT_VALUE;

        public short getFoo() {
            register(FOO);
            return SHORT_DEFAULT_VALUE;
        }
    }

    public static class ShortInstanceSetter extends CallTraceable {

        protected short foo = SHORT_DEFAULT_VALUE;

        public void setFoo(short foo) {
            register(FOO, foo);
        }
    }

    public static class ShortClassGetter extends CallTraceable {

        protected static short foo = SHORT_VALUE;

        public short getFoo() {
            register(FOO);
            return SHORT_DEFAULT_VALUE;
        }
    }

    public static class ShortClassSetter extends CallTraceable {

        protected static short foo = SHORT_DEFAULT_VALUE;

        public void setFoo(short foo) {
            register(FOO, foo);
        }
    }

    public static class IntegerInstanceGetter extends CallTraceable {

        protected int foo = INT_VALUE;

        public int getFoo() {
            register(FOO);
            return INT_DEFAULT_VALUE;
        }
    }

    public static class IntegerInstanceSetter extends CallTraceable {

        protected int foo = INT_DEFAULT_VALUE;

        public void setFoo(int foo) {
            register(FOO, foo);
        }
    }

    public static class IntegerClassGetter extends CallTraceable {

        protected static int foo = INT_VALUE;

        public int getFoo() {
            register(FOO);
            return INT_DEFAULT_VALUE;
        }
    }

    public static class IntegerClassSetter extends CallTraceable {

        protected static int foo = INT_DEFAULT_VALUE;

        public void setFoo(int foo) {
            register(FOO, foo);
        }
    }

    public static class CharacterInstanceGetter extends CallTraceable {

        protected char foo = CHAR_VALUE;

        public char getFoo() {
            register(FOO);
            return CHAR_DEFAULT_VALUE;
        }
    }

    public static class CharacterInstanceSetter extends CallTraceable {

        protected char foo = CHAR_DEFAULT_VALUE;

        public void setFoo(char foo) {
            register(FOO, foo);
        }
    }

    public static class CharacterClassGetter extends CallTraceable {

        protected static char foo = CHAR_VALUE;

        public char getFoo() {
            register(FOO);
            return CHAR_DEFAULT_VALUE;
        }
    }

    public static class CharacterClassSetter extends CallTraceable {

        protected static char foo = CHAR_DEFAULT_VALUE;

        public void setFoo(char foo) {
            register(FOO, foo);
        }
    }

    public static class LongInstanceGetter extends CallTraceable {

        protected long foo = LONG_VALUE;

        public long getFoo() {
            register(FOO);
            return LONG_DEFAULT_VALUE;
        }
    }

    public static class LongInstanceSetter extends CallTraceable {

        protected long foo = LONG_DEFAULT_VALUE;

        public void setFoo(long foo) {
            register(FOO, foo);
        }
    }

    public static class LongClassGetter extends CallTraceable {

        protected static long foo = LONG_VALUE;

        public long getFoo() {
            register(FOO);
            return LONG_DEFAULT_VALUE;
        }
    }

    public static class LongClassSetter extends CallTraceable {

        protected static long foo = LONG_DEFAULT_VALUE;

        public void setFoo(long foo) {
            register(FOO, foo);
        }
    }

    public static class FloatInstanceGetter extends CallTraceable {

        protected float foo = FLOAT_VALUE;

        public float getFoo() {
            register(FOO);
            return FLOAT_DEFAULT_VALUE;
        }
    }

    public static class FloatInstanceSetter extends CallTraceable {

        protected float foo = FLOAT_DEFAULT_VALUE;

        public void setFoo(float foo) {
            register(FOO, foo);
        }
    }

    public static class FloatClassGetter extends CallTraceable {

        protected static float foo = FLOAT_VALUE;

        public float getFoo() {
            register(FOO);
            return FLOAT_DEFAULT_VALUE;
        }
    }

    public static class FloatClassSetter extends CallTraceable {

        protected static float foo = FLOAT_DEFAULT_VALUE;

        public void setFoo(float foo) {
            register(FOO, foo);
        }
    }

    public static class DoubleInstanceGetter extends CallTraceable {

        protected double foo = DOUBLE_VALUE;

        public double getFoo() {
            register(FOO);
            return DOUBLE_DEFAULT_VALUE;
        }
    }

    public static class DoubleInstanceSetter extends CallTraceable {

        protected double foo = DOUBLE_DEFAULT_VALUE;

        public void setFoo(double foo) {
            register(FOO, foo);
        }
    }

    public static class DoubleClassGetter extends CallTraceable {

        protected static double foo = DOUBLE_VALUE;

        public double getFoo() {
            register(FOO);
            return DOUBLE_DEFAULT_VALUE;
        }
    }

    public static class DoubleClassSetter extends CallTraceable {

        protected static double foo = DOUBLE_DEFAULT_VALUE;

        public void setFoo(double foo) {
            register(FOO, foo);
        }
    }

    public static class ObjectInstanceGetter extends CallTraceable {

        protected Object foo = STRING_VALUE;

        public Object getFoo() {
            register(FOO);
            return STRING_DEFAULT_VALUE;
        }
    }

    public static class ObjectInstanceSetter extends CallTraceable {

        protected Object foo = STRING_DEFAULT_VALUE;

        public void setFoo(Object foo) {
            register(FOO, foo);
        }
    }

    public static class ObjectClassGetter extends CallTraceable {

        protected static Object foo = STRING_VALUE;

        public Object getFoo() {
            register(FOO);
            return STRING_DEFAULT_VALUE;
        }
    }

    public static class ObjectClassSetter extends CallTraceable {

        protected static Object foo = STRING_DEFAULT_VALUE;

        public void setFoo(Object foo) {
            register(FOO, foo);
        }
    }
}
