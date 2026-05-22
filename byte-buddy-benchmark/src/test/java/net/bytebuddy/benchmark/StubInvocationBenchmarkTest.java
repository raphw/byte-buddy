package net.bytebuddy.benchmark;

import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.UnsafeAccessRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class StubInvocationBenchmarkTest extends AbstractBlackHoleTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public MethodRule unsafeAccessRule = new UnsafeAccessRule();

    private StubInvocationBenchmark stubInvocationBenchmark;

    @Before
    public void setUp() throws Exception {
        stubInvocationBenchmark = new StubInvocationBenchmark();
        try {
            stubInvocationBenchmark.setUp();
        } catch (Throwable ignored) {
            // cglib and javassist initialization may fail on JDKs where reflective or cross-package
            // class definition is denied; the affected tests are skipped via JavaVersionRule.
        }
    }

    @Test
    @UnsafeAccessRule.Enforce
    public void testBaseline() throws Exception {
        stubInvocationBenchmark.baseline(blackHole);
    }

    @Test
    @UnsafeAccessRule.Enforce
    public void testByteBuddyBenchmark() throws Exception {
        stubInvocationBenchmark.benchmarkByteBuddy(blackHole);
    }

    @Test
    @JavaVersionRule.Enforce(atMost = 10)
    @UnsafeAccessRule.Enforce
    public void testCglibBenchmark() throws Exception {
        stubInvocationBenchmark.benchmarkCglib(blackHole);
    }

    @Test
    @JavaVersionRule.Enforce(atMost = 10)
    @UnsafeAccessRule.Enforce
    public void testJavassistBenchmark() throws Exception {
        stubInvocationBenchmark.benchmarkJavassist(blackHole);
    }

    @Test
    @UnsafeAccessRule.Enforce
    public void testJdkProxyBenchmark() throws Exception {
        stubInvocationBenchmark.benchmarkJdkProxy(blackHole);
    }
}
