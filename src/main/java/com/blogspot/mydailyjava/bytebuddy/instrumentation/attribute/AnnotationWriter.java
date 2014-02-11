package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import org.objectweb.asm.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface AnnotationWriter {

    static interface Target {

        static class OnType implements Target {

            private final ClassVisitor classVisitor;

            public OnType(ClassVisitor classVisitor) {
                this.classVisitor = classVisitor;
            }

            @Override
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible) {
                return classVisitor.visitAnnotation(annotationTypeDescriptor, visible);
            }
        }

        static class OnMethod implements Target {

            private final MethodVisitor methodVisitor;

            public OnMethod(MethodVisitor methodVisitor) {
                this.methodVisitor = methodVisitor;
            }

            @Override
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible) {
                return methodVisitor.visitAnnotation(annotationTypeDescriptor, visible);
            }
        }

        static class OnMethodParameter implements Target {

            private final MethodVisitor methodVisitor;
            private final int parameterIndex;

            public OnMethodParameter(MethodVisitor methodVisitor, int parameterIndex) {
                this.methodVisitor = methodVisitor;
                this.parameterIndex = parameterIndex;
            }

            @Override
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible) {
                return methodVisitor.visitParameterAnnotation(parameterIndex, annotationTypeDescriptor, visible);
            }
        }

        static class OnField implements Target {

            private final FieldVisitor fieldVisitor;

            public OnField(FieldVisitor fieldVisitor) {
                this.fieldVisitor = fieldVisitor;
            }

            @Override
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible) {
                return fieldVisitor.visitAnnotation(annotationTypeDescriptor, visible);
            }
        }

        AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible);
    }

    static class Default implements AnnotationWriter {

        private static final String ASM_IGNORE_NAME = null;

        private final Target target;

        public Default(Target target) {
            this.target = target;
        }

        @Override
        public AnnotationWriter append(Annotation annotation) {
            try {
                tryAppend(annotation);
                return this;
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Could not invoke method of " + annotation + " reflectively", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not access method of " + annotation, e);
            }
        }

        private void tryAppend(Annotation annotation)
                throws InvocationTargetException, IllegalAccessException {
            handle(target.visit(Type.getDescriptor(annotation.annotationType()), true), annotation);
        }

        private void handle(AnnotationVisitor annotationVisitor, Annotation annotation)
                throws InvocationTargetException, IllegalAccessException {
            for (Method method : annotation.annotationType().getDeclaredMethods()) {
                apply(annotationVisitor, method.getReturnType(), method.getName(), method.invoke(annotation));
            }
            annotationVisitor.visitEnd();
        }

        private void apply(AnnotationVisitor annotationVisitor, Class<?> valueType, String name, Object value)
                throws InvocationTargetException, IllegalAccessException {
            if (valueType.isAnnotation()) {
                handle(annotationVisitor.visitAnnotation(name, Type.getDescriptor(valueType)), (Annotation) value);
            } else if (valueType.isEnum()) {
                annotationVisitor.visitEnum(name, Type.getDescriptor(valueType), ((Enum<?>) value).name());
            } else if (valueType.isArray()) {
                AnnotationVisitor arrayVisitor = annotationVisitor.visitArray(name);
                int length = Array.getLength(value);
                Class<?> componentType = valueType.getComponentType();
                for (int index = 0; index < length; index++) {
                    apply(arrayVisitor, componentType, ASM_IGNORE_NAME, Array.get(value, index));
                }
                arrayVisitor.visitEnd();
            } else {
                annotationVisitor.visit(name, value);
            }
        }
    }

    AnnotationWriter append(Annotation annotation);
}
