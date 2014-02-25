package com.blogspot.mydailyjava.bytebuddy.utility;

import java.util.*;

public final class UserInput {

    private static final Set<String> JAVA_KEYWORDS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "abstract", "assert", "boolean", "break", "byte", "case",
                    "catch", "char", "class", "const", "continue", "default",
                    "double", "do", "else", "enum", "extends", "false",
                    "final", "finally", "float", "for", "goto", "if",
                    "implements", "import", "instanceof", "int", "interface", "long",
                    "native", "new", "null", "package", "private", "protected",
                    "public", "return", "short", "static", "strictfp", "super",
                    "switch", "synchronized", "this", "throw", "throws", "transient",
                    "true", "try", "void", "volatile", "while")));

    public static <T> T nonNull(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    public static <T> Class<T> isInterface(Class<T> type) {
        if (nonNull(type).isInterface()) {
            throw new IllegalArgumentException(type + " is not an interface");
        }
        return type;
    }

    public static <T> Class<T> isImplementable(Class<T> type) {
        if (nonNull(type).isArray() || type.isPrimitive()) {
            throw new IllegalArgumentException(type + " cannot be implemented");
        }
        return type;
    }

    public static <T> List<T> join(List<T> list, T element) {
        List<T> result = new ArrayList<T>(list.size() + 1);
        result.addAll(list);
        result.add(element);
        return result;
    }

    public static String isValidIdentifier(String methodName) {
        if (JAVA_KEYWORDS.contains(nonNull(methodName))) {
            throw new IllegalArgumentException("Keyword cannot be used as identifier: " + methodName);
        }
        if(methodName.isEmpty()) {
            throw new IllegalArgumentException("An empty string is not a valid identifier");
        }
        if (!Character.isJavaIdentifierStart(methodName.charAt(0))) {
            throw new IllegalArgumentException("Not a valid identifier: " + methodName);
        }
        for (char character : methodName.toCharArray()) {
            if (!Character.isJavaIdentifierPart(character)) {
                throw new IllegalArgumentException("Not a valid identifier: " + methodName);
            }
        }
        return methodName;
    }

    public static <T extends Collection<?>> T containsElements(T collection, String exceptionMessage) {
        if(collection.size() == 0) {
            throw new IllegalArgumentException(exceptionMessage);
        }
        return collection;
    }

    private UserInput() {
        throw new AssertionError();
    }
}
