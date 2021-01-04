/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.benchmark.runner;

import net.bytebuddy.benchmark.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * A runner for completing a benchmark with only one JMH fork. This benchmark completes rather quick and can give
 * a great first performance indication. A published benchmark should rather be backed by an execution with additional
 * forks.
 */
public class QuickRunner {

    /**
     * A wildcard for the identification of a benchmark by JMH.
     */
    private static final String WILDCARD = ".*";

    /**
     * This class is not supposed to be constructed.
     */
    private QuickRunner() {
        throw new UnsupportedOperationException();
    }

    /**
     * Executes the benchmark.
     *
     * @param args Unused arguments.
     * @throws RunnerException If the benchmark causes an exception.
     */
    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(WILDCARD + SuperClassInvocationBenchmark.class.getSimpleName() + WILDCARD)
                .include(WILDCARD + StubInvocationBenchmark.class.getSimpleName() + WILDCARD)
                .include(WILDCARD + ClassByImplementationBenchmark.class.getSimpleName() + WILDCARD)
                .include(WILDCARD + ClassByExtensionBenchmark.class.getSimpleName() + WILDCARD)
                .include(WILDCARD + TrivialClassCreationBenchmark.class.getSimpleName() + WILDCARD)
                .forks(0) // Should rather be 1 but there seems to be a bug in JMH.
                .build()).run();
    }
}
