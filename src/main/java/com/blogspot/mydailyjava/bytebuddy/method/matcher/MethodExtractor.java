package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.*;

public class MethodExtractor {

    private static class Extraction {

        private final Set<String> extractedMethodSignatures;
        private final List<Method> extractedMethods;
        private final MethodMatcher methodMatcher;

        private Extraction(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
            extractedMethodSignatures = new HashSet<String>();
            extractedMethods = new LinkedList<Method>();
        }

        public void consider(Method method) {
            String methodSignature = method.getName() + Type.getMethodDescriptor(method);
            if (extractedMethodSignatures.add(methodSignature) && methodMatcher.matches(method)) {
                extractedMethods.add(method);
            }
        }

        public List<Method> asList() {
            return new ArrayList<Method>(extractedMethods);
        }
    }

    public static MethodExtractor matching(MethodMatcher methodMatcher) {
        return new MethodExtractor(methodMatcher);
    }

    private final MethodMatcher methodMatcher;

    protected MethodExtractor(MethodMatcher methodMatcher) {
        this.methodMatcher = methodMatcher;
    }

    public List<Method> extractAll(Collection<Class<?>> types) {
        Extraction extraction = new Extraction(methodMatcher);
        for (Class<?> type : types) {
            extractClass(type, extraction);
        }
        return extraction.asList();
    }

    private static void extractClass(Class<?> type, Extraction extraction) {
        List<Class<?>> interfaces = new LinkedList<Class<?>>();
        do {
            extractDeclaredMethods(type, extraction);
            interfaces.addAll(Arrays.asList(type.getInterfaces()));
        } while ((type = type.getSuperclass()) != null);
        extractInterfaces(interfaces, extraction);
    }

    private static void extractInterfaces(Collection<Class<?>> interfaces, Extraction extraction) {
        if (interfaces.size() == 0) {
            return;
        }
        List<Class<?>> nestedInterfaces = new LinkedList<Class<?>>();
        for (Class<?> anInterface : interfaces) {
            extractDeclaredMethods(anInterface, extraction);
            nestedInterfaces.addAll(Arrays.asList(anInterface.getInterfaces()));
        }
        extractInterfaces(nestedInterfaces, extraction);
    }

    private static void extractDeclaredMethods(Class<?> type, Extraction extraction) {
        for (Method method : type.getDeclaredMethods()) {
            extraction.consider(method);
        }
    }
}
