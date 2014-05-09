package net.bytebuddy.benchmark;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
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
     * The base class to be subclassed.
     */
    public static final Class<?> BASE_CLASS = Object.class;

    /**
     * Benchmark for a class creation with Byte Buddy.
     *
     * @return The created instance, in order to avoid JIT inlining.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Class<?> testByteBuddyClassCreation() {
        return new ByteBuddy()
                .subclass(BASE_CLASS)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
    }
}
