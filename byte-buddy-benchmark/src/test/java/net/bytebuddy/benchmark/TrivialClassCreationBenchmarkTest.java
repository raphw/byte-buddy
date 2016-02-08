package net.bytebuddy.benchmark;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TrivialClassCreationBenchmarkTest {

    private TrivialClassCreationBenchmark trivialClassCreationBenchmark;

    @Before
    public void setUp() throws Exception {
        trivialClassCreationBenchmark = new TrivialClassCreationBenchmark();
    }

    @Test
    public void testBaseline() throws Exception {
        Class<?> type = trivialClassCreationBenchmark.baseline();
        assertThat(type, CoreMatchers.<Class<?>>is(TrivialClassCreationBenchmark.BASE_CLASS));
    }

    @Test
    public void testByteBuddyClassCreation() throws Exception {
        Class<?> type = trivialClassCreationBenchmark.benchmarkByteBuddy();
        assertThat(type, not(CoreMatchers.<Class<?>>is(TrivialClassCreationBenchmark.BASE_CLASS)));
        assertThat(type.getSuperclass(), CoreMatchers.<Class<?>>is(TrivialClassCreationBenchmark.BASE_CLASS));
        assertThat(trivialClassCreationBenchmark.benchmarkByteBuddy(), not(CoreMatchers.<Class<?>>is(type)));
    }

    @Test
    public void testCglibClassCreation() throws Exception {
        Class<?> type = trivialClassCreationBenchmark.benchmarkCglib();
        assertThat(type, not(CoreMatchers.<Class<?>>is(TrivialClassCreationBenchmark.BASE_CLASS)));
        assertThat(type.getSuperclass(), CoreMatchers.<Class<?>>is(TrivialClassCreationBenchmark.BASE_CLASS));
        assertThat(trivialClassCreationBenchmark.benchmarkCglib(), not(CoreMatchers.<Class<?>>is(type)));
    }

    @Test
    public void testJavassistClassCreation() throws Exception {
        Class<?> type = trivialClassCreationBenchmark.benchmarkJavassist();
        assertThat(type, not(CoreMatchers.<Class<?>>is(TrivialClassCreationBenchmark.BASE_CLASS)));
        assertThat(type.getSuperclass(), CoreMatchers.<Class<?>>is(TrivialClassCreationBenchmark.BASE_CLASS));
        assertThat(trivialClassCreationBenchmark.benchmarkJavassist(), not(CoreMatchers.<Class<?>>is(type)));
    }

    @Test
    public void testJdkProxyClassCreation() throws Exception {
        Class<?> type = trivialClassCreationBenchmark.benchmarkJdkProxy();
        assertThat(type, not(CoreMatchers.<Class<?>>is(TrivialClassCreationBenchmark.BASE_CLASS)));
        assertThat(type.getSuperclass(), CoreMatchers.<Class<?>>is(Proxy.class));
        assertThat(trivialClassCreationBenchmark.benchmarkJdkProxy(), not(CoreMatchers.<Class<?>>is(type)));
    }
}
