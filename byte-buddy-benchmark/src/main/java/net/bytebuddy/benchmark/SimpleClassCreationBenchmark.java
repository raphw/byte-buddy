package net.bytebuddy.benchmark;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;

/**
 * A benchmark for creating plain subclasses of {@link Object} that do not implement any methods.
 */
public class SimpleClassCreationBenchmark {

    /**
     * Benchmark for a class creation with Byte Buddy.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void testClassCreation() {
    }
}
