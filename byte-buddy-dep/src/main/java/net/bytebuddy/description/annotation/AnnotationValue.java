package net.bytebuddy.description.annotation;

import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.PropertyDispatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Representation of an unloaded annotation value where all values represent either:
 * <ul>
 * <li>Primitive values (as their wrappers), {@link String}s or arrays of primitive types or strings.</li>
 * <li>A {@link TypeDescription} or an array of such a descriptions.</li>
 * <li>An {@link EnumerationDescription} or an array of such a description.</li>
 * <li>An {@link AnnotationDescription} or an array of such a description.</li>
 * </ul>
 * The represented values are not necessarily resolvable, i.e. can contain non-available types, unknown enumeration
 * constants or inconsistent annotations.
 *
 * @param <T> The represented value's unloaded type.
 * @param <S> The represented value's  loaded type.
 */
public interface AnnotationValue<T, S> {

    /**
     * An undefined annotation value.
     */
    AnnotationValue<?, ?> UNDEFINED = null;

    /**
     * Resolves the unloaded value of this annotation.
     *
     * @return The unloaded value of this annotation.
     */
    T resolve();

    /**
     * Resolves the unloaded value of this annotation.
     *
     * @param type The annotation value's unloaded type.
     * @param <S>  The annotation value's unloaded type.
     * @return The unloaded value of this annotation.
     */
    <S> S resolve(Class<? extends S> type);

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
     * @param <U> The represented value's type.
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
         * Resolves the value to the actual value of an annotation. Calling this method might throw a runtime
         * exception if this value is either not defined or not resolved.
         *
         * @param type The value's loaded type.
         * @param <V>  The value's loaded type.
         * @return The actual annotation value represented by this instance.
         */
        <V> V resolve(Class<? extends V> type);

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
                return "AnnotationValue.Loaded.State." + name();
            }
        }

        /**
         * An abstract base implementation of a loaded annotation value.
         *
         * @param <W> The represented loaded type.
         */
        abstract class AbstractBase<W> implements Loaded<W> {

            @Override
            public <S> S resolve(Class<? extends S> type) {
                return type.cast(resolve());
            }
        }
    }

    /**
     * An abstract base implementation of an unloaded annotation value.
     *
     * @param <U> The represented unloaded type.
     * @param <V> The represented loaded type.
     */
    abstract class AbstractBase<U, V> implements AnnotationValue<U, V> {

        @Override
        public <S> S resolve(Class<? extends S> type) {
            return type.cast(resolve());
        }
    }

    /**
     * Represents a primitive value, a {@link java.lang.String} or an array of the latter types.
     *
     * @param <U> The type where primitive values are represented by their boxed type.
     */
    class ForConstant<U> extends AbstractBase<U, U> {

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
        public ForConstant(U value) {
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
            return other == this || (other instanceof AnnotationValue<?, ?> && propertyDispatcher.equals(value, ((AnnotationValue<?, ?>) other).resolve()));
        }

        @Override
        public int hashCode() {
            return propertyDispatcher.hashCode(value);
        }

        @Override
        public String toString() {
            return propertyDispatcher.toString(value);
        }

        /**
         * Represents a trivial loaded value.
         *
         * @param <V> The annotation properties type.
         */
        public static class Loaded<V> extends AnnotationValue.Loaded.AbstractBase<V> {

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
    class ForAnnotation<U extends Annotation> extends AbstractBase<AnnotationDescription, U> {

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
                                                                                          Map<String, ? extends AnnotationValue<?, ?>> annotationValues) {
            return new ForAnnotation<V>(new AnnotationDescription.Latent(annotationType, annotationValues));
        }

        @Override
        public AnnotationDescription resolve() {
            return annotationDescription;
        }

        @Override
        public AnnotationValue.Loaded<U> load(ClassLoader classLoader) throws ClassNotFoundException {
            @SuppressWarnings("unchecked")
            Class<U> annotationType = (Class<U>) Class.forName(annotationDescription.getAnnotationType().getName(), false, classLoader);
            return new Loaded<U>(annotationDescription.prepare(annotationType).load());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || (other instanceof AnnotationValue<?, ?> && annotationDescription.equals(((AnnotationValue<?, ?>) other).resolve()));
        }

        @Override
        public int hashCode() {
            return annotationDescription.hashCode();
        }

        @Override
        public String toString() {
            return annotationDescription.toString();
        }

        /**
         * A loaded version of the described annotation.
         *
         * @param <V> The annotation type.
         */
        public static class Loaded<V extends Annotation> extends AnnotationValue.Loaded.AbstractBase<V> {

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
        public static class IncompatibleRuntimeType extends AnnotationValue.Loaded.AbstractBase<Annotation> {

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
    class ForEnumeration<U extends Enum<U>> extends AbstractBase<EnumerationDescription, U> {

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
            Class<U> enumerationType = (Class<U>) Class.forName(enumerationDescription.getEnumerationType().getName(), false, classLoader);
            return new Loaded<U>(enumerationDescription.load(enumerationType));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || (other instanceof AnnotationValue<?, ?> && enumerationDescription.equals(((AnnotationValue<?, ?>) other).resolve()));
        }

        @Override
        public int hashCode() {
            return enumerationDescription.hashCode();
        }

        @Override
        public String toString() {
            return enumerationDescription.toString();
        }

        /**
         * A loaded representation of an enumeration value.
         *
         * @param <V> The type of the represented enumeration.
         */
        public static class Loaded<V extends Enum<V>> extends AnnotationValue.Loaded.AbstractBase<V> {

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
        public static class UnknownRuntimeEnumeration extends AnnotationValue.Loaded.AbstractBase<Enum<?>> {

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
        public static class IncompatibleRuntimeType extends AnnotationValue.Loaded.AbstractBase<Enum<?>> {

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
    class ForType<U extends Class<U>> extends AbstractBase<TypeDescription, U> {

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
            return this == other || (other instanceof AnnotationValue<?, ?> && typeDescription.equals(((AnnotationValue<?, ?>) other).resolve()));
        }

        @Override
        public int hashCode() {
            return typeDescription.hashCode();
        }

        @Override
        public String toString() {
            return typeDescription.toString();
        }

        /**
         * A loaded annotation value for a given type.
         *
         * @param <U> The represented type.
         */
        protected static class Loaded<U extends Class<U>> extends AnnotationValue.Loaded.AbstractBase<U> {

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
                return PropertyDispatcher.TYPE_LOADED.toString(type);
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
    class ForComplexArray<U, V> extends AbstractBase<U[], V[]> {

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
            return new Loaded<V>((Class<V>) Class.forName(componentType.getName(), false, classLoader), loadedValues);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof AnnotationValue<?, ?>)) return false;
            AnnotationValue<?, ?> loadedOther = (AnnotationValue<?, ?>) other;
            Object otherValue = loadedOther.resolve();
            if (!(otherValue instanceof Object[])) return false;
            Object[] otherArrayValue = (Object[]) otherValue;
            if (annotationValues.size() != otherArrayValue.length) return false;
            Iterator<? extends AnnotationValue<?, ?>> iterator = annotationValues.iterator();
            for (Object value : otherArrayValue) {
                AnnotationValue<?, ?> self = iterator.next();
                if (!self.resolve().equals(value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (AnnotationValue<?, ?> annotationValue : annotationValues) {
                result = 31 * result + annotationValue.hashCode();
            }
            return result;
        }

        @Override
        public String toString() {
            char open, close;
            if (componentType.represents(TypeDescription.class)) {
                open = PropertyDispatcher.TypeRenderer.CURRENT.getOpen();
                close = PropertyDispatcher.TypeRenderer.CURRENT.getClose();
            } else {
                open = '[';
                close = ']';
            }
            StringBuilder stringBuilder = new StringBuilder().append(open);
            for (AnnotationValue<?, ?> annotationValue : annotationValues) {
                stringBuilder.append(annotationValue.toString());
            }
            return stringBuilder.append(close).toString();
        }

        /**
         * Represents a loaded complex array.
         *
         * @param <W> The component type of the loaded array.
         */
        protected static class Loaded<W> extends AnnotationValue.Loaded.AbstractBase<W[]> {

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
                char open, close;
                if (componentType == Class.class) {
                    open = PropertyDispatcher.TypeRenderer.CURRENT.getOpen();
                    close = PropertyDispatcher.TypeRenderer.CURRENT.getClose();
                } else {
                    open = '[';
                    close = ']';
                }
                StringBuilder stringBuilder = new StringBuilder().append(open);
                for (AnnotationValue.Loaded<?> value : values) {
                    stringBuilder.append(value.toString());
                }
                return stringBuilder.append(close).toString();
            }
        }
    }
}
