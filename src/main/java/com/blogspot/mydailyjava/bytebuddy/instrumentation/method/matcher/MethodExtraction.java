package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class MethodExtraction {

    public static MethodExtraction matching(MethodMatcher methodMatcher) {
        return new MethodExtraction(Collections.<MethodDescription>emptyList(), Collections.<String>emptySet(), methodMatcher);
    }

    private final List<MethodDescription> extractedMethods;
    private final Set<String> extractedMethodSignatures;
    private final MethodMatcher methodMatcher;

    private MethodExtraction(List<MethodDescription> extractedMethods,
                             Set<String> extractedMethodSignatures,
                             MethodMatcher methodMatcher) {
        this.extractedMethods = Collections.unmodifiableList(extractedMethods);
        this.extractedMethodSignatures = Collections.unmodifiableSet(extractedMethodSignatures);
        this.methodMatcher = methodMatcher;
    }

    public List<MethodDescription> asList() {
        return new ArrayList<MethodDescription>(extractedMethods);
    }

    public MethodExtraction filter(MethodMatcher methodMatcher) {
        List<MethodDescription> extractedMethods = new ArrayList<MethodDescription>(this.extractedMethods.size());
        Set<String> extractedMethodSignatures = new HashSet<String>(this.extractedMethodSignatures);
        for (MethodDescription methodDescription : this.extractedMethods) {
            if (methodMatcher.matches(methodDescription)) {
                extractedMethods.add(methodDescription);
            }
        }
        return new MethodExtraction(extractedMethods,
                extractedMethodSignatures,
                new JunctionMethodMatcher.Conjunction(methodMatcher, this.methodMatcher));
    }

    private class ExtractionHelper {

        private final List<MethodDescription> extractedMethods;
        private final Set<String> extractedMethodSignatures;

        private ExtractionHelper() {
            this.extractedMethods = new LinkedList<MethodDescription>(MethodExtraction.this.extractedMethods);
            this.extractedMethodSignatures = new HashSet<String>(MethodExtraction.this.extractedMethodSignatures);
        }

        public ExtractionHelper extractType(Class<?> type) {
            extractAllMethods(type);
            extractDeclaredConstructors(type);
            return this;
        }

        private void extractAllMethods(Class<?> type) {
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
                consider(new MethodDescription.ForMethod(method));
            }
        }

        private void extractDeclaredConstructors(Class<?> type) {
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                consider(new MethodDescription.ForConstructor(constructor));
            }
        }

        private void consider(MethodDescription methodDescription) {
            if (extractedMethodSignatures.add(methodDescription.getUniqueSignature()) && methodMatcher.matches(methodDescription)) {
                extractedMethods.add(methodDescription);
            }
        }

        public MethodExtraction toMethodExtraction() {
            return new MethodExtraction(extractedMethods, extractedMethodSignatures, methodMatcher);
        }
    }

    public MethodExtraction extractUniqueDescriptorsFrom(Iterable<? extends Class<?>> types) {
        ExtractionHelper extractionHelper = new ExtractionHelper();
        for (Class<?> type : types) {
            extractionHelper.extractType(type);
        }
        return extractionHelper.toMethodExtraction();
    }

    public MethodExtraction appendUniqueDescriptorsFrom(Class<?> type) {
        return new ExtractionHelper().extractType(type).toMethodExtraction();
    }
}
