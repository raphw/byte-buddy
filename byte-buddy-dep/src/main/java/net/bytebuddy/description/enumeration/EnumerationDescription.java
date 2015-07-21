package net.bytebuddy.description.enumeration;

import net.bytebuddy.description.type.TypeDescription;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes an enumeration value. Note that the {@link java.lang.Object#toString} method always returns the
 * value as if the method was not overridden, i.e. the name of the enumeration constant.
 */
public interface EnumerationDescription {

    /**
     * Returns the name of this instance's enumeration value.
     *
     * @return The name of this enumeration constant.
     */
    String getValue();

    /**
     * Returns the type of this enumeration.
     *
     * @return The type of this enumeration.
     */
    TypeDescription getEnumerationType();

    /**
     * Prepares this enumeration value to be loaded.
     *
     * @param type A type constant representing the enumeration value.
     * @param <T>  The enumeration type.
     * @return The loaded enumeration constant corresponding to this value.
     */
    <T extends Enum<T>> T load(Class<T> type);

    /**
     * An adapter implementation of an enumeration description.
     */
    abstract class AbstractBase implements EnumerationDescription {

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof EnumerationDescription
                    && (((EnumerationDescription) other)).getEnumerationType().equals(getEnumerationType())
                    && (((EnumerationDescription) other)).getValue().equals(getValue());
        }

        @Override
        public int hashCode() {
            return getValue().hashCode() + 31 * getEnumerationType().hashCode();
        }

        @Override
        public String toString() {
            return getValue();
        }
    }

    /**
     * An enumeration description representing a loaded enumeration.
     */
    class ForLoadedEnumeration extends AbstractBase {

        /**
         * The loaded enumeration value.
         */
        private final Enum<?> value;

        /**
         * Creates a new enumeration value representation for a loaded enumeration.
         *
         * @param value The value to represent.
         */
        public ForLoadedEnumeration(Enum<?> value) {
            this.value = value;
        }

        /**
         * Enlists a given array of loaded enumerations as enumeration values.
         *
         * @param enumerations The enumerations to represent.
         * @return A list of the given enumerations.
         */
        public static List<EnumerationDescription> asList(Enum<?>[] enumerations) {
            List<EnumerationDescription> result = new ArrayList<EnumerationDescription>(enumerations.length);
            for (Enum<?> enumeration : enumerations) {
                result.add(new ForLoadedEnumeration(enumeration));
            }
            return result;
        }

        @Override
        public String getValue() {
            return value.name();
        }

        @Override
        public TypeDescription getEnumerationType() {
            return new TypeDescription.ForLoadedType(value.getDeclaringClass());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Enum<T>> T load(Class<T> type) {
            if (value.getDeclaringClass() != type) {
                throw new IllegalArgumentException(type + " does not represent " + value);
            }
            return (T) value;
        }
    }

    /**
     * A latent description of an enumeration value.
     */
    class Latent extends AbstractBase {

        /**
         * The type of the enumeration.
         */
        private final TypeDescription enumerationType;

        /**
         * The value of the enumeration.
         */
        private final String value;

        /**
         * Creates a latent description of an enumeration value.
         *
         * @param enumerationType The enumeration type.
         * @param value           The value of the enumeration.
         */
        public Latent(TypeDescription enumerationType, String value) {
            this.enumerationType = enumerationType;
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public TypeDescription getEnumerationType() {
            return enumerationType;
        }

        @Override
        public <T extends Enum<T>> T load(Class<T> type) {
            if (!enumerationType.represents(type)) {
                throw new IllegalArgumentException(type + " does not represent " + enumerationType);
            }
            return Enum.valueOf(type, value);
        }
    }
}
