/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;

/**
 * A factory for creating method proxies for an auxiliary type. Such proxies are required to allow a type to
 * call methods of a second type that are usually not accessible for the first type. This strategy is also adapted
 * by the Java compiler that creates accessor methods for example to implement inner classes.
 */
public interface MethodAccessorFactory {

    /**
     * Registers an accessor method for a
     * {@link Implementation.SpecialMethodInvocation} which cannot itself be
     * triggered invoked directly from outside a type. The method is registered on the instrumented type
     * with package-private visibility, similarly to a Java compiler's accessor methods.
     *
     * @param specialMethodInvocation The special method invocation.
     * @param accessType              The required access type.
     * @return The accessor method for invoking the special method invocation.
     */
    MethodDescription.InDefinedShape registerAccessorFor(Implementation.SpecialMethodInvocation specialMethodInvocation, AccessType accessType);

    /**
     * Registers a getter for the given {@link net.bytebuddy.description.field.FieldDescription} which might
     * itself not be accessible from outside the class. The returned getter method defines the field type as
     * its return type, does not take any arguments and is of package-private visibility, similarly to the Java
     * compiler's accessor methods. If the field is {@code static}, this accessor method is also {@code static}.
     *
     * @param fieldDescription The field which is to be accessed.
     * @param accessType       The required access type.
     * @return A getter method for the given field.
     */
    MethodDescription.InDefinedShape registerGetterFor(FieldDescription fieldDescription, AccessType accessType);

    /**
     * Registers a setter for the given {@link FieldDescription} which might
     * itself not be accessible from outside the class. The returned setter method defines the field type as
     * its only argument type, returns {@code void} and is of package-private visibility, similarly to the Java
     * compiler's accessor methods. If the field is {@code static}, this accessor method is also {@code static}.
     *
     * @param fieldDescription The field which is to be accessed.
     * @param accessType       The required access type.
     * @return A setter method for the given field.
     */
    MethodDescription.InDefinedShape registerSetterFor(FieldDescription fieldDescription, AccessType accessType);

    /**
     * Indicates the type of access to an accessor method.
     */
    enum AccessType {

        /**
         * An access with {@code public visibility}.
         */
        PUBLIC(Visibility.PUBLIC),

        /**
         * An access with default visibility.
         */
        DEFAULT(Visibility.PACKAGE_PRIVATE);

        /**
         * The implied visibility.
         */
        private final Visibility visibility;

        /**
         * Creates a new access type.
         *
         * @param visibility The implied visibility.
         */
        AccessType(Visibility visibility) {
            this.visibility = visibility;
        }

        /**
         * Returns the implied visibility.
         *
         * @return The implied visibility.
         */
        public Visibility getVisibility() {
            return visibility;
        }
    }

    /**
     * A method accessor factory that forbids any accessor registration.
     */
    enum Illegal implements MethodAccessorFactory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape registerAccessorFor(Implementation.SpecialMethodInvocation specialMethodInvocation, AccessType accessType) {
            throw new IllegalStateException("It is illegal to register an accessor for this type");
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape registerGetterFor(FieldDescription fieldDescription, AccessType accessType) {
            throw new IllegalStateException("It is illegal to register a field getter for this type");
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape registerSetterFor(FieldDescription fieldDescription, AccessType accessType) {
            throw new IllegalStateException("It is illegal to register a field setter for this type");
        }
    }
}
