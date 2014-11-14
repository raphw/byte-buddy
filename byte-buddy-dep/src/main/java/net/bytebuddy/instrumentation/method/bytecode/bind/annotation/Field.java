package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.annotation.*;
import java.util.Collections;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * Using this annotation it is possible to access fields by getter and setter types. Before this annotation can be
 * used, it needs to be installed with two types. The getter type must be defined in a single-method interface
 * with a single method that returns an {@link java.lang.Object} type and takes no arguments. The getter interface
 * must similarly return {@code void} and take a single {@link java.lang.Object} argument. After installing these
 * interfaces with the {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Field.Binder}, this
 * binder needs to be registered with a {@link net.bytebuddy.instrumentation.MethodDelegation} before it can be used.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Field {

    /**
     * A placeholder name to indicate that a field name should be inferred by the name of the intercepted
     * method by the Java bean naming conventions.
     */
    static final String BEAN_PROPERTY = "";

    /**
     * Determines if the proxy should be serializable.
     *
     * @return {@code true} if the proxy should be serializable.
     */
    boolean serializableProxy() default false;

    /**
     * Determines the name of the field that is to be accessed. If this property is not set, a field name is inferred
     * by the intercepted method after the Java beans naming conventions.
     *
     * @return The name of the field to be accessed.
     */
    String value() default "";

    /**
     * Determines which type defines the field that is to be accessed. If this property is not set, the most field
     * that is defined highest in the type hierarchy is accessed.
     *
     * @return The type that defines the accessed field.
     */
    Class<?> definingType() default void.class;

    /**
     * A binder for the {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Field} annotation.
     */
    static class Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Field> {

        private static final MethodDescription DEFINING_TYPE;
        private static final MethodDescription FIELD_NAME;
        private static final MethodDescription SERIALIZABLE_PROXY;

        static {
            MethodList methodList = new TypeDescription.ForLoadedType(Field.class).getDeclaredMethods();
            DEFINING_TYPE = methodList.filter(named("definingType")).getOnly();
            FIELD_NAME = methodList.filter(named("value")).getOnly();
            SERIALIZABLE_PROXY = methodList.filter(named("serializableProxy")).getOnly();
        }

        private final MethodDescription getterMethod;
        private final MethodDescription setterMethod;

        protected Binder(MethodDescription getterMethod, MethodDescription setterMethod) {
            this.getterMethod = getterMethod;
            this.setterMethod = setterMethod;
        }

        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Field> install(Class<?> getterType,
                                                                                        Class<?> setterType) {
            return install(new TypeDescription.ForLoadedType(nonNull(getterType)), new TypeDescription.ForLoadedType(nonNull(setterType)));
        }

        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Field> install(TypeDescription getterType,
                                                                                        TypeDescription setterType) {
            MethodDescription getterMethod = onlyMethod(nonNull(getterType));
            if (!getterMethod.getReturnType().represents(Object.class)) {
                throw new IllegalArgumentException(getterMethod + " must take a single Object-typed parameter");
            } else if (getterMethod.getParameterTypes().size() != 0) {
                throw new IllegalArgumentException(getterMethod + " must not declare parameters");
            }
            MethodDescription setterMethod = onlyMethod(nonNull(setterType));
            if (!setterMethod.getReturnType().represents(void.class)) {
                throw new IllegalArgumentException(setterMethod + " must return void");
            } else if (setterMethod.getParameterTypes().size() != 1 || !setterMethod.getParameterTypes().get(0).represents(Object.class)) {
                throw new IllegalArgumentException(setterMethod + " must declare a single Object-typed parameters");
            }
            return new Binder(getterMethod, setterMethod);
        }

        private static MethodDescription onlyMethod(TypeDescription typeDescription) {
            if (!typeDescription.isInterface()) {
                throw new IllegalArgumentException(typeDescription + " is not an interface");
            } else if (typeDescription.getInterfaces().size() > 0) {
                throw new IllegalArgumentException(typeDescription + " must not extend other interfaces");
            } else if (!typeDescription.isPublic()) {
                throw new IllegalArgumentException(typeDescription + " is mot public");
            }
            MethodList methodCandidates = typeDescription.getDeclaredMethods().filter(not(isStatic()));
            if (methodCandidates.size() != 1) {
                throw new IllegalArgumentException(typeDescription + " must declare exactly one non-static method");
            }
            return methodCandidates.getOnly();
        }

        @Override
        public Class<Field> getHandledType() {
            return Field.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Field> annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            TypeDescription parameterType = target.getParameterTypes().get(targetParameterIndex);
            AccessType accessType;
            if (parameterType.equals(getterMethod.getDeclaringType())) {
                accessType = AccessType.GETTER;
            } else if (parameterType.equals(setterMethod.getDeclaringType())) {
                accessType = AccessType.SETTER;
            } else {
                throw new IllegalStateException(target + " uses a @Field annotation on an non-installed type");
            }
            FieldLocator.Resolution resolution = FieldLocator.of(annotation.getValue(FIELD_NAME, String.class), source)
                    .lookup(annotation.getValue(DEFINING_TYPE, TypeDescription.class), instrumentationTarget.getTypeDescription())
                    .resolve(instrumentationTarget.getTypeDescription());
            return resolution.isResolved()
                    ? new MethodDelegationBinder.ParameterBinding.Anonymous(new AccessorProxy(
                    resolution.getFieldDescription(),
                    assigner,
                    instrumentationTarget.getTypeDescription(),
                    accessType,
                    annotation.getValue(SERIALIZABLE_PROXY, Boolean.class)))
                    : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && getterMethod.equals(((Binder) other).getterMethod)
                    && setterMethod.equals(((Binder) other).setterMethod);
        }

        @Override
        public int hashCode() {
            int result = getterMethod.hashCode();
            result = 31 * result + setterMethod.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Field.Binder{" +
                    "getterMethod=" + getterMethod +
                    ", setterMethod=" + setterMethod +
                    '}';
        }

        protected static enum AccessType {

            GETTER {
                @Override
                protected TypeDescription proxyType(MethodDescription getterMethod, MethodDescription setterMethod) {
                    return getterMethod.getDeclaringType();
                }

                @Override
                protected Instrumentation access(FieldDescription fieldDescription,
                                                 Assigner assigner,
                                                 AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                    return new Getter(fieldDescription, assigner, methodAccessorFactory);
                }
            },

            SETTER {
                @Override
                protected TypeDescription proxyType(MethodDescription getterMethod, MethodDescription setterMethod) {
                    return setterMethod.getDeclaringType();
                }

                @Override
                protected Instrumentation access(FieldDescription fieldDescription,
                                                 Assigner assigner,
                                                 AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                    return new Setter(fieldDescription, assigner, methodAccessorFactory);
                }
            };

            protected abstract TypeDescription proxyType(MethodDescription getterMethod, MethodDescription setterMethod);

            protected abstract Instrumentation access(FieldDescription fieldDescription,
                                                      Assigner assigner,
                                                      AuxiliaryType.MethodAccessorFactory methodAccessorFactory);
        }

        private static enum StaticFieldConstructor implements Instrumentation {

            INSTANCE;

            /**
             * A reference of the {@link Object} type default constructor.
             */
            protected final MethodDescription objectTypeDefaultConstructor;

            /**
             * Creates the constructor call singleton.
             */
            private StaticFieldConstructor() {
                objectTypeDefaultConstructor = new TypeDescription.ForLoadedType(Object.class)
                        .getDeclaredMethods()
                        .filter(isConstructor())
                        .getOnly();
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public ByteCodeAppender appender(Target instrumentationTarget) {
                return new ByteCodeAppender.Simple(MethodVariableAccess.REFERENCE.loadFromIndex(0),
                        MethodInvocation.invoke(objectTypeDefaultConstructor),
                        MethodReturn.VOID);
            }
        }

        protected abstract static class FieldLocator {

            protected static FieldLocator of(String fieldName, MethodDescription methodDescription) {
                return BEAN_PROPERTY.equals(fieldName)
                        ? Legal.consider(methodDescription)
                        : new Legal(fieldName);
            }

            protected abstract LookupEngine lookup(TypeDescription typeDescription, TypeDescription instrumentedTyoe);

            protected abstract static class Resolution {

                protected abstract boolean isResolved();

                protected abstract FieldDescription getFieldDescription();

                protected static class Unresolved extends Resolution {

                    @Override
                    protected boolean isResolved() {
                        return false;
                    }

                    @Override
                    protected FieldDescription getFieldDescription() {
                        throw new IllegalStateException("Cannot resolve an unresolved field lookup");
                    }

                    @Override
                    public int hashCode() {
                        return 17;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return other == this || (other != null && other.getClass() == getClass());
                    }

                    @Override
                    public String toString() {
                        return "Field.Binder.FieldLocator.Resolution.Unresolved{}";
                    }
                }

                protected static class Resolved extends Resolution {

                    private final FieldDescription fieldDescription;

                    protected Resolved(FieldDescription fieldDescription) {
                        this.fieldDescription = fieldDescription;
                    }

                    @Override
                    protected boolean isResolved() {
                        return true;
                    }

                    @Override
                    protected FieldDescription getFieldDescription() {
                        return fieldDescription;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && fieldDescription.equals(((Resolved) other).fieldDescription);
                    }

                    @Override
                    public int hashCode() {
                        return fieldDescription.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "Field.Binder.FieldLocator.Resolution.Resolved{" +
                                "fieldDescription=" + fieldDescription +
                                '}';
                    }
                }
            }

            protected abstract static class LookupEngine {

                protected abstract Resolution resolve(TypeDescription instrumentedType);

                protected static class Illegal extends LookupEngine {

                    @Override
                    protected Resolution resolve(TypeDescription instrumentedType) {
                        return new Resolution.Unresolved();
                    }

                    @Override
                    public int hashCode() {
                        return 17;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return other == this || (other != null && other.getClass() == getClass());
                    }

                    @Override
                    public String toString() {
                        return "Field.Binder.FieldLocator.LookupEngine.Illegal{}";
                    }
                }

                protected static class ForHierarchy extends LookupEngine {

                    private final String fieldName;

                    protected ForHierarchy(String fieldName) {
                        this.fieldName = fieldName;
                    }

                    @Override
                    protected Resolution resolve(TypeDescription instrumentedType) {
                        TypeDescription currentType = instrumentedType;
                        do {
                            for (FieldDescription fieldDescription : currentType.getDeclaredFields()) {
                                if (fieldDescription.getInternalName().equals(fieldName) && fieldDescription.isVisibleTo(instrumentedType)) {
                                    return new Resolution.Resolved(fieldDescription);
                                }
                            }
                        } while ((currentType = currentType.getSupertype()) != null);
                        return new Resolution.Unresolved();
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && fieldName.equals(((ForHierarchy) other).fieldName);
                    }

                    @Override
                    public int hashCode() {
                        return fieldName.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "Field.Binder.FieldLocator.LookupEngine.ForHierarchy{" +
                                "fieldName='" + fieldName + '\'' +
                                '}';
                    }
                }

                protected static class ForExplicitType extends LookupEngine {

                    private final String fieldName;

                    private final TypeDescription typeDescription;

                    protected ForExplicitType(String fieldName, TypeDescription typeDescription) {
                        this.fieldName = fieldName;
                        this.typeDescription = typeDescription;
                    }

                    @Override
                    protected Resolution resolve(TypeDescription instrumentedType) {
                        for (FieldDescription fieldDescription : typeDescription.getDeclaredFields()) {
                            if (fieldDescription.getInternalName().equals(fieldName)) {
                                return fieldDescription.isVisibleTo(instrumentedType)
                                        ? new Resolution.Resolved(fieldDescription)
                                        : new Resolution.Unresolved();
                            }
                        }
                        return new Resolution.Unresolved();
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && fieldName.equals(((ForExplicitType) other).fieldName)
                                && typeDescription.equals(((ForExplicitType) other).typeDescription);
                    }

                    @Override
                    public int hashCode() {
                        int result = fieldName.hashCode();
                        result = 31 * result + typeDescription.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "Field.Binder.FieldLocator.LookupEngine.ForExplicitType{" +
                                "fieldName='" + fieldName + '\'' +
                                ", typeDescription=" + typeDescription +
                                '}';
                    }
                }
            }

            protected static class Legal extends FieldLocator {

                private final String fieldName;

                protected Legal(String fieldName) {
                    this.fieldName = fieldName;
                }

                protected static FieldLocator consider(MethodDescription methodDescription) {
                    String fieldName;
                    if (isSetter().matches(methodDescription)) {
                        fieldName = methodDescription.getInternalName().substring(3);
                    } else if (isGetter().matches(methodDescription)) {
                        fieldName = methodDescription.getInternalName()
                                .substring(methodDescription.getInternalName().startsWith("is") ? 2 : 3);
                    } else {
                        return new Illegal();
                    }
                    return new Legal(Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1));
                }

                @Override
                protected LookupEngine lookup(TypeDescription typeDescription, TypeDescription instrumentedType) {
                    return typeDescription.represents(void.class)
                            ? new LookupEngine.ForHierarchy(fieldName)
                            : new LookupEngine.ForExplicitType(fieldName,
                            typeDescription.represents(TargetType.class) ? instrumentedType : typeDescription);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && fieldName.equals(((Legal) other).fieldName);
                }

                @Override
                public int hashCode() {
                    return fieldName.hashCode();
                }

                @Override
                public String toString() {
                    return "Field.Binder.FieldLocator.Legal{" +
                            "fieldName='" + fieldName + '\'' +
                            '}';
                }
            }

            protected static class Illegal extends FieldLocator {

                @Override
                protected LookupEngine lookup(TypeDescription typeDescription, TypeDescription instrumentedType) {
                    return new LookupEngine.Illegal();
                }

                @Override
                public int hashCode() {
                    return 31;
                }

                @Override
                public boolean equals(Object other) {
                    return other == this || (other != null && other.getClass() == getClass());
                }

                @Override
                public String toString() {
                    return "Field.Binder.FieldLocator.Illegal{}";
                }
            }
        }

        protected static class InstanceFieldConstructor implements Instrumentation {

            private final TypeDescription instrumentedType;

            protected InstanceFieldConstructor(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(AccessorProxy.FIELD_NAME,
                        this.instrumentedType,
                        Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE);
            }

            @Override
            public ByteCodeAppender appender(Target instrumentationTarget) {
                return new Appender(instrumentationTarget);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentedType.equals(((InstanceFieldConstructor) other).instrumentedType);
            }

            @Override
            public int hashCode() {
                return instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "Field.Binder.InstanceFieldConstructor{" +
                        "instrumentedType=" + instrumentedType +
                        '}';
            }

            protected static class Appender implements ByteCodeAppender {

                private final FieldDescription fieldDescription;

                protected Appender(Target instrumentationTarget) {
                    fieldDescription = instrumentationTarget.getTypeDescription()
                            .getDeclaredFields()
                            .named(AccessorProxy.FIELD_NAME);
                }

                @Override
                public boolean appendsCode() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor,
                                  Context instrumentationContext,
                                  MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.REFERENCE.loadFromIndex(0),
                            MethodInvocation.invoke(StaticFieldConstructor.INSTANCE.objectTypeDefaultConstructor),
                            MethodVariableAccess.loadThisReferenceAndArguments(instrumentedMethod),
                            FieldAccess.forField(fieldDescription).putter(),
                            MethodReturn.VOID
                    ).apply(methodVisitor, instrumentationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && fieldDescription.equals(((Appender) other).fieldDescription);
                }

                @Override
                public int hashCode() {
                    return fieldDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "Field.Binder.InstanceFieldConstructor.Appender{" +
                            "fieldDescription=" + fieldDescription +
                            '}';
                }
            }
        }

        protected static class Getter implements Instrumentation {

            private final FieldDescription accessedField;

            private final Assigner assigner;

            private final AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

            protected Getter(FieldDescription accessedField,
                           Assigner assigner,
                           AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                this.accessedField = accessedField;
                this.assigner = assigner;
                this.methodAccessorFactory = methodAccessorFactory;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public ByteCodeAppender appender(Target instrumentationTarget) {
                return new Appender(instrumentationTarget);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Getter getter = (Getter) other;
                return accessedField.equals(getter.accessedField)
                        && assigner.equals(getter.assigner)
                        && methodAccessorFactory.equals(getter.methodAccessorFactory);
            }

            @Override
            public int hashCode() {
                int result = accessedField.hashCode();
                result = 31 * result + assigner.hashCode();
                result = 31 * result + methodAccessorFactory.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "Field.Binder.Getter{" +
                        "accessedField=" + accessedField +
                        ", assigner=" + assigner +
                        ", methodAccessorFactory=" + methodAccessorFactory +
                        '}';
            }

            protected class Appender implements ByteCodeAppender {

                private final TypeDescription typeDescription;

                protected Appender(Target instrumentationTarget) {
                    typeDescription = instrumentationTarget.getTypeDescription();
                }

                @Override
                public boolean appendsCode() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor,
                                  Context instrumentationContext,
                                  MethodDescription instrumentedMethod) {
                    MethodDescription getterMethod = methodAccessorFactory.registerGetterFor(accessedField);
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            accessedField.isStatic()
                                    ? StackManipulation.LegalTrivial.INSTANCE
                                    : new StackManipulation.Compound(
                                    MethodVariableAccess.REFERENCE.loadFromIndex(0),
                                    FieldAccess.forField(typeDescription.getDeclaredFields().named(AccessorProxy.FIELD_NAME)).getter()),
                            MethodInvocation.invoke(getterMethod),
                            assigner.assign(getterMethod.getReturnType(), instrumentedMethod.getReturnType(), true),
                            MethodReturn.returning(instrumentedMethod.getReturnType())
                    ).apply(methodVisitor, instrumentationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                private Getter getOuter() {
                    return Getter.this;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && Getter.this.equals(((Appender) other).getOuter())
                            && typeDescription.equals(((Appender) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode() + 31 * Getter.this.hashCode();
                }

                @Override
                public String toString() {
                    return "Field.Binder.Getter.Appender{" +
                            "getter=" + Getter.this +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }
        }

        protected static class Setter implements Instrumentation {

            private final FieldDescription accessedField;

            private final Assigner assigner;

            private final AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

            protected Setter(FieldDescription accessedField,
                           Assigner assigner,
                           AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                this.accessedField = accessedField;
                this.assigner = assigner;
                this.methodAccessorFactory = methodAccessorFactory;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public ByteCodeAppender appender(Target instrumentationTarget) {
                return new Appender(instrumentationTarget);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Setter getter = (Setter) other;
                return accessedField.equals(getter.accessedField)
                        && assigner.equals(getter.assigner)
                        && methodAccessorFactory.equals(getter.methodAccessorFactory);
            }

            @Override
            public int hashCode() {
                int result = accessedField.hashCode();
                result = 31 * result + assigner.hashCode();
                result = 31 * result + methodAccessorFactory.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "Field.Binder.Setter{" +
                        "accessedField=" + accessedField +
                        ", assigner=" + assigner +
                        ", methodAccessorFactory=" + methodAccessorFactory +
                        '}';
            }

            protected class Appender implements ByteCodeAppender {

                private final TypeDescription typeDescription;

                protected Appender(Target instrumentationTarget) {
                    typeDescription = instrumentationTarget.getTypeDescription();
                }

                @Override
                public boolean appendsCode() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor,
                                  Context instrumentationContext,
                                  MethodDescription instrumentedMethod) {
                    TypeDescription parameterType = instrumentedMethod.getParameterTypes().get(0);
                    MethodDescription setterMethod = methodAccessorFactory.registerSetterFor(accessedField);
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            accessedField.isStatic()
                                    ? StackManipulation.LegalTrivial.INSTANCE
                                    : new StackManipulation.Compound(
                                    MethodVariableAccess.REFERENCE.loadFromIndex(0),
                                    FieldAccess.forField(typeDescription.getDeclaredFields().named(AccessorProxy.FIELD_NAME)).getter()),
                            MethodVariableAccess.forType(parameterType).loadFromIndex(1),
                            assigner.assign(parameterType, setterMethod.getParameterTypes().get(0), true),
                            MethodInvocation.invoke(setterMethod),
                            MethodReturn.VOID
                    ).apply(methodVisitor, instrumentationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }
                private Setter getOuter() {
                    return Setter.this;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && Setter.this.equals(((Appender) other).getOuter())
                            && typeDescription.equals(((Appender) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode() + 31 * Setter.this.hashCode();
                }

                @Override
                public String toString() {
                    return "Field.Binder.Setter.Appender{" +
                            "setter=" + Setter.this +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }
        }

        protected class AccessorProxy implements AuxiliaryType, StackManipulation {

            private static final String FIELD_NAME = "instance";

            private final FieldDescription accessedField;

            private final TypeDescription instrumentedType;

            private final Assigner assigner;

            private final AccessType accessType;

            private final boolean serializableProxy;

            public AccessorProxy(FieldDescription accessedField,
                                 Assigner assigner,
                                 TypeDescription instrumentedType,
                                 AccessType accessType,
                                 boolean serializableProxy) {
                this.accessedField = accessedField;
                this.assigner = assigner;
                this.instrumentedType = instrumentedType;
                this.accessType = accessType;
                this.serializableProxy = serializableProxy;
            }

            @Override
            public DynamicType make(String auxiliaryTypeName,
                                    ClassFileVersion classFileVersion,
                                    MethodAccessorFactory methodAccessorFactory) {
                return new ByteBuddy(classFileVersion)
                        .subclass(accessType.proxyType(getterMethod, setterMethod), ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .name(auxiliaryTypeName)
                        .modifiers(DEFAULT_TYPE_MODIFIER)
                        .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                        .defineConstructor(accessedField.isStatic()
                                ? Collections.<TypeDescription>emptyList()
                                : Collections.singletonList(instrumentedType))
                        .intercept(accessedField.isStatic()
                                ? StaticFieldConstructor.INSTANCE
                                : new InstanceFieldConstructor(instrumentedType))
                        .method(isDeclaredBy(accessType.proxyType(getterMethod, setterMethod)))
                        .intercept(accessType.access(accessedField, assigner, methodAccessorFactory))
                        .make();
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                TypeDescription auxiliaryType = instrumentationContext.register(this);
                return new Compound(
                        TypeCreation.forType(auxiliaryType),
                        Duplication.SINGLE,
                        accessedField.isStatic()
                                ? LegalTrivial.INSTANCE
                                : MethodVariableAccess.REFERENCE.loadFromIndex(0),
                        MethodInvocation.invoke(auxiliaryType.getDeclaredMethods().filter(isConstructor()).getOnly())
                ).apply(methodVisitor, instrumentationContext);
            }

            private Binder getOuter() {
                return Binder.this;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                AccessorProxy that = (AccessorProxy) other;
                return serializableProxy == that.serializableProxy
                        && accessType == that.accessType
                        && accessedField.equals(that.accessedField)
                        && assigner.equals(that.assigner)
                        && Binder.this.equals(that.getOuter())
                        && instrumentedType.equals(that.instrumentedType);
            }

            @Override
            public int hashCode() {
                int result = accessedField.hashCode();
                result = 31 * result + instrumentedType.hashCode();
                result = 31 * result + assigner.hashCode();
                result = 31 * result + Binder.this.hashCode();
                result = 31 * result + accessType.hashCode();
                result = 31 * result + (serializableProxy ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return "Field.Binder.AccessorProxy{" +
                        "accessedField=" + accessedField +
                        ", instrumentedType=" + instrumentedType +
                        ", assigner=" + assigner +
                        ", accessType=" + accessType +
                        ", serializableProxy=" + serializableProxy +
                        ", binder=" + Binder.this +
                        '}';
            }
        }
    }
}
