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

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <p>
 * Defines a method to access a given field by following the Java bean conventions for getters and setters:
 * </p>
 * <ul>
 * <li>Getter: A method named {@code getFoo()} will be instrumented to read and return the value of a field {@code foo}
 * or another field if one was specified explicitly. If a property is of type {@link java.lang.Boolean} or
 * {@code boolean}, the name {@code isFoo()} is also permitted.</li>
 * <li>Setter: A method named {@code setFoo(value)} will be instrumented to write the given argument {@code value}
 * to a field {@code foo} or to another field if one was specified explicitly.</li>
 * </ul>
 * <p>
 * Field accessors always implement a getter if a non-{@code void} value is returned from a method and attempt to define a setter
 * otherwise. If a field accessor is not explicitly defined as a setter via {@link PropertyConfigurable}, an instrumented
 * method must define exactly one parameter. Using the latter API, an explicit parameter index can be defined and a return
 * value can be specified explicitly when {@code void} is not returned.
 * </p>
 */
@HashCodeAndEqualsPlugin.Enhance
public abstract class FieldAccessor implements Implementation {

    /**
     * The field's location.
     */
    protected final FieldLocation fieldLocation;

    /**
     * The assigner to use.
     */
    protected final Assigner assigner;

    /**
     * Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected final Assigner.Typing typing;

    /**
     * Creates a new field accessor.
     *
     * @param fieldLocation The field's location.
     * @param assigner      The assigner to use.
     * @param typing        Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected FieldAccessor(FieldLocation fieldLocation, Assigner assigner, Assigner.Typing typing) {
        this.fieldLocation = fieldLocation;
        this.assigner = assigner;
        this.typing = typing;
    }

    /**
     * Defines a field accessor where any access is targeted to a field named {@code name}.
     *
     * @param name The name of the field to be accessed.
     * @return A field accessor for a field of a given name.
     */
    public static OwnerTypeLocatable ofField(String name) {
        return of(new FieldNameExtractor.ForFixedValue(name));
    }

    /**
     * Defines a field accessor where any access is targeted to a field that matches the methods
     * name with the Java specification for bean properties, i.e. a method {@code getFoo} or {@code setFoo(value)}
     * will either read or write a field named {@code foo}.
     *
     * @return A field accessor that follows the Java naming conventions for bean properties.
     */
    public static OwnerTypeLocatable ofBeanProperty() {
        return of(FieldNameExtractor.ForBeanProperty.INSTANCE);
    }

    /**
     * Defines a custom strategy for determining the field that is accessed by this field accessor.
     *
     * @param fieldNameExtractor The field name extractor to use.
     * @return A field accessor using the given field name extractor.
     */
    public static OwnerTypeLocatable of(FieldNameExtractor fieldNameExtractor) {
        return new ForImplicitProperty(new FieldLocation.Relative(fieldNameExtractor));
    }

    /**
     * Defines a field accessor where the specified field is accessed. The field must be within the hierarchy of the instrumented type.
     *
     * @param field The field being accessed.
     * @return A field accessor for the given field.
     */
    public static AssignerConfigurable of(Field field) {
        return of(new FieldDescription.ForLoadedField(field));
    }

    /**
     * Defines a field accessor where the specified field is accessed. The field must be within the hierarchy of the instrumented type.
     *
     * @param fieldDescription The field being accessed.
     * @return A field accessor for the given field.
     */
    public static AssignerConfigurable of(FieldDescription fieldDescription) {
        return new ForImplicitProperty(new FieldLocation.Absolute(fieldDescription));
    }

    /**
     * A field location represents an identified field description which depends on the instrumented type and method.
     */
    protected interface FieldLocation {

        /**
         * Specifies a field locator factory to use.
         *
         * @param fieldLocatorFactory The field locator factory to use.
         * @return An appropriate field location.
         */
        FieldLocation with(FieldLocator.Factory fieldLocatorFactory);

        /**
         * A prepared field location.
         *
         * @param instrumentedType The instrumented type.
         * @return A prepared field location.
         */
        Prepared prepare(TypeDescription instrumentedType);

        /**
         * A prepared field location.
         */
        interface Prepared {

            /**
             * Resolves the field description to use.
             *
             * @param instrumentedMethod The instrumented method.
             * @return The resolved field description.
             */
            FieldDescription resolve(MethodDescription instrumentedMethod);
        }

        /**
         * An absolute field description representing a previously resolved field.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Absolute implements FieldLocation, Prepared {

            /**
             * The field description.
             */
            private final FieldDescription fieldDescription;

            /**
             * Creates an absolute field location.
             *
             * @param fieldDescription The field description.
             */
            protected Absolute(FieldDescription fieldDescription) {
                this.fieldDescription = fieldDescription;
            }

            /**
             * {@inheritDoc}
             */
            public FieldLocation with(FieldLocator.Factory fieldLocatorFactory) {
                throw new IllegalStateException("Cannot specify a field locator factory for an absolute field location");
            }

