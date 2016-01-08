package net.bytebuddy.implementation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class MethodCallTypeTest extends AbstractImplementationTest {

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
    public TestRule methodRule = new MockitoRule(this);

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
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodCall.invokeSuper().with(value));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, Object.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(definesFieldConstantPool ? 1 : 0));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(instance, instanceOf(Foo.class));
        assertThat(instance.foo(new Object()), is(value));
    }

    @Test
    public void testFieldReference() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodCall.invokeSuper().withReference(value));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredMethod(FOO, Object.class), not(nullValue(Method.class)));
        assertThat(loaded.getLoaded().getDeclaredConstructors().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(definesFieldReference ? 1 : 0));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(instance, instanceOf(Foo.class));
        assertThat(instance.foo(new Object()), sameInstance(value));
    }

    @Test(expected = IllegalStateException.class)
    public void testNonAssignable() throws Exception {
        implement(Foo.class, MethodCall.invokeSuper().with(value).withAssigner(nonAssigner, Assigner.Typing.STATIC));
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
