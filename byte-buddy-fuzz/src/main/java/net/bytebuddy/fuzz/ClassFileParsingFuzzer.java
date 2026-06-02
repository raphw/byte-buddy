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
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;

/**
 * <p>
 * A coverage-guided fuzzing harness that feeds arbitrary bytes to Byte Buddy's {@link TypePool} so that the lazy parsing
 * of class file metadata, generic signatures and annotations is exercised on untrusted input. The invariant under test
 * is that no input, however malformed, may cause anything other than a bounded set of declared exceptions; a
 * {@link StackOverflowError}, an {@link OutOfMemoryError} from a small input, or a non-terminating loop is a defect.
 * </p>
 * <p>
 * Note that the raw {@code byte[]} to {@code ClassReader} path largely delegates to ASM, which is already continuously
 * fuzzed upstream. The differentiated value of this harness is the {@link TypePool} layer that is Byte Buddy's own code,
 * in particular the parsing of generic type signatures.
 * </p>
 */
public final class ClassFileParsingFuzzer {

    /**
     * The name under which the fuzzed class file is described.
     */
    private static final String NAME = "net.bytebuddy.fuzz.Fuzzed";

    /**
     * A constructor that should not be invoked as this class only defines a static fuzzing entry point.
     */
    private ClassFileParsingFuzzer() {
        throw new UnsupportedOperationException("This class is not supposed to be instantiated");
    }

    /**
     * The libFuzzer-style entry point that is invoked once per generated input.
     *
     * @param data A provider for pseudo-random, coverage-guided input values.
     */
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        process(data.consumeRemainingAsBytes());
    }

    /**
     * Describes the supplied bytes as a type and forces the lazy parsing paths that represent Byte Buddy's own code.
     *
     * @param classFile A byte array that represents a, possibly malformed, class file.
     */
    public static void process(byte[] classFile) {
        TypePool typePool = TypePool.Default.of(ClassFileLocator.Simple.of(NAME, classFile));
        try {
            TypePool.Resolution resolution = typePool.describe(NAME);
            if (resolution.isResolved()) {
                TypeDescription typeDescription = resolution.resolve();
                typeDescription.getModifiers();
                typeDescription.getSuperClass(); // Forces generic signature parsing.
                typeDescription.getInterfaces();
                typeDescription.getTypeVariables();
                typeDescription.getDeclaredAnnotations();
                for (FieldDescription.InDefinedShape field : typeDescription.getDeclaredFields()) {
                    field.getType();
                    field.getDeclaredAnnotations();
                }
                for (MethodDescription.InDefinedShape method : typeDescription.getDeclaredMethods()) {
                    method.getReturnType();
                    method.getParameters();
                    method.getExceptionTypes();
                    method.getDeclaredAnnotations();
                }
            }
        } catch (TypePool.Resolution.NoSuchTypeException ignored) {
            // A type that cannot be resolved is a legitimate, well-defined rejection.
        } catch (IllegalArgumentException ignored) {
            // ASM and Byte Buddy reject structurally malformed input with this exception.
        } catch (IllegalStateException ignored) {
            // Lazily evaluated, malformed metadata is rejected with this exception.
        }
        // A StackOverflowError, OutOfMemoryError or non-terminating loop reaching the fuzzer indicates a defect.
    }
}
