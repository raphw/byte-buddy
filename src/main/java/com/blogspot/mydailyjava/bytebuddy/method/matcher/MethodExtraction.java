package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.method.JavaMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class MethodExtraction {

    public static MethodExtraction matching(MethodMatcher methodMatcher) {
        return new MethodExtraction(methodMatcher);
    }

    private final MethodMatcher methodMatcher;
    private final List<JavaMethod> extractedMethods;
    private final Set<String> extractedMethodSignatures;

    protected MethodExtraction(MethodMatcher methodMatcher) {
        this.methodMatcher = methodMatcher;
        extractedMethodSignatures = new HashSet<String>();
        extractedMethods = new LinkedList<JavaMethod>();
    }

    public MethodExtraction extract(Class<?> clazz) {
        extractDeclaredConstructors(clazz);
        extractClass(clazz);
        return this;
    }

    public MethodExtraction appendInterfaces(Collection<Class<?>> interfaces) {
        extractInterfaces(interfaces);
        return this;
    }

    public MethodExtraction appendInterface(Class<?> anInterface) {
        extractInterfaces(Arrays.<Class<?>>asList(anInterface));
        return this;
    }

    private void extractClass(Class<?> type) {
        List<Class<?>> interfaces = new LinkedList<Class<?>>();
        do {
            extractDeclaredMethods(type);
            interfaces.addAll(Arrays.asList(type.getInterfaces()));
        } while ((type = type.getSuperclass()) != null);
        extractInterfaces(interfaces);
    }

    private void extractInterfaces(Collection<Class<?>> interfaces) {
        if (interfaces.size() == 0) {
            return;
        }
        List<Class<?>> nestedInterfaces = new LinkedList<Class<?>>();
        for (Class<?> anInterface : interfaces) {
            extractDeclaredMethods(anInterface);
            nestedInterfaces.addAll(Arrays.asList(anInterface.getInterfaces()));
        }
        extractInterfaces(nestedInterfaces);
    }

    private void extractDeclaredMethods(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            consider(new JavaMethod.ForMethod(method));
        }
    }

    private void extractDeclaredConstructors(Class<?> type) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            consider(new JavaMethod.ForConstructor(constructor));
        }
    }

    private void consider(JavaMethod javaMethod) {
        if (extractedMethodSignatures.add(javaMethod.getUniqueSignature()) && methodMatcher.matches(javaMethod)) {
            extractedMethods.add(javaMethod);
        }
    }

    public List<JavaMethod> asList() {
        return new ArrayList<JavaMethod>(extractedMethods);
    }
}