            /**
             * {@inheritDoc}
             */
            public Prepared prepare(TypeDescription instrumentedType) {
                if (!instrumentedType.isAssignableTo(fieldDescription.getDeclaringType().asErasure())) {
                    throw new IllegalStateException(fieldDescription + " is not declared by " + instrumentedType);
                } else if (!fieldDescription.isAccessibleTo(instrumentedType)) {
                    throw new IllegalStateException("Cannot access " + fieldDescription + " from " + instrumentedType);
                }
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public FieldDescription resolve(MethodDescription instrumentedMethod) {
                return fieldDescription;
            }
        }

        /**
         * A relative field location where a field is located dynamically.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Relative implements FieldLocation {

            /**
             * The field name extractor to use.
             */
            private final FieldNameExtractor fieldNameExtractor;

            /**
             * The field locator factory to use.
             */
            private final FieldLocator.Factory fieldLocatorFactory;

            /**
             * Creates a new relative field location.
             *
             * @param fieldNameExtractor The field name extractor to use.
             */
            protected Relative(FieldNameExtractor fieldNameExtractor) {
                this(fieldNameExtractor, FieldLocator.ForClassHierarchy.Factory.INSTANCE);
            }

            /**
             * Creates a new relative field location.
             *
             * @param fieldNameExtractor  The field name extractor to use.
             * @param fieldLocatorFactory The field locator factory to use.
             */
            private Relative(FieldNameExtractor fieldNameExtractor, FieldLocator.Factory fieldLocatorFactory) {
                this.fieldNameExtractor = fieldNameExtractor;
                this.fieldLocatorFactory = fieldLocatorFactory;
            }

            /**
             * {@inheritDoc}
             */
            public FieldLocation with(FieldLocator.Factory fieldLocatorFactory) {
                return new Relative(fieldNameExtractor, fieldLocatorFactory);
            }

            /**
             * {@inheritDoc}
             */
            public FieldLocation.Prepared prepare(TypeDescription instrumentedType) {
                return new Prepared(fieldNameExtractor, fieldLocatorFactory.make(instrumentedType));
            }

            /**
             * A prepared version of a field location.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Prepared implements FieldLocation.Prepared {

                /**
                 * The field name extractor to use.
                 */
                private final FieldNameExtractor fieldNameExtractor;

                /**
                 * The field locator factory to use.
                 */
                private final FieldLocator fieldLocator;

