package net.bytebuddy.description.field;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Implementations of this interface describe a Java field. Implementations of this interface must provide meaningful
 * {@code equal(Object)} and {@code hashCode()} implementations.
 */
public interface FieldDescription extends ByteCodeElement {

    /**
     * Returns a description of the type of this field.
     *
     * @return A type description of this field.
     */
    TypeDescription getFieldType();

    GenericType getFieldTypeGen();

    /**
     * An abstract base implementation of a field description.
     */
    abstract class AbstractFieldDescription extends AbstractModifierReviewable implements FieldDescription {

        @Override
        public String getInternalName() {
            return getName();
        }

        @Override
        public String getSourceCodeName() {
            return getName();
        }

        @Override
        public String getDescriptor() {
            return getFieldType().getDescriptor();
        }

        @Override
        public String getGenericSignature() {
            return null; // Currently, generics signatures supported poorly.
        }

        @Override
        public boolean isVisibleTo(TypeDescription typeDescription) {
            return getDeclaringType().isVisibleTo(typeDescription)
                    && (isPublic()
                    || typeDescription.equals(getDeclaringType())
                    || (isProtected() && getDeclaringType().isAssignableFrom(typeDescription))
                    || (!isPrivate() && typeDescription.isSamePackage(getDeclaringType())));
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof FieldDescription
                    && getName().equals(((FieldDescription) other).getName())
                    && getDeclaringType().equals(((FieldDescription) other).getDeclaringType());
        }

        @Override
        public int hashCode() {
            return getDeclaringType().hashCode() + 31 * getName().hashCode();
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            if (getModifiers() != 0) {
                stringBuilder.append(Modifier.toString(getModifiers())).append(" ");
            }
            stringBuilder.append(getFieldType().getSourceCodeName()).append(" ");
            stringBuilder.append(getDeclaringType().getSourceCodeName()).append(".");
            return stringBuilder.append(getName()).toString();
        }
    }

    /**
     * An implementation of a field description for a loaded field.
     */
    class ForLoadedField extends AbstractFieldDescription {

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
        public GenericType getFieldTypeGen() {
            return new GenericType.LazyProjection.OfFieldType(field);
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotation(field.getDeclaredAnnotations());
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
     * {@link TypeDescription}.
     */
    class Latent extends AbstractFieldDescription {

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
        public GenericType getFieldTypeGen() {
            return fieldType;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
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
    }
}
