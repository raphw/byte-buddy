package net.bytebuddy.implementation;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;

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
@EqualsAndHashCode
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
     * Creates a getter getter.
     *
     * @param fieldDescription   The field to read the value from.
     * @param instrumentedMethod The getter method.
     * @return A stack manipulation that gets the field's value.
     */
    protected StackManipulation getter(FieldDescription fieldDescription, MethodDescription instrumentedMethod) {
        return access(fieldDescription, instrumentedMethod, new StackManipulation.Compound(FieldAccess.forField(fieldDescription).read(),
                assigner.assign(fieldDescription.getType(), instrumentedMethod.getReturnType(), typing)));
    }

    /**
     * Creates a setter instruction.
     *
     * @param fieldDescription     The field to set a value for.
     * @param parameterDescription The parameter for what value is to be set.
     * @return A stack manipulation that sets the field's value.
     */
    protected StackManipulation setter(FieldDescription fieldDescription, ParameterDescription parameterDescription) {
        if (fieldDescription.isFinal() && parameterDescription.getDeclaringMethod().isMethod()) {
            throw new IllegalArgumentException("Cannot set final field " + fieldDescription + " from " + parameterDescription.getDeclaringMethod());
        }
        return access(fieldDescription,
                parameterDescription.getDeclaringMethod(),
                new StackManipulation.Compound(MethodVariableAccess.load(parameterDescription),
                        assigner.assign(parameterDescription.getType(), fieldDescription.getType(), typing),
                        FieldAccess.forField(fieldDescription).write()));
    }

    /**
     * Checks a field access and loads the {@code this} instance if necessary.
     *
     * @param fieldDescription   The field to get a value
     * @param instrumentedMethod The instrumented method.
     * @param fieldAccess        A stack manipulation describing the field access.
     * @return An appropriate stack manipulation.
     */
    private StackManipulation access(FieldDescription fieldDescription, MethodDescription instrumentedMethod, StackManipulation fieldAccess) {
        if (!fieldAccess.isValid()) {
            throw new IllegalStateException("Incompatible type of " + fieldDescription + " and " + instrumentedMethod);
        } else if (instrumentedMethod.isStatic() && !fieldDescription.isStatic()) {
            throw new IllegalArgumentException("Cannot call instance field " + fieldDescription + " from static method " + instrumentedMethod);
        }
        return new StackManipulation.Compound(fieldDescription.isStatic()
                ? StackManipulation.Trivial.INSTANCE
                : MethodVariableAccess.loadThis(), fieldAccess);
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
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
        @EqualsAndHashCode
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

            @Override
            public FieldLocation with(FieldLocator.Factory fieldLocatorFactory) {
                throw new IllegalStateException("Cannot specify a field locator factory for an absolute field location");
            }

            @Override
            public Prepared prepare(TypeDescription instrumentedType) {
                if (!instrumentedType.isAssignableTo(fieldDescription.getDeclaringType().asErasure())) {
                    throw new IllegalStateException(fieldDescription + " is not declared by " + instrumentedType);
                } else if (!fieldDescription.isVisibleTo(instrumentedType)) {
                    throw new IllegalStateException("Cannot access " + fieldDescription + " from " + instrumentedType);
                }
                return this;
            }

            @Override
            public FieldDescription resolve(MethodDescription instrumentedMethod) {
                return fieldDescription;
            }
        }

        /**
         * A relative field location where a field is located dynamically.
         */
        @EqualsAndHashCode
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

            @Override
            public FieldLocation with(FieldLocator.Factory fieldLocatorFactory) {
                return new Relative(fieldNameExtractor, fieldLocatorFactory);
            }

            @Override
            public FieldLocation.Prepared prepare(TypeDescription instrumentedType) {
                return new Prepared(fieldNameExtractor, fieldLocatorFactory.make(instrumentedType));
            }

            /**
             * A prepared version of a field location.
             */
            @EqualsAndHashCode
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

                @Override
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

            @Override
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
        @EqualsAndHashCode
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

            @Override
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
         * Creates a field accessor for the described field that serves as a setter for the supplied parameter index. The instrumented
         * method must return {@code void} or a chained instrumentation must be supplied.
         *
         * @param index The index of the parameter for which to set the field's value.
         * @return An instrumentation that sets the parameter's value to the described field.
         */
        Implementation.Composable setsArgumentAt(int index);
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
     * A field accessor for an implicit property where a getter or setter property is infered from the signature.
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

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(fieldLocation.prepare(implementationTarget.getInstrumentedType()));
        }

        @Override
        public Composable setsArgumentAt(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("A parameter index cannot be negative: " + index);
            }
            return new ForParameterSetter(fieldLocation, assigner, typing, index);
        }

        @Override
        public PropertyConfigurable withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForImplicitProperty(fieldLocation, assigner, typing);
        }

        @Override
        public AssignerConfigurable in(Class<?> type) {
            return in(new TypeDescription.ForLoadedType(type));
        }

        @Override
        public AssignerConfigurable in(TypeDescription typeDescription) {
            return in(new FieldLocator.ForExactType.Factory(typeDescription));
        }

        @Override
        public AssignerConfigurable in(FieldLocator.Factory fieldLocatorFactory) {
            return new ForImplicitProperty(fieldLocation.with(fieldLocatorFactory), assigner, typing);
        }

        /**
         * An byte code appender for an field accessor implementation.
         */
        protected class Appender implements ByteCodeAppender {

            /**
             * The field's location.
             */
            private final FieldLocation.Prepared fieldLocation;

            /**
             * Creates a new byte code appender for a field accessor implementation.
             *
             * @param fieldLocation The field's location.
             */
            protected Appender(FieldLocation.Prepared fieldLocation) {
                this.fieldLocation = fieldLocation;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                if (!instrumentedMethod.isMethod()) {
                    throw new IllegalArgumentException(instrumentedMethod + " does not describe a field getter or setter");
                }
                FieldDescription fieldDescription = fieldLocation.resolve(instrumentedMethod);
                StackManipulation implementation;
                if (!instrumentedMethod.getReturnType().represents(void.class)) {
                    implementation = new StackManipulation.Compound(getter(fieldDescription, instrumentedMethod), MethodReturn.of(instrumentedMethod.getReturnType()));
                } else if (instrumentedMethod.getReturnType().represents(void.class) && instrumentedMethod.getParameters().size() == 1) {
                    implementation = new StackManipulation.Compound(setter(fieldDescription, instrumentedMethod.getParameters().get(0)), MethodReturn.VOID);
                } else {
                    throw new IllegalArgumentException("Method " + implementationContext + " is no bean property");
                }
                return new Size(implementation.apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private ForImplicitProperty getOuter() {
                return ForImplicitProperty.this;
            }

            @Override // HE: Remove when Lombok support for getOuter is added.
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Appender appender = (Appender) object;
                return fieldLocation.equals(appender.fieldLocation) && ForImplicitProperty.this.equals(appender.getOuter());
            }

            @Override // HE: Remove when Lombok support for getOuter is added.
            public int hashCode() {
                return fieldLocation.hashCode() + 31 * ForImplicitProperty.this.hashCode();
            }
        }
    }

    /**
     * A field accessor that sets a parameters value of a given index.
     */
    @EqualsAndHashCode(callSuper = true)
    protected static class ForParameterSetter extends FieldAccessor implements Implementation.Composable {

        /**
         * The targeted parameter index.
         */
        private final int index;

        /**
         * The termination handler to apply.
         */
        private final TerminationHandler terminationHandler;

        /**
         * Creates a new field accessor.
         *
         * @param fieldLocation The field's location.
         * @param assigner      The assigner to use.
         * @param typing        Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param index         The targeted parameter index.
         */
        protected ForParameterSetter(FieldLocation fieldLocation, Assigner assigner, Assigner.Typing typing, int index) {
            this(fieldLocation, assigner, typing, index, TerminationHandler.RETURNING);
        }

        /**
         * Creates a new field accessor.
         *
         * @param fieldLocation      The field's location.
         * @param assigner           The assigner to use.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param index              The targeted parameter index.
         * @param terminationHandler The termination handler to apply.
         */
        private ForParameterSetter(FieldLocation fieldLocation, Assigner assigner, Assigner.Typing typing, int index, TerminationHandler terminationHandler) {
            super(fieldLocation, assigner, typing);
            this.index = index;
            this.terminationHandler = terminationHandler;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(fieldLocation.prepare(implementationTarget.getInstrumentedType()));
        }

        @Override
        public Implementation andThen(Implementation implementation) {
            return new Compound(new ForParameterSetter(fieldLocation,
                    assigner,
                    typing,
                    index, TerminationHandler.NON_OPERATIONAL), implementation);
        }

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
         * An appender for a field accessor that sets a parameter of a given index.
         */
        protected class Appender implements ByteCodeAppender {

            /**
             * The field's location.
             */
            private final FieldLocation.Prepared fieldLocation;

            /**
             * Creates a new byte code appender for a field accessor implementation.
             *
             * @param fieldLocation The field's location.
             */
            protected Appender(FieldLocation.Prepared fieldLocation) {
                this.fieldLocation = fieldLocation;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                if (instrumentedMethod.getParameters().size() <= index) {
                    throw new IllegalStateException(instrumentedMethod + " does not define a parameter with index " + index);
                } else {
                    return new Size(new StackManipulation.Compound(
                            setter(fieldLocation.resolve(instrumentedMethod), instrumentedMethod.getParameters().get(index)),
                            terminationHandler.resolve(instrumentedMethod)
                    ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
                }
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private ForParameterSetter getOuter() {
                return ForParameterSetter.this;
            }

            @Override // HE: Remove when Lombok support for getOuter is added.
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForParameterSetter.Appender appender = (ForParameterSetter.Appender) object;
                return fieldLocation.equals(appender.fieldLocation) && ForParameterSetter.this.equals(appender.getOuter());
            }

            @Override // HE: Remove when Lombok support for getOuter is added.
            public int hashCode() {
                return fieldLocation.hashCode() + 31 * ForParameterSetter.this.hashCode();
            }
        }
    }
}
