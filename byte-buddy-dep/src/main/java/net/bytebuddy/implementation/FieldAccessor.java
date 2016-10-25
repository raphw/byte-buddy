package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.MethodVisitor;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Defines a method to access a given field by following the Java bean conventions for getters and setters:
 * <ul>
 * <li>Getter: A method named {@code getFoo()} will be instrumented to read and return the value of a field {@code foo}
 * or another field if one was specified explicitly. If a property is of type {@link java.lang.Boolean} or
 * {@code boolean}, the name {@code isFoo()} is also permitted.</li>
 * <li>Setter: A method named {@code setFoo(value)} will be instrumented to write the given argument {@code value}
 * to a field {@code foo} or to another field if one was specified explicitly.</li>
 * </ul>
 */
public abstract class FieldAccessor implements Implementation {

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
     * @param assigner The assigner to use.
     * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected FieldAccessor(Assigner assigner, Assigner.Typing typing) {
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
        return new ForNamedField(Assigner.DEFAULT, Assigner.Typing.STATIC, name);
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
        return new ForUnnamedField(Assigner.DEFAULT, Assigner.Typing.STATIC, fieldNameExtractor);
    }

    /**
     * Applies a field getter implementation.
     *
     * @param methodVisitor         The method visitor to write any instructions to.
     * @param implementationContext The implementation context of the current implementation.
     * @param fieldDescription      The description of the field to read.
     * @param methodDescription     The method that is target of the implementation.
     * @return The required size of the operand stack and local variable array for this implementation.
     */
    protected ByteCodeAppender.Size applyGetter(MethodVisitor methodVisitor,
                                                Implementation.Context implementationContext,
                                                FieldDescription fieldDescription,
                                                MethodDescription methodDescription) {
        StackManipulation stackManipulation = assigner.assign(fieldDescription.getType(), methodDescription.getReturnType(), typing);
        if (!stackManipulation.isValid()) {
            throw new IllegalStateException("Getter type of " + methodDescription + " is not compatible with " + fieldDescription);
        }
        return apply(methodVisitor,
                implementationContext,
                fieldDescription,
                methodDescription,
                new StackManipulation.Compound(
                        FieldAccess.forField(fieldDescription).getter(),
                        stackManipulation
                )
        );
    }

    /**
     * Applies a field setter implementation.
     *
     * @param methodVisitor         The method visitor to write any instructions to.
     * @param implementationContext The implementation context of the current implementation.
     * @param fieldDescription      The description of the field to write to.
     * @param methodDescription     The method that is target of the instrumentation.
     * @return The required size of the operand stack and local variable array for this implementation.
     */
    protected ByteCodeAppender.Size applySetter(MethodVisitor methodVisitor,
                                                Implementation.Context implementationContext,
                                                FieldDescription fieldDescription,
                                                MethodDescription methodDescription) {
        StackManipulation stackManipulation = assigner.assign(methodDescription.getParameters().get(0).getType(), fieldDescription.getType(), typing);
        if (!stackManipulation.isValid()) {
            throw new IllegalStateException("Setter type of " + methodDescription + " is not compatible with " + fieldDescription);
        } else if (fieldDescription.isFinal()) {
            throw new IllegalArgumentException("Cannot apply setter on final field " + fieldDescription);
        }
        return apply(methodVisitor,
                implementationContext,
                fieldDescription,
                methodDescription,
                new StackManipulation.Compound(
                        MethodVariableAccess.of(fieldDescription.getType().asErasure())
                                .loadOffset(methodDescription.getParameters().get(0).getOffset()),
                        stackManipulation,
                        FieldAccess.forField(fieldDescription).putter()
                )
        );
    }

