package net.bytebuddy.instrumentation.field;

import net.bytebuddy.instrumentation.ByteCodeElement;
import net.bytebuddy.instrumentation.ModifierReviewable;
import net.bytebuddy.instrumentation.type.DeclaredInType;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;

/**
 * Implementations of this interface describe a Java field. Implementations of this interface must provide meaningful
 * {@code equal(Object)} and {@code hashCode()} implementations.
 */
public interface FieldDescription extends ModifierReviewable, ByteCodeElement, DeclaredInType, AnnotatedElement {

    /**
     * Returns a description of the type of this field.
     *
     * @return A type description of this field.
     */
    TypeDescription getFieldType();

    /**
     * An abstract base implementation of a field description.
     */
    abstract static class AbstractFieldDescription extends AbstractModifierReviewable implements FieldDescription {

        @Override
        public String getInternalName() {
            return getName();
        }

        @Override
        public String getDescriptor() {
            return getFieldType().getDescriptor();
        }

        @Override
        public boolean isVisibleTo(TypeDescription typeDescription) {
            return isPublic()
                    || typeDescription.equals(getDeclaringType())
                    || (isProtected() && getDeclaringType().isAssignableFrom(typeDescription))
                    || (!isPrivate() && typeDescription.getPackageName().equals(getDeclaringType().getPackageName()));
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof FieldDescription
                    && getName().equals(((FieldDescription) other).getName())
                    && getDeclaringType().equals(((FieldDescription) other).getDeclaringType());
        }

        @Override
        public int hashCode() {
            return (getDeclaringType().getInternalName() + "." + getName()).hashCode();
        }
    }

    /**
     * An implementation of a field description for a loaded field.
     */
    static class ForLoadedField extends AbstractFieldDescription {

        /**
         * The represented loaded field.
         */
        private final Field field;

        /**
         * Creates an immutable field description for a loaded field.
         *
         * @param field The represented field.
         */
        public ForLoadedField(Field field) {
            this.field = field;
        }

        @Override
        public TypeDescription getFieldType() {
            return new TypeDescription.ForLoadedType(field.getType());
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return field.isAnnotationPresent(annotationClass);
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return field.getAnnotation(annotationClass);
        }

        @Override
        public Annotation[] getAnnotations() {
            return field.getAnnotations();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return field.getDeclaredAnnotations();
        }

        @Override
        public String getName() {
            return field.getName();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return new TypeDescription.ForLoadedType(field.getDeclaringClass());
        }

        @Override
        public int getModifiers() {
            return field.getModifiers();
        }

        @Override
        public boolean isSynthetic() {
            return field.isSynthetic();
        }
    }

    /**
     * A latent field description describes a field that is not attached to a declaring
     * {@link net.bytebuddy.instrumentation.type.TypeDescription}.
     */
    static class Latent extends AbstractFieldDescription {

        /**
         * The name of the field.
         */
        private final String fieldName;

        /**
         * The type for which this field is defined.
         */
        private final TypeDescription declaringType;

        /**
         * The type of the field.
         */
        private final TypeDescription fieldType;

        /**
         * The field's modifiers.
         */
        private final int modifiers;

        /**
         * Creates an immutable latent field description.
         *
         * @param fieldName     The name of the field.
         * @param declaringType The type for which this field is defined.
         * @param fieldType     The type of the field.
         * @param modifiers     The field's modifiers.
         */
        public Latent(String fieldName,
                      TypeDescription declaringType,
                      TypeDescription fieldType,
                      int modifiers) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.declaringType = declaringType;
            this.modifiers = modifiers;
        }

        @Override
        public TypeDescription getFieldType() {
            return fieldType;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public String getName() {
            return fieldName;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return declaringType;
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }

        @Override
        public String toString() {
            return "FieldDescription.Latent{" +
                    "fieldName='" + fieldName + '\'' +
                    ", declaringType=" + declaringType +
                    ", fieldType=" + fieldType +
                    ", modifiers=" + modifiers +
                    '}';
        }
    }
}
