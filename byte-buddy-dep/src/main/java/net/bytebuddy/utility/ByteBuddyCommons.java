package net.bytebuddy.utility;

import net.bytebuddy.instrumentation.ModifierContributor;
import net.bytebuddy.instrumentation.type.TypeDescription;
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
     * A mask for modifiers that represents modifiers and constructors.
     */
    public static final int METHOD_MODIFIER_MASK = MEMBER_MODIFIER_MASK | Modifier.ABSTRACT | Modifier.SYNCHRONIZED
            | Modifier.NATIVE | Modifier.STRICT | Opcodes.ACC_BRIDGE | Opcodes.ACC_VARARGS;

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
     * This utility class is not supposed to be instantiated.
     */
    private ByteBuddyCommons() {
        throw new UnsupportedOperationException();
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
     * Validates if a type is an interface.
     *
     * @param type The type to validate.
     * @return The input value.
     */
    public static TypeDescription isInterface(TypeDescription type) {
        if (!nonNull(type).isInterface()) {
            throw new IllegalArgumentException(type + " is not an interface");
        }
        return type;
    }

    /**
     * Validates if a list of type only contains interfaces.
     *
     * @param types The types to validate.
     * @param <T>   The list's type.
     * @return The input value.
     */
    public static <T extends TypeDescription> List<T> isInterface(List<T> types) {
        for (TypeDescription typeDescription : types) {
            isInterface(typeDescription);
        }
        return types;
    }

    /**
     * Validates that a type can be implemented, i.e. is not an array or a primitive.
     *
     * @param type The type to be validated.
     * @return The input value.
     */
    public static TypeDescription isExtendable(TypeDescription type) {
        if (nonNull(type).isArray() || type.isPrimitive()) {
            throw new IllegalArgumentException(type + " is not implementable");
        } else if (type.isFinal()) {
            throw new IllegalArgumentException("Cannot implement a final class such as " + type);
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
     * Validates a mask against a number of modifier contributors and merges their contributions to a modifier.
     *
     * @param mask                The mask to validate against.
     * @param modifierContributor The modifier contributors to merge
     * @return The modifier created by these modifiers.
     */
    public static int resolveModifierContributors(int mask, ModifierContributor... modifierContributor) {
        int modifier = 0;
        Set<Class<?>> modifierContributorTypes = new HashSet<Class<?>>(modifierContributor.length);
        for (ModifierContributor contributor : modifierContributor) {
            if (!modifierContributorTypes.add(contributor.getClass())) {
                throw new IllegalArgumentException(contributor + " is already registered with a different value");
            }
            modifier |= contributor.getMask();
        }
        if ((modifier & ~(mask | Opcodes.ACC_SYNTHETIC)) != 0) {
            throw new IllegalArgumentException("Illegal modifiers " + Arrays.asList(modifierContributor));
        }
        return modifier;
    }

    /**
     * Validates that there are no duplicates for a list of type descriptions.
     *
     * @param typeDescriptions The list to validate.
     * @param <T>              The exact type of the list to validate.
     * @return The same list that was given as an argument.
     */
    public static <T extends List<? extends TypeDescription>> T uniqueTypes(T typeDescriptions) {
        Set<String> types = new HashSet<String>(typeDescriptions.size());
        for (TypeDescription typeDescription : typeDescriptions) {
            if (!types.add(typeDescription.getInternalName())) {
                throw new IllegalArgumentException("Type was found more than once: " + typeDescription);
            }
        }
        return typeDescriptions;
    }

    /**
     * Checks if given types can all be thrown, i.e. extend the {@link java.lang.Throwable} base class.
     *
     * @param types The type to check.
     * @param <T>   the exact given type.
     * @return The given types.
     */
    public static <T extends Iterable<? extends TypeDescription>> T isThrowable(T types) {
        for (TypeDescription typeDescription : types) {
            if (!typeDescription.isAssignableTo(Throwable.class)) {
                throw new IllegalArgumentException("Not a isThrowable type: " + typeDescription);
            }
        }
        return types;
    }
}
