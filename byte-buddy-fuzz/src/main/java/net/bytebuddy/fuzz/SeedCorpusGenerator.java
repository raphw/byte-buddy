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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * Writes a small set of real class files into a directory so that {@link ClassFileParsingFuzzer} starts from valid
 * seeds rather than from random bytes; coverage-guided fuzzing converges far faster this way. The seeds double as
 * positive regression inputs: the parser must handle every one of these well-formed classes without raising an
 * unexpected error.
 * </p>
 * <p>
 * By default the seeds are written into the resource directory that the Jazzer JUnit integration replays for
 * {@code FuzzRegressionTest.classFileParsing}, namely
 * {@code byte-buddy-fuzz/src/test/resources/net/bytebuddy/fuzz/FuzzRegressionTestInputs/classFileParsing}. A different
 * target directory can be supplied as the single command line argument, which is useful when seeding the corpus of the
 * standalone Jazzer driver.
 * </p>
 */
public final class SeedCorpusGenerator {

    /**
     * The class files that are written as seeds. The selection deliberately mixes JDK types of varying complexity with
     * Byte Buddy's own classes so that generic signatures, annotations and a range of constant pool entries are covered.
     */
    private static final Class<?>[] SEED_CLASSES = {
            Object.class,
            String.class,
            Number.class,
            Integer.class,
            Enum.class,
            Runnable.class,
            List.class,
            ArrayList.class,
            HashMap.class,
            Exception.class,
            Thread.class,
            ByteBuddy.class,
            TypeDescription.class,
            ClassFileLocator.class,
            TypePool.class
    };

    /**
     * The default directory, relative to the repository root, into which seeds are written.
     */
    private static final String DEFAULT_DIRECTORY =
            "byte-buddy-fuzz/src/test/resources/net/bytebuddy/fuzz/FuzzRegressionTestInputs/classFileParsing";

    /**
     * A constructor that should not be invoked as this class only defines a static entry point.
     */
    private SeedCorpusGenerator() {
        throw new UnsupportedOperationException("This class is not supposed to be instantiated");
    }

    /**
     * Writes the seed class files into the target directory.
     *
     * @param arguments An optional single argument that specifies the target directory; the default is used otherwise.
     * @throws IOException If a seed file cannot be written.
     */
    public static void main(String[] arguments) throws IOException {
        File directory = new File(arguments.length == 0 ? DEFAULT_DIRECTORY : arguments[0]);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Could not create directory: " + directory);
        }
        int count = 0;
        for (Class<?> type : SEED_CLASSES) {
            byte[] binaryRepresentation;
            try {
                binaryRepresentation = ClassFileLocator.ForClassLoader.read(type);
            } catch (RuntimeException exception) {
                // A class that cannot be located on the current JVM is skipped rather than failing seed generation.
                System.out.println("Skipping " + type.getName() + ": " + exception.getMessage());
                continue;
            }
            File file = new File(directory, type.getName() + ".class");
            OutputStream outputStream = new FileOutputStream(file);
            try {
                outputStream.write(binaryRepresentation);
            } finally {
                outputStream.close();
            }
            count++;
            System.out.println("Wrote " + binaryRepresentation.length + " bytes: " + file);
        }
        System.out.println("Wrote " + count + " seed inputs to " + directory);
    }
}
