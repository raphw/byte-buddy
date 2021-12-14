package net.bytebuddy.utility.dispatcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@RunWith(Parameterized.class)
public class JavaDispatcherDefaultsTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"voidMethod", null},
                {"booleanMethod", false},
                {"byteMethod", (byte) 0},
                {"shortMethod", (short) 0},
                {"charMethod", (char) 0},
                {"intMethod", 0},
                {"longMethod", 0L},
                {"floatMethod", 0f},
                {"doubleMethod", 0d},
                {"objectMethod", null},
                {"booleanArrayMethod", new boolean[0]},
                {"byteArrayMethod", new byte[0]},
                {"shortArrayMethod", new short[0]},
                {"charArrayMethod", new char[0]},
                {"intArrayMethod", new int[0]},
                {"longArrayMethod", new long[0]},
                {"floatArrayMethod", new float[0]},
                {"doubleArrayMethod", new double[0]},
                {"objectArrayMethod", new Object[0]},
                {"objectMultiArrayMethod", new Object[0][]}
        });
    }

    private final String method;

    private final Object value;

    public JavaDispatcherDefaultsTest(String method, Object value) {
        this.method = method;
        this.value = value;
    }

    @Test
    public void testReflection() throws Exception {
        DefaultValueSample sample = JavaDispatcher.of(DefaultValueSample.class, null, false).run();
        assertThat(sample.getClass().getMethod(method).invoke(sample), is(value));
    }

    @Test
    public void testGeneration() throws Exception {
        DefaultValueSample sample = JavaDispatcher.of(DefaultValueSample.class, null, true).run();
        assertThat(sample.getClass().getMethod(method).invoke(sample), is(value));
    }

    @JavaDispatcher.Defaults
    @JavaDispatcher.Proxied("net.bytebuddy.Inexistent")
    @SuppressWarnings("unused")
    public interface DefaultValueSample {

        void voidMethod();

        boolean booleanMethod();

        byte byteMethod();

        short shortMethod();

        char charMethod();

        int intMethod();

        long longMethod();

        float floatMethod();

        double doubleMethod();

        Object objectMethod();

        boolean[] booleanArrayMethod();

        byte[] byteArrayMethod();

        char[] charArrayMethod();

        short[] shortArrayMethod();

        int[] intArrayMethod();

        long[] longArrayMethod();

        float[] floatArrayMethod();

        double[] doubleArrayMethod();

        Object[] objectArrayMethod();

        Object[][] objectMultiArrayMethod();
    }
}