    /**
     * A generic implementation of the application of a {@link net.bytebuddy.implementation.bytecode.ByteCodeAppender}.
     *
     * @param methodVisitor         The method visitor to write any instructions to.
     * @param implementationContext The implementation context of the current implementation.
     * @param fieldDescription      The description of the field to access.
     * @param methodDescription     The method that is target of the implementation.
     * @param fieldAccess           The manipulation that represents the field access.
     * @return A suitable {@link net.bytebuddy.implementation.bytecode.ByteCodeAppender}.
     */
    private ByteCodeAppender.Size apply(MethodVisitor methodVisitor,
                                        Implementation.Context implementationContext,
                                        FieldDescription fieldDescription,
                                        MethodDescription methodDescription,
                                        StackManipulation fieldAccess) {
        if (methodDescription.isStatic() && !fieldDescription.isStatic()) {
            throw new IllegalArgumentException("Cannot call instance field "
                    + fieldDescription + " from static method " + methodDescription);
        }
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                fieldDescription.isStatic()
                        ? StackManipulation.Trivial.INSTANCE
                        : MethodVariableAccess.REFERENCE.loadOffset(0),
                fieldAccess,
                MethodReturn.of(methodDescription.getReturnType().asErasure())
        ).apply(methodVisitor, implementationContext);
        return new ByteCodeAppender.Size(stackSize.getMaximalSize(), methodDescription.getStackSize());
    }

    /**
     * Locates a field's name.
     *
     * @param targetMethod The method that is target of the implementation.
     * @return The name of the field to be located for this implementation.
     */
    protected abstract String getFieldName(MethodDescription targetMethod);

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typing == ((FieldAccessor) other).typing
                && assigner.equals(((FieldAccessor) other).assigner);
    }

    @Override
    public int hashCode() {
        return 31 * assigner.hashCode() + typing.hashCode();
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
        String fieldNameFor(MethodDescription methodDescription);

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
            public String fieldNameFor(MethodDescription methodDescription) {
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
    }

    /**
     * A field accessor that can be configured to use a given assigner and runtime type use configuration.
     */
    public interface AssignerConfigurable extends Implementation {

        /**
         * Returns a field accessor that is identical to this field accessor but uses the given assigner
         * and runtime type use configuration.
         *
         * @param assigner The assigner to use.
         * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @return This field accessor with the given assigner and runtime type use configuration.
         */
        Implementation withAssigner(Assigner assigner, Assigner.Typing typing);
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
         * @param factory A factory that will produce a field locator that will be used to find locate
         *                a field to be accessed.
         * @return This field accessor which will only considered fields that are defined in the given type.
         */
        AssignerConfigurable in(FieldLocator.Factory factory);
    }

    /**
     * Implementation of a field accessor implementation where a field is identified by a method's name following
     * the Java specification for bean properties.
     */
    protected static class ForUnnamedField extends FieldAccessor implements OwnerTypeLocatable {

        /**
         * A factory for creating a field locator for implementing this field accessor.
         */
        private final FieldLocator.Factory fieldLocatorFactory;

        /**
         * The field name extractor to be used.
         */
        private final FieldNameExtractor fieldNameExtractor;

        /**
         * Creates a new field accessor implementation.
         *
         * @param assigner           The assigner to use.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param fieldNameExtractor The field name extractor to use.
         */
        protected ForUnnamedField(Assigner assigner, Assigner.Typing typing, FieldNameExtractor fieldNameExtractor) {
            this(assigner, typing, fieldNameExtractor, FieldLocator.ForClassHierarchy.Factory.INSTANCE);
        }

        /**
         * Creates a new field accessor implementation.
         *
         * @param assigner            The assigner to use.
         * @param typing              Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param fieldNameExtractor  The field name extractor to use.
         * @param fieldLocatorFactory A factory that will produce a field locator that will be used to find locate
         *                            a field to be accessed.
         */
        protected ForUnnamedField(Assigner assigner,
                                  Assigner.Typing typing,
                                  FieldNameExtractor fieldNameExtractor,
                                  FieldLocator.Factory fieldLocatorFactory) {
            super(assigner, typing);
            this.fieldNameExtractor = fieldNameExtractor;
            this.fieldLocatorFactory = fieldLocatorFactory;
        }

        @Override
        public AssignerConfigurable in(FieldLocator.Factory factory) {
            return new ForUnnamedField(assigner, typing, fieldNameExtractor, factory);
        }

        @Override
        public AssignerConfigurable in(Class<?> type) {
            return in(new TypeDescription.ForLoadedType(type));
        }

        @Override
        public AssignerConfigurable in(TypeDescription typeDescription) {
            return typeDescription.represents(TargetType.class)
                    ? in(FieldLocator.ForClassHierarchy.Factory.INSTANCE)
                    : in(new FieldLocator.ForExactType.Factory(typeDescription));
        }

        @Override
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForUnnamedField(assigner, typing, fieldNameExtractor, fieldLocatorFactory);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(fieldLocatorFactory.make(implementationTarget.getInstrumentedType()));
        }

        @Override
        protected String getFieldName(MethodDescription targetMethod) {
            return fieldNameExtractor.fieldNameFor(targetMethod);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && super.equals(other)
                    && fieldNameExtractor.equals(((ForUnnamedField) other).fieldNameExtractor)
                    && fieldLocatorFactory.equals(((ForUnnamedField) other).fieldLocatorFactory);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * super.hashCode() + fieldLocatorFactory.hashCode()) + fieldNameExtractor.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAccessor.ForUnnamedField{" +
                    "assigner=" + assigner +
                    ", typing=" + typing +
                    ", fieldLocatorFactory=" + fieldLocatorFactory +
                    ", fieldNameExtractor=" + fieldNameExtractor +
                    '}';
        }
    }

    /**
     * Implementation of a field accessor implementation where the field name is given explicitly.
     */
    protected static class ForNamedField extends FieldAccessor implements OwnerTypeLocatable {

        /**
         * The name of the field that is accessed.
         */
        private final String fieldName;

        /**
         * The field locator factory for implementing this field accessor.
         */
        private final FieldLocator.Factory fieldLocatorFactory;

        /**
         * Creates a field accessor implementation for a field of a given name.
         *
         * @param assigner  The assigner to use.
         * @param typing    Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param fieldName The name of the field.
         */
        protected ForNamedField(Assigner assigner, Assigner.Typing typing, String fieldName) {
            super(assigner, typing);
            this.fieldName = fieldName;
            fieldLocatorFactory = FieldLocator.ForClassHierarchy.Factory.INSTANCE;
        }

        /**
         * reates a field accessor implementation for a field of a given name.
         *
         * @param fieldName           The name of the field.
         * @param typing              Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @param fieldLocatorFactory A factory that will produce a field locator that will be used to find locate
         *                            a field to be accessed.
         * @param assigner            The assigner to use.
         */
        private ForNamedField(Assigner assigner,
                              Assigner.Typing typing,
                              String fieldName,
                              FieldLocator.Factory fieldLocatorFactory) {
            super(assigner, typing);
            this.fieldName = fieldName;
            this.fieldLocatorFactory = fieldLocatorFactory;
        }

        @Override
        public AssignerConfigurable in(FieldLocator.Factory factory) {
            return new ForNamedField(assigner,
                    typing,
                    fieldName,
                    factory);
        }

        @Override
        public AssignerConfigurable in(Class<?> type) {
            return in(new TypeDescription.ForLoadedType(type));
        }

        @Override
        public AssignerConfigurable in(TypeDescription typeDescription) {
            return typeDescription.represents(TargetType.class)
                    ? in(FieldLocator.ForClassHierarchy.Factory.INSTANCE)
                    : in(new FieldLocator.ForExactType.Factory(typeDescription));
        }

        @Override
        public Implementation withAssigner(Assigner assigner, Assigner.Typing typing) {
            return new ForNamedField(assigner,
                    typing,
                    fieldName,
                    fieldLocatorFactory);
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(fieldLocatorFactory.make(implementationTarget.getInstrumentedType()));
        }

        @Override
        protected String getFieldName(MethodDescription targetMethod) {
            return fieldName;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (!super.equals(other)) return false;
            ForNamedField that = (ForNamedField) other;
            return fieldLocatorFactory.equals(that.fieldLocatorFactory)
                    && fieldName.equals(that.fieldName);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + fieldName.hashCode();
            result = 31 * result + fieldLocatorFactory.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "FieldAccessor.ForNamedField{" +
                    "assigner=" + assigner +
                    ", fieldName='" + fieldName + '\'' +
                    ", typing=" + typing +
                    ", fieldLocatorFactory=" + fieldLocatorFactory +
                    '}';
        }
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
        public Size apply(MethodVisitor methodVisitor,
                          Implementation.Context implementationContext,
                          MethodDescription instrumentedMethod) {
            if (isConstructor().matches(instrumentedMethod)) {
                throw new IllegalArgumentException("Constructors cannot define beans: " + instrumentedMethod);
            }
            FieldLocator.Resolution resolution = fieldLocator.locate(getFieldName(instrumentedMethod));
            if (!resolution.isResolved() || (instrumentedMethod.isStatic() && !resolution.getField().isStatic())) {
                throw new IllegalStateException("Cannot locate accessible field for " + instrumentedMethod);
            }
            if (takesArguments(0).and(not(returns(void.class))).matches(instrumentedMethod)) {
                return applyGetter(methodVisitor,
                        implementationContext,
                        resolution.getField(),
                        instrumentedMethod);
            } else if (takesArguments(1).and(returns(void.class)).matches(instrumentedMethod)) {
                return applySetter(methodVisitor,
                        implementationContext,
                        resolution.getField(),
                        instrumentedMethod);
            } else {
                throw new IllegalArgumentException("Method " + implementationContext + " is no bean property");
            }
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private FieldAccessor getFieldAccessor() {
            return FieldAccessor.this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && fieldLocator.equals(((Appender) other).fieldLocator)
                    && FieldAccessor.this.equals(((Appender) other).getFieldAccessor());
        }

        @Override
        public int hashCode() {
            return 31 * FieldAccessor.this.hashCode() + fieldLocator.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAccessor.Appender{" +
                    "fieldLocator=" + fieldLocator +
                    "fieldAccessor=" + FieldAccessor.this +
                    '}';
        }
    }
}
