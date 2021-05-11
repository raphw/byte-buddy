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
package net.bytebuddy.dynamic.loading;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * A post processor for class files.
 */
public interface ClassFilePostProcessor {

    /**
     * Transforms a class file for a given class.
     *
     * @param classLoader          The class loader which is used to load a class or {@code null} if loaded by the bootstrap loader.
     * @param name                 The binary name of the transformed class.
     * @param protectionDomain     The protection domain of the transformed class or {@code null} if no protection domain is provided.
     * @param binaryRepresentation The binary representation of the class file.
     * @return The class file to use.
     */
    byte[] transform(ClassLoader classLoader, String name, ProtectionDomain protectionDomain, byte[] binaryRepresentation);

    /**
     * A non-operation class file post processor.
     */
    enum NoOp implements ClassFilePostProcessor {

        /**
         * The singelton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public byte[] transform(ClassLoader classLoader, String name, ProtectionDomain protectionDomain, byte[] binaryRepresentation) {
            return binaryRepresentation;
        }
    }

    /**
     * A class file post processor that delegates to an {@link ClassFileTransformer}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForClassFileTransformer implements ClassFilePostProcessor {

        /**
         * Indicates that a class is not currently loaded.
         */
        private static final Class<?> UNLOADED_TYPE = null;

        /**
         * The class file transformer to delegate to.
         */
        private final ClassFileTransformer classFileTransformer;

        /**
         * Creates a new class file post processor for a class file transformer.
         *
         * @param classFileTransformer The class file transformer to delegate to.
         */
        public ForClassFileTransformer(ClassFileTransformer classFileTransformer) {
            this.classFileTransformer = classFileTransformer;
        }

        /**
         * {@inheritDoc}
         */
        public byte[] transform(ClassLoader classLoader, String name, ProtectionDomain protectionDomain, byte[] binaryRepresentation) {
            try {
                byte[] transformed = classFileTransformer.transform(classLoader, name.replace('.', '/'), UNLOADED_TYPE, protectionDomain, binaryRepresentation);
                return transformed == null ? binaryRepresentation : transformed;
            } catch (IllegalClassFormatException exception) {
                throw new IllegalStateException("Failed to transform " + name, exception);
            }
        }
    }
}
