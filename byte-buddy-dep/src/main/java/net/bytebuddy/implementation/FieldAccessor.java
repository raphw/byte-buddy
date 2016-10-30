package net.bytebuddy.implementation;

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
public abstract class FieldAccessor implements Implementation {

    /**
     * The field name extractor to be used.
     */
    protected final FieldNameExtractor fieldNameExtractor;

    /**
     * A factory for creating a field locator for implementing this field accessor.
     */
    protected final FieldLocator.Factory fieldLocatorFactory;

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
     * @param fieldLocatorFactory The field name extractor to be used.
     * @param fieldNameExtractor  A factory for creating a field locator for implementing this field accessor.
     * @param assigner            The assigner to use.
     * @param typing              Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected FieldAccessor(FieldNameExtractor fieldNameExtractor, FieldLocator.Factory fieldLocatorFactory, Assigner assigner, Assigner.Typing typing) {
        this.fieldNameExtractor = fieldNameExtractor;
        this.fieldLocatorFactory = fieldLocatorFactory;
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
        return new ForImplicitProperty(fieldNameExtractor, FieldLocator.ForClassHierarchy.Factory.INSTANCE);
    }

    /**
     * Creates a getter getter.
     *
     * @param fieldDescription   The field to read the value from.
     * @param instrumentedMethod The getter method.
     * @return A stack manipulation that gets the field's value.
     */
    protected StackManipulation getter(FieldDescription fieldDescription, MethodDescription instrumentedMethod) {
        return access(fieldDescription, instrumentedMethod, new StackManipulation.Compound(FieldAccess.forField(fieldDescription).getter(),
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
        if (fieldDescription.isFinal() && !parameterDescription.getDeclaringMethod().isConstructor()) {
            throw new IllegalArgumentException("Cannot apply setter on final field " + fieldDescription + " outside of constructor");
        }
        return access(fieldDescription,
                parameterDescription.getDeclaringMethod(),
                new StackManipulation.Compound(MethodVariableAccess.of(fieldDescription.getType().asErasure()).loadOffset(parameterDescription.getOffset()),
                        assigner.assign(parameterDescription.getType(), fieldDescription.getType(), typing),
                        FieldAccess.forField(fieldDescription).putter()));
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
            throw new IllegalStateException("Cannot access non-static " + fieldDescription + " from " + instrumentedMethod);
        } else if (instrumentedMethod.isStatic() && !fieldDescription.isStatic()) {
            throw new IllegalArgumentException("Cannot call instance field " + fieldDescription + " from static method " + instrumentedMethod);
        }
        return new StackManipulation.Compound(fieldDescription.isStatic()
                ? StackManipulation.Trivial.INSTANCE
                : MethodVariableAccess.REFERENCE.loadOffset(0), fieldAccess);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        FieldAccessor that = (FieldAccessor) object;
        return fieldNameExtractor.equals(that.fieldNameExtractor)
                && fieldLocatorFactory.equals(that.fieldLocatorFactory)
                && assigner.equals(that.assigner)
                && typing == that.typing;
    }

    @Override
    public int hashCode() {
        int result = fieldNameExtractor.hashCode();
        result = 31 * result + fieldLocatorFactory.hashCode();
        result = 31 * result + assigner.hashCode();
        result = 31 * result + typing.hashCode();
        return result;
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
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

            @Override
            public String toString() {
                return "FieldAccessor.FieldNameExtractor.ForBeanProperty." + name();
            }
        }

        /**
         * A field name extractor that returns a fixed value.
         */
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

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForFixedValue that = (ForFixedValue) object;
                return name.equals(that.name);
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }

