package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.LegalTrivialStackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.utility.ByteBuddyCommons;
import org.objectweb.asm.MethodVisitor;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.isGetter;
import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.isSetter;
import static com.blogspot.mydailyjava.bytebuddy.utility.ByteBuddyCommons.isValidIdentifier;
import static com.blogspot.mydailyjava.bytebuddy.utility.ByteBuddyCommons.resolveModifierContributors;

public abstract class FieldAccessor {

    public static interface AssignerConfigurable extends Instrumentation {

        Instrumentation assigner(Assigner assigner, boolean considerRuntimeType);
    }

    public static interface OwnerTypeLocatable extends AssignerConfigurable {

        AssignerConfigurable in(TypeDescription typeDescription);
    }

    public static interface FieldDefinable extends OwnerTypeLocatable {

        AssignerConfigurable defineAs(Class<?> type, ModifierContributor.ForField... modifier);
    }

    public static FieldDefinable ofField(String name) {
        return new ForNamedField(name, defaultAssigner(), defaultConsiderRuntimeType());
    }

    public static OwnerTypeLocatable ofBeanProperty() {
        return new ForBeanProperty(defaultAssigner(), defaultConsiderRuntimeType());
    }

    private static Assigner defaultAssigner() {
        return new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE);
    }

    private static boolean defaultConsiderRuntimeType() {
        return false;
    }

    protected static class ForBeanProperty extends FieldAccessor implements OwnerTypeLocatable {

        private final FieldLocator.Factory fieldLocatorFactory;

        protected ForBeanProperty(Assigner assigner, boolean considerRuntimeType) {
            super(assigner, considerRuntimeType);
            fieldLocatorFactory = FieldLocator.ForInstrumentedTypeHierarchy.Factory.INSTANCE;
        }

        public ForBeanProperty(Assigner assigner, boolean considerRuntimeType, FieldLocator.Factory fieldLocatorFactory) {
            super(assigner, considerRuntimeType);
            this.fieldLocatorFactory = fieldLocatorFactory;
        }

        @Override
        public AssignerConfigurable in(TypeDescription typeDescription) {
            return new ForBeanProperty(assigner, considerRuntimeType, new FieldLocator.ForGivenType(typeDescription));
        }

        @Override
        public Instrumentation assigner(Assigner assigner, boolean considerRuntimeType) {
            return new ForBeanProperty(assigner, considerRuntimeType);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return new Appender(fieldLocatorFactory.make(instrumentedType));
        }

        @Override
        protected String getFieldName(MethodDescription targetMethod) {
            String name = targetMethod.getInternalName();
            name = name.startsWith("is") ? name.substring(2) : name.substring(3);
            if (name.length() == 0) {
                throw new IllegalArgumentException(targetMethod + " does not specify a bean name");
            }
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass()) && super.equals(other)
                    && fieldLocatorFactory.equals(((ForBeanProperty) other).fieldLocatorFactory);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + fieldLocatorFactory.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAccessor.ForBeanProperty{" +
                    "fieldLocatorFactory=" + fieldLocatorFactory +
                    '}';
        }
    }

    protected static class ForNamedField extends FieldAccessor implements FieldDefinable {

        private static interface PreparationHandler {

            static enum NoOp implements PreparationHandler {
                INSTANCE;

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class FieldDefiner implements PreparationHandler {

                private final String name;
                private final TypeDescription typeDescription;
                private final int modifiers;

                public FieldDefiner(String name, Class<?> type, ModifierContributor.ForField... contributor) {
                    this.name = isValidIdentifier(name);
                    typeDescription = new TypeDescription.ForLoadedType(type);
                    modifiers = resolveModifierContributors(ByteBuddyCommons.FIELD_MODIFIER_MASK, contributor);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType.withField(name, typeDescription, modifiers);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    FieldDefiner that = (FieldDefiner) other;
                    return modifiers == that.modifiers
                            && name.equals(that.name)
                            && typeDescription.equals(that.typeDescription);
                }

                @Override
                public int hashCode() {
                    int result = name.hashCode();
                    result = 31 * result + typeDescription.hashCode();
                    result = 31 * result + modifiers;
                    return result;
                }

                @Override
                public String toString() {
                    return "FieldAccessor.ForNamedField.PreparationHandler.FieldDefiner{" +
                            "name='" + name + '\'' +
                            ", typeDescription=" + typeDescription +
                            ", modifiers=" + modifiers +
                            '}';
                }
            }

            InstrumentedType prepare(InstrumentedType instrumentedType);
        }

        private final String fieldName;
        private final PreparationHandler preparationHandler;
        private final FieldLocator.Factory fieldLocatorFactory;

        protected ForNamedField(String fieldName,
                                Assigner assigner,
                                boolean considerRuntimeType) {
            super(assigner, considerRuntimeType);
            this.fieldName = fieldName;
            preparationHandler = PreparationHandler.NoOp.INSTANCE;
            fieldLocatorFactory = FieldLocator.ForInstrumentedTypeHierarchy.Factory.INSTANCE;
        }

        private ForNamedField(String fieldName,
                              PreparationHandler preparationHandler,
                              FieldLocator.Factory fieldLocatorFactory,
                              Assigner assigner,
                              boolean considerRuntimeType) {
            super(assigner, considerRuntimeType);
            this.fieldLocatorFactory = fieldLocatorFactory;
            this.fieldName = fieldName;
            this.preparationHandler = preparationHandler;
        }

        @Override
        public AssignerConfigurable defineAs(Class<?> type, ModifierContributor.ForField... modifier) {
            return new ForNamedField(fieldName,
                    new PreparationHandler.FieldDefiner(fieldName, type, modifier),
                    FieldLocator.ForInstrumentedType.INSTANCE,
                    assigner,
                    considerRuntimeType);
        }

        @Override
        public AssignerConfigurable in(TypeDescription typeDescription) {
            return new ForNamedField(fieldName,
                    preparationHandler,
                    new FieldLocator.ForGivenType(typeDescription),
                    assigner,
                    considerRuntimeType);
        }

        @Override
        public Instrumentation assigner(Assigner assigner, boolean considerRuntimeType) {
            return new ForNamedField(fieldName, preparationHandler, fieldLocatorFactory, assigner, considerRuntimeType);
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return preparationHandler.prepare(instrumentedType);
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return new Appender(fieldLocatorFactory.make(instrumentedType));
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
                    && fieldName.equals(that.fieldName)
                    && preparationHandler.equals(that.preparationHandler);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + fieldName.hashCode();
            result = 31 * result + preparationHandler.hashCode();
            result = 31 * result + fieldLocatorFactory.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "FieldAccessor.ForNamedField{" +
                    "fieldName='" + fieldName + '\'' +
                    ", preparationHandler=" + preparationHandler +
                    ", fieldLocatorFactory=" + fieldLocatorFactory +
                    '}';
        }
    }

    protected static interface FieldLocator {

        static interface Factory {

            FieldLocator make(TypeDescription instrumentedType);
        }

        static class ForInstrumentedTypeHierarchy implements FieldLocator {

            public static enum Factory implements FieldLocator.Factory {
                INSTANCE;

                @Override
                public FieldLocator make(TypeDescription instrumentedType) {
                    return new ForInstrumentedTypeHierarchy(instrumentedType);
                }
            }

            private final TypeDescription instrumentedType;

            public ForInstrumentedTypeHierarchy(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            @Override
            public FieldDescription locate(String name) {
                TypeDescription currentType = instrumentedType;
                boolean isSelf = true;
                do {
                    for (FieldDescription fieldDescription : currentType.getDeclaredFields()) {
                        if (fieldDescription.getName().equals(name)
                                && (isSelf || !fieldDescription.isPrivate())
                                && (!fieldDescription.isPackagePrivate() || fieldDescription.isVisibleTo(instrumentedType))) {
                            return fieldDescription;
                        }
                    }
                    isSelf = false;
                } while (!(currentType = currentType.getSupertype()).represents(Object.class));
                throw new IllegalArgumentException("There is no field " + name + " that is visible for " + instrumentedType);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentedType.equals(((ForInstrumentedTypeHierarchy) other).instrumentedType);
            }

            @Override
            public int hashCode() {
                return instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "FieldLocator.ForInstrumentedTypeHierarchy{instrumentedType=" + instrumentedType + '}';
            }
        }

        static class ForGivenType implements FieldLocator, Factory {

            private final TypeDescription targetType;

            public ForGivenType(TypeDescription targetType) {
                this.targetType = targetType;
            }

            @Override
            public FieldLocator make(TypeDescription instrumentedType) {
                return this;
            }

            @Override
            public FieldDescription locate(String name) {
                return targetType.getDeclaredFields().named(name);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && targetType.equals(((ForGivenType) other).targetType);
            }

            @Override
            public int hashCode() {
                return targetType.hashCode();
            }

            @Override
            public String toString() {
                return "FieldLocator.ForGivenType{targetType=" + targetType + '}';
            }
        }

        static enum ForInstrumentedType implements Factory {
            INSTANCE;

            @Override
            public FieldLocator make(TypeDescription instrumentedType) {
                return new ForGivenType(instrumentedType);
            }
        }

        FieldDescription locate(String name);
    }

    protected class Appender implements ByteCodeAppender {

        private final FieldLocator fieldLocator;

        private Appender(FieldLocator fieldLocator) {
            this.fieldLocator = fieldLocator;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            if (isGetter().matches(instrumentedMethod)) {
                return applyGetter(methodVisitor,
                        instrumentationContext,
                        fieldLocator.locate(getFieldName(instrumentedMethod)),
                        instrumentedMethod);
            } else if (isSetter().matches(instrumentedMethod)) {
                return applySetter(methodVisitor,
                        instrumentationContext,
                        fieldLocator.locate(getFieldName(instrumentedMethod)),
                        instrumentedMethod);
            } else {
                throw new IllegalArgumentException("Method " + instrumentationContext + " is no bean property");
            }
        }

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

    protected final Assigner assigner;
    protected final boolean considerRuntimeType;

    protected FieldAccessor(Assigner assigner, boolean considerRuntimeType) {
        this.assigner = assigner;
        this.considerRuntimeType = considerRuntimeType;
    }

    protected ByteCodeAppender.Size applyGetter(MethodVisitor methodVisitor,
                                                Instrumentation.Context instrumentationContext,
                                                FieldDescription fieldDescription,
                                                MethodDescription methodDescription) {
        return apply(methodVisitor,
                instrumentationContext,
                fieldDescription,
                methodDescription,
                new StackManipulation.Compound(
                        FieldAccess.forField(fieldDescription).getter(),
                        assigner.assign(fieldDescription.getFieldType(),
                                methodDescription.getReturnType(),
                                considerRuntimeType)
                )
        );
    }

    protected ByteCodeAppender.Size applySetter(MethodVisitor methodVisitor,
                                                Instrumentation.Context instrumentationContext,
                                                FieldDescription fieldDescription,
                                                MethodDescription methodDescription) {
        if (fieldDescription.isFinal()) {
            throw new IllegalArgumentException("Cannot apply setter on final field " + fieldDescription);
        }
        return apply(methodVisitor,
                instrumentationContext,
                fieldDescription,
                methodDescription,
                new StackManipulation.Compound(
                        MethodVariableAccess.forType(fieldDescription.getFieldType())
                                .loadFromIndex(methodDescription.getParameterOffset(0)),
                        assigner.assign(methodDescription.getParameterTypes().get(0),
                                fieldDescription.getFieldType(),
                                considerRuntimeType),
                        FieldAccess.forField(fieldDescription).putter()
                )
        );
    }

    private ByteCodeAppender.Size apply(MethodVisitor methodVisitor,
                                        Instrumentation.Context instrumentationContext,
                                        FieldDescription fieldDescription,
                                        MethodDescription methodDescription,
                                        StackManipulation fieldAccess) {
        if (methodDescription.isStatic() && !fieldDescription.isStatic()) {
            throw new IllegalArgumentException("Cannot call instance field "
                    + fieldDescription + " from static method " + methodDescription);
        }
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                fieldDescription.isStatic()
                        ? LegalTrivialStackManipulation.INSTANCE
                        : MethodVariableAccess.REFERENCE.loadFromIndex(0),
                fieldAccess,
                MethodReturn.returning(methodDescription.getReturnType())
        ).apply(methodVisitor, instrumentationContext);
        return new ByteCodeAppender.Size(stackSize.getMaximalSize(), methodDescription.getStackSize());
    }

    protected abstract String getFieldName(MethodDescription targetMethod);

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && considerRuntimeType == ((FieldAccessor) other).considerRuntimeType
                && assigner.equals(((FieldAccessor) other).assigner);
    }

    @Override
    public int hashCode() {
        return 31 * assigner.hashCode() + (considerRuntimeType ? 1 : 0);
    }
}
