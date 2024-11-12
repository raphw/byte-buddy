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
package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.AsmClassReader;
import net.bytebuddy.utility.AsmClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * A class writer strategy is responsible for the creation of a {@link ClassWriter} when creating a type.
 *
 * @deprecated Use {@link AsmClassWriter.Factory}.
 */
@Deprecated
public interface ClassWriterStrategy {

    /**
     * Resolves a class writer.
     *
     * @param flags    The flags to set.
     * @param typePool A type pool for locating types.
     * @return The class writer to use.
     */
    ClassWriter resolve(int flags, TypePool typePool);

    /**
     * Resolves a class writer.
     *
     * @param flags       The flags to set.
     * @param typePool    A type pool for locating types.
     * @param classReader The class reader from which the original class is read.
     * @return The class writer to use.
     */
    ClassWriter resolve(int flags, TypePool typePool, ClassReader classReader);

    /**
     * Default implementations of class writer strategies.
     *
     * @deprecated Use {@link AsmClassWriter.Factory.Suppressing} or {@link net.bytebuddy.ByteBuddy#withIgnoredClassReader()}.
     */
    @Deprecated
    enum Default implements ClassWriterStrategy {

        /**
         * A class writer strategy that retains the original class's constant pool if applicable.
         */
        CONSTANT_POOL_RETAINING {
            /** {@inheritDoc} */
            public ClassWriter resolve(int flags, TypePool typePool, ClassReader classReader) {
                return new FrameComputingClassWriter(classReader, flags, typePool);
            }
        },

        /**
         * A class writer strategy that discards the original class's constant pool if applicable.
         */
        CONSTANT_POOL_DISCARDING {
            /** {@inheritDoc} */
            public ClassWriter resolve(int flags, TypePool typePool, ClassReader classReader) {
                return resolve(flags, typePool);
            }
        };

        /**
         * {@inheritDoc}
         */
        public ClassWriter resolve(int flags, TypePool typePool) {
            return new FrameComputingClassWriter(flags, typePool);
        }
    }

    /**
     * A class writer factory that delegates to a {@link ClassWriterStrategy}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Delegating implements AsmClassWriter.Factory {

        /**
         * The class writer strategy to delegate to.
         */
        private final ClassWriterStrategy classWriterStrategy;

        /**
         * Creates a delegating class writer factory.
         *
         * @param classWriterStrategy The class writer strategy to delegate to.
         */
        public Delegating(ClassWriterStrategy classWriterStrategy) {
            this.classWriterStrategy = classWriterStrategy;
        }

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
            return new AsmClassWriter.ForAsm(classWriterStrategy.resolve(flags, typePool));
        }

        /**
         * {@inheritDoc}
         */
        public AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool) {
            ClassReader unwrapped = classReader.unwrap(ClassReader.class);
            return new AsmClassWriter.ForAsm(unwrapped == null
                    ? classWriterStrategy.resolve(flags, typePool)
                    : classWriterStrategy.resolve(flags, typePool, unwrapped));
        }
    }

    /**
     * A class writer that piggy-backs on Byte Buddy's {@link TypePool} to avoid class loading or look-up errors when redefining a class.
     * This is not available when creating a new class where automatic frame computation is however not normally a requirement.
     *
     * @deprecated Use {@link AsmClassWriter.FrameComputingClassWriter}.
     */
    @Deprecated
    class FrameComputingClassWriter extends AsmClassWriter.FrameComputingClassWriter {

        /**
         * Creates a new frame computing class writer.
         *
         * @param flags    The flags to be handed to the writer.
         * @param typePool The type pool to use for computing stack map frames, if required.
         */
        public FrameComputingClassWriter(int flags, TypePool typePool) {
            super(flags, typePool);
        }

        /**
         * Creates a new frame computing class writer.
         *
         * @param classReader The class reader from which the original class is read.
         * @param flags       The flags to be handed to the writer.
         * @param typePool    The type pool to use for computing stack map frames, if required.
         */
        public FrameComputingClassWriter(ClassReader classReader, int flags, TypePool typePool) {
            super(classReader, flags, typePool);
        }
    }
}
