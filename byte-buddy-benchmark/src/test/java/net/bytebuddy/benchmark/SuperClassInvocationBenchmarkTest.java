package net.bytebuddy.benchmark;

import org.junit.Before;
import org.junit.Test;

public class SuperClassInvocationBenchmarkTest extends AbstractBlackHoleTest {

    private SuperClassInvocationBenchmark superClassInvocationBenchmark;

    @Before
    public void setUp() throws Exception {
        superClassInvocationBenchmark = new SuperClassInvocationBenchmark();
        superClassInvocationBenchmark.setUp();
    }

    @Test
    public void testBaseline() throws Exception {
        superClassInvocationBenchmark.baseline(blackHole);
    }

    @Test
    public void testByteBuddyWithProxiesBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkByteBuddyWithProxy(blackHole);
    }

    @Test
    public void testByteBuddyWithAccessorsBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkByteBuddyWithAccessor(blackHole);
    }

    @Test
    public void testByteBuddyWithPrefixBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkByteBuddyWithPrefix(blackHole);
    }

    @Test
    public void testByteBuddySpecializedBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkByteBuddySpecialized(blackHole);
    }

    @Test
    public void testCglibBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkCglib(blackHole);
    }

    @Test
    public void testJavassistBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkJavassist(blackHole);
    }
}
