package net.bytebuddy.description.annotation;

import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.utility.PropertyDispatcher;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * An annotation description describes {@link java.lang.annotation.Annotation} meta data of a class without this class
 * being required to be loaded. All values of an annotation are therefore represented in unloaded state:
 * <ul>
 * <li>{@link java.lang.Class} instances are represented as {@link TypeDescription}s.</li>
 * <li>{@link java.lang.Enum} instances are represented as
 * {@link net.bytebuddy.description.enumeration.EnumerationDescription}s.</li>
 * <li>{@link java.lang.annotation.Annotation}s are described as
 * {@link AnnotationDescription}s.</li>
 * <li>All primitive types are represented as their wrapper types.</li>
 * </ul>
 * An annotation can however be loaded in order to access unwrapped values. This will cause a loading of the classes
 * of these values.
 */
public interface AnnotationDescription {

    /**
     * Returns the value of the given method for this annotation value. Note that all return values are wrapped as
     * described by {@link AnnotationDescription}.
     *
     * @param methodDescription The method for the value to be requested.
     * @return The value for the given method.
     */
    Object getValue(MethodDescription.InDefinedShape methodDescription);

    /**
     * Returns the value of the given method for this annotation value and performs a casting to the given value. Note
     * that all return values are wrapped described by
     * {@link AnnotationDescription}.
     *
     * @param methodDescription The method for the value to be requested.
     * @param type              The type to which the returned value should be casted.
     * @param <T>               The given type of the return value.
     * @return The value for the given method casted to {@code type}.
     */
    <T> T getValue(MethodDescription.InDefinedShape methodDescription, Class<T> type);

    /**
     * Returns a description of the annotation type of this annotation.
     *
     * @return A description of the annotation type of this annotation.
     */
    TypeDescription getAnnotationType();

    /**
     * Links this annotation description to a given annotation type such that it can be loaded. This does not cause
     * the values of this annotation to be loaded.
     *
     * @param annotationType The loaded annotation type of this annotation description.
     * @param <T>            The type of the annotation.
     * @return A loadable version of this annotation description.
     */
    <T extends Annotation> Loadable<T> prepare(Class<T> annotationType);

    /**
     * Returns this annotation's retention policy.
     *
     * @return This annotation's retention policy.
     */
    RetentionPolicy getRetention();

    /**
     * Checks if this annotation is inherited.
     *
     * @return {@code true} if this annotation is inherited.
     * @see Inherited
     */
    boolean isInherited();

    /**
     * Checks if this annotation is documented.
     *
     * @return {@code true} if this annotation is documented.
     * @see Documented
     */
    boolean isDocumented();

    /**
     * A description of an annotation's value.
     *
     * @param <T> The type of the annotation's value when it is not loaded.
     * @param <S> The type of the annotation's value when it is loaded.
     */
    interface AnnotationValue<T, S> {

        /**
         * Resolves the unloaded value of this annotation.
         *
         * @return The unloaded value of this annotation.
         */
        T resolve();

        /**
         * Returns the loaded value of this annotation.
         *
         * @param classLoader The class loader for loading this value.
         * @return The loaded value of this annotation.
         * @throws ClassNotFoundException If a type that represents a loaded value cannot be found.
         */
        Loaded<S> load(ClassLoader classLoader) throws ClassNotFoundException;

        /**
         * A loaded variant of an {@link AnnotationValue}. While
         * implementations of this value are required to be processed successfully by a
         * {@link java.lang.ClassLoader} they might still be unresolved. Typical errors on loading an annotation
         * value are:
         * <ul>
         * <li>{@link java.lang.annotation.IncompleteAnnotationException}: An annotation does not define a value
         * even though no default value for a property is provided.</li>
         * <li>{@link java.lang.EnumConstantNotPresentException}: An annotation defines an unknown value for
         * a known enumeration.</li>
         * <li>{@link java.lang.annotation.AnnotationTypeMismatchException}: An annotation property is not
         * of the expected type.</li>
         * </ul>
         * Implementations of this interface must implement methods for {@link Object#hashCode()} and
         * {@link Object#toString()} that resemble those used for the annotation values of an actual
         * {@link java.lang.annotation.Annotation} implementation. Also, instances must implement
         * {@link java.lang.Object#equals(Object)} to return {@code true} for other instances of
         * this interface that represent the same annotation value.
         *
         * @param <U> The type of the loaded value of this annotation.
         */
        interface Loaded<U> {

            /**
             * Returns the state of the represented loaded annotation value.
             *
             * @return The state represented by this instance.
             */
            State getState();

            /**
             * Resolves the value to the actual value of an annotation. Calling this method might throw a runtime
             * exception if this value is either not defined or not resolved.
             *
             * @return The actual annotation value represented by this instance.
             */
            U resolve();

            /**
             * Represents the state of a {@link Loaded} annotation property.
             */
            enum State {

                /**
                 * A non-defined annotation value describes an annotation property which is missing such that
                 * an {@link java.lang.annotation.IncompleteAnnotationException} would be thrown.
                 */
                NON_DEFINED,

                /**
                 * A non-resolved annotation value describes an annotation property which does not represent a
                 * valid value but an exceptional state.
                 */
                NON_RESOLVED,

                /**
                 * A resolved annotation value describes an annotation property with an actual value.
                 */
                RESOLVED;

                /**
                 * Returns {@code true} if the related annotation value is defined, i.e. either represents
                 * an actual value or an exceptional state.
                 *
                 * @return {@code true} if the related annotation value is defined.
                 */
                public boolean isDefined() {
                    return this != NON_DEFINED;
                }

                /**
                 * Returns {@code true} if the related annotation value is resolved, i.e. represents an actual
                 * value.
                 *
                 * @return {@code true} if the related annotation value is resolved.
                 */
                public boolean isResolved() {
                    return this == RESOLVED;
                }

                @Override
                public String toString() {
                    return "AnnotationDescription.AnnotationValue.Loaded.State." + name();
                }
            }
        }

        /**
         * Represents a primitive value, a {@link java.lang.String} or an array of the latter types.
         *
         * @param <U> The type where primitive values are represented by their boxed type.
         */
        class Trivial<U> implements AnnotationValue<U, U> {

            /**
             * The represented value.
             */
            private final U value;

            /**
             * A property dispatcher for the given value.
             */
            private final PropertyDispatcher propertyDispatcher;

            /**
             * Creates a new representation of a trivial annotation value.
             *
             * @param value The value to represent.
             */
            public Trivial(U value) {
                this.value = value;
                propertyDispatcher = PropertyDispatcher.of(value.getClass());
            }

            @Override
            public U resolve() {
                return value;
            }

            @Override
            public AnnotationValue.Loaded<U> load(ClassLoader classLoader) {
                return new Loaded<U>(value, propertyDispatcher);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && propertyDispatcher.equals(value, ((Trivial) other).value);
            }

            @Override
            public int hashCode() {
                return propertyDispatcher.hashCode(value);
            }

            @Override
            public String toString() {
                return "AnnotationDescription.AnnotationValue.Trivial{" +
                        "value=" + value +
                        ", propertyDispatcher=" + propertyDispatcher +
                        '}';
            }

            /**
             * Represents a trivial loaded value.
             *
             * @param <V> The annotation properties type.
             */
            public static class Loaded<V> implements AnnotationValue.Loaded<V> {

                /**
                 * The represented value.
                 */
                private final V value;

                /**
                 * The property dispatcher for computing this value's hash code, string and equals implementation.
                 */
                private final PropertyDispatcher propertyDispatcher;

                /**
                 * Creates a new trivial loaded annotation value representation.
                 *
                 * @param value              The represented value.
                 * @param propertyDispatcher The property dispatcher for computing this value's hash
                 *                           code, string and equals implementation.
                 */
                public Loaded(V value, PropertyDispatcher propertyDispatcher) {
                    this.value = value;
                    this.propertyDispatcher = propertyDispatcher;
                }

                @Override
                public State getState() {
                    return State.RESOLVED;
                }

                @Override
                public V resolve() {
                    return propertyDispatcher.conditionalClone(value);
                }

                @Override
                public int hashCode() {
                    return propertyDispatcher.hashCode(value);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof AnnotationValue.Loaded<?>)) return false;
                    AnnotationValue.Loaded<?> loadedOther = (AnnotationValue.Loaded<?>) other;
                    return loadedOther.getState().isResolved() && propertyDispatcher.equals(value, loadedOther.resolve());
                }

