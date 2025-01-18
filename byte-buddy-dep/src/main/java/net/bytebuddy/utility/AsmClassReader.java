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

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.security.PrivilegedAction;

/**
 * A facade for creating a class reader that accepts {@link ClassVisitor} instances and reader flags.
 */
public interface AsmClassReader {

    /**
     * Indicates that no custom attributes should be mapped.
     */
    Attribute[] NO_ATTRIBUTES = new Attribute[0];

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
         * Default implementations for factories of {@link AsmClassReader}s.
         */
        enum Default implements Factory {

            /**
             * Uses a processor as it is configured by {@link OpenedClassReader#PROCESSOR_PROPERTY},
             * or {@link AsmClassWriter.Factory.Default#ASM_FIRST} if no implicit processor is defined.
             */
            IMPLICIT {
                /**
                 * {@inheritDoc}
                 */
                public AsmClassReader make(byte[] binaryRepresentation, boolean experimental) {
                    return FACTORY.make(binaryRepresentation, experimental);
                }
            },

            /**
             * A factory for a class reader that uses ASM's internal implementation whenever possible.
             */
            ASM_FIRST {
                /**
                 * {@inheritDoc}
                 */
                public AsmClassReader make(byte[] binaryRepresentation, boolean experimental) {
                    return ClassFileVersion.ofClassFile(binaryRepresentation).isGreaterThan(ClassFileVersion.latest())
                            ? CLASS_FILE_API_ONLY.make(binaryRepresentation)
                            : ASM_ONLY.make(binaryRepresentation);
                }
            },

            /**
             * A factory for a class reader that uses the class file API whenever possible.
             */
            CLASS_FILE_API_FIRST {
                /**
                 * {@inheritDoc}
                 */
                public AsmClassReader make(byte[] binaryRepresentation, boolean experimental) {
                    return ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V24)
                            ? CLASS_FILE_API_ONLY.make(binaryRepresentation)
                            : ASM_ONLY.make(binaryRepresentation);
                }
            },

            /**
             * A factory for a class reader that always uses ASM's internal implementation.
             */
            ASM_ONLY {
                /**
                 * {@inheritDoc}
                 */
                public AsmClassReader make(byte[] binaryRepresentation, boolean experimental) {
                    return new ForAsm(OpenedClassReader.of(binaryRepresentation, experimental));
                }
            },

            /**
             * A factory for a class reader that always uses the class file API.
             */
            CLASS_FILE_API_ONLY {
                /**
                 * {@inheritDoc}
                 */
                public AsmClassReader make(byte[] binaryRepresentation, boolean experimental) {
                    return new AsmClassReader.ForClassFileApi(ForClassFileApi.DISPATCHER.make(
                            binaryRepresentation,
                            NO_ATTRIBUTES));
                }
            };

            /**
             * The implicit factory to use for writing class files.
             */
            private static final Factory FACTORY;

            /*
             * Resolves the implicit reader factory, if any.
             */
            static {
                String processor;
                try {
                    processor = doPrivileged(new GetSystemPropertyAction(OpenedClassReader.PROCESSOR_PROPERTY));
                } catch (Throwable ignored) {
                    processor = null;
                }
                FACTORY = processor == null ? Default.ASM_FIRST : Default.valueOf(processor);
            }

            /**
             * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
             *
             * @param action The action to execute from a privileged context.
             * @param <T>    The type of the action's resolved value.
             * @return The action's resolved value.
             */
            @MaybeNull
            @AccessControllerPlugin.Enhance
            private static <T> T doPrivileged(PrivilegedAction<T> action) {
                return action.run();
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassReader make(byte[] binaryRepresentation) {
                return make(binaryRepresentation, OpenedClassReader.EXPERIMENTAL);
            }
        }
    }

    /**
     * A class reader for ASM's own {@link ClassReader}.
     */
    class ForAsm implements AsmClassReader {

        /**
         * The class reader that represents the class file to be read.
         */
        private final ClassReader classReader;

        /**
         * Creates a new ASM class reader that uses ASM's internal implementation.
         *
         * @param classReader The class reader that represents the class file to be read.
         */
        public ForAsm(ClassReader classReader) {
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

    /**
     * A class reader that is based upon the Class File API.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForClassFileApi implements AsmClassReader {

        /**
         * A dispatcher to interact with {@code codes.rafael.asmjdkbridge.JdkClassReader}.
         */
        protected static final JdkClassReader DISPATCHER = doPrivileged(JavaDispatcher.of(
                JdkClassReader.class,
                ForClassFileApi.class.getClassLoader()));

        /**
         * The class reader that represents the class file to be read.
         */
        private final Object classReader;

        /**
         * Creates a new class reader that is based upon the Class File API.
         *
         * @param classReader The class reader that represents the class file to be read.
         */
        public ForClassFileApi(Object classReader) {
            if (!DISPATCHER.isInstance(classReader)) {
                throw new IllegalArgumentException();
            }
            this.classReader = classReader;
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
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
            DISPATCHER.accept(classReader, classVisitor, flags);
        }

        /**
         * A dispatcher to interact with {@code codes.rafael.asmjdkbridge.JdkClassReader}.
         */
        @JavaDispatcher.Proxied("codes.rafael.asmjdkbridge.JdkClassReader")
        protected interface JdkClassReader {

            /**
             * Checks if the supplied object is an instance of {@code codes.rafael.asmjdkbridge.JdkClassReader}.
             *
             * @param value The instance to evaluate.
             * @return {@code true} if the supplied object is an instance of {@code codes.rafael.asmjdkbridge.JdkClassReader}.
             */
            @JavaDispatcher.Instance
            boolean isInstance(Object value);

            /**
             * Creates an instance of {@code codes.rafael.asmjdkbridge.JdkClassReader}.
             *
             * @param binaryRepresentation The binary representation of a class file to represent through the reader.
             * @param attribute            An array of attribute prototypes.
             * @return A new instance of {@code codes.rafael.asmjdkbridge.JdkClassReader}.
             */
            @JavaDispatcher.IsConstructor
            Object make(byte[] binaryRepresentation, Attribute[] attribute);

            /**
             * Accepts a class reader to visit the represented class file.
             *
             * @param classReader  The class reader that is being visited.
             * @param classVisitor The class visitor to visit the class.
             * @param flags        The flags to consider during reading.
             */
            void accept(Object classReader, ClassVisitor classVisitor, int flags);
        }
    }
}
