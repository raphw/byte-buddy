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
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.SuperMethodCall;

import java.lang.reflect.Type;

/**
 * <p>
 * A coverage-guided fuzzing harness that drives Byte Buddy's class generation pipeline into many shapes and then
 * <i>loads</i> the result so that the JVM's byte code verifier checks it. The verifier is a zero-cost oracle: a class
 * that Byte Buddy emits but that the JVM rejects with a {@link VerifyError} or {@link ClassFormatError} is a defect.
 * </p>
 * <p>
 * Unlike the parsing harness, this exercises Byte Buddy's <i>own</i> code rather than the underlying ASM library, which
 * makes it the higher-value target.
 * </p>
 * <p>
 * The harness can be run by the standalone Jazzer driver via {@code --target_class} or replayed as a regression test
 * through the Jazzer JUnit integration; see {@code FuzzRegressionTest}.
 * </p>
 */
public final class ClassGenerationFuzzer {

    /**
     * A constructor that should not be invoked as this class only defines a static fuzzing entry point.
     */
    private ClassGenerationFuzzer() {
        throw new UnsupportedOperationException("This class is not supposed to be instantiated");
    }

    /**
     * The libFuzzer-style entry point that is invoked once per generated input.
     *
     * @param data A provider for pseudo-random, coverage-guided input values.
     */
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        ClassFileVersion classFileVersion;
        try {
            classFileVersion = ClassFileVersion.ofJavaVersion(data.consumeInt(5, ClassFileVersion.latest().getJavaVersion()));
        } catch (IllegalArgumentException ignored) {
            return; // An unsupported class file version is a legitimate, well-defined rejection.
        }
        DynamicType.Builder<?> builder = new ByteBuddy(classFileVersion)
                .subclass(Object.class)
                .name("net.bytebuddy.fuzz.Generated$" + Integer.toHexString(data.consumeInt()));
        int methods = data.consumeInt(0, 16);
        try {
            for (int index = 0; index < methods; index++) {
                builder = builder
                        .defineMethod("method" + index, returnType(data), modifiers(data))
                        .intercept(implementation(data));
            }
            DynamicType.Unloaded<?> unloaded = builder.make(); // The generation step.
            // Loading the type triggers the JVM's byte code verifier, the actual oracle of this harness.
            unloaded.load(ClassGenerationFuzzer.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        } catch (IllegalArgumentException ignored) {
            // Byte Buddy rejecting an invalid request is correct behavior, not a defect.
        } catch (IllegalStateException ignored) {
            // An illegal combination of definition and implementation is rejected during creation.
        }
        // A VerifyError, ClassFormatError or NoClassDefFoundError escaping this method indicates a defect.
    }

    /**
     * Selects a return type for a generated method.
     *
     * @param data A provider for pseudo-random, coverage-guided input values.
     * @return A return type for a generated method.
     */
    private static Type returnType(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 4)) {
            case 0:
                return void.class;
            case 1:
                return int.class;
            case 2:
                return long.class;
            case 3:
                return boolean.class;
            default:
                return Object.class;
        }
    }

    /**
     * Selects a set of modifiers for a generated method.
     *
     * @param data A provider for pseudo-random, coverage-guided input values.
     * @return A set of modifiers for a generated method.
     */
    private static ModifierContributor.ForMethod[] modifiers(FuzzedDataProvider data) {
        Visibility visibility;
        switch (data.consumeInt(0, 2)) {
            case 0:
                visibility = Visibility.PUBLIC;
                break;
            case 1:
                visibility = Visibility.PROTECTED;
                break;
            default:
                visibility = Visibility.PACKAGE_PRIVATE;
                break;
        }
        return new ModifierContributor.ForMethod[]{visibility, data.consumeBoolean() ? Ownership.STATIC : Ownership.MEMBER};
    }

    /**
     * Selects an implementation for a generated method. Many combinations of return type and implementation are
     * intentionally invalid; Byte Buddy is expected to reject those with a well-defined exception rather than by
     * emitting unverifiable byte code.
     *
     * @param data A provider for pseudo-random, coverage-guided input values.
     * @return An implementation for a generated method.
     */
    private static Implementation implementation(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 2)) {
            case 0:
                return StubMethod.INSTANCE;
            case 1:
                return SuperMethodCall.INSTANCE;
            default:
                return FixedValue.nullValue();
        }
    }
}
