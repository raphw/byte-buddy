package net.bytebuddy.description.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationDescriptionLatentTest extends AbstractAnnotationDescriptionTest {

    @SuppressWarnings("unchecked")
    private static AnnotationDescription build(Annotation annotation) throws Exception {
        AnnotationDescription.Builder builder = AnnotationDescription.Builder.ofType(annotation.annotationType());
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            Object value = method.invoke(annotation);
            if (value instanceof Annotation) {
                builder = builder.define(method.getName(), (Annotation) value);
            } else if (value instanceof Annotation[]) {
                builder = builder.defineAnnotationArray(method.getName(), (Class) method.getReturnType().getComponentType(), (Annotation[]) value);
            } else if (value instanceof Enum<?>) {
                builder = builder.define(method.getName(), (Enum<?>) value);
            } else if (value instanceof Enum<?>[]) {
                builder = builder.defineEnumerationArray(method.getName(), (Class) method.getReturnType().getComponentType(), (Enum[]) value);
            } else if (value instanceof Class<?>) {
                builder = builder.define(method.getName(), (Class<?>) value);
            } else if (value instanceof Class<?>[]) {
                builder = builder.defineTypeArray(method.getName(), (Class<?>[]) value);
            } else if (value instanceof String) {
                builder = builder.define(method.getName(), (String) value);
            } else if (value instanceof String[]) {
                builder = builder.defineArray(method.getName(), (String[]) value);
            } else if (value instanceof Boolean) {
                builder = builder.define(method.getName(), (Boolean) value);
            } else if (value instanceof Byte) {
                builder = builder.define(method.getName(), (Byte) value);
            } else if (value instanceof Character) {
                builder = builder.define(method.getName(), (Character) value);
            } else if (value instanceof Short) {
                builder = builder.define(method.getName(), (Short) value);
            } else if (value instanceof Integer) {
                builder = builder.define(method.getName(), (Integer) value);
            } else if (value instanceof Long) {
                builder = builder.define(method.getName(), (Long) value);
            } else if (value instanceof Float) {
                builder = builder.define(method.getName(), (Float) value);
            } else if (value instanceof Double) {
                builder = builder.define(method.getName(), (Double) value);
            } else if (value instanceof boolean[]) {
                builder = builder.defineArray(method.getName(), (boolean[]) value);
            } else if (value instanceof byte[]) {
                builder = builder.defineArray(method.getName(), (byte[]) value);
            } else if (value instanceof char[]) {
                builder = builder.defineArray(method.getName(), (char[]) value);
            } else if (value instanceof short[]) {
                builder = builder.defineArray(method.getName(), (short[]) value);
            } else if (value instanceof int[]) {
                builder = builder.defineArray(method.getName(), (int[]) value);
            } else if (value instanceof long[]) {
                builder = builder.defineArray(method.getName(), (long[]) value);
            } else if (value instanceof float[]) {
                builder = builder.defineArray(method.getName(), (float[]) value);
            } else if (value instanceof double[]) {
                builder = builder.defineArray(method.getName(), (double[]) value);
            } else {
                throw new IllegalArgumentException("Cannot handle: " + method);
            }
        }
        return builder.build();
    }

    @Override
    protected AnnotationDescription describe(Annotation annotation, Class<?> declaringType) {
        try {
            return build(annotation);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}