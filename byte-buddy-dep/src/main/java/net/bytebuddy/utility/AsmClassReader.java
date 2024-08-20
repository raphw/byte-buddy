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
package net.bytebuddy.utility;

import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

/**
 * A facade for creating a class reader that accepts {@link ClassVisitor} instances and reader flags.
 */
public interface AsmClassReader {

    /**
     * Unwraps a class reader to the underlying reader mechanism.
     *
     * @param type The type of the reader that should be unwrapped.
     * @param <T>  The type to unwrap.
     * @return The unwrapped instance or {@code null} if the underlying instance does not represent this type.
     */
    @MaybeNull
    <T> T unwrap(Class<T> type);

    /**
     * Accepts a class visitor to read a class.
     *
     * @param classVisitor The class visitor who should be used as a callback for a class file.
     * @param flags        The flags to consider while reading a class.
     */
    void accept(ClassVisitor classVisitor, int flags);

    /**
     * A factory to create a {@link AsmClassReader}.
     */
    interface Factory {

        /**
         * Creates a class reader for a given class file.
         *
         * @param binaryRepresentation The class file's binary representation.
         * @return A class reader representation for the supplied class file.
         */
        AsmClassReader make(byte[] binaryRepresentation);

        /**
         * Creates a class reader for a given class file.
         *
         * @param binaryRepresentation The class file's binary representation.
         * @param experimental         {@code true} if unknown Java class files versions should also be considered.
         * @return A class reader representation for the supplied class file.
         */
        AsmClassReader make(byte[] binaryRepresentation, boolean experimental);

        /**
         * A default implementation that creates a pure ASM {@link ClassReader}.
         */
        enum Default implements Factory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public AsmClassReader make(byte[] binaryRepresentation) {
                return new AsmClassReader.Default(OpenedClassReader.of(binaryRepresentation));
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassReader make(byte[] binaryRepresentation, boolean experimental) {
                return new AsmClassReader.Default(OpenedClassReader.of(binaryRepresentation, experimental));
            }
        }
    }

    /**
     * A class reader for ASM's default {@link ClassReader}.
     */
    class Default implements AsmClassReader {

        /**
         * Indicates that no custom attributes should be mapped.
         */
        private static final Attribute[] NO_ATTRIBUTES = new Attribute[0];

        /**
         * The class reader that represents the class file to be read.
         */
        private final ClassReader classReader;

        /**
         * Creates a new default ASM class reader.
         *
         * @param classReader The class reader that represents the class file to be read.
         */
        public Default(ClassReader classReader) {
            this.classReader = classReader;
        }

        /**
         * {@inheritDoc}
         */
        @MaybeNull
        public <T> T unwrap(Class<T> type) {
            return type.isInstance(classReader)
                    ? type.cast(classReader)
                    : null;
        }

        /**
         * {@inheritDoc}
         */
        public void accept(ClassVisitor classVisitor, int flags) {
            classReader.accept(classVisitor, NO_ATTRIBUTES, flags);
        }
    }
}
