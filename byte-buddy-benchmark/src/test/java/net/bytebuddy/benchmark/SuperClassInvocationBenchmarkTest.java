package net.bytebuddy.benchmark;

import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.UnsafeAccessRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class SuperClassInvocationBenchmarkTest extends AbstractBlackHoleTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public MethodRule unsafeAccessRule = new UnsafeAccessRule();

    private SuperClassInvocationBenchmark superClassInvocationBenchmark;

    @Before
    public void setUp() throws Exception {
        superClassInvocationBenchmark = new SuperClassInvocationBenchmark();
        try {
            superClassInvocationBenchmark.setUp();
        } catch (Throwable ignored) {
            // cglib and javassist initialization may fail on JDKs where reflective or cross-package
            // class definition is denied; the affected tests are skipped via JavaVersionRule.
        }
    }

    @Test
    @UnsafeAccessRule.Enforce
    public void testBaseline() throws Exception {
        superClassInvocationBenchmark.baseline(blackHole);
    }

    @Test
    @UnsafeAccessRule.Enforce
    public void testByteBuddyWithProxiesBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkByteBuddyWithProxy(blackHole);
    }

    @Test
    @UnsafeAccessRule.Enforce
    public void testByteBuddyWithAccessorsBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkByteBuddyWithAccessor(blackHole);
    }

    @Test
    @UnsafeAccessRule.Enforce
    public void testByteBuddyWithPrefixBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkByteBuddyWithPrefix(blackHole);
    }

    @Test
    @UnsafeAccessRule.Enforce
    public void testByteBuddySpecializedBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkByteBuddySpecialized(blackHole);
    }

    @Test
    @JavaVersionRule.Enforce(atMost = 10)
    @UnsafeAccessRule.Enforce
    public void testCglibBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkCglib(blackHole);
    }

    @Test
    @JavaVersionRule.Enforce(atMost = 10)
    @UnsafeAccessRule.Enforce
    public void testJavassistBenchmark() throws Exception {
        superClassInvocationBenchmark.benchmarkJavassist(blackHole);
    }
}
