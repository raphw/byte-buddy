/*
 * Copyright 2014 - 2020 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.description.annotation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.CachedReturnPlugin;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

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
     * Returns the state of the represented annotation value.
     *
     * @return The state represented by this instance.
     */
    State getState();

    /**
     * Filters this annotation value as a valid value of the provided property.
     *
     * @param property The property to filter against.
     * @return This annotation value or a new annotation value that describes why this value is not a valid value for the supplied property.
     */
    AnnotationValue<T, S> filter(MethodDescription.InDefinedShape property);

    /**
     * Filters this annotation value as a valid value of the provided property.
     *
     * @param property       The property to filter against.
     * @param typeDefinition The expected type.
     * @return This annotation value or a new annotation value that describes why this value is not a valid value for the supplied property.
     */
    AnnotationValue<T, S> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition);

    /**
     * Resolves the unloaded value of this annotation. The return value of this method is not defined if this annotation value is invalid.
     *
     * @return The unloaded value of this annotation.
     */
    T resolve();

    /**
     * Resolves the unloaded value of this annotation. The return value of this method is not defined if this annotation value is invalid.
     *
     * @param type The annotation value's unloaded type.
     * @param <W>  The annotation value's unloaded type.
     * @return The unloaded value of this annotation.
     */
    <W> W resolve(Class<? extends W> type);

    /**
     * Returns the loaded value of this annotation.
     *
     * @param classLoader The class loader for loading this value.
     * @return The loaded value of this annotation.
     */
    Loaded<S> load(ClassLoader classLoader);

    /**
     * A rendering dispatcher is responsible for resolving annotation values to {@link String} representations.
     */
    enum RenderingDispatcher {

        /**
         * A rendering dispatcher for any VM previous to Java 9.
         */
        LEGACY_VM('[', ']') {
            @Override
            public String toSourceString(char value) {
                return Character.toString(value);
            }

            @Override
            public String toSourceString(long value) {
                return Long.toString(value);
            }

            @Override
            public String toSourceString(float value) {
                return Float.toString(value);
            }

            @Override
            public String toSourceString(double value) {
                return Double.toString(value);
            }

            @Override
            public String toSourceString(String value) {
                return value;
            }

            @Override
            public String toSourceString(TypeDescription value) {
                return value.toString();
            }
        },

        /**
         * A rendering dispatcher for Java 9 onward.
         */
        JAVA_9_CAPABLE_VM('{', '}') {
            @Override
            public String toSourceString(char value) {
                StringBuilder stringBuilder = new StringBuilder().append('\'');
                if (value == '\'') {
                    stringBuilder.append("\\\'");
                } else {
                    stringBuilder.append(value);
                }
                return stringBuilder.append('\'').toString();
            }

            @Override
            public String toSourceString(long value) {
                return Math.abs(value) <= Integer.MAX_VALUE
                        ? String.valueOf(value)
                        : value + "L";
            }

            @Override
            public String toSourceString(float value) {
                return Math.abs(value) <= Float.MAX_VALUE // Float.isFinite(value)
                        ? value + "f"
                        : (Float.isInfinite(value) ? (value < 0.0f ? "-1.0f/0.0f" : "1.0f/0.0f") : "0.0f/0.0f");
            }

            @Override
            public String toSourceString(double value) {
                return Math.abs(value) <= Double.MAX_VALUE // Double.isFinite(value)
                        ? Double.toString(value)
                        : (Double.isInfinite(value) ? (value < 0.0d ? "-1.0/0.0" : "1.0/0.0") : "0.0/0.0");
            }

            @Override
            public String toSourceString(String value) {
                return "\"" + (value.indexOf('"') == -1
                        ? value
                        : value.replace("\"", "\\\"")) + "\"";
            }

            @Override
            public String toSourceString(TypeDescription value) {
                return value.getActualName() + ".class";
            }
        },

        /**
         * A rendering dispatcher for Java 14 onward.
         */
        JAVA_14_CAPABLE_VM('{', '}') {
            @Override
            public String toSourceString(byte value) {
                return "(byte)0x" + Integer.toHexString(value);
            }

            @Override
            public String toSourceString(char value) {
                StringBuilder stringBuilder = new StringBuilder().append('\'');
                if (value == '\'') {
                    stringBuilder.append("\\\'");
                } else {
                    stringBuilder.append(value);
                }
                return stringBuilder.append('\'').toString();
            }

            @Override
            public String toSourceString(long value) {
                return value + "L";
            }

            @Override
            public String toSourceString(float value) {
                return Math.abs(value) <= Float.MAX_VALUE // Float.isFinite(value)
                        ? value + "f"
                        : (Float.isInfinite(value) ? (value < 0.0f ? "-1.0f/0.0f" : "1.0f/0.0f") : "0.0f/0.0f");
            }

            @Override
            public String toSourceString(double value) {
                return Math.abs(value) <= Double.MAX_VALUE // Double.isFinite(value)
                        ? Double.toString(value)
                        : (Double.isInfinite(value) ? (value < 0.0d ? "-1.0/0.0" : "1.0/0.0") : "0.0/0.0");
            }

            @Override
            public String toSourceString(String value) {
                return "\"" + (value.indexOf('"') == -1
                        ? value
                        : value.replace("\"", "\\\"")) + "\"";
            }

            @Override
            public String toSourceString(TypeDescription value) {
                return value.getActualName() + ".class";
            }
        };

        /**
         * The rendering dispatcher for the current VM.
         */
        public static final RenderingDispatcher CURRENT;

        static {
            ClassFileVersion classFileVersion = ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V6);
            if (classFileVersion.isAtLeast(ClassFileVersion.JAVA_V14)) {
                CURRENT = RenderingDispatcher.JAVA_14_CAPABLE_VM;
            } else if (classFileVersion.isAtLeast(ClassFileVersion.JAVA_V9)) {
                CURRENT = RenderingDispatcher.JAVA_9_CAPABLE_VM;
            } else {
                CURRENT = RenderingDispatcher.LEGACY_VM;
            }
        }

        /**
         * The opening brace of an array {@link String} representation.
         */
        private final char openingBrace;

        /**
         * The closing brace of an array {@link String} representation.
         */
        private final char closingBrace;

        /**
         * Creates a new rendering dispatcher.
         *
         * @param openingBrace The opening brace of an array {@link String} representation.
         * @param closingBrace The closing brace of an array {@link String} representation.
         */
        RenderingDispatcher(char openingBrace, char closingBrace) {
            this.openingBrace = openingBrace;
            this.closingBrace = closingBrace;
        }

        /**
         * Represents the supplied {@code boolean} value as a {@link String}.
         *
         * @param value The {@code boolean} value to render.
         * @return An appropriate {@link String} representation.
         */
        public String toSourceString(boolean value) {
            return Boolean.toString(value);
        }

        /**
         * Represents the supplied {@code boolean} value as a {@link String}.
         *
         * @param value The {@code boolean} value to render.
         * @return An appropriate {@link String} representation.
         */
        public String toSourceString(byte value) {
            return Byte.toString(value);
        }

        /**
         * Represents the supplied {@code short} value as a {@link String}.
         *
         * @param value The {@code short} value to render.
         * @return An appropriate {@link String} representation.
         */
        public String toSourceString(short value) {
            return Short.toString(value);
        }

        /**
         * Represents the supplied {@code char} value as a {@link String}.
         *
         * @param value The {@code char} value to render.
         * @return An appropriate {@link String} representation.
         */
        public abstract String toSourceString(char value);

        /**
         * Represents the supplied {@code int} value as a {@link String}.
         *
         * @param value The {@code int} value to render.
         * @return An appropriate {@link String} representation.
         */
        public String toSourceString(int value) {
            return Integer.toString(value);
        }

        /**
         * Represents the supplied {@code long} value as a {@link String}.
         *
         * @param value The {@code long} value to render.
         * @return An appropriate {@link String} representation.
         */
        public abstract String toSourceString(long value);

        /**
         * Represents the supplied {@code float} value as a {@link String}.
         *
         * @param value The {@code float} value to render.
         * @return An appropriate {@link String} representation.
         */
        public abstract String toSourceString(float value);

        /**
         * Represents the supplied {@code double} value as a {@link String}.
         *
         * @param value The {@code double} value to render.
         * @return An appropriate {@link String} representation.
         */
        public abstract String toSourceString(double value);

        /**
         * Represents the supplied {@link String} value as a {@link String}.
         *
         * @param value The {@link String} value to render.
         * @return An appropriate {@link String} representation.
         */
        public abstract String toSourceString(String value);

        /**
         * Represents the supplied {@link TypeDescription} value as a {@link String}.
         *
         * @param value The {@link TypeDescription} value to render.
         * @return An appropriate {@link String} representation.
         */
        public abstract String toSourceString(TypeDescription value);

        /**
         * Represents the supplied list elements as a {@link String}.
         *
         * @param values The elements to render where each element is represented by its {@link Object#toString()} representation.
         * @return An appropriate {@link String} representation.
         */
        public String toSourceString(List<?> values) {
            StringBuilder stringBuilder = new StringBuilder().append(openingBrace);
            boolean first = true;
            for (Object value : values) {
                if (first) {
                    first = false;
                } else {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(value);
            }
            return stringBuilder.append(closingBrace).toString();
        }

        /**
         * Resolves the supplied type description's component tag.
         *
         * @param typeDescription The type to resolve.
         * @return The character that describes the component tag as an {@code int} to ease concatenation.
         */
        public int toComponentTag(TypeDescription typeDescription) {
            if (typeDescription.represents(boolean.class)) {
                return 'Z';
            } else if (typeDescription.represents(byte.class)) {
                return 'B';
            } else if (typeDescription.represents(short.class)) {
                return 'S';
            } else if (typeDescription.represents(char.class)) {
                return 'C';
            } else if (typeDescription.represents(int.class)) {
                return 'I';
            } else if (typeDescription.represents(long.class)) {
                return 'J';
            } else if (typeDescription.represents(float.class)) {
                return 'F';
            } else if (typeDescription.represents(double.class)) {
                return 'D';
            } else if (typeDescription.represents(String.class)) {
                return 's';
            } else if (typeDescription.represents(Class.class)) {
                return 'c';
            } else if (typeDescription.isEnum()) {
                return 'e';
            } else if (typeDescription.isAnnotation()) {
                return '@';
            } else if (typeDescription.isArray()) {
                return '[';
            } else {
                throw new IllegalArgumentException("Not an annotation component: " + typeDescription);
            }
        }
    }

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
         * Verifies if this loaded value represents the supplied loaded value.
         *
         * @param value A loaded annotation value.
         * @return {@code true} if the supplied annotation value is represented by this annotation value.
         */
        boolean represents(Object value);

        /**
         * An abstract base implementation of a loaded annotation value.
         *
         * @param <W> The represented loaded type.
         */
        abstract class AbstractBase<W> implements Loaded<W> {

            /**
             * {@inheritDoc}
             */
            public <X> X resolve(Class<? extends X> type) {
                return type.cast(resolve());
            }

            /**
             * A base implementation for an unresolved property.
             *
             * @param <Z> The represented loaded type.
             */
            public abstract static class ForUnresolvedProperty<Z> extends AbstractBase<Z> {

                /**
                 * {@inheritDoc}
                 */
                public State getState() {
                    return State.UNRESOLVED;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean represents(Object value) {
                    return false;
                }
            }
        }
    }

    /**
     * Represents the state of a {@link Loaded} annotation property.
     */
    enum State {

        /**
         * An undefined annotation value describes an annotation property which is missing such that
         * an {@link java.lang.annotation.IncompleteAnnotationException} would be thrown.
         */
        UNDEFINED,

        /**
         * An unresolved annotation value describes an annotation property which does not represent a
         * valid value but an exceptional state.
         */
        UNRESOLVED,

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
            return this != UNDEFINED;
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
    }

    /**
     * An abstract base implementation of an unloaded annotation value.
     *
     * @param <U> The represented unloaded type.
     * @param <V> The represented loaded type.
     */
    abstract class AbstractBase<U, V> implements AnnotationValue<U, V> {

        /**
         * {@inheritDoc}
         */
        public <W> W resolve(Class<? extends W> type) {
            return type.cast(resolve());
        }

        @Override
        public AnnotationValue<U, V> filter(MethodDescription.InDefinedShape property) {
            return filter(property, property.getReturnType());
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
         * The property delegate for the value's type.
         */
        private final PropertyDelegate propertyDelegate;

        /**
         * Creates a new constant annotation value.
         *
         * @param value            The represented value.
         * @param propertyDelegate The property delegate for the value's type.
         */
        protected ForConstant(U value, PropertyDelegate propertyDelegate) {
            this.value = value;
            this.propertyDelegate = propertyDelegate;
        }

        /**
         * Creates an annotation value for a {@code boolean} value.
         *
         * @param value The {@code boolean} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<Boolean, Boolean> of(boolean value) {
            return new ForConstant<Boolean>(value, PropertyDelegate.ForNonArrayType.BOOLEAN);
        }

        /**
         * Creates an annotation value for a {@code byte} value.
         *
         * @param value The {@code byte} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<Byte, Byte> of(byte value) {
            return new ForConstant<Byte>(value, PropertyDelegate.ForNonArrayType.BYTE);
        }

        /**
         * Creates an annotation value for a {@code short} value.
         *
         * @param value The {@code short} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<Short, Short> of(short value) {
            return new ForConstant<Short>(value, PropertyDelegate.ForNonArrayType.SHORT);
        }

        /**
         * Creates an annotation value for a {@code char} value.
         *
         * @param value The {@code char} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<Character, Character> of(char value) {
            return new ForConstant<Character>(value, PropertyDelegate.ForNonArrayType.CHARACTER);
        }

        /**
         * Creates an annotation value for a {@code int} value.
         *
         * @param value The {@code int} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<Integer, Integer> of(int value) {
            return new ForConstant<Integer>(value, PropertyDelegate.ForNonArrayType.INTEGER);
        }

        /**
         * Creates an annotation value for a {@code long} value.
         *
         * @param value The {@code long} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<Long, Long> of(long value) {
            return new ForConstant<Long>(value, PropertyDelegate.ForNonArrayType.LONG);
        }

        /**
         * Creates an annotation value for a {@code float} value.
         *
         * @param value The {@code float} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<Float, Float> of(float value) {
            return new ForConstant<Float>(value, PropertyDelegate.ForNonArrayType.FLOAT);
        }

        /**
         * Creates an annotation value for a {@code double} value.
         *
         * @param value The {@code double} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<Double, Double> of(double value) {
            return new ForConstant<Double>(value, PropertyDelegate.ForNonArrayType.DOUBLE);
        }

        /**
         * Creates an annotation value for a {@link String} value.
         *
         * @param value The {@link String} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<String, String> of(String value) {
            return new ForConstant<String>(value, PropertyDelegate.ForNonArrayType.STRING);
        }

        /**
         * Creates an annotation value for a {@code boolean[]} value.
         *
         * @param value The {@code boolean[]} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<boolean[], boolean[]> of(boolean... value) {
            return new ForConstant<boolean[]>(value, PropertyDelegate.ForArrayType.BOOLEAN);
        }

        /**
         * Creates an annotation value for a {@code byte[]} value.
         *
         * @param value The {@code byte[]} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<byte[], byte[]> of(byte... value) {
            return new ForConstant<byte[]>(value, PropertyDelegate.ForArrayType.BYTE);
        }

        /**
         * Creates an annotation value for a {@code short[]} value.
         *
         * @param value The {@code short[]} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<short[], short[]> of(short... value) {
            return new ForConstant<short[]>(value, PropertyDelegate.ForArrayType.SHORT);
        }

        /**
         * Creates an annotation value for a {@code char[]} value.
         *
         * @param value The {@code char[]} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<char[], char[]> of(char... value) {
            return new ForConstant<char[]>(value, PropertyDelegate.ForArrayType.CHARACTER);
        }

        /**
         * Creates an annotation value for a {@code int[]} value.
         *
         * @param value The {@code int[]} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<int[], int[]> of(int... value) {
            return new ForConstant<int[]>(value, PropertyDelegate.ForArrayType.INTEGER);
        }

        /**
         * Creates an annotation value for a {@code long[]} value.
         *
         * @param value The {@code long[]} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<long[], long[]> of(long... value) {
            return new ForConstant<long[]>(value, PropertyDelegate.ForArrayType.LONG);
        }

        /**
         * Creates an annotation value for a {@code float[]} value.
         *
         * @param value The {@code float[]} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<float[], float[]> of(float... value) {
            return new ForConstant<float[]>(value, PropertyDelegate.ForArrayType.FLOAT);
        }

        /**
         * Creates an annotation value for a {@code double[]} value.
         *
         * @param value The {@code double[]} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<double[], double[]> of(double... value) {
            return new ForConstant<double[]>(value, PropertyDelegate.ForArrayType.DOUBLE);
        }

        /**
         * Creates an annotation value for a {@code String[]} value.
         *
         * @param value The {@code String[]} value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<String[], String[]> of(String... value) {
            return new ForConstant<String[]>(value, PropertyDelegate.ForArrayType.STRING);
        }

        /**
         * Creates an annotation value for any constant value, i.e any primitive (wrapper) type,
         * any primitive array type or any {@link String} value or array. If no constant annotation
         * type is provided, a runtime exception is thrown.
         *
         * @param value The value to represent.
         * @return An appropriate annotation value.
         */
        public static AnnotationValue<?, ?> of(Object value) {
            if (value instanceof Boolean) {
                return of(((Boolean) value).booleanValue());
            } else if (value instanceof Byte) {
                return of(((Byte) value).byteValue());
            } else if (value instanceof Short) {
                return of(((Short) value).shortValue());
            } else if (value instanceof Character) {
                return of(((Character) value).charValue());
            } else if (value instanceof Integer) {
                return of(((Integer) value).intValue());
            } else if (value instanceof Long) {
                return of(((Long) value).longValue());
            } else if (value instanceof Float) {
                return of(((Float) value).floatValue());
            } else if (value instanceof Double) {
                return of(((Double) value).doubleValue());
            } else if (value instanceof String) {
                return of((String) value);
            } else if (value instanceof boolean[]) {
                return of((boolean[]) value);
            } else if (value instanceof byte[]) {
                return of((byte[]) value);
            } else if (value instanceof short[]) {
                return of((short[]) value);
            } else if (value instanceof char[]) {
                return of((char[]) value);
            } else if (value instanceof int[]) {
                return of((int[]) value);
            } else if (value instanceof long[]) {
                return of((long[]) value);
            } else if (value instanceof float[]) {
                return of((float[]) value);
            } else if (value instanceof double[]) {
                return of((double[]) value);
            } else if (value instanceof String[]) {
                return of((String[]) value);
            } else {
                throw new IllegalArgumentException("Not a constant annotation value: " + value);
            }
        }

        /**
         * {@inheritDoc}
         */
        public State getState() {
            return State.RESOLVED;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue<U, U> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition) {
            return typeDefinition.asErasure().asBoxed().represents(value.getClass()) ? this : new ForMismatchedType<U, U>(property, value.getClass().isArray()
                    ? "Array with component tag: " + RenderingDispatcher.CURRENT.toComponentTag(TypeDescription.ForLoadedType.of(value.getClass().getComponentType()))
                    : value.getClass().toString() + '[' + value + ']');
        }

        /**
         * {@inheritDoc}
         */
        public U resolve() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue.Loaded<U> load(ClassLoader classLoader) {
            return new Loaded<U>(value, propertyDelegate);
        }

        @Override
        @CachedReturnPlugin.Enhance("hashCode")
        public int hashCode() {
            return propertyDelegate.hashCode(value);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof AnnotationValue<?, ?> && propertyDelegate.equals(value, ((AnnotationValue<?, ?>) other).resolve());
        }

        @Override
        public String toString() {
            return propertyDelegate.toString(value);
        }

        /**
         * A property delegate for a constant annotation value.
         */
        protected interface PropertyDelegate {

            /**
             * Copies the provided value, if it is not immutable.
             *
             * @param value The value to copy.
             * @param <S>   The value's type.
             * @return A copy of the provided instance or the provided value, if it is immutable.
             */
            <S> S copy(S value);

            /**
             * Computes the value's hash code.
             *
             * @param value The value for which to compute the hash code.
             * @return The hash code of the provided value.
             */
            int hashCode(Object value);

            /**
             * Determines if another value is equal to a constant annotation value.
             *
             * @param self  The value that is represented as a constant annotation value.
             * @param other Any other value for which to determine equality.
             * @return {@code true} if the provided value is equal to the represented value.
             */
            boolean equals(Object self, Object other);

            /**
             * Renders the supplied value as a {@link String}.
             *
             * @param value The value to render.
             * @return An appropriate {@link String} representation of the provided value.
             */
            String toString(Object value);

            /**
             * A property delegate for a non-array type.
             */
            enum ForNonArrayType implements PropertyDelegate {

                /**
                 * A property delegate for a {@code boolean} value.
                 */
                BOOLEAN {
                    /** {@inheritDoc} */
                    public String toString(Object value) {
                        return RenderingDispatcher.CURRENT.toSourceString((Boolean) value);
                    }
                },

                /**
                 * A property delegate for a {@code byte} value.
                 */
                BYTE {
                    /** {@inheritDoc} */
                    public String toString(Object value) {
                        return RenderingDispatcher.CURRENT.toSourceString((Byte) value);
                    }
                },

                /**
                 * A property delegate for a {@code short} value.
                 */
                SHORT {
                    /** {@inheritDoc} */
                    public String toString(Object value) {
                        return RenderingDispatcher.CURRENT.toSourceString((Short) value);
                    }
                },

                /**
                 * A property delegate for a {@code char} value.
                 */
                CHARACTER {
                    /** {@inheritDoc} */
                    public String toString(Object value) {
                        return RenderingDispatcher.CURRENT.toSourceString((Character) value);
                    }
                },

                /**
                 * A property delegate for a {@code int} value.
                 */
                INTEGER {
                    /** {@inheritDoc} */
                    public String toString(Object value) {
                        return RenderingDispatcher.CURRENT.toSourceString((Integer) value);
                    }
                },

                /**
                 * A property delegate for a {@code long} value.
                 */
                LONG {
                    /** {@inheritDoc} */
                    public String toString(Object value) {
                        return RenderingDispatcher.CURRENT.toSourceString((Long) value);
                    }
                },

                /**
                 * A property delegate for a {@code float} value.
                 */
                FLOAT {
                    /** {@inheritDoc} */
                    public String toString(Object value) {
                        return RenderingDispatcher.CURRENT.toSourceString((Float) value);
                    }
                },

                /**
                 * A property delegate for a {@code double} value.
                 */
                DOUBLE {
                    /** {@inheritDoc} */
                    public String toString(Object value) {
                        return RenderingDispatcher.CURRENT.toSourceString((Double) value);
                    }
                },

                /**
                 * A property delegate for a {@link String} value.
                 */
                STRING {
                    /** {@inheritDoc} */
                    public String toString(Object value) {
                        return RenderingDispatcher.CURRENT.toSourceString((String) value);
                    }
                };

                /**
                 * {@inheritDoc}
                 */
                public <S> S copy(S value) {
                    return value;
                }

                /**
                 * {@inheritDoc}
                 */
                public int hashCode(Object value) {
                    return value.hashCode();
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean equals(Object self, Object other) {
                    return self.equals(other);
                }
            }

            /**
             * A property delegate for an array type of a constant value.
             */
            enum ForArrayType implements PropertyDelegate {

                /**
                 * A property delegate for a {@code boolean[]} value.
                 */
                BOOLEAN {
                    @Override
                    protected Object doCopy(Object value) {
                        return ((boolean[]) value).clone();
                    }

                    /** {@inheritDoc} */
                    public int hashCode(Object value) {
                        return Arrays.hashCode((boolean[]) value);
                    }

                    /** {@inheritDoc} */
                    public boolean equals(Object self, Object other) {
                        return other instanceof boolean[] && Arrays.equals((boolean[]) self, (boolean[]) other);
                    }

                    @Override
                    protected String toString(Object array, int index) {
                        return ForNonArrayType.BOOLEAN.toString(Array.getBoolean(array, index));
                    }
                },

                /**
                 * A property delegate for a {@code byte[]} value.
                 */
                BYTE {
                    @Override
                    protected Object doCopy(Object value) {
                        return ((byte[]) value).clone();
                    }

                    /** {@inheritDoc} */
                    public int hashCode(Object value) {
                        return Arrays.hashCode((byte[]) value);
                    }

                    /** {@inheritDoc} */
                    public boolean equals(Object self, Object other) {
                        return other instanceof byte[] && Arrays.equals((byte[]) self, (byte[]) other);
                    }

                    @Override
                    protected String toString(Object array, int index) {
                        return ForNonArrayType.BYTE.toString(Array.getByte(array, index));
                    }
                },

                /**
                 * A property delegate for a {@code short[]} value.
                 */
                SHORT {
                    @Override
                    protected Object doCopy(Object value) {
                        return ((short[]) value).clone();
                    }

                    /** {@inheritDoc} */
                    public int hashCode(Object value) {
                        return Arrays.hashCode((short[]) value);
                    }

                    /** {@inheritDoc} */
                    public boolean equals(Object self, Object other) {
                        return other instanceof short[] && Arrays.equals((short[]) self, (short[]) other);
                    }

                    @Override
                    protected String toString(Object array, int index) {
                        return ForNonArrayType.SHORT.toString(Array.getShort(array, index));
                    }
                },

                /**
                 * A property delegate for a {@code char[]} value.
                 */
                CHARACTER {
                    @Override
                    protected Object doCopy(Object value) {
                        return ((char[]) value).clone();
                    }

                    /** {@inheritDoc} */
                    public int hashCode(Object value) {
                        return Arrays.hashCode((char[]) value);
                    }

                    /** {@inheritDoc} */
                    public boolean equals(Object self, Object other) {
                        return other instanceof char[] && Arrays.equals((char[]) self, (char[]) other);
                    }

                    @Override
                    protected String toString(Object array, int index) {
                        return ForNonArrayType.CHARACTER.toString(Array.getChar(array, index));
                    }
                },

                /**
                 * A property delegate for a {@code int[]} value.
                 */
                INTEGER {
                    @Override
                    protected Object doCopy(Object value) {
                        return ((int[]) value).clone();
                    }

                    /** {@inheritDoc} */
                    public int hashCode(Object value) {
                        return Arrays.hashCode((int[]) value);
                    }

                    /** {@inheritDoc} */
                    public boolean equals(Object self, Object other) {
                        return other instanceof int[] && Arrays.equals((int[]) self, (int[]) other);
                    }

                    @Override
                    protected String toString(Object array, int index) {
                        return ForNonArrayType.INTEGER.toString(Array.getInt(array, index));
                    }
                },

                /**
                 * A property delegate for a {@code long[]} value.
                 */
                LONG {
                    @Override
                    protected Object doCopy(Object value) {
                        return ((long[]) value).clone();
                    }

                    /** {@inheritDoc} */
                    public int hashCode(Object value) {
                        return Arrays.hashCode((long[]) value);
                    }

                    /** {@inheritDoc} */
                    public boolean equals(Object self, Object other) {
                        return other instanceof long[] && Arrays.equals((long[]) self, (long[]) other);
                    }

                    @Override
                    protected String toString(Object array, int index) {
                        return ForNonArrayType.LONG.toString(Array.getLong(array, index));
                    }
                },

                /**
                 * A property delegate for a {@code float[]} value.
                 */
                FLOAT {
                    @Override
                    protected Object doCopy(Object value) {
                        return ((float[]) value).clone();
                    }

                    /** {@inheritDoc} */
                    public int hashCode(Object value) {
                        return Arrays.hashCode((float[]) value);
                    }

                    /** {@inheritDoc} */
                    public boolean equals(Object self, Object other) {
                        return other instanceof float[] && Arrays.equals((float[]) self, (float[]) other);
                    }

                    @Override
                    protected String toString(Object array, int index) {
                        return ForNonArrayType.FLOAT.toString(Array.getFloat(array, index));
                    }
                },

                /**
                 * A property delegate for a {@code double[]} value.
                 */
                DOUBLE {
                    @Override
                    protected Object doCopy(Object value) {
                        return ((double[]) value).clone();
                    }

                    /** {@inheritDoc} */
                    public int hashCode(Object value) {
                        return Arrays.hashCode((double[]) value);
                    }

                    /** {@inheritDoc} */
                    public boolean equals(Object self, Object other) {
                        return other instanceof double[] && Arrays.equals((double[]) self, (double[]) other);
                    }

                    @Override
                    protected String toString(Object array, int index) {
                        return ForNonArrayType.DOUBLE.toString(Array.getDouble(array, index));
                    }
                },

                /**
                 * A property delegate for a {@code String[]} value.
                 */
                STRING {
                    @Override
                    protected Object doCopy(Object value) {
                        return ((String[]) value).clone();
                    }

                    /** {@inheritDoc} */
                    public int hashCode(Object value) {
                        return Arrays.hashCode((String[]) value);
                    }

                    /** {@inheritDoc} */
                    public boolean equals(Object self, Object other) {
                        return other instanceof String[] && Arrays.equals((String[]) self, (String[]) other);
                    }

                    @Override
                    protected String toString(Object array, int index) {
                        return ForNonArrayType.STRING.toString(Array.get(array, index));
                    }
                };

                /**
                 * {@inheritDoc}
                 */
                @SuppressWarnings("unchecked")
                public <S> S copy(S value) {
                    return (S) doCopy(value);
                }

                /**
                 * Creates a copy of the provided array.
                 *
                 * @param value The array to copy.
                 * @return A shallow copy of the provided array.
                 */
                protected abstract Object doCopy(Object value);

                /**
                 * {@inheritDoc}
                 */
                public String toString(Object value) {
                    List<String> elements = new ArrayList<String>(Array.getLength(value));
                    for (int index = 0; index < Array.getLength(value); index++) {
                        elements.add(toString(value, index));
                    }
                    return RenderingDispatcher.CURRENT.toSourceString(elements);
                }

                /**
                 * Renders the array element at the specified index.
                 *
                 * @param array The array for which an element should be rendered.
                 * @param index The index of the array element to render.
                 * @return A {@link String} representation of the array element at the supplied index.
                 */
                protected abstract String toString(Object array, int index);
            }
        }

        /**
         * Represents a trivial loaded value.
         *
         * @param <V> The annotation properties type.
         */
        protected static class Loaded<V> extends AnnotationValue.Loaded.AbstractBase<V> {

            /**
             * The represented value.
             */
            private final V value;

            /**
             * The property delegate for the value's type.
             */
            private final PropertyDelegate propertyDelegate;

            /**
             * Creates a new loaded representation of a constant value.
             *
             * @param value            The represented value.
             * @param propertyDelegate The property delegate for the value's type.
             */
            protected Loaded(V value, PropertyDelegate propertyDelegate) {
                this.value = value;
                this.propertyDelegate = propertyDelegate;
            }

            /**
             * {@inheritDoc}
             */
            public State getState() {
                return State.RESOLVED;
            }

            /**
             * {@inheritDoc}
             */
            public V resolve() {
                return propertyDelegate.copy(value);
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(Object value) {
                return propertyDelegate.equals(this.value, value);
            }

            @Override
            @CachedReturnPlugin.Enhance("hashCode")
            public int hashCode() {
                return propertyDelegate.hashCode(value);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (!(other instanceof AnnotationValue.Loaded<?>)) {
                    return false;
                }
                AnnotationValue.Loaded<?> annotationValue = (AnnotationValue.Loaded<?>) other;
                return annotationValue.getState().isResolved() && propertyDelegate.equals(value, annotationValue.resolve());
            }

            @Override
            public String toString() {
                return propertyDelegate.toString(value);
            }
        }
    }

    /**
     * A description of an {@link java.lang.annotation.Annotation} as a value of another annotation.
     *
     * @param <U> The type of the annotation.
     */
    class ForAnnotationDescription<U extends Annotation> extends AbstractBase<AnnotationDescription, U> {

        /**
         * The annotation description that this value represents.
         */
        private final AnnotationDescription annotationDescription;

        /**
         * Creates a new annotation value for a given annotation description.
         *
         * @param annotationDescription The annotation description that this value represents.
         */
        public ForAnnotationDescription(AnnotationDescription annotationDescription) {
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
            return new ForAnnotationDescription<V>(new AnnotationDescription.Latent(annotationType, annotationValues));
        }

        /**
         * {@inheritDoc}
         */
        public State getState() {
            return State.RESOLVED;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue<AnnotationDescription, U> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition) {
            return typeDefinition.asErasure().equals(annotationDescription.getAnnotationType())
                    ? this
                    : new ForMismatchedType<AnnotationDescription, U>(property, annotationDescription.getAnnotationType().toString() + '[' + annotationDescription + ']');
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationDescription resolve() {
            return annotationDescription;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public AnnotationValue.Loaded<U> load(ClassLoader classLoader) {
            try {
                return new Loaded<U>(annotationDescription
                        .prepare((Class<U>) Class.forName(annotationDescription.getAnnotationType().getName(), false, classLoader))
                        .load());
            } catch (ClassNotFoundException exception) {
                return new ForMissingType.Loaded<U>(annotationDescription.getAnnotationType().getName(), exception);
            }
        }

        @Override
        public int hashCode() {
            return annotationDescription.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof AnnotationValue<?, ?> && annotationDescription.equals(((AnnotationValue<?, ?>) other).resolve());
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

            /**
             * {@inheritDoc}
             */
            public State getState() {
                return State.RESOLVED;
            }

            /**
             * {@inheritDoc}
             */
            public V resolve() {
                return annotation;
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(Object value) {
                return annotation.equals(value);
            }

            @Override
            public int hashCode() {
                return annotation.hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (!(other instanceof AnnotationValue.Loaded<?>)) {
                    return false;
                }
                AnnotationValue.Loaded<?> annotationValue = (AnnotationValue.Loaded<?>) other;
                return annotationValue.getState().isResolved() && annotation.equals(annotationValue.resolve());
            }

            @Override
            public String toString() {
                return annotation.toString();
            }
        }
    }

    /**
     * A description of an {@link java.lang.Enum} as a value of an annotation.
     *
     * @param <U> The type of the enumeration.
     */
    class ForEnumerationDescription<U extends Enum<U>> extends AbstractBase<EnumerationDescription, U> {

        /**
         * The enumeration that is represented.
         */
        private final EnumerationDescription enumerationDescription;

        /**
         * Creates a new description of an annotation value for a given enumeration.
         *
         * @param enumerationDescription The enumeration that is to be represented.
         */
        public ForEnumerationDescription(EnumerationDescription enumerationDescription) {
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
            return new ForEnumerationDescription<V>(value);
        }

        /**
         * {@inheritDoc}
         */
        public EnumerationDescription resolve() {
            return enumerationDescription;
        }

        /**
         * {@inheritDoc}
         */
        public State getState() {
            return State.RESOLVED;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue<EnumerationDescription, U> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition) {
            return typeDefinition.asErasure().equals(enumerationDescription.getEnumerationType())
                    ? this
                    : new ForMismatchedType<EnumerationDescription, U>(property, enumerationDescription.getEnumerationType().toString() + '[' + enumerationDescription.getValue() + ']');
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public AnnotationValue.Loaded<U> load(ClassLoader classLoader) {
            try {
                return new Loaded<U>(enumerationDescription.load((Class<U>) Class.forName(enumerationDescription.getEnumerationType().getName(), false, classLoader)));
            } catch (ClassNotFoundException exception) {
                return new ForMissingType.Loaded<U>(enumerationDescription.getEnumerationType().getName(), exception);
            }
        }

        @Override
        public int hashCode() {
            return enumerationDescription.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof AnnotationValue<?, ?> && enumerationDescription.equals(((AnnotationValue<?, ?>) other).resolve());
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

            /**
             * {@inheritDoc}
             */
            public State getState() {
                return State.RESOLVED;
            }

            /**
             * {@inheritDoc}
             */
            public V resolve() {
                return enumeration;
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(Object value) {
                return enumeration.equals(value);
            }

            @Override
            public int hashCode() {
                return enumeration.hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (!(other instanceof AnnotationValue.Loaded<?>)) {
                    return false;
                }
                AnnotationValue.Loaded<?> annotationValue = (AnnotationValue.Loaded<?>) other;
                return annotationValue.getState().isResolved() && enumeration.equals(annotationValue.resolve());
            }

            @Override
            public String toString() {
                return enumeration.toString();
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
            public static class WithIncompatibleRuntimeType extends AnnotationValue.Loaded.AbstractBase<Enum<?>> {

                /**
                 * The runtime type which is not an enumeration type.
                 */
                private final Class<?> type;

                /**
                 * Creates a new representation for an incompatible runtime type.
                 *
                 * @param type The runtime type which is not an enumeration type.
                 */
                public WithIncompatibleRuntimeType(Class<?> type) {
                    this.type = type;
                }

                /**
                 * {@inheritDoc}
                 */
                public State getState() {
                    return State.UNRESOLVED;
                }

                /**
                 * {@inheritDoc}
                 */
                public Enum<?> resolve() {
                    throw new IncompatibleClassChangeError("Not an enumeration type: " + type.toString());
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean represents(Object value) {
                    return false;
                }

                /* hashCode, equals and toString are intentionally not implemented */
            }
        }

        /**
         * Represents a property with an enumeration constant that is not defined by an enumeration type.
         *
         * @param <U> The enumerationl type.
         */
        public static class WithUnknownConstant<U extends Enum<U>> extends AbstractBase<EnumerationDescription, U> {

            /**
             * A description of the enumeration type.
             */
            private final TypeDescription typeDescription;

            /**
             * The enumeration constant value.
             */
            private final String value;

            /**
             * Creates a property description for an enumeration value that does not exist for the enumeration type.
             *
             * @param typeDescription A description of the enumeration type.
             * @param value           The enumeration constant value.
             */
            public WithUnknownConstant(TypeDescription typeDescription, String value) {
                this.typeDescription = typeDescription;
                this.value = value;
            }

            /**
             * {@inheritDoc}
             */
            public State getState() {
                return State.UNRESOLVED;
            }

            /**
             * {@inheritDoc}
             */
            public AnnotationValue<EnumerationDescription, U> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition) {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public EnumerationDescription resolve() {
                throw new IllegalStateException(typeDescription + " does not declare enumeration constant " + value);
            }

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked")
            public AnnotationValue.Loaded<U> load(ClassLoader classLoader) {
                try {
                    // Type casting to Object is required for Java 6 compilability.
                    return (AnnotationValue.Loaded<U>) (Object) new Loaded((Class<Enum<?>>) Class.forName(typeDescription.getName(), false, classLoader), value);
                } catch (ClassNotFoundException exception) {
                    return new ForMissingType.Loaded<U>(typeDescription.getName(), exception);
                }
            }

            /* does not implement hashCode and equals method to mimic OpenJDK behavior. */

            @Override
            public String toString() {
                return value + " /* Warning: constant not present! */";
            }

            /**
             * Represents a property with an enumeration constant that is not defined by an enumeration type.
             */
            public static class Loaded extends AnnotationValue.Loaded.AbstractBase.ForUnresolvedProperty<Enum<?>> {

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
                public Loaded(Class<? extends Enum<?>> enumType, String value) {
                    this.enumType = enumType;
                    this.value = value;
                }

                /**
                 * {@inheritDoc}
                 */
                public Enum<?> resolve() {
                    throw new EnumConstantNotPresentException(enumType, value);
                }

                /* does not implement hashCode and equals method to mimic OpenJDK behavior. */

                @Override
                public String toString() {
                    return value + " /* Warning: constant not present! */";
                }
            }
        }
    }

    /**
     * A description of a {@link java.lang.Class} as a value of an annotation.
     *
     * @param <U> The type of the {@link java.lang.Class} that is described.
     */
    class ForTypeDescription<U extends Class<U>> extends AbstractBase<TypeDescription, U> {

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
        public ForTypeDescription(TypeDescription typeDescription) {
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
            return new ForTypeDescription<V>(typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public State getState() {
            return State.RESOLVED;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue<TypeDescription, U> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition) {
            return typeDefinition.asErasure().represents(Class.class)
                    ? this
                    : new ForMismatchedType<TypeDescription, U>(property, Class.class.getName() + '[' + typeDescription.getName() + ']');
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription resolve() {
            return typeDescription;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public AnnotationValue.Loaded<U> load(ClassLoader classLoader) {
            try {
                return new Loaded<U>((U) Class.forName(typeDescription.getName(), NO_INITIALIZATION, classLoader));
            } catch (ClassNotFoundException exception) {
                return new ForMissingType.Loaded<U>(typeDescription.getName(), exception);
            }
        }

        @Override
        public int hashCode() {
            return typeDescription.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof AnnotationValue<?, ?> && typeDescription.equals(((AnnotationValue<?, ?>) other).resolve());
        }

        @Override
        public String toString() {
            return RenderingDispatcher.CURRENT.toSourceString(typeDescription);
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

            /**
             * {@inheritDoc}
             */
            public State getState() {
                return State.RESOLVED;
            }

            /**
             * {@inheritDoc}
             */
            public U resolve() {
                return type;
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(Object value) {
                return type.equals(value);
            }

            @Override
            public int hashCode() {
                return type.hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (!(other instanceof AnnotationValue.Loaded<?>)) {
                    return false;
                }
                AnnotationValue.Loaded<?> annotationValue = (AnnotationValue.Loaded<?>) other;
                return annotationValue.getState().isResolved() && type.equals(annotationValue.resolve());
            }

            @Override
            public String toString() {
                return RenderingDispatcher.CURRENT.toSourceString(TypeDescription.ForLoadedType.of(type));
            }
        }
    }

    /**
     * Describes a complex array that is the value of an annotation. Complex arrays are arrays that might trigger the loading
     * of user-defined types, i.e. {@link java.lang.Class}, {@link java.lang.annotation.Annotation} and {@link java.lang.Enum}
     * instances.
     *
     * @param <U> The array type of the annotation's value when it is not loaded.
     * @param <V> The array type of the annotation's value when it is loaded.
     */
    class ForDescriptionArray<U, V> extends AbstractBase<U, V> {

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
        private final List<? extends AnnotationValue<?, ?>> values;

        /**
         * Creates a new complex array.
         *
         * @param unloadedComponentType The component type for arrays containing unloaded versions of the annotation array's values.
         * @param componentType         A description of the component type when it is loaded.
         * @param values                A list of values of the array elements.
         */
        public ForDescriptionArray(Class<?> unloadedComponentType,
                                   TypeDescription componentType,
                                   List<? extends AnnotationValue<?, ?>> values) {
            this.unloadedComponentType = unloadedComponentType;
            this.componentType = componentType;
            this.values = values;
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
                values.add(ForEnumerationDescription.<W>of(value));
            }
            return new ForDescriptionArray<EnumerationDescription[], W[]>(EnumerationDescription.class, enumerationType, values);
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
                values.add(new ForAnnotationDescription<W>(value));
            }
            return new ForDescriptionArray<AnnotationDescription[], W[]>(AnnotationDescription.class, annotationType, values);
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
                values.add((AnnotationValue) ForTypeDescription.<Class>of(value));
            }
            return new ForDescriptionArray<TypeDescription[], Class<?>[]>(TypeDescription.class, TypeDescription.CLASS, values);
        }

        /**
         * {@inheritDoc}
         */
        public State getState() {
            return State.RESOLVED;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public AnnotationValue<U, V> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition) {
            if (typeDefinition.isArray() && typeDefinition.getComponentType().asErasure().equals(componentType)) {
                for (AnnotationValue<?, ?> value : values) {
                    value = value.filter(property, typeDefinition.getComponentType());
                    if (value.getState() != State.RESOLVED) {
                        return (AnnotationValue<U, V>) value;
                    }
                }
                return this;
            } else {
                return new ForMismatchedType<U, V>(property, "Array with component tag: " + RenderingDispatcher.CURRENT.toComponentTag(componentType));
            }
        }

        /**
         * {@inheritDoc}
         */
        public U resolve() {
            @SuppressWarnings("unchecked")
            U resolved = (U) Array.newInstance(unloadedComponentType, values.size());
            int index = 0;
            for (AnnotationValue<?, ?> value : values) {
                Array.set(resolved, index++, value.resolve());
            }
            return resolved;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public AnnotationValue.Loaded<V> load(ClassLoader classLoader) {
            List<AnnotationValue.Loaded<?>> values = new ArrayList<AnnotationValue.Loaded<?>>(this.values.size());
            for (AnnotationValue<?, ?> value : this.values) {
                values.add(value.load(classLoader));
            }
            try {
                return new Loaded<V>((Class<V>) Class.forName(componentType.getName(), false, classLoader), values);
            } catch (ClassNotFoundException exception) {
                return new ForMissingType.Loaded<V>(componentType.getName(), exception);
            }
        }

        @Override
        @CachedReturnPlugin.Enhance("hashCode")
        public int hashCode() {
            int result = 1;
            for (AnnotationValue<?, ?> value : values) {
                result = 31 * result + value.hashCode();
            }
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof AnnotationValue<?, ?>)) {
                return false;
            }
            AnnotationValue<?, ?> annotationValue = (AnnotationValue<?, ?>) other;
            Object value = annotationValue.resolve();
            if (value == null || !value.getClass().isArray()) {
                return false;
            }
            if (values.size() != Array.getLength(value)) {
                return false;
            }
            Iterator<? extends AnnotationValue<?, ?>> iterator = values.iterator();
            for (int index = 0; index < values.size(); index++) {
                AnnotationValue<?, ?> self = iterator.next();
                if (!self.resolve().equals(Array.get(value, index))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return RenderingDispatcher.CURRENT.toSourceString(values);
        }

        /**
         * Represents a loaded complex array.
         *
         * @param <W> The type of the loaded array.
         */
        protected static class Loaded<W> extends AnnotationValue.Loaded.AbstractBase<W> {

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

            /**
             * {@inheritDoc}
             */
            public State getState() {
                for (AnnotationValue.Loaded<?> value : values) {
                    if (!value.getState().isResolved()) {
                        return State.UNRESOLVED;
                    }
                }
                return State.RESOLVED;
            }

            /**
             * {@inheritDoc}
             */
            public W resolve() {
                @SuppressWarnings("unchecked")
                W array = (W) Array.newInstance(componentType, values.size());
                int index = 0;
                for (AnnotationValue.Loaded<?> annotationValue : values) {
                    Array.set(array, index++, annotationValue.resolve());
                }
                return array;
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(Object value) {
                if (!(value instanceof Object[])) return false;
                if (value.getClass().getComponentType() != componentType) return false;
                Object[] array = (Object[]) value;
                if (values.size() != array.length) return false;
                Iterator<AnnotationValue.Loaded<?>> iterator = values.iterator();
                for (Object aValue : array) {
                    AnnotationValue.Loaded<?> self = iterator.next();
                    if (!self.represents(aValue)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            @CachedReturnPlugin.Enhance("hashCode")
            public int hashCode() {
                int result = 1;
                for (AnnotationValue.Loaded<?> value : values) {
                    result = 31 * result + value.hashCode();
                }
                return result;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (!(other instanceof AnnotationValue.Loaded<?>)) {
                    return false;
                }
                AnnotationValue.Loaded<?> annotationValue = (AnnotationValue.Loaded<?>) other;
                if (!annotationValue.getState().isResolved()) {
                    return false;
                }
                Object value = annotationValue.resolve();
                if (!(value instanceof Object[])) {
                    return false;
                }
                Object[] arrayValue = (Object[]) value;
                if (values.size() != arrayValue.length) {
                    return false;
                }
                Iterator<AnnotationValue.Loaded<?>> iterator = values.iterator();
                for (Object aValue : arrayValue) {
                    AnnotationValue.Loaded<?> self = iterator.next();
                    if (!self.getState().isResolved() || !self.resolve().equals(aValue)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String toString() {
                return RenderingDispatcher.CURRENT.toSourceString(values);
            }
        }
    }

    /**
     * An annotation value for a type that could not be loaded.
     *
     * @param <U> The type of the annotation's value when it is not loaded.
     * @param <V> The type of the annotation's value when it is loaded.
     */
    class ForMissingType<U, V> extends AbstractBase<U, V> {

        /**
         * The type's binary name.
         */
        private final String typeName;

        /**
         * Creates a new annotation value for a type that cannot be loaded.
         *
         * @param typeName The type's binary name.
         */
        public ForMissingType(String typeName) {
            this.typeName = typeName;
        }

        /**
         * {@inheritDoc}
         */
        public State getState() {
            return State.UNRESOLVED;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue<U, V> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public U resolve() {
            throw new IllegalStateException("Type not found: " + typeName);
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue.Loaded<V> load(ClassLoader classLoader) {
            return new Loaded<V>(typeName, new ClassNotFoundException(typeName));
        }

        /* does not implement hashCode and equals method to mimic OpenJDK behavior. */

        @Override
        public String toString() {
            return typeName + ".class /* Warning: type not present! */";
        }

        /**
         * Represents a missing type during an annotation's resolution.
         *
         * @param <U> The represented type.
         */
        public static class Loaded<U> extends AnnotationValue.Loaded.AbstractBase.ForUnresolvedProperty<U> {

            /**
             * The type's binary name.
             */
            private final String typeName;

            /**
             * The exception describing the missing type.
             */
            private final ClassNotFoundException exception;

            /**
             * The type's binary name.
             *
             * @param typeName  The type's binary name.
             * @param exception The exception describing the missing type.
             */
            public Loaded(String typeName, ClassNotFoundException exception) {
                this.typeName = typeName;
                this.exception = exception;
            }

            /**
             * {@inheritDoc}
             */
            public U resolve() {
                throw new TypeNotPresentException(typeName, exception);
            }

            @Override
            public String toString() {
                return typeName + ".class /* Warning: type not present! */";
            }
        }
    }

    /**
     * Describes an annotation value that does not match the annotation' type for a property.
     *
     * @param <U> The type of the annotation's value when it is not loaded.
     * @param <V> The type of the annotation's value when it is loaded.
     */
    class ForMismatchedType<U, V> extends AbstractBase<U, V> {

        /**
         * The property that does not defines a non-matching value.
         */
        private final MethodDescription.InDefinedShape property;

        /**
         * A value description of the property.
         */
        private final String value;

        /**
         * Creates an annotation description for a mismatched typeName.
         *
         * @param property The property that does not defines a non-matching value.
         * @param value    A value description of the property.
         */
        public ForMismatchedType(MethodDescription.InDefinedShape property, String value) {
            this.property = property;
            this.value = value;
        }

        /**
         * {@inheritDoc}
         */
        public State getState() {
            return State.UNRESOLVED;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue<U, V> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public U resolve() {
            throw new IllegalStateException(property + " cannot define " + value);
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue.Loaded<V> load(ClassLoader classLoader) {
            try {
                Class<?> type = Class.forName(property.getDeclaringType().getName(), false, classLoader);
                try {
                    return new Loaded<V>(type.getMethod(property.getName()), value);
                } catch (NoSuchMethodException exception) {
                    return new ForIncompatibleType.Loaded<V>(type);
                }
            } catch (ClassNotFoundException exception) {
                return new ForMissingType.Loaded<V>(property.getDeclaringType().getName(), exception);
            }
        }

        /* does not implement hashCode and equals method to mimic OpenJDK behavior. */

        @Override
        public String toString() {
            return "/* Warning type mismatch! \"" + value + "\" */";
        }

        /**
         * Describes an annotation value for a property that is not assignable to it.
         *
         * @param <W> The type of the annotation's expected value.
         */
        public static class Loaded<W> extends AnnotationValue.Loaded.AbstractBase.ForUnresolvedProperty<W> {

            /**
             * The annotation property that is not well-defined.
             */
            private final Method property;

            /**
             * A value description of the incompatible property or {@code null}.
             */
            private final String value;

            /**
             * Creates a new loaded version of a property with an incompatible type.
             *
             * @param property The annotation property that is not well-defined.
             * @param value    A value description of the incompatible property or {@code null}.
             */
            public Loaded(Method property, String value) {
                this.property = property;
                this.value = value;
            }

            /**
             * {@inheritDoc}
             */
            public W resolve() {
                throw new AnnotationTypeMismatchException(property, value);
            }
        }
    }

    /**
     * Represents a missing annotation property which is not represented by a default value.
     *
     * @param <U> The type of the annotation's value when it is not loaded.
     * @param <V> The type of the annotation's value when it is loaded.
     */
    class ForMissingValue<U, V> extends AnnotationValue.AbstractBase<U, V> {

        /**
         * The annotation type for which a property is not defined.
         */
        private final TypeDescription typeDescription;

        /**
         * The name of the property without an annotation value.
         */
        private final String property;

        /**
         * Creates a new missing annotation value.
         *
         * @param typeDescription The annotation type for which a property is not defined.
         * @param property        The name of the property without an annotation value.
         */
        public ForMissingValue(TypeDescription typeDescription, String property) {
            this.typeDescription = typeDescription;
            this.property = property;
        }

        /**
         * {@inheritDoc}
         */
        public State getState() {
            return State.UNDEFINED;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue<U, V> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public AnnotationValue.Loaded<V> load(ClassLoader classLoader) {
            try {
                Class<? extends Annotation> type = (Class<? extends Annotation>) Class.forName(typeDescription.getName(), false, classLoader);
                return type.isAnnotation()
                        ? new Loaded<V>(type, property)
                        : new ForIncompatibleType.Loaded<V>(type);
            } catch (ClassNotFoundException exception) {
                return new ForMissingType.Loaded<V>(typeDescription.getName(), exception);
            }
        }

        /**
         * {@inheritDoc}
         */
        public U resolve() {
            throw new IllegalStateException(typeDescription + " does not define " + property);
        }

        /* does not implement hashCode and equals method to mimic OpenJDK behavior. */

        /**
         * Describes an annotation value for a property that is not assignable to it.
         *
         * @param <W> The type of the annotation's expected value.
         */
        public static class Loaded<W> extends AnnotationValue.Loaded.AbstractBase<W> {

            /**
             * The annotation type.
             */
            private final Class<? extends Annotation> type;

            /**
             * The name of the property for which the annotation value is missing.
             */
            private final String property;

            /**
             * Creates a new loaded representation for an unresolved property.
             *
             * @param type     The annotation type.
             * @param property The name of the property for which the annotation value is missing.
             */
            public Loaded(Class<? extends Annotation> type, String property) {
                this.type = type;
                this.property = property;
            }

            /**
             * {@inheritDoc}
             */
            public State getState() {
                return State.UNDEFINED;
            }

            /**
             * {@inheritDoc}
             */
            public W resolve() {
                throw new IncompleteAnnotationException(type, property);
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(Object value) {
                return false;
            }

            /* does not implement hashCode and equals method to mimic OpenJDK behavior. */
        }
    }

    /**
     * Represents an annotation value where its declared type does not fulfil an expectation.
     *
     * @param <U> The type of the annotation's value when it is not loaded.
     * @param <V> The type of the annotation's value when it is loaded.
     */
    class ForIncompatibleType<U, V> extends AnnotationValue.AbstractBase<U, V> {

        /**
         * A description of the type that does not fulfil an expectation.
         */
        private final TypeDescription typeDescription;

        /**
         * Creates a new description for an annotation value that does not fulfil expectations.
         *
         * @param typeDescription A description of the type that does not fulfil the expectations.
         */
        public ForIncompatibleType(TypeDescription typeDescription) {
            this.typeDescription = typeDescription;
        }

        /**
         * {@inheritDoc}
         */
        public State getState() {
            return State.UNRESOLVED;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue<U, V> filter(MethodDescription.InDefinedShape property, TypeDefinition typeDefinition) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public U resolve() {
            throw new IllegalStateException("Property is defined with an incompatible runtime type: " + typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValue.Loaded<V> load(ClassLoader classLoader) {
            try {
                return new Loaded<V>(Class.forName(typeDescription.getName(), false, classLoader));
            } catch (ClassNotFoundException exception) {
                return new ForMissingType.Loaded<V>(typeDescription.getName(), exception);
            }
        }

        /* does not implement hashCode and equals method to mimic OpenJDK behavior. */

        @Override
        public String toString() {
            return "/* Warning type incompatibility! \"" + typeDescription.getName() + "\" */";
        }

        /**
         * A description of annotation value for a type that does not fulfil an expectation.
         *
         * @param <W> The type of the annotation's expected value.
         */
        public static class Loaded<W> extends AnnotationValue.Loaded.AbstractBase.ForUnresolvedProperty<W> {

            /**
             * The type that does not fulfil an expectation.
             */
            private final Class<?> type;

            /**
             * Creates a new description of an annotation.
             *
             * @param type The type that does not fulfil an expectation.
             */
            public Loaded(Class<?> type) {
                this.type = type;
            }

            /**
             * {@inheritDoc}
             */
            public W resolve() {
                throw new IncompatibleClassChangeError(type.toString());
            }

            /* does not implement hashCode and equals method to mimic OpenJDK behavior. */

            @Override
            public String toString() {
                return "/* Warning type incompatibility! \"" + type.getName() + "\" */";
            }
        }
    }
}
