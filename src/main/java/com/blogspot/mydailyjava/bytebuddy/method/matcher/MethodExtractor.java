package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.*;

public class MethodExtractor {

    public static MethodExtractor matching(MethodMatcher methodMatcher) {
        return new MethodExtractor(methodMatcher);
    }

    private final MethodMatcher methodMatcher;

    protected MethodExtractor(MethodMatcher methodMatcher) {
        this.methodMatcher = methodMatcher;
    }

    public List<Method> extractUniqueMethodsFrom(Class<?> type) {
        List<Method> extracted = new LinkedList<Method>();
        Set<String> usedDescriptors = new HashSet<String>();
        LinkedHashSet<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
        do {
            extractMethodsDeclaredIn(type, usedDescriptors, extracted);
            collectInterfacesFrom(type, interfaces);
            type = type.getSuperclass();
        } while (type != null);
        for (Class<?> anInterface : interfaces) {
            extractMethodsDeclaredIn(anInterface, usedDescriptors, extracted);
        }
        return extracted;
    }

    private static void collectInterfacesFrom(Class<?> type, LinkedHashSet<Class<?>> interfaces) {
        for (Class<?> anInterface : type.getInterfaces()) {
            interfaces.add(anInterface);
            collectInterfacesFrom(anInterface, interfaces);
        }
    }

    private void extractMethodsDeclaredIn(Class<?> type, Set<String> usedDescriptors, List<Method> extracted) {
        for (Method method : type.getDeclaredMethods()) {
            if (methodMatcher.matches(method) && !usedDescriptors.add(Type.getMethodDescriptor(method))) {
                extracted.add(method);
            }
        }
    }
}
