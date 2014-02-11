package com.blogspot.mydailyjava.bytebuddy.instrumentation.field;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ByteCodeElement;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierReviewable;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.DeclaredInType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;

public interface FieldDescription extends ModifierReviewable, ByteCodeElement, DeclaredInType, AnnotatedElement {

    static abstract class AbstractFieldDescription extends AbstractModifierReviewable implements FieldDescription {

        @Override
        public String getInternalName() {
            return getName();
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

    static class ForLoadedField extends AbstractFieldDescription {

        private final Field field;

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
        public String getDescriptor() {
            return Type.getDescriptor(field.getType());
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

    TypeDescription getFieldType();
}
