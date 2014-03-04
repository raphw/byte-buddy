package com.blogspot.mydailyjava.bytebuddy.utility;

import java.util.*;

/**
 * Represents a collection of common helper functions.
 */
public final class ByteBuddyCommons {

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

    /**
     * Validates that a value is not {@code null}.
     *
     * @param value The input value to be validated.
     * @param <T>   The type of the input value.
     * @return The input value.
     */
    public static <T> T nonNull(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    /**
     * Validates if a type is an interface.
     *
     * @param type The type to validate.
     * @param <T>  The {@code Class}'s generic type.
     * @return The input value.
     */
    public static <T> Class<T> isInterface(Class<T> type) {
        if (!nonNull(type).isInterface()) {
            throw new IllegalArgumentException(type + " is not an interface");
        }
        return type;
    }

    /**
     * Validates that a type can be implemented, i.e. is not an array or a primitive.
     *
     * @param type The type to be validated.
     * @param <T>  The {@code Class}'s generic type.
     * @return The input value.
     */
    public static <T> Class<T> isImplementable(Class<T> type) {
        if (nonNull(type).isArray() || type.isPrimitive()) {
            throw new IllegalArgumentException(type + " cannot be implemented");
        }
        return type;
    }

    /**
     * Creates a list that contains all elements of a given list with an additional appended element.
     *
     * @param list    The list of elements to be appended first.
     * @param element The additional element.
     * @param <T>     The list's generic type.
     * @return An {@link java.util.ArrayList} containing all elements.
     */
    public static <T> List<T> join(List<T> list, T element) {
        List<T> result = new ArrayList<T>(list.size() + 1);
        result.addAll(list);
        result.add(element);
        return result;
    }

    /**
     * Creates a list that contains all elements of a given list with an additional prepended element.
     *
     * @param list    The list of elements to be appended last.
     * @param element The additional element.
     * @param <T>     The list's generic type.
     * @return An {@link java.util.ArrayList} containing all elements.
     */
    public static <T> List<T> join(T element, List<T> list) {
        List<T> result = new ArrayList<T>(list.size() + 1);
        result.add(element);
        result.addAll(list);
        return result;
    }

    /**
     * Validates that a string represents a valid Java identifier, i.e. is not a Java keyword and is built up
     * by Java identifier compatible characters.
     *
     * @param methodName The identifier to validate.
     * @return The same identifier.
     */
    public static String isValidIdentifier(String methodName) {
        if (JAVA_KEYWORDS.contains(nonNull(methodName))) {
            throw new IllegalArgumentException("Keyword cannot be used as identifier: " + methodName);
        }
        if (methodName.isEmpty()) {
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

    /**
     * Validates that a collection is not empty.
     *
     * @param collection       The collection to be validated.
     * @param exceptionMessage The message of the exception that is thrown if the collection does not contain an element.
     * @param <T>              The type of the collection.
     * @return The same collection that was validated.
     */
    public static <T extends Collection<?>> T isNotEmpty(T collection, String exceptionMessage) {
        if (collection.size() == 0) {
            throw new IllegalArgumentException(exceptionMessage);
        }
        return collection;
    }

    private ByteBuddyCommons() {
        throw new AssertionError();
    }
}