            @Override
            public String toString() {
                return "FieldAccessor.FieldNameExtractor.ForFixedValue{" +
                        "name='" + name + '\'' +
                        '}';
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
         * @param fieldNameExtractor  The field name extractor to use.
         * @param fieldLocatorFactory The field locator factory to use.
         */
        protected ForImplicitProperty(FieldNameExtractor fieldNameExtractor, FieldLocator.Factory fieldLocatorFactory) {
            this(fieldNameExtractor, fieldLocatorFactory, Assigner.DEFAULT, Assigner.Typing.STATIC);
        }

        /**
         * Creates a field accessor for an implicit property.
         *
         * @param fieldNameExtractor  The field name extractor to use.
         * @param fieldLocatorFactory The field locator factory to use.
         * @param assigner            The assigner to use.
         * @param typing              The typing to use.
         */
        private ForImplicitProperty(FieldNameExtractor fieldNameExtractor,
                                    FieldLocator.Factory fieldLocatorFactory,
                                    Assigner assigner,
                                    Assigner.Typing typing) {
            super(fieldNameExtractor, fieldLocatorFactory, assigner, typing);
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(fieldLocatorFactory.make(implementationTarget.getInstrumentedType()));
        }

        @Override
        public Composable setsArgumentAt(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("A parameter index cannot be negative: " + index);
            }
            return new ForParameterSetter(fieldNameExtractor, fieldLocatorFactory, assigner, typing, index);
        }

