package net.bytebuddy.utility;

import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Represents a collection of common helper functions.
 */
public final class ByteBuddyCommons {

    /**
     * A mask for modifiers that represent a type's, method's or field's visibility.
     */
    public static final int VISIBILITY_MODIFIER_MASK = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    /**
     * A mask for modifiers that are represented by types and members.
     */
    public static final int GENERAL_MODIFIER_MASK = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_DEPRECATED;

    /**
     * A mask for modifiers that represents types.
     */
    public static final int TYPE_MODIFIER_MASK = VISIBILITY_MODIFIER_MASK | GENERAL_MODIFIER_MASK
            | Modifier.ABSTRACT | Modifier.FINAL | Modifier.INTERFACE | Modifier.STRICT | Opcodes.ACC_ANNOTATION
            | Opcodes.ACC_ENUM | Opcodes.ACC_STRICT | Opcodes.ACC_SUPER;

    /**
     * A mask for modifiers that represents type members.
     */
    public static final int MEMBER_MODIFIER_MASK = VISIBILITY_MODIFIER_MASK | TYPE_MODIFIER_MASK
            | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.STATIC;

    /**
     * A mask for modifiers that represents fields.
     */
    public static final int FIELD_MODIFIER_MASK = MEMBER_MODIFIER_MASK | Modifier.TRANSIENT | Modifier.VOLATILE;

    /**
     * A mask for modifiers that represents methods and constructors.
     */
    public static final int METHOD_MODIFIER_MASK = MEMBER_MODIFIER_MASK | Modifier.ABSTRACT | Modifier.SYNCHRONIZED
            | Modifier.NATIVE | Modifier.STRICT | Opcodes.ACC_BRIDGE | Opcodes.ACC_VARARGS;

    /**
     * A mask for modifiers that represents method parameters.
     */
    public static final int PARAMETER_MODIFIER_MASK = Modifier.FINAL | Opcodes.ACC_MANDATED | Opcodes.ACC_SYNTHETIC;

