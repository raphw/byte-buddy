package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class MethodCallTypeTest {

    private static final String FOO = "foo";

    private static final String STRING_VALUE = "foo";

    private static final Bar ENUM_VALUE = Bar.INSTANCE;

    private static final Class<?> CLASS_VALUE = Object.class;

    private static final boolean BOOLEAN_VALUE = true;

    private static final byte BYTE_VALUE = 42;

    private static final short SHORT_VALUE = 42;

    private static final char CHAR_VALUE = '@';

    private static final int INT_VALUE = 42;

    private static final long LONG_VALUE = 42L;

    private static final float FLOAT_VALUE = 42f;

    private static final double DOUBLE_VALUE = 42d;

    private static final Object NULL_CONSTANT = null;

    private static final Object REFERENCE_VALUE = new Object();

    private final Object value;

    private final boolean definesFieldReference;

    private final boolean definesFieldConstantPool;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Assigner nonAssigner;

    public MethodCallTypeTest(Object value, boolean definesFieldReference, boolean definesFieldConstantPool) {
        this.value = value;
        this.definesFieldReference = definesFieldReference;
        this.definesFieldConstantPool = definesFieldConstantPool;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BOOLEAN_VALUE, true, false},
                {BYTE_VALUE, true, false},
                {SHORT_VALUE, true, false},
                {CHAR_VALUE, true, false},
                {INT_VALUE, true, false},
                {LONG_VALUE, true, false},
                {FLOAT_VALUE, true, false},
                {DOUBLE_VALUE, true, false},
                {NULL_CONSTANT, false, false},
                {STRING_VALUE, true, false},
                {CLASS_VALUE, true, false},
                {ENUM_VALUE, true, false},
                {REFERENCE_VALUE, true, true}
        });
    }

    @Before
    public void setUp() throws Exception {
        when(nonAssigner.assign(Mockito.any(TypeDescription.Generic.class), Mockito.any(TypeDescription.Generic.class), Mockito.any(Assigner.Typing.class)))
                .thenReturn(StackManipulation.Illegal.INSTANCE);
    }

    @Test
    public void testFieldConstantPool() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodCall.invokeSuper().with(value))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, Object.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(definesFieldConstantPool ? 1 : 0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(instance, instanceOf(Foo.class));
        assertThat(instance.foo(new Object()), is(value));
    }

    @Test
    public void testFieldReference() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodCall.invokeSuper().withReference(value))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, Object.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(definesFieldReference ? 1 : 0));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(instance, instanceOf(Foo.class));
        assertThat(instance.foo(new Object()), sameInstance(value));
    }

    @Test(expected = IllegalStateException.class)
    public void testNonAssignable() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodCall.invokeSuper().with(value).withAssigner(nonAssigner, Assigner.Typing.STATIC))
                .make();
    }

    public enum Bar {
        INSTANCE;
    }

    public static class Foo {

        public Object foo(Object value) {
            return value;
        }
    }
}
