package net.bytebuddy.dynamic;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.MethodCall;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.TextConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.modifier.Ownership;
import net.bytebuddy.modifier.TypeManifestation;
import net.bytebuddy.modifier.Visibility;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public abstract class AbstractDynamicTypeBuilderTest {

    private static final String FOO = "foo";

    private static final boolean BOOLEAN_VALUE = true;

    private static final int INTEGER_VALUE = 42;

    private static final long LONG_VALUE = 42L;

    private static final float FLOAT_VALUE = 42f;

    private static final double DOUBLE_VALUE = 42d;

    private static final String BOOLEAN_FIELD = "booleanField";

    private static final String BYTE_FIELD = "byteField";

    private static final String CHARACTER_FIELD = "characterField";

    private static final String SHORT_FIELD = "shortField";

    private static final String INTEGER_FIELD = "integerField";

    private static final String LONG_FIELD = "longField";

    private static final String FLOAT_FIELD = "floatField";

    private static final String DOUBLE_FIELD = "doubleField";

    private static final String STRING_FIELD = "stringField";

    protected abstract DynamicType.Builder<?> create();

    @Test
    public void testMethodDefinition() throws Exception {
        Class<?> type = create()
                .defineMethod(FOO, Object.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .throwing(Exception.class)
                .intercept(new Instrumentation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Method method = type.getDeclaredMethod(FOO);
        assertEquals(Object.class, method.getReturnType());
        assertArrayEquals(new Class<?>[]{Exception.class}, method.getExceptionTypes());
        assertThat(method.getModifiers(), is(Modifier.PUBLIC));
        assertThat(method.invoke(type.newInstance()), is((Object) FOO));
    }

    @Test
    public void testAbstractMethodDefinition() throws Exception {
        Class<?> type = create()
                .modifiers(Visibility.PUBLIC, TypeManifestation.ABSTRACT)
                .defineMethod(FOO, Object.class, Collections.<Class<?>>emptyList(), Visibility.PUBLIC)
                .throwing(Exception.class)
                .withoutCode()
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Method method = type.getDeclaredMethod(FOO);
        assertEquals(Object.class, method.getReturnType());
        assertArrayEquals(new Class<?>[]{Exception.class}, method.getExceptionTypes());
        assertThat(method.getModifiers(), is(Modifier.PUBLIC | Modifier.ABSTRACT));
    }

    @Test
    public void testConstructorDefinition() throws Exception {
        Class<?> type = create()
                .defineConstructor(Collections.<Class<?>>singletonList(Void.class), Visibility.PUBLIC)
                .throwing(Exception.class)
                .intercept(MethodCall.invoke(Object.class.getDeclaredConstructor()))
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Constructor<?> constructor = type.getDeclaredConstructor(Void.class);
        assertArrayEquals(new Class<?>[]{Exception.class}, constructor.getExceptionTypes());
        assertThat(constructor.getModifiers(), is(Modifier.PUBLIC));
        assertThat(constructor.newInstance((Object) null), notNullValue(Object.class));
    }

    @Test
    public void testFieldDefinition() throws Exception {
        Class<?> type = create()
                .defineField(FOO, Void.class, Visibility.PUBLIC)
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Field field = type.getDeclaredField(FOO);
        assertEquals(Void.class, field.getType());
        assertThat(field.getModifiers(), is(Modifier.PUBLIC));
    }

    @Test
    public void testFieldDefaultValueDefinition() throws Exception {
        Class<?> type = create()
                .defineField(BOOLEAN_FIELD, boolean.class, Visibility.PUBLIC, Ownership.STATIC).value(BOOLEAN_VALUE)
                .defineField(BYTE_FIELD, byte.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(SHORT_FIELD, short.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(CHARACTER_FIELD, char.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(INTEGER_FIELD, int.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(LONG_FIELD, long.class, Visibility.PUBLIC, Ownership.STATIC).value(LONG_VALUE)
                .defineField(FLOAT_FIELD, float.class, Visibility.PUBLIC, Ownership.STATIC).value(FLOAT_VALUE)
                .defineField(DOUBLE_FIELD, double.class, Visibility.PUBLIC, Ownership.STATIC).value(DOUBLE_VALUE)
                .defineField(STRING_FIELD, String.class, Visibility.PUBLIC, Ownership.STATIC).value(FOO)
                .make()
                .load(new URLClassLoader(new URL[0], null), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredField(BOOLEAN_FIELD).get(null), is((Object) BOOLEAN_VALUE));
        assertThat(type.getDeclaredField(BYTE_FIELD).get(null), is((Object) (byte) INTEGER_VALUE));
        assertThat(type.getDeclaredField(SHORT_FIELD).get(null), is((Object) (short) INTEGER_VALUE));
        assertThat(type.getDeclaredField(CHARACTER_FIELD).get(null), is((Object) (char) INTEGER_VALUE));
        assertThat(type.getDeclaredField(INTEGER_FIELD).get(null), is((Object) INTEGER_VALUE));
        assertThat(type.getDeclaredField(LONG_FIELD).get(null), is((Object) LONG_VALUE));
        assertThat(type.getDeclaredField(FLOAT_FIELD).get(null), is((Object) FLOAT_VALUE));
        assertThat(type.getDeclaredField(DOUBLE_FIELD).get(null), is((Object) DOUBLE_VALUE));
        assertThat(type.getDeclaredField(STRING_FIELD).get(null), is((Object) FOO));
    }
}
