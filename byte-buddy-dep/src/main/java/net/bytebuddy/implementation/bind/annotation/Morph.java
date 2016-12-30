package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.collection.ArrayAccess;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatchers;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.annotation.*;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * This annotation instructs Byte Buddy to inject a proxy class that calls a method's super method with
 * explicit arguments. For this, the {@link Morph.Binder}
 * needs to be installed for an interface type that takes an argument of the array type {@link java.lang.Object} and
 * returns a non-array type of {@link java.lang.Object}. This is an alternative to using the
 * {@link net.bytebuddy.implementation.bind.annotation.SuperCall} or
 * {@link net.bytebuddy.implementation.bind.annotation.DefaultCall} annotations which call a super
 * method using the same arguments as the intercepted method was invoked with.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Morph {

    /**
     * Determines if the injected proxy for this parameter should be serializable.
     *
     * @return {@code true} if the proxy should be serializable.
     */
    boolean serializableProxy() default false;

    /**
     * Determines if the proxy should attempt to invoke a default method. If the default method is ambiguous,
     * use the {@link Morph#defaultTarget()} property instead which allows to determine an explicit interface
     * on which the default method should be invoked on. If this other method is used, this property is ignored.
     *
     * @return {@code true} if a default method should be ignored.
     */
    boolean defaultMethod() default false;

    /**
     * The type on which a default method should be invoked. When this property is not set and the
     * {@link Morph#defaultMethod()} property is set to {@code false}, a normal super method invocation is attempted.
     *
     * @return The target interface of a default method call.
     */
    Class<?> defaultTarget() default void.class;

    /**
     * A binder for the {@link net.bytebuddy.implementation.bind.annotation.Morph} annotation.
     */
    class Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph> {

        /**
         * A reference to the serializable proxy method.
         */
        private static final MethodDescription.InDefinedShape SERIALIZABLE_PROXY;

        /**
         * A reference to the default method method.
         */
        private static final MethodDescription.InDefinedShape DEFAULT_METHOD;

        /**
         * A reference to the default target method.
         */
        private static final MethodDescription.InDefinedShape DEFAULT_TARGET;

        /*
         * Looks up references for all annotation properties of the morph annotation.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methodList = new TypeDescription.ForLoadedType(Morph.class).getDeclaredMethods();
            SERIALIZABLE_PROXY = methodList.filter(named("serializableProxy")).getOnly();
            DEFAULT_METHOD = methodList.filter(named("defaultMethod")).getOnly();
            DEFAULT_TARGET = methodList.filter(named("defaultTarget")).getOnly();
        }

        /**
         * The method which is overridden for generating the proxy class.
         */
        private final MethodDescription forwardingMethod;

        /**
         * Creates a new binder.
         *
         * @param forwardingMethod The method which is overridden for generating the proxy class.
         */
        protected Binder(MethodDescription forwardingMethod) {
            this.forwardingMethod = forwardingMethod;
        }

        /**
         * Installs a given type for use on a {@link net.bytebuddy.implementation.bind.annotation.Morph}
         * annotation. The given type must be an interface without any super interfaces and a single method which
         * maps an {@link java.lang.Object} array to a {@link java.lang.Object} type. The use of generics is
         * permitted.
         *
         * @param type The type to install.
         * @return A binder for the {@link net.bytebuddy.implementation.bind.annotation.Morph}
         * annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph> install(Class<?> type) {
            return install(new TypeDescription.ForLoadedType(type));
        }

        /**
         * Installs a given type for use on a {@link net.bytebuddy.implementation.bind.annotation.Morph}
         * annotation. The given type must be an interface without any super interfaces and a single method which
         * maps an {@link java.lang.Object} array to a {@link java.lang.Object} type. The use of generics is
         * permitted.
         *
         * @param typeDescription The type to install.
         * @return A binder for the {@link net.bytebuddy.implementation.bind.annotation.Morph}
         * annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph> install(TypeDescription typeDescription) {
            return new Binder(onlyMethod(typeDescription));
        }

        /**
         * Extracts the only method of a given type and validates to fit the constraints of the morph annotation.
         *
         * @param typeDescription The type to extract the method from.
         * @return The only method after validation.
         */
        private static MethodDescription onlyMethod(TypeDescription typeDescription) {
            if (!typeDescription.isInterface()) {
                throw new IllegalArgumentException(typeDescription + " is not an interface");
            } else if (!typeDescription.getInterfaces().isEmpty()) {
                throw new IllegalArgumentException(typeDescription + " must not extend other interfaces");
            } else if (!typeDescription.isPublic()) {
                throw new IllegalArgumentException(typeDescription + " is mot public");
            }
            MethodList<?> methodCandidates = typeDescription.getDeclaredMethods().filter(isAbstract());
            if (methodCandidates.size() != 1) {
                throw new IllegalArgumentException(typeDescription + " must declare exactly one abstract method");
            }
            MethodDescription methodDescription = methodCandidates.getOnly();
            if (!methodDescription.getReturnType().asErasure().represents(Object.class)) {
                throw new IllegalArgumentException(methodDescription + " does not return an Object-type");
            } else if (methodDescription.getParameters().size() != 1 || !methodDescription.getParameters().get(0).getType().asErasure().represents(Object[].class)) {
                throw new IllegalArgumentException(methodDescription + " does not take a single argument of type Object[]");
            }
            return methodDescription;
        }

        @Override
        public Class<Morph> getHandledType() {
            return Morph.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Morph> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            if (!target.getType().asErasure().equals(forwardingMethod.getDeclaringType())) {
                throw new IllegalStateException("Illegal use of @Morph for " + target + " which was installed for " + forwardingMethod.getDeclaringType());
            }
            Implementation.SpecialMethodInvocation specialMethodInvocation;
            TypeDescription typeDescription = annotation.getValue(DEFAULT_TARGET).resolve(TypeDescription.class);
            if (typeDescription.represents(void.class) && !annotation.getValue(DEFAULT_METHOD).resolve(Boolean.class)) {
                specialMethodInvocation = implementationTarget.invokeSuper(source.asSignatureToken());
            } else {
                specialMethodInvocation = (typeDescription.represents(void.class)
                        ? DefaultMethodLocator.Implicit.INSTANCE
                        : new DefaultMethodLocator.Explicit(typeDescription)).resolve(implementationTarget, source);
            }
            return specialMethodInvocation.isValid()
                    ? new MethodDelegationBinder.ParameterBinding.Anonymous(new RedirectionProxy(forwardingMethod.getDeclaringType().asErasure(),
                    implementationTarget.getInstrumentedType(),
                    specialMethodInvocation,
                    assigner,
                    annotation.getValue(SERIALIZABLE_PROXY).resolve(Boolean.class)))
                    : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && forwardingMethod.equals(((Binder) other).forwardingMethod);
        }

        @Override
        public int hashCode() {
            return forwardingMethod.hashCode();
        }

        @Override
        public String toString() {
            return "Morph.Binder{forwardingMethod=" + forwardingMethod + '}';
        }

        /**
         * A default method locator is responsible for looking up a default method to a given source method.
         */
        protected interface DefaultMethodLocator {

            /**
             * Locates the correct default method to a given source method.
             *
             * @param implementationTarget The current implementation target.
             * @param source               The source method for which a default method should be looked up.
             * @return A special method invocation of the default method or an illegal special method invocation,
             * if no suitable invocation could be located.
             */
            Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget,
                                                           MethodDescription source);

            /**
             * An implicit default method locator that only permits the invocation of a default method if the source
             * method itself represents a method that was defined on a default method interface.
             */
            enum Implicit implements DefaultMethodLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source) {
                    Implementation.SpecialMethodInvocation specialMethodInvocation = null;
                    for (TypeDescription candidate : implementationTarget.getInstrumentedType().getInterfaces().asErasures()) {
                        if (source.isSpecializableFor(candidate)) {
                            if (specialMethodInvocation != null) {
                                return Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
                            }
                            specialMethodInvocation = implementationTarget.invokeDefault(candidate, source.asSignatureToken());
                        }
                    }
                    return specialMethodInvocation != null
                            ? specialMethodInvocation
                            : Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
                }

                @Override
                public String toString() {
                    return "Morph.Binder.DefaultMethodLocator.Implicit." + name();
                }
            }

            /**
             * An explicit default method locator attempts to look up a default method in the specified interface type.
             */
            class Explicit implements DefaultMethodLocator {

                /**
                 * A description of the type on which the default method should be invoked.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new explicit default method locator.
                 *
                 * @param typeDescription The actual target interface as explicitly defined by
                 *                        {@link DefaultCall#targetType()}.
                 */
                public Explicit(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget,
                                                                      MethodDescription source) {
                    if (!typeDescription.isInterface()) {
                        throw new IllegalStateException(source + " method carries default method call parameter on non-interface type");
                    }
                    return implementationTarget.invokeDefault(typeDescription, source.asSignatureToken());
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && typeDescription.equals(((Explicit) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "Morph.Binder.DefaultMethodLocator.Explicit{typeDescription=" + typeDescription + '}';
                }
            }
        }

        /**
         * A proxy that implements the installed interface in order to allow for a morphed super method invocation.
         */
        protected static class RedirectionProxy implements AuxiliaryType, StackManipulation {

            /**
             * The name of the field that carries an instance for invoking a super method on.
             */
            protected static final String FIELD_NAME = "target";

            /**
             * The interface type that is implemented by the generated proxy.
             */
            private final TypeDescription morphingType;

            /**
             * The type that is instrumented on which the super method is invoked.
             */
            private final TypeDescription instrumentedType;

            /**
             * The special method invocation to be executed by the morphing type via an accessor on the
             * instrumented type.
             */
            private final Implementation.SpecialMethodInvocation specialMethodInvocation;

            /**
             * The assigner to use.
             */
            private final Assigner assigner;

            /**
             * Determines if the generated proxy should be {@link java.io.Serializable}.
             */
            private final boolean serializableProxy;

            /**
             * Creates a new redirection proxy.
             *
             * @param morphingType            The interface type that is implemented by the generated proxy.
             * @param instrumentedType        The type that is instrumented on which the super method is invoked.
             * @param specialMethodInvocation The special method invocation to be executed by the morphing type via
             *                                an accessor on the instrumented type.
             * @param assigner                The assigner to use.
             * @param serializableProxy       {@code true} if the proxy should be serializable.
             */
            protected RedirectionProxy(TypeDescription morphingType,
                                       TypeDescription instrumentedType,
                                       Implementation.SpecialMethodInvocation specialMethodInvocation,
                                       Assigner assigner,
                                       boolean serializableProxy) {
                this.morphingType = morphingType;
                this.instrumentedType = instrumentedType;
                this.specialMethodInvocation = specialMethodInvocation;
                this.assigner = assigner;
                this.serializableProxy = serializableProxy;
            }

            @Override
            public DynamicType make(String auxiliaryTypeName,
                                    ClassFileVersion classFileVersion,
                                    MethodAccessorFactory methodAccessorFactory) {
                return new ByteBuddy(classFileVersion)
                        .subclass(morphingType, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .name(auxiliaryTypeName)
                        .modifiers(DEFAULT_TYPE_MODIFIER)
                        .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                        .defineConstructor().withParameters(specialMethodInvocation.getMethodDescription().isStatic()
                                ? Collections.<TypeDescription>emptyList()
                                : Collections.singletonList(instrumentedType))
                        .intercept(specialMethodInvocation.getMethodDescription().isStatic()
                                ? StaticFieldConstructor.INSTANCE
                                : new InstanceFieldConstructor(instrumentedType))
                        .method(ElementMatchers.<MethodDescription>isAbstract().and(isDeclaredBy(morphingType)))
                        .intercept(new MethodCall(methodAccessorFactory.registerAccessorFor(specialMethodInvocation, MethodAccessorFactory.AccessType.DEFAULT), assigner))
                        .make();
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                TypeDescription forwardingType = implementationContext.register(this);
                return new Compound(
                        TypeCreation.of(forwardingType),
                        Duplication.SINGLE,
                        specialMethodInvocation.getMethodDescription().isStatic()
                                ? Trivial.INSTANCE
                                : MethodVariableAccess.loadThis(),
                        MethodInvocation.invoke(forwardingType.getDeclaredMethods().filter(isConstructor()).getOnly())
                ).apply(methodVisitor, implementationContext);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                RedirectionProxy that = (RedirectionProxy) other;
                return serializableProxy == that.serializableProxy
                        && assigner.equals(that.assigner)
                        && instrumentedType.equals(that.instrumentedType)
                        && morphingType.equals(that.morphingType)
                        && specialMethodInvocation.equals(that.specialMethodInvocation);
            }

            @Override
            public int hashCode() {
                int result = morphingType.hashCode();
                result = 31 * result + specialMethodInvocation.hashCode();
                result = 31 * result + assigner.hashCode();
                result = 31 * result + instrumentedType.hashCode();
                result = 31 * result + (serializableProxy ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return "Morph.Binder.RedirectionProxy{" +
                        "morphingType=" + morphingType +
                        ", specialMethodInvocation=" + specialMethodInvocation +
                        ", assigner=" + assigner +
                        ", serializableProxy=" + serializableProxy +
                        ", instrumentedType=" + instrumentedType +
                        '}';
            }

            /**
             * Creates an instance of the proxy when instrumenting a static method.
             */
            protected enum StaticFieldConstructor implements Implementation {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * A reference of the {@link Object} type default constructor.
                 */
                private final MethodDescription objectTypeDefaultConstructor;

                /**
                 * Creates the constructor call singleton.
                 */
                StaticFieldConstructor() {
                    objectTypeDefaultConstructor = TypeDescription.OBJECT.getDeclaredMethods()
                            .filter(isConstructor())
                            .getOnly();
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new ByteCodeAppender.Simple(MethodVariableAccess.loadThis(), MethodInvocation.invoke(objectTypeDefaultConstructor), MethodReturn.VOID);
                }

                @Override
                public String toString() {
                    return "Morph.Binder.RedirectionProxy.StaticFieldConstructor." + name();
                }
            }

            /**
             * Creates an instance of the proxy when instrumenting an instance method.
             */
            protected static class InstanceFieldConstructor implements Implementation {

                /**
                 * The instrumented type.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * Creates a new instance field constructor implementation.
                 *
                 * @param instrumentedType The instrumented type.
                 */
                protected InstanceFieldConstructor(TypeDescription instrumentedType) {
                    this.instrumentedType = instrumentedType;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType.withField(new FieldDescription.Token(RedirectionProxy.FIELD_NAME,
                            Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE,
                            this.instrumentedType.asGenericType()));
                }

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new Appender(implementationTarget);
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
                    return "Morph.Binder.RedirectionProxy.InstanceFieldConstructor{" +
                            "instrumentedType=" + instrumentedType +
                            '}';
                }

                /**
                 * The byte code appender that implements the constructor.
                 */
                protected static class Appender implements ByteCodeAppender {

                    /**
                     * The field that carries the instance on which the super method is invoked.
                     */
                    private final FieldDescription fieldDescription;

                    /**
                     * Creates a new appender.
                     *
                     * @param implementationTarget The current implementation target.
                     */
                    protected Appender(Target implementationTarget) {
                        fieldDescription = implementationTarget.getInstrumentedType()
                                .getDeclaredFields()
                                .filter((named(RedirectionProxy.FIELD_NAME)))
                                .getOnly();
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor,
                                      Context implementationContext,
                                      MethodDescription instrumentedMethod) {
                        StackManipulation.Size stackSize = new StackManipulation.Compound(
                                MethodVariableAccess.loadThis(),
                                MethodInvocation.invoke(StaticFieldConstructor.INSTANCE.objectTypeDefaultConstructor),
                                MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                                FieldAccess.forField(fieldDescription).write(),
                                MethodReturn.VOID
                        ).apply(methodVisitor, implementationContext);
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
                        return "Morph.Binder.RedirectionProxy.InstanceFieldConstructor.Appender{" +
                                "fieldDescription=" + fieldDescription +
                                '}';
                    }
                }
            }

            /**
             * Implements a the method call of the morphing method.
             */
            protected static class MethodCall implements Implementation {

                /**
                 * The accessor method to invoke from the proxy's method.
                 */
                private final MethodDescription accessorMethod;

                /**
                 * The assigner to be used.
                 */
                private final Assigner assigner;

                /**
                 * Creates a new method call implementation for a proxy method.
                 *
                 * @param accessorMethod The accessor method to invoke from the proxy's method.
                 * @param assigner       The assigner to be used.
                 */
                protected MethodCall(MethodDescription accessorMethod, Assigner assigner) {
                    this.accessorMethod = accessorMethod;
                    this.assigner = assigner;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new Appender(implementationTarget);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && accessorMethod.equals(((MethodCall) other).accessorMethod)
                            && assigner.equals(((MethodCall) other).assigner);
                }

                @Override
                public int hashCode() {
                    return accessorMethod.hashCode() + 31 * assigner.hashCode();
                }

                @Override
                public String toString() {
                    return "Morph.Binder.RedirectionProxy.MethodCall{" +
                            "accessorMethod=" + accessorMethod +
                            ", assigner=" + assigner +
                            '}';
                }

                /**
                 * The byte code appender to implement the method.
                 */
                protected class Appender implements ByteCodeAppender {

                    /**
                     * The proxy type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new appender.
                     *
                     * @param implementationTarget The current implementation target.
                     */
                    protected Appender(Target implementationTarget) {
                        typeDescription = implementationTarget.getInstrumentedType();
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor,
                                      Context implementationContext,
                                      MethodDescription instrumentedMethod) {
                        StackManipulation arrayReference = MethodVariableAccess.REFERENCE.loadFrom(1);
                        StackManipulation[] parameterLoading = new StackManipulation[accessorMethod.getParameters().size()];
                        int index = 0;
                        for (TypeDescription.Generic parameterType : accessorMethod.getParameters().asTypeList()) {
                            parameterLoading[index] = new StackManipulation.Compound(arrayReference,
                                    IntegerConstant.forValue(index),
                                    ArrayAccess.REFERENCE.load(),
                                    assigner.assign(TypeDescription.Generic.OBJECT, parameterType, Assigner.Typing.DYNAMIC));
                            index++;
                        }
                        StackManipulation.Size stackSize = new StackManipulation.Compound(
                                accessorMethod.isStatic()
                                        ? Trivial.INSTANCE
                                        : new StackManipulation.Compound(
                                        MethodVariableAccess.loadThis(),
                                        FieldAccess.forField(typeDescription.getDeclaredFields()
                                                .filter((named(RedirectionProxy.FIELD_NAME)))
                                                .getOnly()).read()),
                                new StackManipulation.Compound(parameterLoading),
                                MethodInvocation.invoke(accessorMethod),
                                assigner.assign(accessorMethod.getReturnType(), instrumentedMethod.getReturnType(), Assigner.Typing.DYNAMIC),
                                MethodReturn.REFERENCE
                        ).apply(methodVisitor, implementationContext);
                        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                    }

                    /**
                     * Returns the outer instance.
                     *
                     * @return The outer instance.
                     */
                    private MethodCall getMethodCall() {
                        return MethodCall.this;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && MethodCall.this.equals(((Appender) other).getMethodCall())
                                && typeDescription.equals(((Appender) other).typeDescription);
                    }

                    @Override
                    public int hashCode() {
                        return typeDescription.hashCode() + 31 * MethodCall.this.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "Morph.Binder.RedirectionProxy.MethodCall.Appender{" +
                                "typeDescription=" + typeDescription +
                                ", methodCall=" + MethodCall.this +
                                '}';
                    }
                }
            }
        }
    }
}