        @Override
        public PropertyConfigurable withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForImplicitProperty(fieldNameExtractor, fieldLocatorFactory, assigner, typing);
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
            return new ForImplicitProperty(fieldNameExtractor, fieldLocatorFactory, assigner, typing);
        }

        @Override
        public String toString() {
            return "FieldAccessor.ForImplicitProperty{" +
                    "fieldNameExtractor=" + fieldNameExtractor +
                    ", fieldLocatorFactory=" + fieldLocatorFactory +
                    ", assigner=" + assigner +
                    ", typing=" + typing +
                    "}";
        }

        /**
         * An byte code appender for an field accessor implementation.
         */
        protected class Appender implements ByteCodeAppender {

            /**
             * The field locator for implementing this appender.
             */
            private final FieldLocator fieldLocator;

            /**
             * Creates a new byte code appender for a field accessor implementation.
             *
             * @param fieldLocator The field locator for this byte code appender.
             */
            protected Appender(FieldLocator fieldLocator) {
                this.fieldLocator = fieldLocator;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                if (!instrumentedMethod.isMethod()) {
                    throw new IllegalArgumentException(instrumentedMethod + " does not describe a field getter or setter");
                }
                FieldLocator.Resolution resolution = fieldLocator.locate(fieldNameExtractor.resolve(instrumentedMethod));
                StackManipulation implementation;
                if (!resolution.isResolved()) {
                    throw new IllegalStateException("Cannot locate accessible field for " + instrumentedMethod);
                } else if (!instrumentedMethod.getReturnType().represents(void.class)) {
                    implementation = new StackManipulation.Compound(getter(resolution.getField(), instrumentedMethod), MethodReturn.of(instrumentedMethod.getReturnType()));
                } else if (instrumentedMethod.getReturnType().represents(void.class) && instrumentedMethod.getParameters().size() == 1) {
                    implementation = new StackManipulation.Compound(setter(resolution.getField(), instrumentedMethod.getParameters().get(0)), MethodReturn.VOID);
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

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Appender appender = (Appender) object;
                return fieldLocator.equals(appender.fieldLocator) && ForImplicitProperty.this.equals(appender.getOuter());
            }

            @Override
            public int hashCode() {
                return fieldLocator.hashCode() + 31 * ForImplicitProperty.this.hashCode();
            }

            @Override
            public String toString() {
                return "FieldAccessor.ForImplicitProperty.Appender{" +
                        "outer=" + ForImplicitProperty.this +
                        ", fieldLocator=" + fieldLocator +
                        '}';
            }
        }
    }

    /**
     * A field accessor that sets a parameters value of a given index.
     */
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
         * @param fieldLocatorFactory The field name extractor to be used.
         * @param fieldNameExtractor  A factory for creating a field locator for implementing this field accessor.
         * @param assigner            The assigner to use.
         * @param typing              Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param index               The targeted parameter index.
         */
        protected ForParameterSetter(FieldNameExtractor fieldNameExtractor,
                                     FieldLocator.Factory fieldLocatorFactory,
                                     Assigner assigner,
                                     Assigner.Typing typing, int index) {
            this(fieldNameExtractor, fieldLocatorFactory, assigner, typing, index, TerminationHandler.RETURNING);
        }

        /**
         * Creates a new field accessor.
         *
         * @param fieldLocatorFactory The field name extractor to be used.
         * @param fieldNameExtractor  A factory for creating a field locator for implementing this field accessor.
         * @param assigner            The assigner to use.
         * @param typing              Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param index               The targeted parameter index.
         * @param terminationHandler  The termination handler to apply.
         */
        private ForParameterSetter(FieldNameExtractor fieldNameExtractor,
                                   FieldLocator.Factory fieldLocatorFactory,
                                   Assigner assigner,
                                   Assigner.Typing typing,
                                   int index,
                                   TerminationHandler terminationHandler) {
            super(fieldNameExtractor, fieldLocatorFactory, assigner, typing);
            this.index = index;
            this.terminationHandler = terminationHandler;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(fieldLocatorFactory.make(implementationTarget.getInstrumentedType()));
        }

        @Override
        public Implementation andThen(Implementation implementation) {
            return new Compound(new ForParameterSetter(fieldNameExtractor,
                    fieldLocatorFactory,
                    assigner,
                    typing,
                    index, TerminationHandler.NON_OPERATIONAL), implementation);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            if (!super.equals(object)) return false;
            ForParameterSetter that = (ForParameterSetter) object;
            return index == that.index && terminationHandler == that.terminationHandler;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + index;
            result = 31 * result + terminationHandler.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "FieldAccessor.ForParameterSetter{" +
                    "fieldNameExtractor=" + fieldNameExtractor +
                    ", fieldLocatorFactory=" + fieldLocatorFactory +
                    ", assigner=" + assigner +
                    ", typing=" + typing +
                    ", index=" + index +
                    ", terminationHandler=" + terminationHandler +
                    "}";
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

            @Override
            public String toString() {
                return "FieldAccessor.ForParameterSetter.TerminationHandler." + name();
            }
        }

        /**
         * An appender for a field accessor that sets a parameter of a given index.
         */
        protected class Appender implements ByteCodeAppender {

            /**
             * The field locator for implementing this appender.
             */
            private final FieldLocator fieldLocator;

            /**
             * Creates a new byte code appender for a field accessor implementation.
             *
             * @param fieldLocator The field locator for this byte code appender.
             */
            protected Appender(FieldLocator fieldLocator) {
                this.fieldLocator = fieldLocator;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                FieldLocator.Resolution resolution = fieldLocator.locate(fieldNameExtractor.resolve(instrumentedMethod));
                if (!resolution.isResolved()) {
                    throw new IllegalStateException("Cannot locate accessible field for " + instrumentedMethod + " with " + fieldLocator);
                } else if (instrumentedMethod.getParameters().size() <= index) {
                    throw new IllegalStateException(instrumentedMethod + " does not define a parameter with index " + index);
                } else {
                    return new Size(new StackManipulation.Compound(
                            setter(resolution.getField(), instrumentedMethod.getParameters().get(index)),
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

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForParameterSetter.Appender appender = (ForParameterSetter.Appender) object;
                return fieldLocator.equals(appender.fieldLocator) && ForParameterSetter.this.equals(appender.getOuter());
            }

            @Override
            public int hashCode() {
                return fieldLocator.hashCode() + 31 * ForParameterSetter.this.hashCode();
            }

            @Override
            public String toString() {
                return "FieldAccessor.ForParameterSetter.Appender{" +
                        "outer=" + ForParameterSetter.this +
                        ", fieldLocator=" + fieldLocator +
                        '}';
            }
        }
    }
}
