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
package net.bytebuddy.description.type;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import org.objectweb.asm.Opcodes;

/**
 * A package description represents a Java package.
 */
public interface PackageDescription extends NamedElement.WithRuntimeName, AnnotationSource {

    /**
     * The name of a Java class representing a package description.
     */
    String PACKAGE_CLASS_NAME = "package-info";

    /**
     * The modifiers of a Java class representing a package description.
     */
    int PACKAGE_MODIFIERS = Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC;

    /**
     * A named constant for an undefined package what applies for primitive and array types.
     */
    PackageDescription UNDEFINED = null;

    /**
     * Checks if this package contains the provided type.
     *
     * @param typeDescription The type to examine.
     * @return {@code true} if the given type contains the provided type.
     */
    boolean contains(TypeDescription typeDescription);

    /**
     * An abstract base implementation of a package description.
     */
    abstract class AbstractBase implements PackageDescription {

        /**
         * {@inheritDoc}
         */
        public String getInternalName() {
            return getName().replace('.', '/');
        }

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            return getName();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(TypeDescription typeDescription) {
            return this.equals(typeDescription.getPackage());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof PackageDescription && getName().equals(((PackageDescription) other).getName());
        }

        @Override
        public String toString() {
            return "package " + getName();
        }
    }

    /**
     * A simple implementation of a package without annotations.
     */
    class Simple extends AbstractBase {

        /**
         * The name of the package.
         */
        private final String name;

        /**
         * Creates a new simple package.
         *
         * @param name The name of the package.
         */
        public Simple(String name) {
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return name;
        }
    }

    /**
     * Represents a loaded {@link java.lang.Package} wrapped as a
     * {@link PackageDescription}.
     */
    class ForLoadedPackage extends AbstractBase {

        /**
         * The represented package.
         */
        private final Package aPackage;

        /**
         * Creates a new loaded package representation.
         *
         * @param aPackage The represented package.
         */
        public ForLoadedPackage(Package aPackage) {
            this.aPackage = aPackage;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotations(aPackage.getDeclaredAnnotations());
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return aPackage.getName();
        }
    }
}
