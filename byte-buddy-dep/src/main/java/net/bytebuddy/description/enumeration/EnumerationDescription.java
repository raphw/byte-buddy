/*
 * Copyright 2014 - Present Rafael Winterhalter
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
package net.bytebuddy.description.enumeration;

import net.bytebuddy.build.CachedReturnPlugin;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes an enumeration value. Note that the {@link java.lang.Object#toString} method always returns the
 * value as if the method was not overridden, i.e. the name of the enumeration constant.
 */
public interface EnumerationDescription extends NamedElement {

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

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            return getValue();
        }

        @Override
        @CachedReturnPlugin.Enhance("hashCode")
        public int hashCode() {
            return getValue().hashCode() + 31 * getEnumerationType().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof EnumerationDescription)) {
                return false;
            }
            EnumerationDescription enumerationDescription = (EnumerationDescription) other;
            return getEnumerationType().equals(enumerationDescription.getEnumerationType()) && getValue().equals(enumerationDescription.getValue());
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

        /**
         * {@inheritDoc}
         */
        public String getValue() {
            return value.name();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getEnumerationType() {
            return TypeDescription.ForLoadedType.of(value.getDeclaringClass());
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public <T extends Enum<T>> T load(Class<T> type) {
            return value.getDeclaringClass() == type
                    ? (T) value
                    : Enum.valueOf(type, value.name());
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

        /**
         * {@inheritDoc}
         */
        public String getValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getEnumerationType() {
            return enumerationType;
        }

        /**
         * {@inheritDoc}
         */
        public <T extends Enum<T>> T load(Class<T> type) {
            if (!enumerationType.represents(type)) {
                throw new IllegalArgumentException(type + " does not represent " + enumerationType);
            }
            return Enum.valueOf(type, value);
        }
    }
}
