package net.bytebuddy.benchmark;

import org.junit.Before;
import org.junit.Test;

public class StubInvocationBenchmarkTest extends AbstractBlackHoleTest {

    private StubInvocationBenchmark stubInvocationBenchmark;

    @Before
    public void setUp() throws Exception {
        stubInvocationBenchmark = new StubInvocationBenchmark();
        stubInvocationBenchmark.setUp();
    }

    @Test
    public void testBaseline() throws Exception {
        stubInvocationBenchmark.baseline(blackHole);
    }

    @Test
    public void testByteBuddyBenchmark() throws Exception {
        stubInvocationBenchmark.benchmarkByteBuddy(blackHole);
    }

    @Test
    public void testCglibBenchmark() throws Exception {
        stubInvocationBenchmark.benchmarkCglib(blackHole);
    }

    @Test
    public void testJavassistBenchmark() throws Exception {
        stubInvocationBenchmark.benchmarkJavassist(blackHole);
    }

    @Test
    public void testJdkProxyBenchmark() throws Exception {
        stubInvocationBenchmark.benchmarkJdkProxy(blackHole);
    }
}
