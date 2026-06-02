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
package net.bytebuddy.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * <p>
 * Replays the saved corpus and any discovered crashing inputs for the fuzzing harnesses as ordinary JUnit 5 tests so
 * that regressions are caught in continuous integration without running a full fuzzing campaign. Each {@link FuzzTest}
 * delegates to the corresponding standalone harness entry point.
 * </p>
 * <p>
 * To instead run an open-ended fuzzing campaign locally, set the {@code JAZZER_FUZZ} environment variable to a non-empty
 * value (for example {@code JAZZER_FUZZ=1 mvn -Pfuzz test}) or invoke the standalone Jazzer driver against the harness
 * classes directly.
 * </p>
 */
public class FuzzRegressionTest {

    /**
     * Replays the corpus for the class generation harness.
     *
     * @param data A provider for the recorded input values.
     */
    @FuzzTest
    public void classGeneration(FuzzedDataProvider data) {
        ClassGenerationFuzzer.fuzzerTestOneInput(data);
    }

    /**
     * Replays the corpus for the class file parsing harness.
     *
     * @param data A provider for the recorded input values.
     */
    @FuzzTest
    public void classFileParsing(FuzzedDataProvider data) {
        ClassFileParsingFuzzer.fuzzerTestOneInput(data);
    }
}
