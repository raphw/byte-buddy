package net.bytebuddy.instrumentation.type;

import net.bytebuddy.instrumentation.NamedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;

/**
 * A package description represents a Java package.
 */
public interface PackageDescription extends NamedElement, AnnotatedElement {

    /**
     * Checks if this package description represents a sealed package.
     *
     * @return {@code true} if this package is sealed.
     */
    boolean isSealed();

    /**
     * An abstract base implementation of a package description.
     */
    public abstract static class AbstractPackageDescription implements PackageDescription {

        @Override
        public String getInternalName() {
            return getName().replace('.', '/');
        }

        @Override
        public String getSourceCodeName() {
            return getName();
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof PackageDescription
                    && getName().equals(((PackageDescription) other).getName());
        }

        @Override
        public String toString() {
            return "package " + getName();
        }
    }

    /**
     * A simple implementation of a package without annotations.
     */
    public static class Simple extends AbstractPackageDescription {

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

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isSealed() {
            return false;
        }
    }

    /**
     * Represents a loaded {@link java.lang.Package} wrapped as a
     * {@link net.bytebuddy.instrumentation.type.PackageDescription}.
     */
    public static class ForLoadedPackage extends AbstractPackageDescription {

        /**
         * The represented package.
         */
        private final Package aPackage;

        /**
         * Creates a new loaded package representation.
         *
         * @param aPackage The represented package.
         */
        protected ForLoadedPackage(Package aPackage) {
            this.aPackage = aPackage;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotation(aPackage.getDeclaredAnnotations());
        }

        @Override
        public String getName() {
            return aPackage.getName();
        }

        @Override
        public boolean isSealed() {
            return aPackage.isSealed();
        }
    }
}
