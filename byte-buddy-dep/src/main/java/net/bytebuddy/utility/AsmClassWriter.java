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

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * A facade for creating a {@link ClassVisitor} that writes a class file.
 */
public interface AsmClassWriter {

    /**
     * Returns the {@link ClassVisitor} to use for writing the class file.
     *
     * @return An appropriate class visitor.
     */
    ClassVisitor getVisitor();

    /**
     * Returns the binary representation of the created class file.
     *
     * @return The binary representation of the created class file.
     */
    byte[] getBinaryRepresentation();

    /**
     * A factory for creating an {@link AsmClassWriter}.
     */
    interface Factory {

        /**
         * Creates a new class writer for the given flags.
         *
         * @param flags The flags to consider while writing a class file.
         * @return An appropriate class writer.
         */
        AsmClassWriter make(int flags);

        /**
         * Creates a new class writer for the given flags, possibly based on a previous class file representation.
         *
         * @param flags       The flags to consider while writing a class file.
         * @param classReader A class reader to consider for writing a class file.
         * @return An appropriate class writer.
         */
        AsmClassWriter make(int flags, AsmClassReader classReader);

        /**
         * Creates a new class writer for the given flags.
         *
         * @param flags    The flags to consider while writing a class file.
         * @param typePool A type pool to use for resolving type information for frame generation.
         * @return An appropriate class writer.
         */
        AsmClassWriter make(int flags, TypePool typePool);

        /**
         * Creates a new class writer for the given flags, possibly based on a previous class file representation.
         *
         * @param flags       The flags to consider while writing a class file.
         * @param classReader A class reader to consider for writing a class file.
         * @param typePool    A type pool to use for resolving type information for frame generation.
         * @return An appropriate class writer.
         */
        AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool);

        /**
         * A default class writer factory for ASM's {@link ClassWriter}.
         */
        enum Default implements Factory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags) {
                return make(flags, TypePool.Empty.INSTANCE);
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, AsmClassReader classReader) {
                return make(flags, classReader, TypePool.Empty.INSTANCE);
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, TypePool typePool) {
                return new AsmClassWriter.Default(new FrameComputingClassWriter(flags, typePool));
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool) {
                ClassReader unwrapped = classReader.unwrap(ClassReader.class);
                return new AsmClassWriter.Default(unwrapped == null
                        ? new FrameComputingClassWriter(flags, typePool)
                        : new FrameComputingClassWriter(unwrapped, flags, typePool));
            }
        }

        /**
         * A class writer factory that suppresses any class reader implementation that might be provided
         * upon constructing a class writer.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Suppressing implements Factory {

            /**
             * The factory to delegate to.
             */
            private final Factory delegate;

            /**
             * Creates a suppressing class writer factory.
             *
             * @param delegate The factory to delegate to.
             */
            public Suppressing(Factory delegate) {
                this.delegate = delegate;
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags) {
                return delegate.make(flags);
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, AsmClassReader classReader) {
                return delegate.make(flags);
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, TypePool typePool) {
                return delegate.make(flags, typePool);
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool) {
                return delegate.make(flags, typePool);
            }
        }
    }

    /**
     * A default implementation for ASM's {@link ClassWriter}.
     */
    class Default implements AsmClassWriter {

        /**
         * The represented class writer.
         */
        private final ClassWriter classWriter;

        /**
         * Creates a new default class writer.
         *
         * @param classWriter The represented class writer.
         */
        public Default(ClassWriter classWriter) {
            this.classWriter = classWriter;
        }

        /**
         * {@inheritDoc}
         */
        public ClassVisitor getVisitor() {
            return classWriter;
        }

        /**
         * {@inheritDoc}
         */
        public byte[] getBinaryRepresentation() {
            return classWriter.toByteArray();
        }
    }

    /**
     * A class writer that piggy-backs on Byte Buddy's {@link TypePool} to avoid class loading or look-up errors when redefining a class.
     * This is not available when creating a new class where automatic frame computation is however not normally a requirement.
     */
    class FrameComputingClassWriter extends ClassWriter {

        /**
         * The type pool to use for computing stack map frames, if required.
         */
        private final TypePool typePool;

        /**
         * Creates a new frame computing class writer.
         *
         * @param flags    The flags to be handed to the writer.
         * @param typePool The type pool to use for computing stack map frames, if required.
         */
        public FrameComputingClassWriter(int flags, TypePool typePool) {
            super(flags);
            this.typePool = typePool;
        }

        /**
         * Creates a new frame computing class writer.
         *
         * @param classReader The class reader from which the original class is read.
         * @param flags       The flags to be handed to the writer.
         * @param typePool    The type pool to use for computing stack map frames, if required.
         */
        public FrameComputingClassWriter(ClassReader classReader, int flags, TypePool typePool) {
            super(classReader, flags);
            this.typePool = typePool;
        }

        /**
         * {@inheritDoc}
         */
        protected String getCommonSuperClass(String leftTypeName, String rightTypeName) {
            TypeDescription leftType = typePool.describe(leftTypeName.replace('/', '.')).resolve();
            TypeDescription rightType = typePool.describe(rightTypeName.replace('/', '.')).resolve();
            if (leftType.isAssignableFrom(rightType)) {
                return leftType.getInternalName();
            } else if (leftType.isAssignableTo(rightType)) {
                return rightType.getInternalName();
            } else if (leftType.isInterface() || rightType.isInterface()) {
                return TypeDescription.ForLoadedType.of(Object.class).getInternalName();
            } else {
                do {
                    TypeDescription.Generic superClass = leftType.getSuperClass();
                    if (superClass == null) {
                        return TypeDescription.ForLoadedType.of(Object.class).getInternalName();
                    }
                    leftType = superClass.asErasure();
                } while (!leftType.isAssignableFrom(rightType));
                return leftType.getInternalName();
            }
        }
    }
}
