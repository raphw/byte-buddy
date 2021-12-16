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
import net.bytebuddy.utility.nullability.AlwaysNull;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;

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
    byte[] transform(@MaybeNull ClassLoader classLoader, String name, @MaybeNull ProtectionDomain protectionDomain, byte[] binaryRepresentation);

    /**
     * A non-operation class file post processor.
     */
    enum NoOp implements ClassFilePostProcessor {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public byte[] transform(@MaybeNull ClassLoader classLoader, String name, @MaybeNull ProtectionDomain protectionDomain, byte[] binaryRepresentation) {
            return binaryRepresentation;
        }
    }

    /**
     * A class file post processor that delegates to an {@link ClassFileTransformer}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForClassFileTransformer implements ClassFilePostProcessor {

        /**
         * A protection domain that represents all permissions.
         */
        protected static final ProtectionDomain ALL_PRIVILEGES = new ProtectionDomain(null, new AllPermissionsCollection());

        /**
         * Indicates that a class is not currently loaded.
         */
        @AlwaysNull
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
        public byte[] transform(@MaybeNull ClassLoader classLoader, String name, @MaybeNull ProtectionDomain protectionDomain, byte[] binaryRepresentation) {
            try {
                byte[] transformed = classFileTransformer.transform(classLoader, name.replace('.', '/'),
                        UNLOADED_TYPE,
                        protectionDomain == null
                                ? ALL_PRIVILEGES
                                : protectionDomain,
                        binaryRepresentation);
                return transformed == null ? binaryRepresentation : transformed;
            } catch (IllegalClassFormatException exception) {
                throw new IllegalStateException("Failed to transform " + name, exception);
            }
        }

        /**
         * A permission collection that implies all permissions.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class AllPermissionsCollection extends PermissionCollection {

            /**
             * The serial version UID.
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void add(Permission permission) {
                throw new UnsupportedOperationException("add");
            }

            @Override
            public boolean implies(Permission permission) {
                return true;
            }

            @Override
            public Enumeration<Permission> elements() {
                return Collections.enumeration(Collections.<Permission>singleton(new AllPermission()));
            }
        }
    }
}
