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

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Field {

    static final String BEAN_PROPERTY = "";

    boolean serializableProxy() default false;

    String value() default "";

    Class<?> definingType() default void.class;

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

        private abstract static class FieldLocator {

            protected static FieldLocator of(String fieldName, MethodDescription methodDescription) {
                return BEAN_PROPERTY.equals(fieldName)
                        ? Legal.consider(methodDescription)
                        : new Legal(fieldName);
            }

            protected abstract LookupEngine lookup(TypeDescription typeDescription, TypeDescription instrumentedTyoe);

            private abstract static class Resolution {

                protected abstract boolean isResolved();

                protected abstract FieldDescription getFieldDescription();

                private static class Unresolved extends Resolution {

                    @Override
                    protected boolean isResolved() {
                        return false;
                    }

                    @Override
                    protected FieldDescription getFieldDescription() {
                        throw new IllegalStateException("Cannot resolve an unresolved field lookup");
                    }
                }

                private static class Resolved extends Resolution {

                    private final FieldDescription fieldDescription;

                    private Resolved(FieldDescription fieldDescription) {
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
                }

            }

            private abstract static class LookupEngine {

                protected abstract Resolution resolve(TypeDescription instrumentedType);

                private static class Illegal extends LookupEngine {

                    @Override
                    protected Resolution resolve(TypeDescription instrumentedType) {
                        return new Resolution.Unresolved();
                    }
                }

                private static class ForHierarchy extends LookupEngine {

                    private final String fieldName;

                    private ForHierarchy(String fieldName) {
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
                }

                private static class ForExplicitType extends LookupEngine {

                    private final String fieldName;

                    private final TypeDescription typeDescription;

                    private ForExplicitType(String fieldName, TypeDescription typeDescription) {
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
            }

            protected static class Illegal extends FieldLocator {

                @Override
                protected LookupEngine lookup(TypeDescription typeDescription, TypeDescription instrumentedType) {
                    return new LookupEngine.Illegal();
                }
            }
        }

        private static class InstanceFieldConstructor implements Instrumentation {

            private final TypeDescription instrumentedType;

            private InstanceFieldConstructor(TypeDescription instrumentedType) {
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

            private static class Appender implements ByteCodeAppender {

                private final FieldDescription fieldDescription;

                private Appender(Target instrumentationTarget) {
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
            }
        }

        private static class Getter implements Instrumentation {

            private final FieldDescription accessedField;

            private final Assigner assigner;

            private final AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

            private Getter(FieldDescription accessedField,
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

            private class Appender implements ByteCodeAppender {

                private final TypeDescription typeDescription;

                private Appender(Target instrumentationTarget) {
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
            }
        }

        private static class Setter implements Instrumentation {

            private final FieldDescription accessedField;

            private final Assigner assigner;

            private final AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

            private Setter(FieldDescription accessedField,
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

            private class Appender implements ByteCodeAppender {

                private final TypeDescription typeDescription;

                private Appender(Target instrumentationTarget) {
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
        }
    }
}