                /**
                 * Creates a new relative field location.
                 *
                 * @param fieldNameExtractor The field name extractor to use.
                 * @param fieldLocator       The field locator to use.
                 */
                protected Prepared(FieldNameExtractor fieldNameExtractor, FieldLocator fieldLocator) {
                    this.fieldNameExtractor = fieldNameExtractor;
                    this.fieldLocator = fieldLocator;
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldDescription resolve(MethodDescription instrumentedMethod) {
                    FieldLocator.Resolution resolution = fieldLocator.locate(fieldNameExtractor.resolve(instrumentedMethod));
                    if (!resolution.isResolved()) {
                        throw new IllegalStateException("Cannot resolve field for " + instrumentedMethod + " using " + fieldLocator);
                    }
                    return resolution.getField();
                }
            }
        }
    }

    /**
     * A field name extractor is responsible for determining a field name to a method that is implemented
     * to access this method.
     */
    public interface FieldNameExtractor {

        /**
         * Extracts a field name to be accessed by a getter or setter method.
         *
         * @param methodDescription The method for which a field name is to be determined.
         * @return The name of the field to be accessed by this method.
         */
        String resolve(MethodDescription methodDescription);

        /**
         * A {@link net.bytebuddy.implementation.FieldAccessor.FieldNameExtractor} that determines a field name
         * according to the rules of Java bean naming conventions.
         */
        enum ForBeanProperty implements FieldNameExtractor {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public String resolve(MethodDescription methodDescription) {
                String name = methodDescription.getInternalName();
                int crop;
                if (name.startsWith("get") || name.startsWith("set")) {
                    crop = 3;
                } else if (name.startsWith("is")) {
                    crop = 2;
                } else {
                    throw new IllegalArgumentException(methodDescription + " does not follow Java bean naming conventions");
                }
                name = name.substring(crop);
                if (name.length() == 0) {
                    throw new IllegalArgumentException(methodDescription + " does not specify a bean name");
                }
                return Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }
        }

        /**
         * A field name extractor that returns a fixed value.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForFixedValue implements FieldNameExtractor {

            /**
             * The name to return.
             */
            private final String name;

            /**
             * Creates a new field name extractor for a fixed value.
             *
             * @param name The name to return.
             */
            protected ForFixedValue(String name) {
                this.name = name;
            }

            /**
             * {@inheritDoc}
             */
            public String resolve(MethodDescription methodDescription) {
                return name;
            }
        }
    }

    /**
     * A field accessor that allows to define the access to be a field write of a given argument.
     */
    public interface PropertyConfigurable extends Implementation {

        /**
         * <p>
         * Defines a setter of the specified parameter for the field being described.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param index The index of the parameter for which to set the field's value.
         * @return An instrumentation that sets the parameter's value to the described field.
         */
        Composable setsArgumentAt(int index);

        /**
         * <p>
         * Defines a setter of the described field's default value, i.e. {@code null} or a primitive type's
         * representation of {@code 0}.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @return An instrumentation that sets the field's default value.
         */
        Composable setsDefaultValue();

        /**
         * <p>
         * Defines a setter of a given value for the described field. If the value is a constant value, it will be
         * defined as a constant assignment, otherwise it is defined as a reference value that is stored in a static
         * field of the instrumented type.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param value The value to set.
         * @return An instrumentation that sets the field's value as specified.
         */
        Composable setsValue(Object value);

        /**
         * <p>
         * Defines a setter of a given class constant value for the described field.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param typeDescription The type to set to the described field.
         * @return An instrumentation that sets the field's value to the given class constant.
         */
        Composable setsValue(TypeDescription typeDescription);

        /**
         * <p>
         * Defines a setter of a given constant value for the described field.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param constant The constant to set as a value.
         * @return An instrumentation that sets the field's value to the given constant.
         */
        Composable setsValue(JavaConstant constant);

        /**
         * <p>
         * Defines a setter of a value that is represented by a stack manipulation.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param stackManipulation A stack manipulation to load the field's value.
         * @param type              The field value's type.
         * @return An instrumentation that sets the field's value to the given value.
         */
        Composable setsValue(StackManipulation stackManipulation, Type type);

        /**
         * <p>
         * Defines a setter of a value that is represented by a stack manipulation.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param stackManipulation A stack manipulation to load the field's value.
         * @param typeDescription   The field value's type.
         * @return An instrumentation that sets the field's value to the given value.
         */
        Composable setsValue(StackManipulation stackManipulation, TypeDescription.Generic typeDescription);

        /**
         * <p>
         * Defines a setter of a given value for the described field. The value is kept as a referenced that is stored
         * in a static field of the instrumented type. The field name is chosen based on the value's hash code.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param value The value to set.
         * @return An instrumentation that sets the field's value as specified.
         */
        Composable setsReference(Object value);

        /**
         * <p>
         * Defines a setter of a given value for the described field. The value is kept as a referenced that is stored
         * in a static field of the instrumented type.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param value The value to set.
         * @param name  The name of the field.
         * @return An instrumentation that sets the field's value as specified.
         */
        Composable setsReference(Object value, String name);

        /**
         * <p>
         * Defines a setter of a value that sets another field's value.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param field The field that holds the value to be set.
         * @return An instrumentation that sets the field's value to the specified field's value.
         */
        Composable setsFieldValueOf(Field field);

        /**
         * <p>
         * Defines a setter of a value that sets another field's value.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param fieldDescription The field that holds the value to be set.
         * @return An instrumentation that sets the field's value to the specified field's value.
         */
        Composable setsFieldValueOf(FieldDescription fieldDescription);

        /**
         * <p>
         * Defines a setter of a value that sets another field's value.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param fieldName The name of the field that is specified by the instrumented type.
         * @return An instrumentation that sets the field's value to the specified field's value.
         */
        Composable setsFieldValueOf(String fieldName);

        /**
         * <p>
         * Defines a setter of a value that sets another field's value.
         * </p>
         * <p>
         * <b>Note</b>: If the instrumented method does not return {@code void}, a chained instrumentation must be supplied.
         * </p>
         *
         * @param fieldNameExtractor A field name extractor for the field that is specified by the instrumented type.
         * @return An instrumentation that sets the field's value to the specified field's value.
         */
        Composable setsFieldValueOf(FieldNameExtractor fieldNameExtractor);
    }

    /**
     * A field accessor that can be configured to use a given assigner and runtime type use configuration.
     */
    public interface AssignerConfigurable extends PropertyConfigurable {

        /**
         * Returns a field accessor that is identical to this field accessor but uses the given assigner
         * and runtime type use configuration.
         *
         * @param assigner The assigner to use.
         * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @return This field accessor with the given assigner and runtime type use configuration.
         */
        PropertyConfigurable withAssigner(Assigner assigner, Assigner.Typing typing);
    }

    /**
     * A field accessor that can be configured to locate a field in a specific manner.
     */
    public interface OwnerTypeLocatable extends AssignerConfigurable {

        /**
         * Determines that a field should only be considered when it was defined in a given type.
         *
         * @param type The type to be considered.
         * @return This field accessor which will only considered fields that are defined in the given type.
         */
        AssignerConfigurable in(Class<?> type);

        /**
         * Determines that a field should only be considered when it was defined in a given type.
         *
         * @param typeDescription A description of the type to be considered.
         * @return This field accessor which will only considered fields that are defined in the given type.
         */
        AssignerConfigurable in(TypeDescription typeDescription);

        /**
         * Determines that a field should only be considered when it was identified by a field locator that is
         * produced by the given factory.
         *
         * @param fieldLocatorFactory A factory that will produce a field locator that will be used to find locate
         *                            a field to be accessed.
         * @return This field accessor which will only considered fields that are defined in the given type.
         */
        AssignerConfigurable in(FieldLocator.Factory fieldLocatorFactory);
    }

    /**
     * A field accessor for an implicit property where a getter or setter property is inferred from the signature.
     */
    protected static class ForImplicitProperty extends FieldAccessor implements OwnerTypeLocatable {

        /**
         * Creates a field accessor for an implicit property.
         *
         * @param fieldLocation The field's location.
         */
        protected ForImplicitProperty(FieldLocation fieldLocation) {
            this(fieldLocation, Assigner.DEFAULT, Assigner.Typing.STATIC);
        }

        /**
         * Creates a field accessor for an implicit property.
         *
         * @param fieldLocation The field's location.
         * @param assigner      The assigner to use.
         * @param typing        The typing to use.
         */
        private ForImplicitProperty(FieldLocation fieldLocation, Assigner assigner, Assigner.Typing typing) {
            super(fieldLocation, assigner, typing);
        }

        /**
         * {@inheritDoc}
         */
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getInstrumentedType(), fieldLocation.prepare(implementationTarget.getInstrumentedType()));
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsArgumentAt(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("A parameter index cannot be negative: " + index);
            }
            return new ForSetter.OfParameterValue(fieldLocation,
                    assigner,
                    typing,
                    ForSetter.TerminationHandler.RETURNING,
                    index);
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsDefaultValue() {
            return new ForSetter.OfDefaultValue(fieldLocation, assigner, typing, ForSetter.TerminationHandler.RETURNING);
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsValue(Object value) {
            Class<?> type = value.getClass();
            if (type == String.class) {
                return setsValue(new TextConstant((String) value), String.class);
            } else if (type == Class.class) {
                return setsValue(ClassConstant.of(TypeDescription.ForLoadedType.of((Class<?>) value)), Class.class);
            } else if (type == Boolean.class) {
                return setsValue(IntegerConstant.forValue((Boolean) value), boolean.class);
            } else if (type == Byte.class) {
                return setsValue(IntegerConstant.forValue((Byte) value), byte.class);
            } else if (type == Short.class) {
                return setsValue(IntegerConstant.forValue((Short) value), short.class);
            } else if (type == Character.class) {
                return setsValue(IntegerConstant.forValue((Character) value), char.class);
            } else if (type == Integer.class) {
                return setsValue(IntegerConstant.forValue((Integer) value), int.class);
            } else if (type == Long.class) {
                return setsValue(LongConstant.forValue((Long) value), long.class);
            } else if (type == Float.class) {
                return setsValue(FloatConstant.forValue((Float) value), float.class);
            } else if (type == Double.class) {
                return setsValue(DoubleConstant.forValue((Double) value), double.class);
            } else if (JavaType.METHOD_HANDLE.getTypeStub().isAssignableFrom(type)) {
                return setsValue(new JavaConstantValue(JavaConstant.MethodHandle.ofLoaded(value)), type);
            } else if (JavaType.METHOD_TYPE.getTypeStub().represents(type)) {
                return setsValue(new JavaConstantValue(JavaConstant.MethodType.ofLoaded(value)), type);
            } else {
                return setsReference(value);
            }
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsValue(TypeDescription typeDescription) {
            return setsValue(ClassConstant.of(typeDescription), Class.class);
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsValue(JavaConstant constant) {
            return setsValue(new JavaConstantValue(constant), constant.getType().asGenericType());
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsValue(StackManipulation stackManipulation, Type type) {
            return setsValue(stackManipulation, TypeDescription.Generic.Sort.describe(type));
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsValue(StackManipulation stackManipulation, TypeDescription.Generic typeDescription) {
            return new ForSetter.OfConstantValue(fieldLocation,
                    assigner,
                    typing,
                    ForSetter.TerminationHandler.RETURNING,
                    typeDescription,
                    stackManipulation);
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsReference(Object value) {
            return setsReference(value, ForSetter.OfReferenceValue.PREFIX + "$" + RandomString.hashOf(value.hashCode()));
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsReference(Object value, String name) {
            return new ForSetter.OfReferenceValue(fieldLocation,
                    assigner,
                    typing,
                    ForSetter.TerminationHandler.RETURNING,
                    value,
                    name);
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsFieldValueOf(Field field) {
            return setsFieldValueOf(new FieldDescription.ForLoadedField(field));
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsFieldValueOf(FieldDescription fieldDescription) {
            return new ForSetter.OfFieldValue(fieldLocation,
                    assigner,
                    typing,
                    ForSetter.TerminationHandler.RETURNING,
                    new FieldLocation.Absolute(fieldDescription));
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsFieldValueOf(String fieldName) {
            return setsFieldValueOf(new FieldNameExtractor.ForFixedValue(fieldName));
        }

        /**
         * {@inheritDoc}
         */
        public Composable setsFieldValueOf(FieldNameExtractor fieldNameExtractor) {
            return new ForSetter.OfFieldValue(fieldLocation,
                    assigner,
                    typing,
                    ForSetter.TerminationHandler.RETURNING,
                    new FieldLocation.Relative(fieldNameExtractor));
        }

        /**
         * {@inheritDoc}
         */
        public PropertyConfigurable withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForImplicitProperty(fieldLocation, assigner, typing);
        }

        /**
         * {@inheritDoc}
         */
        public AssignerConfigurable in(Class<?> type) {
            return in(TypeDescription.ForLoadedType.of(type));
        }

        /**
         * {@inheritDoc}
         */
        public AssignerConfigurable in(TypeDescription typeDescription) {
            return in(new FieldLocator.ForExactType.Factory(typeDescription));
        }

        /**
         * {@inheritDoc}
         */
        public AssignerConfigurable in(FieldLocator.Factory fieldLocatorFactory) {
            return new ForImplicitProperty(fieldLocation.with(fieldLocatorFactory), assigner, typing);
        }

        /**
         * An byte code appender for an field accessor implementation.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class Appender implements ByteCodeAppender {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * The field's location.
             */
            private final FieldLocation.Prepared fieldLocation;

            /**
             * Creates a new byte code appender for a field accessor implementation.
             *
             * @param instrumentedType The instrumented type.
             * @param fieldLocation    The field's location.
             */
            protected Appender(TypeDescription instrumentedType, FieldLocation.Prepared fieldLocation) {
                this.instrumentedType = instrumentedType;
                this.fieldLocation = fieldLocation;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                if (!instrumentedMethod.isMethod()) {
                    throw new IllegalArgumentException(instrumentedMethod + " does not describe a field getter or setter");
                }
                FieldDescription fieldDescription = fieldLocation.resolve(instrumentedMethod);
                if (!instrumentedType.isAssignableTo(fieldDescription.getDeclaringType().asErasure())) {
                    throw new IllegalStateException(fieldDescription + " is not declared in the hierarchy of " + instrumentedType);
                } else if (!fieldDescription.isAccessibleTo(instrumentedType)) {
                    throw new IllegalStateException("Cannot access " + fieldDescription + " from " + instrumentedType);
                } else if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                    throw new IllegalStateException("Cannot set instance field " + fieldDescription + " from " + instrumentedMethod);
                }
                StackManipulation implementation, initialization = fieldDescription.isStatic()
                        ? StackManipulation.Trivial.INSTANCE
                        : MethodVariableAccess.loadThis();
                if (!instrumentedMethod.getReturnType().represents(void.class)) {
                    implementation = new StackManipulation.Compound(
                            initialization,
                            FieldAccess.forField(fieldDescription).read(),
                            assigner.assign(fieldDescription.getType(), instrumentedMethod.getReturnType(), typing),
                            MethodReturn.of(instrumentedMethod.getReturnType())
                    );
                } else if (instrumentedMethod.getReturnType().represents(void.class) && instrumentedMethod.getParameters().size() == 1) {
                    if (fieldDescription.isFinal() && instrumentedMethod.isMethod()) {
                        throw new IllegalStateException("Cannot set final field " + fieldDescription + " from " + instrumentedMethod);
                    }
                    implementation = new StackManipulation.Compound(
                            initialization,
                            MethodVariableAccess.load(instrumentedMethod.getParameters().get(0)),
                            assigner.assign(instrumentedMethod.getParameters().get(0).getType(), fieldDescription.getType(), typing),
                            FieldAccess.forField(fieldDescription).write(),
                            MethodReturn.VOID
                    );
                } else {
                    throw new IllegalArgumentException("Method " + implementationContext + " is no bean property");
                }
                if (!implementation.isValid()) {
                    throw new IllegalStateException("Cannot set or get value of " + instrumentedMethod + " using " + fieldDescription);
                }
                return new Size(implementation.apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }

    /**
     * A field accessor for a field setter.
     *
     * @param <T> The type of the value that is initialized per instrumented type.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected abstract static class ForSetter<T> extends FieldAccessor implements Implementation.Composable {

        /**
         * The termination handler to apply.
         */
        private final TerminationHandler terminationHandler;

        /**
         * Creates a new field accessor for a setter instrumentation.
         *
         * @param fieldLocation      The field's location.
         * @param assigner           The assigner to use.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param terminationHandler The termination handler to apply.
         */
        protected ForSetter(FieldLocation fieldLocation, Assigner assigner, Assigner.Typing typing, TerminationHandler terminationHandler) {
            super(fieldLocation, assigner, typing);
            this.terminationHandler = terminationHandler;
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getInstrumentedType(),
                    initialize(implementationTarget.getInstrumentedType()),
                    fieldLocation.prepare(implementationTarget.getInstrumentedType()));
        }

        /**
         * Initializes a value to be used during method instrumentation.
         *
         * @param instrumentedType The instrumented type.
         * @return The initialized value.
         */
        protected abstract T initialize(TypeDescription instrumentedType);

        /**
         * Resolves the stack manipulation to load the value being set.
         *
         * @param initialized        The method that was initialized for the instrumented type.
         * @param fieldDescription   The field to set the value for.
         * @param instrumentedType   The instrumented type.
         * @param instrumentedMethod The instrumented method.
         * @return The stack manipulation to apply.
         */
        protected abstract StackManipulation resolve(T initialized,
                                                     FieldDescription fieldDescription,
                                                     TypeDescription instrumentedType,
                                                     MethodDescription instrumentedMethod);

        /**
         * A termination handler is responsible for handling a field accessor's return.
         */
        protected enum TerminationHandler {

            /**
             * Returns {@code void} or throws an exception if this is not the return type of the instrumented method.
             */
            RETURNING {
                @Override
                protected StackManipulation resolve(MethodDescription instrumentedMethod) {
                    if (!instrumentedMethod.getReturnType().represents(void.class)) {
                        throw new IllegalStateException("Cannot implement setter with return value for " + instrumentedMethod);
                    }
                    return MethodReturn.VOID;
                }
            },

            /**
             * Does not return from the method at all.
             */
            NON_OPERATIONAL {
                @Override
                protected StackManipulation resolve(MethodDescription instrumentedMethod) {
                    return StackManipulation.Trivial.INSTANCE;
                }
            };

            /**
             * Resolves the return instruction.
             *
             * @param instrumentedMethod The instrumented method.
             * @return An appropriate stack manipulation.
             */
            protected abstract StackManipulation resolve(MethodDescription instrumentedMethod);
        }

        /**
         * A setter instrumentation for a parameter value.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class OfParameterValue extends ForSetter<Void> {

            /**
             * The parameter's index.
             */
            private final int index;

            /**
             * Creates a new setter instrumentation for a parameter value.
             *
             * @param fieldLocation      The field's location.
             * @param assigner           The assigner to use.
             * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
             * @param terminationHandler The termination handler to apply.
             * @param index              The parameter's index.
             */
            protected OfParameterValue(FieldLocation fieldLocation,
                                       Assigner assigner,
                                       Assigner.Typing typing,
                                       TerminationHandler terminationHandler,
                                       int index) {
                super(fieldLocation, assigner, typing, terminationHandler);
                this.index = index;
            }

            /**
             * {@inheritDoc}
             */
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            /**
             * {@inheritDoc}
             */
            protected Void initialize(TypeDescription instrumentedType) {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            protected StackManipulation resolve(Void unused,
                                                FieldDescription fieldDescription,
                                                TypeDescription instrumentedType,
                                                MethodDescription instrumentedMethod) {
                if (instrumentedMethod.getParameters().size() <= index) {
                    throw new IllegalStateException(instrumentedMethod + " does not define a parameter with index " + index);
                } else {
                    return new StackManipulation.Compound(
                            MethodVariableAccess.load(instrumentedMethod.getParameters().get(index)),
                            assigner.assign(instrumentedMethod.getParameters().get(index).getType(), fieldDescription.getType(), typing)
                    );
                }
            }

            /**
             * {@inheritDoc}
             */
            public Implementation andThen(Implementation implementation) {
                return new Compound(new OfParameterValue(fieldLocation,
                        assigner,
                        typing,
                        TerminationHandler.NON_OPERATIONAL,
                        index), implementation);
            }

            /**
             * {@inheritDoc}
             */
            public Composable andThen(Composable implementation) {
                return new Compound.Composable(new OfParameterValue(fieldLocation,
                        assigner,
                        typing,
                        TerminationHandler.NON_OPERATIONAL,
                        index), implementation);
            }
        }

        /**
         * A setter instrumentation that sets a {@code null} or a primitive type's default value.
         */
        protected static class OfDefaultValue extends ForSetter<Void> {

            /**
             * Creates an intrumentation that sets a field's default value.
             *
             * @param fieldLocation      The field's location.
             * @param assigner           The assigner to use.
             * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
             * @param terminationHandler The termination handler to apply.
             */
            protected OfDefaultValue(FieldLocation fieldLocation, Assigner assigner, Assigner.Typing typing, TerminationHandler terminationHandler) {
                super(fieldLocation, assigner, typing, terminationHandler);
            }

            /**
             * {@inheritDoc}
             */
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            /**
             * {@inheritDoc}
             */
            protected Void initialize(TypeDescription instrumentedType) {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            protected StackManipulation resolve(Void initialized,
                                                FieldDescription fieldDescription,
                                                TypeDescription instrumentedType,
                                                MethodDescription instrumentedMethod) {
                return DefaultValue.of(fieldDescription.getType());
            }

            /**
             * {@inheritDoc}
             */
            public Implementation andThen(Implementation implementation) {
                return new Compound(new OfDefaultValue(fieldLocation,
                        assigner,
                        typing,
                        TerminationHandler.NON_OPERATIONAL), implementation);
            }

            /**
             * {@inheritDoc}
             */
            public Composable andThen(Composable implementation) {
                return new Compound.Composable(new OfDefaultValue(fieldLocation,
                        assigner,
                        typing,
                        TerminationHandler.NON_OPERATIONAL), implementation);
            }
        }

        /**
         * An instrumentation that sets a constant value to a field.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class OfConstantValue extends ForSetter<Void> {

            /**
             * The value's type.
             */
            private final TypeDescription.Generic typeDescription;

            /**
             * A stack manipulation to load the constant value.
             */
            private final StackManipulation stackManipulation;

            /**
             * Creates a setter instrumentation for setting a constant value.
             *
             * @param fieldLocation      The field's location.
             * @param assigner           The assigner to use.
             * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
             * @param terminationHandler The termination handler to apply.
             * @param typeDescription    The value's type.
             * @param stackManipulation  A stack manipulation to load the constant value.
             */
            protected OfConstantValue(FieldLocation fieldLocation,
                                      Assigner assigner,
                                      Assigner.Typing typing,
                                      TerminationHandler terminationHandler,
                                      TypeDescription.Generic typeDescription,
                                      StackManipulation stackManipulation) {
                super(fieldLocation, assigner, typing, terminationHandler);
                this.typeDescription = typeDescription;
                this.stackManipulation = stackManipulation;
            }

            /**
             * {@inheritDoc}
             */
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            /**
             * {@inheritDoc}
             */
            protected Void initialize(TypeDescription instrumentedType) {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            protected StackManipulation resolve(Void unused,
                                                FieldDescription fieldDescription,
                                                TypeDescription instrumentedType,
                                                MethodDescription instrumentedMethod) {
                return new StackManipulation.Compound(stackManipulation, assigner.assign(typeDescription, fieldDescription.getType(), typing));
            }

            /**
             * {@inheritDoc}
             */
            public Implementation andThen(Implementation implementation) {
                return new Compound(new OfConstantValue(fieldLocation,
                        assigner,
                        typing,
                        TerminationHandler.NON_OPERATIONAL,
                        typeDescription,
                        stackManipulation), implementation);
            }

            /**
             * {@inheritDoc}
             */
            public Composable andThen(Composable implementation) {
                return new Compound.Composable(new OfConstantValue(fieldLocation,
                        assigner,
                        typing,
                        TerminationHandler.NON_OPERATIONAL,
                        typeDescription,
                        stackManipulation), implementation);
            }
        }

        /**
         * An instrumentation that sets a field to a reference value that is stored in a static field of the instrumented type.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class OfReferenceValue extends ForSetter<FieldDescription.InDefinedShape> {

            /**
             * The prefix used for implicitly named cached fields.
             */
            protected static final String PREFIX = "fixedFieldValue";

            /**
             * The value to store.
             */
            private final Object value;

            /**
             * The name of the field to store the reference in.
             */
            private final String name;

            /**
             * Creates a setter instrumentation for setting a value stored in a static field of the instrumented type.
             *
             * @param fieldLocation      The field's location.
             * @param assigner           The assigner to use.
             * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
             * @param terminationHandler The termination handler to apply.
             * @param value              The value to store.
             * @param name               The name of the field to store the reference in.
             */
            protected OfReferenceValue(FieldLocation fieldLocation,
                                       Assigner assigner,
                                       Assigner.Typing typing,
                                       TerminationHandler terminationHandler,
                                       Object value,
                                       String name) {
                super(fieldLocation, assigner, typing, terminationHandler);
                this.value = value;
                this.name = name;
            }

            /**
             * {@inheritDoc}
             */
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType
                        .withField(new FieldDescription.Token(name, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, TypeDescription.ForLoadedType.of(value.getClass()).asGenericType()))
                        .withInitializer(new LoadedTypeInitializer.ForStaticField(name, value));
            }

            /**
             * {@inheritDoc}
             */
            protected FieldDescription.InDefinedShape initialize(TypeDescription instrumentedType) {
                return instrumentedType.getDeclaredFields().filter(named(name)).getOnly();
            }

            /**
             * {@inheritDoc}
             */
            protected StackManipulation resolve(FieldDescription.InDefinedShape target,
                                                FieldDescription fieldDescription,
                                                TypeDescription instrumentedType,
                                                MethodDescription instrumentedMethod) {
                if (fieldDescription.isFinal() && instrumentedMethod.isMethod()) {
                    throw new IllegalArgumentException("Cannot set final field " + fieldDescription + " from " + instrumentedMethod);
                }
                return new StackManipulation.Compound(
                        FieldAccess.forField(target).read(),
                        assigner.assign(TypeDescription.ForLoadedType.of(value.getClass()).asGenericType(), fieldDescription.getType(), typing)
                );
            }

            /**
             * {@inheritDoc}
             */
            public Implementation andThen(Implementation implementation) {
                return new Compound(new OfReferenceValue(fieldLocation,
                        assigner,
                        typing,
                        TerminationHandler.NON_OPERATIONAL,
                        value,
                        name), implementation);
            }

            /**
             * {@inheritDoc}
             */
            public Composable andThen(Composable implementation) {
                return new Compound.Composable(new OfReferenceValue(fieldLocation,
                        assigner,
                        typing,
                        TerminationHandler.NON_OPERATIONAL,
                        value,
                        name), implementation);
            }
        }

        /**
         * A setter that reads a value of another field and sets this value.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class OfFieldValue extends ForSetter<FieldLocation.Prepared> {

            /**
             * The target field locator.
             */
            private final FieldLocation target;

            /**
             * Creates a setter that sets another field value.
             *
             * @param fieldLocation      The field's location.
             * @param assigner           The assigner to use.
             * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
             * @param terminationHandler The termination handler to apply.
             * @param target             The target field locator.
             */
            protected OfFieldValue(FieldLocation fieldLocation,
                                   Assigner assigner,
                                   Assigner.Typing typing,
                                   TerminationHandler terminationHandler,
                                   FieldLocation target) {
                super(fieldLocation, assigner, typing, terminationHandler);
                this.target = target;
            }

            /**
             * {@inheritDoc}
             */
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            /**
             * {@inheritDoc}
             */
            protected FieldLocation.Prepared initialize(TypeDescription instrumentedType) {
                return target.prepare(instrumentedType);
            }

            /**
             * {@inheritDoc}
             */
            protected StackManipulation resolve(FieldLocation.Prepared target,
                                                FieldDescription fieldDescription,
                                                TypeDescription instrumentedType,
                                                MethodDescription instrumentedMethod) {
                FieldDescription resolved = target.resolve(instrumentedMethod);
                if (!resolved.isStatic() && instrumentedMethod.isStatic()) {
                    throw new IllegalStateException("Cannot set instance field " + fieldDescription + " from " + instrumentedMethod);
                }
                return new StackManipulation.Compound(
                        resolved.isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.loadThis(),
                        FieldAccess.forField(resolved).read(),
                        assigner.assign(resolved.getType(), fieldDescription.getType(), typing)
                );
            }

            /**
             * {@inheritDoc}
             */
            public Implementation andThen(Implementation implementation) {
                return new Compound(new OfFieldValue(fieldLocation,
                        assigner,
                        typing,
                        TerminationHandler.NON_OPERATIONAL,
                        target), implementation);
            }

            /**
             * {@inheritDoc}
             */
            public Composable andThen(Composable implementation) {
                return new Compound.Composable(new OfFieldValue(fieldLocation,
                        assigner,
                        typing,
                        TerminationHandler.NON_OPERATIONAL,
                        target), implementation);
            }
        }

        /**
         * An appender to implement a field setter.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class Appender implements ByteCodeAppender {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * The initialized value.
             */
            private final T initialized;

            /**
             * The set field's prepared location.
             */
            private final FieldLocation.Prepared fieldLocation;

            /**
             * Creates a new appender for a field setter.
             *
             * @param instrumentedType The instrumented type.
             * @param initialized      The initialized value.
             * @param fieldLocation    The set field's prepared location.
             */
            protected Appender(TypeDescription instrumentedType, T initialized, FieldLocation.Prepared fieldLocation) {
                this.instrumentedType = instrumentedType;
                this.initialized = initialized;
                this.fieldLocation = fieldLocation;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                FieldDescription fieldDescription = fieldLocation.resolve(instrumentedMethod);
                if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                    throw new IllegalStateException("Cannot set instance field " + fieldDescription + " from " + instrumentedMethod);
                } else if (fieldDescription.isFinal() && instrumentedMethod.isMethod()) {
                    throw new IllegalStateException("Cannot set final field " + fieldDescription + " from " + instrumentedMethod);
                }
                StackManipulation stackManipulation = resolve(initialized, fieldDescription, instrumentedType, instrumentedMethod);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Set value cannot be assigned to " + fieldDescription);
                }
                return new Size(new StackManipulation.Compound(
                        instrumentedMethod.isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.loadThis(),
                        stackManipulation,
                        FieldAccess.forField(fieldDescription).write(),
                        terminationHandler.resolve(instrumentedMethod)
                ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }
}
