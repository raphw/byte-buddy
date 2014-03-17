package com.blogspot.mydailyjava.bytebuddy.utility;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.mockito.MockSettings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.*;

import static org.mockito.Mockito.*;

public final class MockedDescription {

    private static final String DEFAULT = "##None";

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface ForMethod {

        String name() default DEFAULT;

        String[] parameterTypes() default {DEFAULT};

        String returnType() default DEFAULT;

        String declaredBy() default DEFAULT;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface ForConstructor {

        String[] parameterTypes() default {DEFAULT};

        String declaredBy() default DEFAULT;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface ForField {

        String fieldType() default DEFAULT;

        String declaredBy() default DEFAULT;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface ForType {

        String name() default DEFAULT;

        String extending() default DEFAULT;
    }

    public static void inject(Object instance) throws Exception {
        Map<String, TypeDescription> fieldNameToTypeMock = new HashMap<String, TypeDescription>();
        Map<MethodDescription, String> methodDeclaredBy = new HashMap<MethodDescription, String>();
        Map<FieldDescription, String> fieldDeclaredBy = new HashMap<FieldDescription, String>();
        Map<MethodDescription, List<String>> parameterDeclaredBy = new HashMap<MethodDescription, List<String>>();
        Map<MethodDescription, String> returnTypeDeclaredBy = new HashMap<MethodDescription, String>();
        Map<TypeDescription, String> typeExtends = new HashMap<TypeDescription, String>();
        Map<FieldDescription, String> fieldType = new HashMap<FieldDescription, String>();
        Class<?> type = instance.getClass();
        do {
            doInject(instance,
                    type,
                    fieldNameToTypeMock,
                    methodDeclaredBy,
                    fieldDeclaredBy,
                    parameterDeclaredBy,
                    returnTypeDeclaredBy,
                    typeExtends,
                    fieldType);
        } while ((type = type.getSuperclass()) != null);
        for (Map.Entry<TypeDescription, String> entry : typeExtends.entrySet()) {
            TypeDescription target = fieldExists(fieldNameToTypeMock.get(entry.getValue()), entry.getValue());
            when(entry.getKey().getSupertype()).thenReturn(target);
        }
        for (Map.Entry<MethodDescription, List<String>> entry : parameterDeclaredBy.entrySet()) {
            List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(entry.getValue().size());
            for (String fieldName : entry.getValue()) {
                typeDescriptions.add(fieldExists(fieldNameToTypeMock.get(fieldName), fieldName));
            }
            when(entry.getKey().getParameterTypes()).thenReturn(new TypeList.Explicit(typeDescriptions));
        }
        for (Map.Entry<MethodDescription, String> entry : returnTypeDeclaredBy.entrySet()) {
            when(entry.getKey().getReturnType()).thenReturn(fieldExists(fieldNameToTypeMock.get(entry.getKey()), entry.getValue()));
        }
        Map<TypeDescription, List<MethodDescription>> declarations = new HashMap<TypeDescription, List<MethodDescription>>();
        for (Map.Entry<MethodDescription, String> entry : methodDeclaredBy.entrySet()) {
            TypeDescription declaringType = fieldExists(fieldNameToTypeMock.get(entry.getValue()), entry.getValue());
            List<MethodDescription> parameters = declarations.get(declaringType);
            if (parameters == null) {
                parameters = new ArrayList<MethodDescription>();
                declarations.put(declaringType, parameters);
            }
            parameters.add(entry.getKey());
        }
        for (Map.Entry<TypeDescription, List<MethodDescription>> entry : declarations.entrySet()) {
            when(entry.getKey().getDeclaredMethods()).thenReturn(new MethodList.Explicit(entry.getValue()));
            for (MethodDescription methodDescription : entry.getValue()) {
                when(methodDescription.getDeclaringType()).thenReturn(entry.getKey());
            }
        }
        Map<TypeDescription, List<FieldDescription>> fieldDescriptions = new HashMap<TypeDescription, List<FieldDescription>>();
        for (Map.Entry<FieldDescription, String> entry : fieldDeclaredBy.entrySet()) {
            TypeDescription declaringType = fieldExists(fieldNameToTypeMock.get(entry.getValue()), entry.getValue());
            List<FieldDescription> fields = fieldDescriptions.get(declaringType);
            if (fields == null) {
                fields = new ArrayList<FieldDescription>();
                fieldDescriptions.put(declaringType, fields);
            }
            fields.add(entry.getKey());
        }
        for (Map.Entry<TypeDescription, List<FieldDescription>> entry : fieldDescriptions.entrySet()) {
            when(entry.getKey().getDeclaredFields()).thenReturn(new FieldList.Explicit(entry.getValue()));
            for (FieldDescription methodDescription : entry.getValue()) {
                when(methodDescription.getDeclaringType()).thenReturn(entry.getKey());
            }
        }
        for (Map.Entry<FieldDescription, String> entry : fieldType.entrySet()) {
            when(entry.getKey().getFieldType()).thenReturn(fieldExists(fieldNameToTypeMock.get(entry.getValue()), entry.getValue()));
        }
    }

    private static void doInject(Object instance,
                                 Class<?> type,
                                 Map<String, TypeDescription> fieldNameToTypeMock,
                                 Map<MethodDescription, String> methodDeclaredBy,
                                 Map<FieldDescription, String> fieldDeclaredBy,
                                 Map<MethodDescription, List<String>> parameterDeclaredBy,
                                 Map<MethodDescription, String> returnTypeDeclaredBy,
                                 Map<TypeDescription, String> typeExtends,
                                 Map<FieldDescription, String> fieldType) throws Exception {
        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);
            MockSettings mockSettings = withSettings();
            mockSettings.name(field.getName());
            mockSettings.defaultAnswer(RETURNS_DEEP_STUBS);
            if (field.isAnnotationPresent(ForMethod.class) || field.isAnnotationPresent(ForConstructor.class)) {
                assertFieldType(field, MethodDescription.class);
                MethodDescription mockedDescription = mock(MethodDescription.class, mockSettings);
                field.set(instance, mockedDescription);
                String declaredBy;
                String[] parameterTypes;
                if (field.isAnnotationPresent(ForConstructor.class)) {
                    when(mockedDescription.getInternalName()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
                    declaredBy = field.getAnnotation(ForConstructor.class).declaredBy();
                    parameterTypes = field.getAnnotation(ForConstructor.class).parameterTypes();
                    when(mockedDescription.getReturnType()).thenReturn(new TypeDescription.ForLoadedType(void.class));
                } else {
                    String methodName = field.getAnnotation(ForMethod.class).name();
                    if (!DEFAULT.equals(methodName)) {
                        when(mockedDescription.getName()).thenReturn(methodName);
                        when(mockedDescription.getInternalName()).thenReturn(methodName);
                    }
                    String returnType = field.getAnnotation(ForMethod.class).returnType();
                    if (!DEFAULT.equals(returnType)) {
                        returnTypeDeclaredBy.put(mockedDescription, returnType);
                    }
                    declaredBy = field.getAnnotation(ForMethod.class).declaredBy();
                    parameterTypes = field.getAnnotation(ForMethod.class).parameterTypes();
                }
                if (!DEFAULT.equals(declaredBy)) {
                    methodDeclaredBy.put(mockedDescription, declaredBy);
                }
                if (!(parameterTypes.length == 1 && DEFAULT.equals(parameterTypes[0]))) {
                    parameterDeclaredBy.put(mockedDescription, Arrays.asList(parameterTypes));
                }
            } else if (field.isAnnotationPresent(ForType.class)) {
                assertFieldType(field, TypeDescription.class);
                TypeDescription mockedDescription = mock(TypeDescription.class, mockSettings);
                field.set(instance, mockedDescription);
                String name = field.getAnnotation(ForType.class).name();
                if (!DEFAULT.equals(name)) {
                    when(mockedDescription.getName()).thenReturn(name);
                    when(mockedDescription.getInternalName()).thenReturn(name.replace('.', '/'));
                }
                String extending = field.getAnnotation(ForType.class).extending();
                if (!DEFAULT.equals(extending)) {
                    typeExtends.put(mockedDescription, extending);
                }
                if (!(fieldNameToTypeMock.put(field.getName(), mockedDescription) == null)) {
                    throw new IllegalStateException("Name for type description field is used twice: " + field);
                }
            } else if (field.isAnnotationPresent(ForField.class)) {
                assertFieldType(field, FieldDescription.class);
                FieldDescription mockedDescription = mock(FieldDescription.class, mockSettings);
                field.set(instance, mockedDescription);
                String declaredBy = field.getAnnotation(ForField.class).declaredBy();
                if (!DEFAULT.equals(declaredBy)) {
                    fieldDeclaredBy.put(mockedDescription, declaredBy);
                }
                String typeReference = field.getAnnotation(ForField.class).fieldType();
                if (!DEFAULT.equals(typeReference)) {
                    fieldType.put(mockedDescription, typeReference);
                }
            }
        }
    }

    private static TypeDescription fieldExists(TypeDescription typeDescription, String fieldName) {
        if (typeDescription == null) {
            throw new IllegalStateException("There is no mocked type defined for a field named " + fieldName);
        }
        return typeDescription;
    }

    private static void assertFieldType(Field field, Class<?> annotationType) {
        if (!annotationType.isAssignableFrom(field.getType())) {
            throw new IllegalStateException(field + " cannot be assigned a value for " + annotationType);
        }
    }

    private MockedDescription() {
        throw new AssertionError();
    }
}
