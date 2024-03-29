package net.bytebuddy.description.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AnnotationDescriptionLatentTest extends AbstractAnnotationDescriptionTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static AnnotationDescription build(Annotation annotation) throws Exception {
        AnnotationDescription.Builder builder = AnnotationDescription.Builder.ofType(annotation.annotationType());
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            try {
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
                    throw new IllegalArgumentException("Unexpected annotation property: " + method);
                }
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getTargetException();
                if (cause instanceof TypeNotPresentException) {
                    builder = builder.define(method.getName(), new AnnotationValue.ForMissingType<Void, Void>(((TypeNotPresentException) cause).typeName()));
                } else if (cause instanceof EnumConstantNotPresentException) {
                    builder = builder.define(method.getName(), new AnnotationValue.ForEnumerationDescription.WithUnknownConstant(
                            new TypeDescription.ForLoadedType(((EnumConstantNotPresentException) cause).enumType()),
                            ((EnumConstantNotPresentException) cause).constantName()));
                } else if (cause instanceof AnnotationTypeMismatchException) {
                    builder = builder.define(method.getName(), new AnnotationValue.ForMismatchedType<Void, Void>(
                            new MethodDescription.ForLoadedMethod(((AnnotationTypeMismatchException) cause).element()),
                            ((AnnotationTypeMismatchException) cause).foundType()));
                } else if (!(cause instanceof IncompleteAnnotationException)) {
                    throw exception;
                }
            }
        }
        return builder.build(false);
    }

    protected AnnotationDescription describe(Annotation annotation, Class<?> declaringType) {
        try {
            return build(annotation);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
