package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.CallTraceable;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class SuperMethodCallTest extends AbstractImplementationTest {

    private static final String OBJECT_METHOD = "reference";

    private static final String BOOLEAN_METHOD = "aBoolean";

    private static final String BYTE_METHOD = "aByte";

    private static final String SHORT_METHOD = "aShort";

    private static final String CHAR_METHOD = "aChar";

    private static final String INT_METHOD = "aInt";

    private static final String LONG_METHOD = "aLong";

    private static final String FLOAT_METHOD = "aFloat";

    private static final String DOUBLE_METHOD = "aDouble";

    private static final String VOID_METHOD = "aVoid";

    private static final String PARAMETERS_METHOD = "parameters";

    private static final String STRING_VALUE = "foo";

    private static final boolean BOOLEAN_VALUE = true;

    private static final byte BYTE_VALUE = 42;

    private static final short SHORT_VALUE = 42;

    private static final char CHAR_VALUE = '@';

    private static final int INT_VALUE = 42;

    private static final long LONG_VALUE = 42L;

    private static final float FLOAT_VALUE = 42f;

    private static final double DOUBLE_VALUE = 42d;

    private final Matcher<?> matcher;

    private final String methodName;

    private final Class<?>[] methodParameterTypes;

    private final Object[] methodArguments;

    public SuperMethodCallTest(Matcher<?> matcher,
                               String methodName,
                               Class<?>[] methodParameterTypes,
                               Object[] methodArguments) {
        this.matcher = matcher;
        this.methodName = methodName;
        this.methodParameterTypes = methodParameterTypes;
        this.methodArguments = methodArguments;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {is(STRING_VALUE), OBJECT_METHOD, new Class<?>[0], new Object[0]},
                {is(BOOLEAN_VALUE), BOOLEAN_METHOD, new Class<?>[0], new Object[0]},
                {is(BYTE_VALUE), BYTE_METHOD, new Class<?>[0], new Object[0]},
                {is(SHORT_VALUE), SHORT_METHOD, new Class<?>[0], new Object[0]},
                {is(CHAR_VALUE), CHAR_METHOD, new Class<?>[0], new Object[0]},
                {is(INT_VALUE), INT_METHOD, new Class<?>[0], new Object[0]},
                {is(LONG_VALUE), LONG_METHOD, new Class<?>[0], new Object[0]},
                {is(FLOAT_VALUE), FLOAT_METHOD, new Class<?>[0], new Object[0]},
                {is(DOUBLE_VALUE), DOUBLE_METHOD, new Class<?>[0], new Object[0]},
                {nullValue(), VOID_METHOD, new Class<?>[0], new Object[0]},
                {nullValue(), PARAMETERS_METHOD,
                        new Class<?>[]{long.class, float.class, int.class, double.class, Object.class},
                        new Object[]{LONG_VALUE, FLOAT_VALUE, INT_VALUE, DOUBLE_VALUE, STRING_VALUE}}
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInstrumentedMethod() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, SuperMethodCall.INSTANCE);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(11));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(instance, instanceOf(Foo.class));
        assertThat(loaded.getLoaded().getDeclaredMethod(methodName, methodParameterTypes)
                .invoke(instance, methodArguments), (Matcher) matcher);
        instance.assertOnlyCall(methodName, methodArguments);
    }

    @SuppressWarnings("unused")
    public static class Foo extends CallTraceable {

        public Object reference() {
            register(OBJECT_METHOD);
            return STRING_VALUE;
        }

        public boolean aBoolean() {
            register(BOOLEAN_METHOD);
            return BOOLEAN_VALUE;
        }

        public byte aByte() {
            register(BYTE_METHOD);
            return BYTE_VALUE;
        }

        public short aShort() {
            register(SHORT_METHOD);
            return SHORT_VALUE;
        }

        public char aChar() {
            register(CHAR_METHOD);
            return CHAR_VALUE;
        }

        public int aInt() {
            register(INT_METHOD);
            return INT_VALUE;
        }

        public long aLong() {
            register(LONG_METHOD);
            return LONG_VALUE;
        }

        public float aFloat() {
            register(FLOAT_METHOD);
            return FLOAT_VALUE;
        }

        public double aDouble() {
            register(DOUBLE_METHOD);
            return DOUBLE_VALUE;
        }

        public void aVoid() {
            register(VOID_METHOD);
        }

        public void parameters(long l, float f, int i, double d, Object o) {
            register(PARAMETERS_METHOD, l, f, i, d, o);
        }
    }
}
