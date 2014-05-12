package net.bytebuddy.benchmark;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TrivialClassCreationBenchmarkTest {

    private TrivialClassCreationBenchmark trivialClassCreationBenchmark;

    @Before
    public void setUp() throws Exception {
        trivialClassCreationBenchmark = new TrivialClassCreationBenchmark();
    }

    @Test
    public void testByteBuddyClassCreation() throws Exception {
        Class<?> type = trivialClassCreationBenchmark.benchmarkByteBuddy();
        assertNotEquals(TrivialClassCreationBenchmark.BASE_CLASS, type);
        assertEquals(TrivialClassCreationBenchmark.BASE_CLASS, type.getSuperclass());
        assertNotEquals(type, trivialClassCreationBenchmark.benchmarkByteBuddy());
    }

    @Test
    public void testCglibClassCreation() throws Exception {
        Class<?> type = trivialClassCreationBenchmark.benchmarkCglib();
        assertNotEquals(TrivialClassCreationBenchmark.BASE_CLASS, type);
        assertEquals(TrivialClassCreationBenchmark.BASE_CLASS, type.getSuperclass());
        assertNotEquals(type, trivialClassCreationBenchmark.benchmarkCglib());
    }

    @Test
    public void testJavassistClassCreation() throws Exception {
        Class<?> type = trivialClassCreationBenchmark.benchmarkJavassist();
        assertNotEquals(TrivialClassCreationBenchmark.BASE_CLASS, type);
        assertEquals(TrivialClassCreationBenchmark.BASE_CLASS, type.getSuperclass());
        assertNotEquals(type, trivialClassCreationBenchmark.benchmarkJavassist());
    }

    @Test
    public void testJdkProxyClassCreation() throws Exception {
        Class<?> type = trivialClassCreationBenchmark.benchmarkJdkProxy();
        assertNotEquals(Proxy.class, type);
        assertEquals(Proxy.class, type.getSuperclass());
        assertNotEquals(type, trivialClassCreationBenchmark.benchmarkJdkProxy());
    }
}