                @Override
                public String toString() {
                    return propertyDispatcher.toString(value);
                }
            }
        }

        /**
         * A description of an {@link java.lang.annotation.Annotation} as a value of another annotation.
         *
         * @param <U> The type of the annotation.
         */
        class ForAnnotation<U extends Annotation> implements AnnotationValue<AnnotationDescription, U> {

            /**
             * The annotation description that this value represents.
             */
            private final AnnotationDescription annotationDescription;

            /**
             * Creates a new annotation value for a given annotation description.
             *
             * @param annotationDescription The annotation description that this value represents.
             */
            public ForAnnotation(AnnotationDescription annotationDescription) {
                this.annotationDescription = annotationDescription;
            }

            /**
             * Creates an annotation value instance for describing the given annotation type and values.
             *
             * @param annotationType   The annotation type.
             * @param annotationValues The values of the annotation.
             * @param <V>              The type of the annotation.
             * @return An annotation value representing the given annotation.
             */
            public static <V extends Annotation> AnnotationValue<AnnotationDescription, V> of(TypeDescription annotationType,
                                                                                              Map<String, AnnotationValue<?, ?>> annotationValues) {
                return new ForAnnotation<V>(new AnnotationDescription.Latent(annotationType, annotationValues));
            }

            @Override
            public AnnotationDescription resolve() {
                return annotationDescription;
            }

            @Override
            public AnnotationValue.Loaded<U> load(ClassLoader classLoader) throws ClassNotFoundException {
                @SuppressWarnings("unchecked")
                Class<U> annotationType = (Class<U>) classLoader.loadClass(annotationDescription.getAnnotationType().getName());
                return new Loaded<U>(annotationDescription.prepare(annotationType).load(classLoader));
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ForAnnotation that = (ForAnnotation) other;
                return annotationDescription.equals(that.annotationDescription);
            }

            @Override
            public int hashCode() {
                return annotationDescription.hashCode();
            }

            @Override
            public String toString() {
                return "AnnotationDescription.AnnotationValue.ForAnnotation{" +
                        "annotationDescription=" + annotationDescription +
                        '}';
            }

            /**
             * A loaded version of the described annotation.
             *
             * @param <V> The annotation type.
             */
            public static class Loaded<V extends Annotation> implements AnnotationValue.Loaded<V> {

                /**
                 * The loaded version of the represented annotation.
                 */
                private final V annotation;

                /**
                 * Creates a representation of a loaded annotation.
                 *
                 * @param annotation The represented annotation.
                 */
                public Loaded(V annotation) {
                    this.annotation = annotation;
                }

                @Override
                public State getState() {
                    return State.RESOLVED;
                }

                @Override
                public V resolve() {
                    return annotation;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof AnnotationValue.Loaded<?>)) return false;
                    AnnotationValue.Loaded<?> loadedOther = (AnnotationValue.Loaded<?>) other;
                    return loadedOther.getState().isResolved() && annotation.equals(loadedOther.resolve());
                }

                @Override
                public int hashCode() {
                    return annotation.hashCode();
                }

                @Override
                public String toString() {
                    return annotation.toString();
                }
            }

            /**
             * <p>
             * Represents an annotation value which was attempted to ba loaded by a type that does not represent
             * an annotation value.
             * </p>
             * <p>
             * <b>Note</b>: Neither of {@link Object#hashCode()}, {@link Object#toString()} and
             * {@link java.lang.Object#equals(Object)} are implemented specifically what resembles the way
             * such exceptional states are represented in the Open JDK's annotation implementations.
             * </p>
             */
            public static class IncompatibleRuntimeType implements AnnotationValue.Loaded<Annotation> {

                /**
                 * The incompatible runtime type which is not an annotation type.
                 */
                private final Class<?> incompatibleType;

                /**
                 * Creates a new representation for an annotation with an incompatible runtime type.
                 *
                 * @param incompatibleType The incompatible runtime type which is not an annotation type.
                 */
                public IncompatibleRuntimeType(Class<?> incompatibleType) {
                    this.incompatibleType = incompatibleType;
                }

                @Override
                public State getState() {
                    return State.NON_RESOLVED;
                }

                @Override
                public Annotation resolve() {
                    throw new IncompatibleClassChangeError("Not an annotation type: " + incompatibleType.toString());
                }

                /* does intentionally not implement hashCode, equals and toString */
            }
        }

        /**
         * A description of an {@link java.lang.Enum} as a value of an annotation.
         *
         * @param <U> The type of the enumeration.
         */
        class ForEnumeration<U extends Enum<U>> implements AnnotationValue<EnumerationDescription, U> {

            /**
             * The enumeration that is represented.
             */
            private final EnumerationDescription enumerationDescription;

            /**
             * Creates a new description of an annotation value for a given enumeration.
             *
             * @param enumerationDescription The enumeration that is to be represented.
             */
            public ForEnumeration(EnumerationDescription enumerationDescription) {
                this.enumerationDescription = enumerationDescription;
            }

            /**
             * Creates a new annotation value for the given enumeration description.
             *
             * @param value The value to represent.
             * @param <V>   The type of the represented enumeration.
             * @return An annotation value that describes the given enumeration.
             */
            public static <V extends Enum<V>> AnnotationValue<EnumerationDescription, V> of(EnumerationDescription value) {
                return new ForEnumeration<V>(value);
            }

            @Override
            public EnumerationDescription resolve() {
                return enumerationDescription;
            }

            @Override
            public AnnotationValue.Loaded<U> load(ClassLoader classLoader) throws ClassNotFoundException {
                @SuppressWarnings("unchecked")
                Class<U> enumerationType = (Class<U>) classLoader.loadClass(enumerationDescription.getEnumerationType().getName());
                return new Loaded<U>(enumerationDescription.load(enumerationType));
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ForEnumeration that = (ForEnumeration) other;
                return enumerationDescription.equals(that.enumerationDescription);
            }

            @Override
            public int hashCode() {
                return enumerationDescription.hashCode();
            }

            @Override
            public String toString() {
                return "AnnotationDescription.AnnotationValue.ForEnumeration{" +
                        "enumerationDescription=" + enumerationDescription +
                        '}';
            }

            /**
             * A loaded representation of an enumeration value.
             *
             * @param <V> The type of the represented enumeration.
             */
            public static class Loaded<V extends Enum<V>> implements AnnotationValue.Loaded<V> {

                /**
                 * The represented enumeration.
                 */
                private final V enumeration;

                /**
                 * Creates a loaded version of an enumeration description.
                 *
                 * @param enumeration The represented enumeration.
                 */
                public Loaded(V enumeration) {
                    this.enumeration = enumeration;
                }

                @Override
                public State getState() {
                    return State.RESOLVED;
                }

                @Override
                public V resolve() {
                    return enumeration;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof AnnotationValue.Loaded<?>)) return false;
                    AnnotationValue.Loaded<?> loadedOther = (AnnotationValue.Loaded<?>) other;
                    return loadedOther.getState().isResolved() && enumeration.equals(loadedOther.resolve());
                }

                @Override
                public int hashCode() {
                    return enumeration.hashCode();
                }

                @Override
                public String toString() {
                    return enumeration.toString();
                }
            }

            /**
             * <p>
             * Represents an annotation's enumeration value for a constant that does not exist for the runtime
             * enumeration type.
             * </p>
             * <p>
             * <b>Note</b>: Neither of {@link Object#hashCode()}, {@link Object#toString()} and
             * {@link java.lang.Object#equals(Object)} are implemented specifically what resembles the way
             * such exceptional states are represented in the Open JDK's annotation implementations.
             * </p>
             */
            public static class UnknownRuntimeEnumeration implements AnnotationValue.Loaded<Enum<?>> {

                /**
                 * The loaded enumeration type.
                 */
                private final Class<? extends Enum<?>> enumType;

                /**
                 * The value for which no enumeration constant exists at runtime.
                 */
                private final String value;

                /**
                 * Creates a new representation for an unknown enumeration constant of an annotation.
                 *
                 * @param enumType The loaded enumeration type.
                 * @param value    The value for which no enumeration constant exists at runtime.
                 */
                public UnknownRuntimeEnumeration(Class<? extends Enum<?>> enumType, String value) {
                    this.enumType = enumType;
                    this.value = value;
                }

                @Override
                public State getState() {
                    return State.NON_RESOLVED;
                }

                @Override
                public Enum<?> resolve() {
                    throw new EnumConstantNotPresentException(enumType, value);
                }

                /* hashCode, equals and toString are intentionally not implemented */
            }

            /**
             * <p>
             * Represents an annotation's enumeration value for a runtime type that is not an enumeration type.
             * </p>
             * <p>
             * <b>Note</b>: Neither of {@link Object#hashCode()}, {@link Object#toString()} and
             * {@link java.lang.Object#equals(Object)} are implemented specifically what resembles the way
             * such exceptional states are represented in the Open JDK's annotation implementations.
             * </p>
             */
            public static class IncompatibleRuntimeType implements AnnotationValue.Loaded<Enum<?>> {

                /**
                 * The runtime type which is not an enumeration type.
                 */
                private final Class<?> type;

                /**
                 * Creates a new representation for an incompatible runtime type.
                 *
                 * @param type The runtime type which is not an enumeration type.
                 */
                public IncompatibleRuntimeType(Class<?> type) {
                    this.type = type;
                }

                @Override
                public State getState() {
                    return State.NON_RESOLVED;
                }

                @Override
                public Enum<?> resolve() {
                    throw new IncompatibleClassChangeError("Not an enumeration type: " + type.toString());
                }

                /* hashCode, equals and toString are intentionally not implemented */
            }
        }

        /**
         * A description of a {@link java.lang.Class} as a value of an annotation.
         *
         * @param <U> The type of the {@link java.lang.Class} that is described.
         */
        class ForType<U extends Class<U>> implements AnnotationValue<TypeDescription, U> {

            /**
             * Indicates to a class loading process that class initializers are not required to be executed when loading a type.
             */
            private static final boolean NO_INITIALIZATION = false;

            /**
             * A description of the represented type.
             */
            private final TypeDescription typeDescription;

            /**
             * Creates a new annotation value that represents a type.
             *
             * @param typeDescription The represented type.
             */
            public ForType(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            /**
             * Creates an annotation value for representing the given type.
             *
             * @param typeDescription The type to represent.
             * @param <V>             The represented type.
             * @return An annotation value that represents the given type.
             */
            public static <V extends Class<V>> AnnotationValue<TypeDescription, V> of(TypeDescription typeDescription) {
                return new ForType<V>(typeDescription);
            }

            @Override
            public TypeDescription resolve() {
                return typeDescription;
            }

            @Override
            @SuppressWarnings("unchecked")
            public AnnotationValue.Loaded<U> load(ClassLoader classLoader) throws ClassNotFoundException {
                return new Loaded<U>((U) Class.forName(typeDescription.getName(), NO_INITIALIZATION, classLoader));
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ForType forType = (ForType) other;
                return typeDescription.equals(forType.typeDescription);
            }

            @Override
            public int hashCode() {
                return typeDescription.hashCode();
            }

            @Override
            public String toString() {
                return "AnnotationDescription.AnnotationValue.ForType{" +
                        "typeDescription=" + typeDescription +
                        '}';
            }

            /**
             * A loaded annotation value for a given type.
             *
             * @param <U> The represented type.
             */
            protected static class Loaded<U extends Class<U>> implements AnnotationValue.Loaded<U> {

                /**
                 * The represented type.
                 */
                private final U type;

                /**
                 * Creates a new loaded annotation value for a given type.
                 *
                 * @param type The represented type.
                 */
                public Loaded(U type) {
                    this.type = type;
                }

                @Override
                public State getState() {
                    return State.RESOLVED;
                }

                @Override
                public U resolve() {
                    return type;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof AnnotationValue.Loaded<?>)) return false;
                    AnnotationValue.Loaded<?> loadedOther = (AnnotationValue.Loaded<?>) other;
                    return loadedOther.getState().isResolved() && type.equals(loadedOther.resolve());
                }

                @Override
                public int hashCode() {
                    return type.hashCode();
                }

                @Override
                public String toString() {
                    return type.toString();
                }
            }
        }

        /**
         * Describes a complex array that is the value of an annotation. Complex arrays are arrays that might trigger the loading
         * of user-defined types, i.e. {@link java.lang.Class}, {@link java.lang.annotation.Annotation} and {@link java.lang.Enum}
         * instances.
         *
         * @param <U> The component type of the annotation's value when it is not loaded.
         * @param <V> The component type of the annotation's value when it is loaded.
         */
        class ForComplexArray<U, V> implements AnnotationValue<U[], V[]> {

            /**
             * The component type for arrays containing unloaded versions of the annotation array's values.
             */
            private final Class<?> unloadedComponentType;

            /**
             * A description of the component type when it is loaded.
             */
            private final TypeDescription componentType;

            /**
             * A list of values of the array elements.
             */
            private final List<? extends AnnotationValue<?, ?>> annotationValues;

            /**
             * Creates a new complex array.
             *
             * @param unloadedComponentType The component type for arrays containing unloaded versions of the annotation array's values.
             * @param componentType         A description of the component type when it is loaded.
             * @param annotationValues      A list of values of the array elements.
             */
            protected ForComplexArray(Class<?> unloadedComponentType,
                                      TypeDescription componentType,
                                      List<? extends AnnotationValue<?, ?>> annotationValues) {
                this.unloadedComponentType = unloadedComponentType;
                this.componentType = componentType;
                this.annotationValues = annotationValues;
            }

            /**
             * Creates a new complex array of enumeration descriptions.
             *
             * @param enumerationType        A description of the type of the enumeration.
             * @param enumerationDescription An array of enumeration descriptions.
             * @param <W>                    The type of the enumeration.
             * @return A description of the array of enumeration values.
             */
            public static <W extends Enum<W>> AnnotationValue<EnumerationDescription[], W[]> of(TypeDescription enumerationType,
                                                                                                EnumerationDescription[] enumerationDescription) {
                List<AnnotationValue<EnumerationDescription, W>> values = new ArrayList<AnnotationValue<EnumerationDescription, W>>(enumerationDescription.length);
                for (EnumerationDescription value : enumerationDescription) {
                    if (!value.getEnumerationType().equals(enumerationType)) {
                        throw new IllegalArgumentException(value + " is not of " + enumerationType);
                    }
                    values.add(ForEnumeration.<W>of(value));
                }
                return new ForComplexArray<EnumerationDescription, W>(EnumerationDescription.class, enumerationType, values);
            }

            /**
             * Creates a new complex array of annotation descriptions.
             *
             * @param annotationType        A description of the type of the annotation.
             * @param annotationDescription An array of annotation descriptions.
             * @param <W>                   The type of the annotation.
             * @return A description of the array of enumeration values.
             */
            public static <W extends Annotation> AnnotationValue<AnnotationDescription[], W[]> of(TypeDescription annotationType,
                                                                                                  AnnotationDescription[] annotationDescription) {
                List<AnnotationValue<AnnotationDescription, W>> values = new ArrayList<AnnotationValue<AnnotationDescription, W>>(annotationDescription.length);
                for (AnnotationDescription value : annotationDescription) {
                    if (!value.getAnnotationType().equals(annotationType)) {
                        throw new IllegalArgumentException(value + " is not of " + annotationType);
                    }
                    values.add(new ForAnnotation<W>(value));
                }
                return new ForComplexArray<AnnotationDescription, W>(AnnotationDescription.class, annotationType, values);
            }

            /**
             * Creates a new complex array of annotation descriptions.
             *
             * @param typeDescription A description of the types contained in the array.
             * @return A description of the array of enumeration values.
             */
            @SuppressWarnings("unchecked")
            public static AnnotationValue<TypeDescription[], Class<?>[]> of(TypeDescription[] typeDescription) {
                List<AnnotationValue<TypeDescription, Class<?>>> values = new ArrayList<AnnotationValue<TypeDescription, Class<?>>>(typeDescription.length);
                for (TypeDescription value : typeDescription) {
                    values.add((AnnotationValue) ForType.<Class>of(value));
                }
                return new ForComplexArray<TypeDescription, Class<?>>(TypeDescription.class, TypeDescription.CLASS, values);
            }

            @Override
            public U[] resolve() {
                @SuppressWarnings("unchecked")
                U[] value = (U[]) Array.newInstance(unloadedComponentType, annotationValues.size());
                int index = 0;
                for (AnnotationValue<?, ?> annotationValue : annotationValues) {
                    Array.set(value, index++, annotationValue.resolve());
                }
                return value;
            }

            @Override
            @SuppressWarnings("unchecked")
            public AnnotationValue.Loaded<V[]> load(ClassLoader classLoader) throws ClassNotFoundException {
                List<AnnotationValue.Loaded<?>> loadedValues = new ArrayList<AnnotationValue.Loaded<?>>(annotationValues.size());
                for (AnnotationValue<?, ?> value : annotationValues) {
                    loadedValues.add(value.load(classLoader));
                }
                return new Loaded<V>((Class<V>) classLoader.loadClass(componentType.getName()), loadedValues);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ForComplexArray that = (ForComplexArray) other;
                return annotationValues.equals(that.annotationValues)
                        && componentType.equals(that.componentType)
                        && unloadedComponentType.equals(that.unloadedComponentType);
            }

            @Override
            public int hashCode() {
                int result = unloadedComponentType.hashCode();
                result = 31 * result + componentType.hashCode();
                result = 31 * result + annotationValues.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AnnotationDescription.AnnotationValue.ForComplexArra{" +
                        "unloadedComponentType=" + unloadedComponentType +
                        ", componentType=" + componentType +
                        ", annotationValues=" + annotationValues +
                        '}';
            }

            /**
             * Represents a loaded complex array.
             *
             * @param <W> The component type of the loaded array.
             */
            protected static class Loaded<W> implements AnnotationValue.Loaded<W[]> {

                /**
                 * The loaded component type of the array.
                 */
                private final Class<W> componentType;

                /**
                 * A list of loaded values that the represented array contains.
                 */
                private final List<AnnotationValue.Loaded<?>> values;

                /**
                 * Creates a new loaded value representing a complex array.
                 *
                 * @param componentType The loaded component type of the array.
                 * @param values        A list of loaded values that the represented array contains.
                 */
                protected Loaded(Class<W> componentType, List<AnnotationValue.Loaded<?>> values) {
                    this.componentType = componentType;
                    this.values = values;
                }

                @Override
                public State getState() {
                    for (AnnotationValue.Loaded<?> value : values) {
                        if (!value.getState().isResolved()) {
                            return State.NON_RESOLVED;
                        }
                    }
                    return State.RESOLVED;
                }

                @Override
                public W[] resolve() {
                    @SuppressWarnings("unchecked")
                    W[] array = (W[]) Array.newInstance(componentType, values.size());
                    int index = 0;
                    for (AnnotationValue.Loaded<?> annotationValue : values) {
                        Array.set(array, index++, annotationValue.resolve());
                    }
                    return array;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof AnnotationValue.Loaded<?>)) return false;
                    AnnotationValue.Loaded<?> loadedOther = (AnnotationValue.Loaded<?>) other;
                    if (!loadedOther.getState().isResolved()) return false;
                    Object otherValue = loadedOther.resolve();
                    if (!(otherValue instanceof Object[])) return false;
                    Object[] otherArrayValue = (Object[]) otherValue;
                    if (values.size() != otherArrayValue.length) return false;
                    Iterator<AnnotationValue.Loaded<?>> iterator = values.iterator();
                    for (Object value : otherArrayValue) {
                        AnnotationValue.Loaded<?> self = iterator.next();
                        if (!self.getState().isResolved() || !self.resolve().equals(value)) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public int hashCode() {
                    int result = 1;
                    for (AnnotationValue.Loaded<?> value : values) {
                        result = 31 * result + value.hashCode();
                    }
                    return result;
                }

                @Override
                public String toString() {
                    StringBuilder stringBuilder = new StringBuilder("[");
                    for (AnnotationValue.Loaded<?> value : values) {
                        stringBuilder.append(value.toString());
                    }
                    return stringBuilder.append("]").toString();
                }
            }
        }
    }

    /**
     * An annotation description that is linked to a given loaded annotation type which allows its representation
     * as a fully loaded instance.
     *
     * @param <S> The annotation type.
     */
    interface Loadable<S extends Annotation> extends AnnotationDescription {

        /**
         * Loads this annotation description. This causes all classes referenced by the annotation value to be loaded.
         * Without specifying a class loader, the annotation's class loader which was used to prepare this instance
         * is used.
         *
         * @return A loaded version of this annotation description.
         * @throws java.lang.ClassNotFoundException If any linked classes of the annotation cannot be loaded.
         */
        S load() throws ClassNotFoundException;

        /**
         * Loads this annotation description. This causes all classes referenced by the annotation value to be loaded.
         *
         * @param classLoader The class loader to be used for loading the annotation's linked types.
         * @return A loaded version of this annotation description.
         * @throws java.lang.ClassNotFoundException If any linked classes of the annotation cannot be loaded.
         */
        S load(ClassLoader classLoader) throws ClassNotFoundException;

        /**
         * Loads this annotation description. This causes all classes referenced by the annotation value to be loaded.
         * Without specifying a class loader, the annotation's class loader which was used to prepare this instance
         * is used. Any {@link java.lang.ClassNotFoundException} is wrapped in an {@link java.lang.IllegalStateException}.
         *
         * @return A loaded version of this annotation description.
         */
        S loadSilent();

        /**
         * Loads this annotation description. This causes all classes referenced by the annotation value to be loaded.
         * Any {@link java.lang.ClassNotFoundException} is wrapped in an {@link java.lang.IllegalStateException}.
         *
         * @param classLoader The class loader to be used for loading the annotation's linked types.
         * @return A loaded version of this annotation description.
         */
        S loadSilent(ClassLoader classLoader);
    }

    /**
     * An {@link java.lang.reflect.InvocationHandler} for implementing annotations.
     *
     * @param <T> The type of the handled annotation.
     */
    class AnnotationInvocationHandler<T extends Annotation> implements InvocationHandler {

        /**
         * The name of the {@link Object#hashCode()} method.
         */
        private static final String HASH_CODE = "hashCode";

        /**
         * The name of the {@link Object#equals(Object)} method.
         */
        private static final String EQUALS = "equals";

        /**
         * The name of the {@link Object#toString()} method.
         */
        private static final String TO_STRING = "toString";

        /**
         * The loaded annotation type.
         */
        private final Class<? extends Annotation> annotationType;

        /**
         * A sorted list of values of this annotation.
         */
        private final LinkedHashMap<Method, AnnotationValue.Loaded<?>> values;

        /**
         * Creates a new invocation handler.
         *
         * @param annotationType The loaded annotation type.
         * @param values         A sorted list of values of this annotation.
         */
        protected AnnotationInvocationHandler(Class<T> annotationType, LinkedHashMap<Method, AnnotationValue.Loaded<?>> values) {
            this.annotationType = annotationType;
            this.values = values;
        }

        /**
         * @param classLoader    The class loader that should be used for loading the annotation's values.
         * @param annotationType The annotation's type.
         * @param values         The values that the annotation contains.
         * @param <S>            The type of the handled annotation.
         * @return An appropriate invocation handler.
         * @throws ClassNotFoundException If the class of an instance that is contained by this annotation could not be found.
         */
        public static <S extends Annotation> InvocationHandler of(ClassLoader classLoader,
                                                                  Class<S> annotationType,
                                                                  Map<String, AnnotationDescription.AnnotationValue<?, ?>> values)
                throws ClassNotFoundException {
            Method[] declaredMethod = annotationType.getDeclaredMethods();
            LinkedHashMap<Method, AnnotationValue.Loaded<?>> loadedValues = new LinkedHashMap<Method, AnnotationValue.Loaded<?>>(declaredMethod.length);
            for (Method method : declaredMethod) {
                AnnotationDescription.AnnotationValue<?, ?> annotationValue = values.get(method.getName());
                loadedValues.put(method, annotationValue == null
                        ? DefaultValue.of(method)
                        : annotationValue.load(classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader));
            }
            return new AnnotationInvocationHandler<S>(annotationType, loadedValues);
        }

        /**
         * Resolves any primitive type to its wrapper type.
         *
         * @param type The type to resolve.
         * @return The resolved type.
         */
        private static Class<?> asWrapper(Class<?> type) {
            if (type.isPrimitive()) {
                if (type == boolean.class) {
                    return Boolean.class;
                } else if (type == byte.class) {
                    return Byte.class;
                } else if (type == short.class) {
                    return Short.class;
                } else if (type == char.class) {
                    return Character.class;
                } else if (type == int.class) {
                    return Integer.class;
                } else if (type == long.class) {
                    return Long.class;
                } else if (type == float.class) {
                    return Float.class;
                } else if (type == double.class) {
                    return Double.class;
                }
            }
            return type;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            if (method.getDeclaringClass() != annotationType) {
                if (method.getName().equals(HASH_CODE)) {
                    return hashCodeRepresentation();
                } else if (method.getName().equals(EQUALS) && method.getParameterTypes().length == 1) {
                    return equalsRepresentation(proxy, arguments[0]);
                } else if (method.getName().equals(TO_STRING)) {
                    return toStringRepresentation();
                } else /* method.getName().equals("annotationType") */ {
                    return annotationType;
                }
            }
            Object value = values.get(method).resolve();
            if (!asWrapper(method.getReturnType()).isAssignableFrom(value.getClass())) {
                throw new AnnotationTypeMismatchException(method, value.getClass().toString());
            }
            return value;
        }

        /**
         * Returns the string representation of the represented annotation.
         *
         * @return The string representation of the represented annotation.
         */
        protected String toStringRepresentation() {
            StringBuilder toString = new StringBuilder();
            toString.append('@');
            toString.append(annotationType.getName());
            toString.append('(');
            boolean firstMember = true;
            for (Map.Entry<Method, AnnotationDescription.AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                if (!entry.getValue().getState().isDefined()) {
                    continue;
                }
                if (firstMember) {
                    firstMember = false;
                } else {
                    toString.append(", ");
                }
                toString.append(entry.getKey().getName());
                toString.append('=');
                toString.append(entry.getValue().toString());
            }
            toString.append(')');
            return toString.toString();
        }

        /**
         * Returns the hash code of the represented annotation.
         *
         * @return The hash code of the represented annotation.
         */
        private int hashCodeRepresentation() {
            int hashCode = 0;
            for (Map.Entry<Method, AnnotationDescription.AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                if (!entry.getValue().getState().isDefined()) {
                    continue;
                }
                hashCode += (127 * entry.getKey().getName().hashCode()) ^ entry.getValue().hashCode();
            }
            return hashCode;
        }

        /**
         * Checks if another instance is equal to this instance.
         *
         * @param self  The annotation proxy instance.
         * @param other The instance to be examined for equality to the represented instance.
         * @return {@code true} if the given instance is equal to the represented instance.
         */
        private boolean equalsRepresentation(Object self, Object other) {
            if (self == other) {
                return true;
            } else if (!annotationType.isInstance(other)) {
                return false;
            } else if (Proxy.isProxyClass(other.getClass())) {
                InvocationHandler invocationHandler = Proxy.getInvocationHandler(other);
                if (invocationHandler instanceof AnnotationInvocationHandler) {
                    return invocationHandler.equals(this);
                }
            }
            try {
                for (Map.Entry<Method, AnnotationDescription.AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                    if (entry.getValue().getState().isResolved()) {
                        try {
                            if (!PropertyDispatcher.of(entry.getKey().getReturnType())
                                    .equals(entry.getValue().resolve(), entry.getKey().invoke(other))) {
                                return false;
                            }
                        } catch (RuntimeException exception) {
                            return false; // Incomplete annotations are not equal to one another.
                        }
                    } else {
                        return false;
                    }
                }
            } catch (InvocationTargetException ignored) {
                return false;
            } catch (IllegalAccessException exception) {
                throw new AssertionError(exception);
            }
            return true;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof AnnotationInvocationHandler)) return false;
            AnnotationInvocationHandler that = (AnnotationInvocationHandler) other;
            if (!annotationType.equals(that.annotationType)) {
                return false;
            }
            for (Map.Entry<Method, ?> entry : values.entrySet()) {
                Object value = that.values.get(entry.getKey());
                if (!PropertyDispatcher.of(value.getClass()).equals(value, entry.getValue())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = annotationType.hashCode();
            result = 31 * result + values.hashCode();
            for (Map.Entry<Method, ?> entry : values.entrySet()) {
                result = 31 * result + PropertyDispatcher.of(entry.getValue().getClass()).hashCode(entry.getValue());
            }
            return result;
        }

        @Override
        public String toString() {
            return "TypePool.LazyTypeDescription.AnnotationInvocationHandler{" +
                    "annotationType=" + annotationType +
                    ", values=" + values +
                    '}';
        }

        /**
         * Represents a default value for an annotation property that is not explicitly defined by
         * an annotation.
         */
        protected static class DefaultValue implements AnnotationDescription.AnnotationValue.Loaded<Object> {

            /**
             * The represented default value.
             */
            private final Object defaultValue;

            /**
             * The property dispatcher for the type of the default value.
             */
            private final PropertyDispatcher propertyDispatcher;

            /**
             * Creates a new representation of an existent default value.
             *
             * @param defaultValue The represented, non-{@code null} default value.
             */
            private DefaultValue(Object defaultValue) {
                this.defaultValue = defaultValue;
                propertyDispatcher = PropertyDispatcher.of(defaultValue.getClass());
            }

            /**
             * Creates a default value representation for a given method which might or might not provide such
             * a default value.
             *
             * @param method The method for which a default value is to be extracted.
             * @return An annotation value representation for the given method.
             */
            @SuppressWarnings("unchecked")
            protected static AnnotationDescription.AnnotationValue.Loaded<?> of(Method method) {
                Object defaultValue = method.getDefaultValue();
                return defaultValue == null
                        ? new Missing((Class<? extends Annotation>) method.getDeclaringClass(), method.getName())
                        : new DefaultValue(defaultValue);
            }

            @Override
            public State getState() {
                return State.RESOLVED;
            }

            @Override
            public Object resolve() {
                return propertyDispatcher.conditionalClone(defaultValue);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (!(other instanceof AnnotationDescription.AnnotationValue.Loaded<?>)) return false;
                AnnotationDescription.AnnotationValue.Loaded<?> loaded = (AnnotationDescription.AnnotationValue.Loaded<?>) other;
                return loaded.getState().isResolved() && propertyDispatcher.equals(defaultValue, loaded.resolve());
            }

            @Override
            public int hashCode() {
                return propertyDispatcher.hashCode(defaultValue);
            }

            @Override
            public String toString() {
                return propertyDispatcher.toString(defaultValue);
            }
        }

        /**
         * Represents a missing annotation property which is not represented by a default value.
         */
        private static class Missing implements AnnotationDescription.AnnotationValue.Loaded<Void> {

            /**
             * The annotation type.
             */
            private final Class<? extends Annotation> annotationType;

            /**
             * The name of the property without an annotation value.
             */
            private final String property;

            /**
             * Creates a new representation for a missing annotation property.
             *
             * @param annotationType The annotation type.
             * @param property       The name of the property without an annotation value.
             */
            private Missing(Class<? extends Annotation> annotationType, String property) {
                this.annotationType = annotationType;
                this.property = property;
            }

            @Override
            public State getState() {
                return State.NON_DEFINED;
            }

            @Override
            public Void resolve() {
                throw new IncompleteAnnotationException(annotationType, property);
            }
        }
    }

    /**
     * An adapter implementation of an annotation.
     */
    abstract class AbstractBase implements AnnotationDescription {

        @Override
        public RetentionPolicy getRetention() {
            AnnotationDescription.Loadable<Retention> retention = getAnnotationType().getDeclaredAnnotations().ofType(Retention.class);
            return retention == null
                    ? RetentionPolicy.SOURCE
                    : retention.loadSilent().value();
        }

        @Override
        public boolean isInherited() {
            return getAnnotationType().getDeclaredAnnotations().isAnnotationPresent(Inherited.class);
        }

        @Override
        public boolean isDocumented() {
            return getAnnotationType().getDeclaredAnnotations().isAnnotationPresent(Documented.class);
        }

        @Override
        public <T> T getValue(MethodDescription.InDefinedShape methodDescription, Class<T> type) {
            return type.cast(getValue(methodDescription));
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof AnnotationDescription)) {
                return false;
            }
            AnnotationDescription annotationDescription = ((AnnotationDescription) other);
            if (!annotationDescription.getAnnotationType().equals(getAnnotationType())) {
                return false;
            }
            for (MethodDescription.InDefinedShape methodDescription : getAnnotationType().getDeclaredMethods()) {
                Object value = getValue(methodDescription);
                if (!PropertyDispatcher.of(value.getClass()).equals(value, annotationDescription.getValue(methodDescription))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            for (MethodDescription.InDefinedShape methodDescription : getAnnotationType().getDeclaredMethods()) {
                Object value = getValue(methodDescription);
                hashCode += 31 * PropertyDispatcher.of(value.getClass()).hashCode(value);
            }
            return hashCode;
        }

        @Override
        public String toString() {
            StringBuilder toString = new StringBuilder();
            toString.append('@');
            toString.append(getAnnotationType().getName());
            toString.append('(');
            boolean firstMember = true;
            for (MethodDescription.InDefinedShape methodDescription : getAnnotationType().getDeclaredMethods()) {
                if (firstMember) {
                    firstMember = false;
                } else {
                    toString.append(", ");
                }
                toString.append(methodDescription.getName());
                toString.append('=');
                Object value = getValue(methodDescription);
                toString.append(PropertyDispatcher.of(value.getClass()).toString(value));
            }
            toString.append(')');
            return toString.toString();
        }

        /**
         * An abstract implementation of a loadable annotation description.
         *
         * @param <S> The annotation type this instance was prepared for.
         */
        public abstract static class ForPrepared<S extends Annotation> extends AbstractBase implements Loadable<S> {

            /**
             * The error message to be displayed on a {@link java.lang.ClassNotFoundException}.
             */
            public static final String ERROR_MESSAGE = "Could not load a type that is linked by the annotation value";

            @Override
            public S loadSilent() {
                try {
                    return load();
                } catch (ClassNotFoundException exception) {
                    throw new IllegalStateException(ERROR_MESSAGE, exception);
                }
            }

            @Override
            public S loadSilent(ClassLoader classLoader) {
                try {
                    return load(classLoader);
                } catch (ClassNotFoundException exception) {
                    throw new IllegalStateException(ERROR_MESSAGE, exception);
                }
            }
        }
    }

    /**
     * A description of an already loaded annotation.
     *
     * @param <S> The type of the annotation.
     */
    class ForLoadedAnnotation<S extends Annotation> extends AbstractBase.ForPrepared<S> {

        /**
         * The represented annotation value.
         */
        private final S annotation;

        /**
         * Creates a new annotation description for a loaded annotation.
         *
         * @param annotation The annotation to represent.
         */
        protected ForLoadedAnnotation(S annotation) {
            this.annotation = annotation;
        }

        /**
         * Creates a description of the given annotation.
         *
         * @param annotation The annotation to be described.
         * @param <U>        The type of the annotation.
         * @return A description of the given annotation.
         */
        public static <U extends Annotation> Loadable<U> of(U annotation) {
            return new ForLoadedAnnotation<U>(annotation);
        }

        /**
         * A helper method for converting a loaded type into a representation that is also capable of representing
         * unloaded descriptions of annotation values as specified by
         * {@link AnnotationDescription}.
         *
         * @param value           The loaded value.
         * @param typeDescription The annotation type of the value. This cannot be inferred as enumerations
         *                        can implement annotation interfaces and because annotations could be implemented as
         *                        an enumeration what creates an ambiguity.
         * @return The wrapped representation as specified by
         * {@link AnnotationDescription}.
         */
        public static Object describe(Object value, TypeDescription typeDescription) {
            // Because enums can implement annotation interfaces, the enum property needs to be checked first.
            if (typeDescription.represents(Class.class)) {
                value = new TypeDescription.ForLoadedType((Class<?>) value);
            } else if (typeDescription.represents(Class[].class)) {
                value = new TypeList.ForLoadedType((Class<?>[]) value).toArray(new TypeDescription[((Class<?>[]) value).length]);
            } else if (typeDescription.isAssignableTo(Enum.class)) {
                value = new EnumerationDescription.ForLoadedEnumeration((Enum<?>) value);
            } else if (typeDescription.isAssignableTo(Enum[].class)) {
                value = EnumerationDescription.ForLoadedEnumeration.asList((Enum<?>[]) value).toArray(new EnumerationDescription[((Enum<?>[]) value).length]);
            } else if (typeDescription.isAssignableTo(Annotation.class)) {
                value = ForLoadedAnnotation.of((Annotation) value);
            } else if (typeDescription.isAssignableTo(Annotation[].class)) {
                value = new AnnotationList.ForLoadedAnnotation((Annotation[]) value).toArray(new AnnotationDescription[((Annotation[]) value).length]);
            }
            return value;
        }

        @Override
        public S load() {
            return annotation;
        }

        @Override
        public S load(ClassLoader classLoader) {
            ClassLoader thisClassLoader = annotation.getClass().getClassLoader();
            ClassLoader otherClassLoader = classLoader;
            while (otherClassLoader != null) {
                if (otherClassLoader == thisClassLoader) {
                    break;
                }
                otherClassLoader = otherClassLoader.getParent();
            }
            if (otherClassLoader != thisClassLoader) {
                throw new IllegalArgumentException(annotation + " is not loaded using " + classLoader);
            }
            return load();
        }

        @Override
        public Object getValue(MethodDescription.InDefinedShape methodDescription) {
            if (!methodDescription.getDeclaringType().represents(annotation.annotationType())) {
                throw new IllegalArgumentException(methodDescription + " does not represent " + annotation.annotationType());
            }
            try {
                boolean accessible = methodDescription.isAccessibleTo(new TypeDescription.ForLoadedType(getClass()));
                Method method = methodDescription instanceof MethodDescription.ForLoadedMethod
                        ? ((MethodDescription.ForLoadedMethod) methodDescription).getLoadedMethod()
                        : null;
                if (method == null || (!accessible && !method.isAccessible())) {
                    method = annotation.annotationType().getDeclaredMethod(methodDescription.getName());
                    if (!accessible) {
                        AccessController.doPrivileged(new MethodAccessibilityAction(method));
                    }
                }
                return describe(method.invoke(annotation), methodDescription.getReturnType().asErasure());
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot access annotation property " + methodDescription, exception);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
            if (annotation.annotationType() != annotationType) {
                throw new IllegalArgumentException(annotation + " type is not of identical to " + annotationType);
            }
            return (Loadable<T>) this;
        }

        @Override
        public TypeDescription getAnnotationType() {
            return new TypeDescription.ForLoadedType(annotation.annotationType());
        }


        /**
         * Sets a method accessible.
         */
        protected static class MethodAccessibilityAction implements PrivilegedAction<Void> {

            /**
             * Indicates that this action returns nothing.
             */
            private static final Void NOTHING = null;

            /**
             * The method to make accessible.
             */
            private final Method method;

            /**
             * Creates a new method accessibility action.
             *
             * @param method The method to make accessible.
             */
            protected MethodAccessibilityAction(Method method) {
                this.method = method;
            }

            @Override
            public Void run() {
                method.setAccessible(true);
                return NOTHING;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                MethodAccessibilityAction that = (MethodAccessibilityAction) other;
                return method.equals(that.method);
            }

            @Override
            public int hashCode() {
                return method.hashCode();
            }

            @Override
            public String toString() {
                return "AnnotationDescription.ForLoadedAnnotation.MethodAccessibilityAction{" +
                        "method=" + method +
                        '}';
            }
        }
    }

    /**
     * A latent description of an annotation value that is defined explicitly.
     */
    class Latent extends AbstractBase {

        /**
         * The type of the annotation.
         */
        private final TypeDescription annotationType;

        /**
         * The values of the annotation mapped by their property name.
         */
        private final Map<String, AnnotationValue<?, ?>> annotationValues;

        /**
         * Creates a new latent annotation description.
         *
         * @param annotationType   The type of the annotation.
         * @param annotationValues The values of the annotation mapped by their property name.
         */
        protected Latent(TypeDescription annotationType, Map<String, AnnotationValue<?, ?>> annotationValues) {
            this.annotationType = annotationType;
            this.annotationValues = annotationValues;
        }

        @Override
        public Object getValue(MethodDescription.InDefinedShape methodDescription) {
            AnnotationValue<?, ?> value = annotationValues.get(methodDescription.getName());
            if (value != null) {
                return value.resolve();
            }
            Object defaultValue = methodDescription.getDefaultValue();
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new IllegalArgumentException("No value defined for: " + methodDescription);
        }

        @Override
        public TypeDescription getAnnotationType() {
            return annotationType;
        }

        @Override
        public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
            if (!this.annotationType.represents(annotationType)) {
                throw new IllegalArgumentException("Not a compatible annotation type: " + annotationType);
            }
            return new Loadable<T>(annotationType);
        }

        /**
         * A loadable annotation description of a latent annotation description.
         *
         * @param <S> The annotation type.
         */
        protected class Loadable<S extends Annotation> extends AbstractBase.ForPrepared<S> {

            /**
             * The annotation type.
             */
            private final Class<S> annotationType;

            /**
             * Creates a loadable version of a latent annotation description.
             *
             * @param annotationType The annotation type.
             */
            protected Loadable(Class<S> annotationType) {
                this.annotationType = annotationType;
            }

            @Override
            public S load() throws ClassNotFoundException {
                return load(annotationType.getClassLoader());
            }

            @Override
            @SuppressWarnings("unchecked")
            public S load(ClassLoader classLoader) throws ClassNotFoundException {
                return (S) Proxy.newProxyInstance(classLoader,
                        new Class<?>[]{annotationType},
                        AnnotationDescription.AnnotationInvocationHandler.of(classLoader, annotationType, annotationValues));
            }

            @Override
            public Object getValue(MethodDescription.InDefinedShape methodDescription) {
                return Latent.this.getValue(methodDescription);
            }

            @Override
            public TypeDescription getAnnotationType() {
                return Latent.this.getAnnotationType();
            }

            @Override
            public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
                return Latent.this.prepare(annotationType);
            }
        }
    }

    /**
     * A builder for pragmatically creating {@link net.bytebuddy.description.annotation.AnnotationDescription}.
     */
    class Builder {

        /**
         * The annotation type.
         */
        private final TypeDescription annotationType;

        /**
         * A mapping of annotation properties to their annotation values.
         */
        private final Map<String, AnnotationValue<?, ?>> annotationValues;

        /**
         * Creates a builder for an annotation description.
         *
         * @param annotationType   The annotation type.
         * @param annotationValues A mapping of annotation properties to their annotation values.
         */
        protected Builder(TypeDescription annotationType, Map<String, AnnotationValue<?, ?>> annotationValues) {
            this.annotationType = annotationType;
            this.annotationValues = annotationValues;
        }

        /**
         * Creates a builder for creating an annotation of the given type.
         *
         * @param annotationType The annotation type.
         * @return A builder for creating an annotation of the given type.
         */
        public static Builder forType(Class<? extends Annotation> annotationType) {
            return forType(new TypeDescription.ForLoadedType(nonNull(annotationType)));
        }

        /**
         * Creates a builder for creating an annotation of the given type.
         *
         * @param annotationType A description of the annotation type.
         * @return A builder for creating an annotation of the given type.
         */
        public static Builder forType(TypeDescription annotationType) {
            if (!annotationType.isAnnotation()) {
                throw new IllegalArgumentException("Not an annotation type: " + annotationType);
            }
            return new Builder(annotationType, Collections.<String, AnnotationValue<?, ?>>emptyMap());
        }

        /**
         * Returns a builder with the additional, given property.
         *
         * @param property The name of the property to define.
         * @param value    An explicit description of the annotation value.
         * @return A builder with the additional, given property.
         */
        public Builder define(String property, AnnotationValue<?, ?> value) {
            MethodList<?> methodDescriptions = annotationType.getDeclaredMethods().filter(named(nonNull(property)));
            if (methodDescriptions.isEmpty()) {
                throw new IllegalArgumentException(annotationType + " does not define a property named " + property);
            } else if (!methodDescriptions.getOnly().getReturnType().asErasure().isAnnotationValue(value.resolve())) {
                throw new IllegalArgumentException(value + " cannot be assigned to " + property);
            }
            Map<String, AnnotationValue<?, ?>> annotationValues = new HashMap<String, AnnotationValue<?, ?>>(this.annotationValues.size() + 1);
            annotationValues.putAll(this.annotationValues);
            if (annotationValues.put(methodDescriptions.getOnly().getName(), nonNull(value)) != null) {
                throw new IllegalArgumentException("Property already defined: " + property);
            }
            return new Builder(annotationType, annotationValues);
        }

        /**
         * Returns a builder with the additional enumeration property.
         *
         * @param property The name of the property to define.
         * @param value    The enumeration value to define.
         * @return A builder with the additional enumeration property.
         */
        public Builder define(String property, Enum<?> value) {
            return define(property, new EnumerationDescription.ForLoadedEnumeration(nonNull(value)));
        }

        /**
         * Returns a builder with the additional enumeration property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumeration.
         * @param value           The enumeration value to define.
         * @return A builder with the additional enumeration property.
         */
        public Builder define(String property, TypeDescription enumerationType, String value) {
            return define(property, new EnumerationDescription.Latent(nonNull(enumerationType), nonNull(value)));
        }

        /**
         * Returns a builder with the additional enumeration property.
         *
         * @param property The name of the property to define.
         * @param value    A description of the enumeration value to define.
         * @return A builder with the additional enumeration property.
         */
        @SuppressWarnings("unchecked")
        public Builder define(String property, EnumerationDescription value) {
            return define(property, AnnotationValue.ForEnumeration.<Enum>of(nonNull(value)));
        }

        /**
         * Returns a builder with the additional annotation property.
         *
         * @param property   The name of the property to define.
         * @param annotation The annotation value to define.
         * @return A builder with the additional annotation property.
         */
        public Builder define(String property, Annotation annotation) {
            return define(property, new ForLoadedAnnotation<Annotation>(nonNull(annotation)));
        }

        /**
         * Returns a builder with the additional annotation property.
         *
         * @param property              The name of the property to define.
         * @param annotationDescription A description of the annotation value to define.
         * @return A builder with the additional annotation property.
         */
        public Builder define(String property, AnnotationDescription annotationDescription) {
            return define(property, new AnnotationValue.ForAnnotation<Annotation>(nonNull(annotationDescription)));
        }

        /**
         * Returns a builder with the additional class property.
         *
         * @param property The name of the property to define.
         * @param type     The class value to define.
         * @return A builder with the additional class property.
         */
        public Builder define(String property, Class<?> type) {
            return define(property, new TypeDescription.ForLoadedType(nonNull(type)));
        }

        /**
         * Returns a builder with the additional class property.
         *
         * @param property        The name of the property to define.
         * @param typeDescription A description of the type to define as a property value.
         * @return A builder with the additional class property.
         */
        @SuppressWarnings("unchecked")
        public Builder define(String property, TypeDescription typeDescription) {
            return define(property, AnnotationValue.ForType.<Class>of(typeDescription));
        }

        /**
         * Returns a builder with the additional enumeration array property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumeration, i.e. the component type of the enumeration array.
         * @param value           The enumeration values to be contained by the array.
         * @param <T>             The enumeration type.
         * @return A builder with the additional class property.
         */
        public <T extends Enum<?>> Builder defineEnumerationArray(String property, Class<T> enumerationType, T... value) {
            EnumerationDescription[] enumerationDescription = new EnumerationDescription[value.length];
            int index = 0;
            for (T aValue : value) {
                enumerationDescription[index++] = new EnumerationDescription.ForLoadedEnumeration(nonNull(aValue));
            }
            return defineEnumerationArray(property, new TypeDescription.ForLoadedType(nonNull(enumerationType)), enumerationDescription);
        }

        /**
         * Returns a builder with the additional enumeration array property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumerations, i.e. is the component type of the enumeration array.
         * @param value           The enumeration values to be contained by the array.
         * @return A builder with the additional enumeration property.
         */
        public Builder defineEnumerationArray(String property, TypeDescription enumerationType, String... value) {
            if (!enumerationType.isEnum()) {
                throw new IllegalArgumentException("Not an enumeration type: " + enumerationType);
            }
            EnumerationDescription[] enumerationDescription = new EnumerationDescription[value.length];
            for (int i = 0; i < value.length; i++) {
                enumerationDescription[i] = new EnumerationDescription.Latent(nonNull(enumerationType), nonNull(value[i]));
            }
            return defineEnumerationArray(property, enumerationType, enumerationDescription);
        }

        /**
         * Returns a builder with the additional enumeration array property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumerations, i.e. the component type of the enumeration array.
         * @param value           Descriptions of the enumerations to be contained by the array.
         * @return A builder with the additional enumeration property.
         */
        @SuppressWarnings("unchecked")
        public Builder defineEnumerationArray(String property, TypeDescription enumerationType, EnumerationDescription... value) {
            return define(property, AnnotationValue.ForComplexArray.<Enum>of(enumerationType, nonNull(value)));
        }

        /**
         * Returns a builder with the additional annotation array property.
         *
         * @param property       The name of the property to define.
         * @param annotationType The type of the annotations, i.e. the component type of the enumeration array.
         * @param annotation     The annotation values to be contained by the array.
         * @param <T>            The annotation type.
         * @return A builder with the additional annotation property.
         */
        public <T extends Annotation> Builder defineAnnotationArray(String property, Class<T> annotationType, T... annotation) {
            return defineAnnotationArray(property,
                    new TypeDescription.ForLoadedType(nonNull(annotationType)),
                    new AnnotationList.ForLoadedAnnotation(nonNull(annotation)).toArray(new AnnotationDescription[annotation.length]));
        }

        /**
         * Returns a builder with the additional annotation array property.
         *
         * @param property              The name of the property to define.
         * @param annotationType        The type of the annotations, i.e. the component type of the enumeration array.
         * @param annotationDescription Descriptions of the annotation values to be contained by the array.
         * @return A builder with the additional annotation property.
         */
        public Builder defineAnnotationArray(String property, TypeDescription annotationType, AnnotationDescription... annotationDescription) {
            return define(property, AnnotationValue.ForComplexArray.of(annotationType, nonNull(annotationDescription)));
        }

        /**
         * Returns a builder with the additional type array property.
         *
         * @param property The name of the property to define.
         * @param type     The types that should be contained by the array.
         * @return A builder with the additional type array property.
         */
        public Builder defineTypeArray(String property, Class<?>... type) {
            return defineTypeArray(property, new TypeList.ForLoadedType(nonNull(type)).toArray(new TypeDescription[type.length]));
        }

        /**
         * Returns a builder with the additional type array property.
         *
         * @param property        The name of the property to define.
         * @param typeDescription Descriptions of the types that should be contained by the array.
         * @return A builder with the additional type array property.
         */
        @SuppressWarnings("unchecked")
        public Builder defineTypeArray(String property, TypeDescription... typeDescription) {
            return define(property, AnnotationValue.ForComplexArray.of(typeDescription));
        }

        /**
         * Returns a builder with the additional {@code boolean} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code boolean} value to define for the property.
         * @return A builder with the additional {@code boolean} property.
         */
        public Builder define(String property, boolean value) {
            return define(property, new AnnotationValue.Trivial<Boolean>(value));
        }

        /**
         * Returns a builder with the additional {@code byte} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code byte} value to define for the property.
         * @return A builder with the additional {@code byte} property.
         */
        public Builder define(String property, byte value) {
            return define(property, new AnnotationValue.Trivial<Byte>(value));
        }

        /**
         * Returns a builder with the additional {@code char} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code char} value to define for the property.
         * @return A builder with the additional {@code char} property.
         */
        public Builder define(String property, char value) {
            return define(property, new AnnotationValue.Trivial<Character>(value));
        }

        /**
         * Returns a builder with the additional {@code short} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code short} value to define for the property.
         * @return A builder with the additional {@code short} property.
         */
        public Builder define(String property, short value) {
            return define(property, new AnnotationValue.Trivial<Short>(value));
        }

        /**
         * Returns a builder with the additional {@code int} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code int} value to define for the property.
         * @return A builder with the additional {@code int} property.
         */
        public Builder define(String property, int value) {
            return define(property, new AnnotationValue.Trivial<Integer>(value));
        }

        /**
         * Returns a builder with the additional {@code long} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code long} value to define for the property.
         * @return A builder with the additional {@code long} property.
         */
        public Builder define(String property, long value) {
            return define(property, new AnnotationValue.Trivial<Long>(value));
        }

        /**
         * Returns a builder with the additional {@code float} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code float} value to define for the property.
         * @return A builder with the additional {@code float} property.
         */
        public Builder define(String property, float value) {
            return define(property, new AnnotationValue.Trivial<Float>(value));
        }

        /**
         * Returns a builder with the additional {@code double} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code double} value to define for the property.
         * @return A builder with the additional {@code double} property.
         */
        public Builder define(String property, double value) {
            return define(property, new AnnotationValue.Trivial<Double>(value));
        }

        /**
         * Returns a builder with the additional {@link java.lang.String} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@link java.lang.String} value to define for the property.
         * @return A builder with the additional {@link java.lang.String} property.
         */
        public Builder define(String property, String value) {
            return define(property, new AnnotationValue.Trivial<String>(value));
        }

        /**
         * Returns a builder with the additional {@code boolean} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code boolean} values to define for the property.
         * @return A builder with the additional {@code boolean} array property.
         */
        public Builder defineArray(String property, boolean... value) {
            return define(property, new AnnotationValue.Trivial<boolean[]>(value));
        }

        /**
         * Returns a builder with the additional {@code byte} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code byte} values to define for the property.
         * @return A builder with the additional {@code byte} array property.
         */
        public Builder defineArray(String property, byte... value) {
            return define(property, new AnnotationValue.Trivial<byte[]>(value));
        }

        /**
         * Returns a builder with the additional {@code char} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code char} values to define for the property.
         * @return A builder with the additional {@code char} array property.
         */
        public Builder defineArray(String property, char... value) {
            return define(property, new AnnotationValue.Trivial<char[]>(value));
        }

        /**
         * Returns a builder with the additional {@code short} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code short} values to define for the property.
         * @return A builder with the additional {@code short} array property.
         */
        public Builder defineArray(String property, short... value) {
            return define(property, new AnnotationValue.Trivial<short[]>(value));
        }

        /**
         * Returns a builder with the additional {@code int} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code int} values to define for the property.
         * @return A builder with the additional {@code int} array property.
         */
        public Builder defineArray(String property, int... value) {
            return define(property, new AnnotationValue.Trivial<int[]>(value));
        }

        /**
         * Returns a builder with the additional {@code long} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code long} values to define for the property.
         * @return A builder with the additional {@code long} array property.
         */
        public Builder defineArray(String property, long... value) {
            return define(property, new AnnotationValue.Trivial<long[]>(value));
        }

        /**
         * Returns a builder with the additional {@code float} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code float} values to define for the property.
         * @return A builder with the additional {@code float} array property.
         */
        public Builder defineArray(String property, float... value) {
            return define(property, new AnnotationValue.Trivial<float[]>(value));
        }

        /**
         * Returns a builder with the additional {@code double} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code double} values to define for the property.
         * @return A builder with the additional {@code double} array property.
         */
        public Builder defineArray(String property, double... value) {
            return define(property, new AnnotationValue.Trivial<double[]>(value));
        }

        /**
         * Returns a builder with the additional {@link java.lang.String} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@link java.lang.String} array value to define for the property.
         * @return A builder with the additional {@link java.lang.String} array property.
         */
        public Builder defineArray(String property, String... value) {
            return define(property, new AnnotationValue.Trivial<String[]>(value));
        }

        /**
         * Creates an annotation description for the values that were defined for this builder.
         *
         * @return An appropriate annotation description.
         */
        public AnnotationDescription make() {
            for (MethodDescription methodDescription : annotationType.getDeclaredMethods()) {
                if (annotationValues.get(methodDescription.getName()) == null && methodDescription.getDefaultValue() == null) {
                    throw new IllegalStateException("No value or default value defined for " + methodDescription.getName());
                }
            }
            return new Latent(annotationType, annotationValues);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && annotationType.equals(((Builder) other).annotationType)
                    && annotationValues.equals(((Builder) other).annotationValues);
        }

        @Override
        public int hashCode() {
            int result = annotationType.hashCode();
            result = 31 * result + annotationValues.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "AnnotationDescription.Builder{" +
                    "annotationType=" + annotationType +
                    ", annotationValues=" + annotationValues +
                    '}';
        }
    }
}
