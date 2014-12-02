package net.bytebuddy.instrumentation.type;

import net.bytebuddy.instrumentation.NamedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;

public interface PackageDescription extends NamedElement, AnnotatedElement {

    public abstract static class AbstractPackageDescription implements PackageDescription {

        protected String getParentName() {
            String name = getName();
            int packageIndex = name.lastIndexOf('.');
            return packageIndex == -1
                    ? null
                    : name.substring(0, packageIndex);
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

    public static class Simple extends AbstractPackageDescription {

        private final String name;

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
    }

    public static class ForLoadedPackage extends AbstractPackageDescription {

        private final Package aPackage;

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
    }
}