    /**
     * A collection of all keywords of the Java programming language.
     */
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
                    "true", "try", "void", "volatile", "while"))
    );

    /**
     * A set of all generic type sorts that can possibly define an extendable type.
     */
    private static final Set<GenericTypeDescription.Sort> EXTENDABLE_TYPES = EnumSet.of(GenericTypeDescription.Sort.NON_GENERIC,
            GenericTypeDescription.Sort.PARAMETERIZED);

    /**
     * This utility class is not supposed to be instantiated.
     */
    private ByteBuddyCommons() {
        throw new UnsupportedOperationException("This type describes a utility and is not supposed to be instantiated");
    }

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
     * Validates that no value of an array is {@code null}.
     *
     * @param value The input value to be validated.
     * @param <T>   The component type of the input value.
     * @return The input value.
     */
    public static <T> T[] nonNull(T[] value) {
        for (T object : value) {
            nonNull(object);
        }
        return value;
    }

    /**
     * Validates that a type is an annotation type.
     *
     * @param typeDescription The type to validate.
     * @param <T>             The type of the input value.
     * @return The input value.
     */
    public static <T extends TypeDescription> T isAnnotation(T typeDescription) {
        if (!typeDescription.isAnnotation()) {
            throw new IllegalArgumentException(typeDescription + " is not an annotation type");
        }
        return typeDescription;
    }

    /**
     * Validates that a type is a throwable type.
     *
     * @param typeDescription The type to validate.
     * @param <T>             The type of the input value.
     * @return The input value.
     */
    public static <T extends GenericTypeDescription> T isThrowable(T typeDescription) {
        if (!isActualType(typeDescription).asErasure().isAssignableTo(Throwable.class)) {
            throw new IllegalArgumentException("Cannot throw instances of: " + typeDescription);
        }
        return typeDescription;
    }

    /**
     * Validates that a collection of types only contains throwable types.
     *
     * @param typeDescriptions The types to validate.
     * @param <T>              The type of the input value.
     * @return The input value.
     */
    public static <T extends Collection<? extends GenericTypeDescription>> T isThrowable(T typeDescriptions) {
        for (GenericTypeDescription typeDescription : typeDescriptions) {
            isThrowable(typeDescription);
        }
        return typeDescriptions;
    }

    /**
     * Validates that a type is defineable, i.e. is represented as byte code.
     *
     * @param typeDescription The type to validate.
     * @param <T>             The actual type of the validated instance.
     * @return The input value.
     */
    public static <T extends TypeDescription> T isDefineable(T typeDescription) {
        if (typeDescription.isArray()) {
            throw new IllegalArgumentException("Cannot explicitly define an array type: " + typeDescription);
        } else if (typeDescription.isPrimitive()) {
            throw new IllegalArgumentException("Cannot explicitly define a primitive type: " + typeDescription);
        }
        return typeDescription;
    }

    /**
     * Validates that a type is extendable, i.e. is not an array, a primitive type or a {@code final} type.
     *
     * @param typeDescription The type to validate.
     * @param <T>             The actual type of the validated instance.
     * @return The input value.
     */
    public static <T extends GenericTypeDescription> T isExtendable(T typeDescription) {
        if (!EXTENDABLE_TYPES.contains(typeDescription.getSort())) {
            throw new IllegalArgumentException("Cannot extend generic type: " + typeDescription);
        } else if (isDefineable(typeDescription.asErasure()).isFinal()) {
            throw new IllegalArgumentException("Cannot extend a final type: " + typeDescription);
        }
        return typeDescription;
    }

    /**
     * Verifies that a type can be implemented.
     *
     * @param typeDescription The type to verify.
     * @param <T>             The actual type of the input.
     * @return The input value.
     */
    public static <T extends GenericTypeDescription> T isImplementable(T typeDescription) {
        if (!isExtendable(typeDescription).asErasure().isInterface()) {
            throw new IllegalArgumentException("Not an interface: " + typeDescription);
        }
        return typeDescription;
    }

    /**
     * Verifies that a collection of types can be implemented.
     *
     * @param typeDescriptions The types to verify.
     * @param <T>              The actual type of the input.
     * @return The input value.
     */
    public static <T extends Collection<? extends GenericTypeDescription>> T isImplementable(T typeDescriptions) {
        for (GenericTypeDescription typeDescription : typeDescriptions) {
            isImplementable(typeDescription);
        }
        return typeDescriptions;
    }

    /**
     * Validates that a type represents an actual type, i.e. not a wildcard and not {@code void}.
     *
     * @param typeDescription The type to validate.
     * @param <T>             The actual type of the argument.
     * @return The input value.
     */
    public static <T extends GenericTypeDescription> T isActualType(T typeDescription) {
        if (isActualTypeOrVoid(typeDescription).represents(void.class)) {
            throw new IllegalArgumentException("The void non-type cannot be assigned a value");
        }
        return typeDescription;
    }

    /**
     * Validates that a collection of types represents an actual type, i.e. not a wildcard and not {@code void}.
     *
     * @param typeDescriptions The types to validate.
     * @param <T>              The actual type of the argument.
     * @return The input value.
     */
    public static <T extends Collection<? extends GenericTypeDescription>> T isActualType(T typeDescriptions) {
        for (GenericTypeDescription typeDescription : typeDescriptions) {
            isActualType(typeDescription);
        }
        return typeDescriptions;
    }

    /**
     * Validates that a type represents an actual type or {@code void}, i.e. not a wildcard.
     *
     * @param typeDescription The type to validate.
     * @param <T>             The actual type of the argument.
     * @return The input value.
     */
    public static <T extends GenericTypeDescription> T isActualTypeOrVoid(T typeDescription) {
        if (typeDescription.getSort().isWildcard()) {
            throw new IllegalArgumentException("Not a top-level type: " + typeDescription);
        }
        return typeDescription;
    }

    /**
     * Validates that a collection of types represents an actual type or {@code void}, i.e. not a wildcard.
     *
     * @param typeDescriptions The types to validate.
     * @param <T>              The actual type of the argument.
     * @return The input value.
     */
    public static <T extends Collection<? extends GenericTypeDescription>> T isActualTypeOrVoid(T typeDescriptions) {
        for (GenericTypeDescription typeDescription : typeDescriptions) {
            isActualTypeOrVoid(typeDescription);
        }
        return typeDescriptions;
    }

    /**
     * Validates that the given collection of elements is unique.
     *
     * @param elements The elements to validate for being unique.
     * @param <T>      The actual type.
     * @return The given value.
     */
    public static <T extends Collection<?>> T unique(T elements) {
        Set<Object> found = new HashSet<Object>(elements.size());
        for (Object element : elements) {
            if (!found.add(element)) {
                throw new IllegalArgumentException("Duplicate element: " + element);
            }
        }
        return elements;
    }

    /**
     * Creates a list that contains all elements of a given list with an additional appended element.
     *
     * @param list    The list of elements to be appended first.
     * @param element The additional element.
     * @param <T>     The list's generic type.
     * @return An {@link java.util.ArrayList} containing all elements.
     */
    public static <T> List<T> join(List<? extends T> list, T element) {
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
    public static <T> List<T> join(T element, List<? extends T> list) {
        List<T> result = new ArrayList<T>(list.size() + 1);
        result.add(element);
        result.addAll(list);
        return result;
    }

    /**
     * Joins two lists.
     *
     * @param leftList  The left list.
     * @param rightList The right list.
     * @param <T>       The most specific common type of both lists.
     * @return A combination of both lists.
     */
    public static <T> List<T> join(List<? extends T> leftList, List<? extends T> rightList) {
        List<T> result = new ArrayList<T>(leftList.size() + rightList.size());
        result.addAll(leftList);
        result.addAll(rightList);
        return result;
    }

    /**
     * Filters all values from the {@code right} list and only includes them in the result of the {@code left} list
     * if they are not already present.
     *
     * @param left  The left list which is not filtered.
     * @param right The right list of which only elements are added if they are not present in the left list or the right list.
     * @param <T>   The type of the list.
     * @return A list with the elements of the right lists filtered.
     */
    public static <T> List<T> filterUnique(List<? extends T> left, List<? extends T> right) {
        List<T> result = new ArrayList<T>(left.size() + right.size());
        result.addAll(left);
        for (T element : right) {
            if (!result.contains(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Appends the given element to the list only if the element is not yet contained in the given list.
     *
     * @param list    The list of elements.
     * @param element The element to append.
     * @param <T>     The type of the list.
     * @return A list with the appended element.
     */
    public static <T> List<T> joinUnique(List<? extends T> list, T element) {
        List<T> result = new ArrayList<T>(list.size() + 1);
        for (T listElement : list) {
            if (listElement.equals(element)) {
                throw new IllegalArgumentException("Conflicting elements: " + listElement + " and " + element);
            }
            result.add(listElement);
        }
        result.add(element);
        return result;
    }

    /**
     * Validates that a collection of generic type descriptions does not contain duplicate type erasure.
     *
     * @param typeDescriptions The type descriptions to validate for being unique.
     * @param <T>              The actual type of the argument.
     * @return The input value.
     */
    public static <T extends Collection<? extends GenericTypeDescription>> T uniqueRaw(T typeDescriptions) {
        Map<TypeDescription, GenericTypeDescription> types = new HashMap<TypeDescription, GenericTypeDescription>(typeDescriptions.size());
        for (GenericTypeDescription typeDescription : typeDescriptions) {
            GenericTypeDescription conflictingType = types.put(typeDescription.asErasure(), typeDescription);
            if (conflictingType != null) {
                throw new IllegalArgumentException("Duplicate types: " + typeDescription + " and " + conflictingType);
            }
        }
        return typeDescriptions;
    }

    /**
     * Joins two collections where the left collection must only contain unique elements. If two elements represent the same type
     * erasure but are not equal, an exception is thrown-
     *
     * @param left  The left collection.
     * @param right The right collection.
     * @param <T>   The element type.
     * @return A joined list of both given lists.
     */
    public static <T extends GenericTypeDescription> List<T> joinUniqueRaw(Collection<? extends T> left, Collection<? extends T> right) {
        List<T> result = new ArrayList<T>(left.size() + right.size());
        Map<TypeDescription, GenericTypeDescription> types = new HashMap<TypeDescription, GenericTypeDescription>();
        for (T typeDescription : left) {
            types.put(typeDescription.asErasure(), typeDescription);
            result.add(typeDescription);
        }
        for (T typeDescription : right) {
            GenericTypeDescription conflictingType = types.put(typeDescription.asErasure(), typeDescription);
            if (conflictingType != null && !conflictingType.equals(typeDescription)) {
                throw new IllegalArgumentException("Conflicting type erasures: " + conflictingType + " and " + typeDescription);
            } else if (conflictingType == null) {
                result.add(typeDescription);
            }
        }
        return result;
    }

    /**
     * Validates that a string represents a valid Java identifier, i.e. is not a Java keyword and is built up
     * by Java identifier compatible characters.
     *
     * @param identifier The identifier to validate.
     * @return The same identifier.
     */
    public static String isValidIdentifier(String identifier) {
        if (JAVA_KEYWORDS.contains(nonNull(identifier))) {
            throw new IllegalArgumentException("Keyword cannot be used as Java identifier: " + identifier);
        }
        if (identifier.isEmpty()) {
            throw new IllegalArgumentException("An empty string is not a valid Java identifier");
        }
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            throw new IllegalArgumentException("Not a valid Java identifier: " + identifier);
        }
        for (char character : identifier.toCharArray()) {
            if (!Character.isJavaIdentifierPart(character)) {
                throw new IllegalArgumentException("Not a valid Java identifier: " + identifier);
            }
        }
        return identifier;
    }

    /**
     * Validates a Java type name to be valid.
     *
     * @param typeName The suggested name.
     * @return The same name that was given as an argument.
     */
    public static String isValidTypeName(String typeName) {
        String[] segments = nonNull(typeName).split("\\.");
        for (String segment : segments) {
            isValidIdentifier(segment);
        }
        return typeName;
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

    /**
     * Validates that a collection is empty.
     *
     * @param collection       The collection to be validated.
     * @param exceptionMessage The message of the exception that is thrown if the collection does contain an element.
     * @param <T>              The type of the collection.
     * @return The same collection that was validated.
     */
    public static <T extends Collection<?>> T isEmpty(T collection, String exceptionMessage) {
        if (collection.size() > 0) {
            throw new IllegalArgumentException(exceptionMessage);
        }
        return collection;
    }

    /**
     * Validates a mask against a number of modifier contributors and merges their contributions to a modifier.
     *
     * @param mask                The mask to validate against.
     * @param modifierContributor The modifier contributors to merge
     * @return The modifier created by these modifiers.
     */
    public static int resolveModifierContributors(int mask, ModifierContributor... modifierContributor) {
        int modifiers = 0;
        Set<Class<?>> modifierContributorTypes = new HashSet<Class<?>>(modifierContributor.length);
        for (ModifierContributor contributor : modifierContributor) {
            if (!modifierContributorTypes.add(contributor.getClass())) {
                throw new IllegalArgumentException(contributor + " is already registered with a different value");
            }
            modifiers |= contributor.getMask();
        }
        if ((modifiers & ~(mask | Opcodes.ACC_SYNTHETIC)) != 0) {
            throw new IllegalArgumentException("Illegal modifiers: " + Arrays.asList(modifierContributor));
        }
        return modifiers;
    }

    /**
     * Converts a collection to a list, either by casting or by explicit conversion.
     *
     * @param collection The collection to convert to a list.
     * @param <T>        The element type of the collection.
     * @return The list representing the elements of the collection.
     */
    public static <T> List<T> toList(Collection<T> collection) {
        return collection instanceof List
                ? (List<T>) collection
                : new ArrayList<T>(collection);
    }

    /**
     * Converts an iterable to a list, either by casting or by explicit conversion.
     *
     * @param iterable The iterable to convert to a list.
     * @param <T>      The element type of the collection.
     * @return The list representing the elements of the iterable.
     */
    public static <T> List<T> toList(Iterable<T> iterable) {
        if (iterable instanceof Collection) {
            return toList((Collection<T>) iterable);
        } else {
            List<T> list = new LinkedList<T>();
            for (T element : iterable) {
                list.add(element);
            }
            return list;
        }
    }
}
