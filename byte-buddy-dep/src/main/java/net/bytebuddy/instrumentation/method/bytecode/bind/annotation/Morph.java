package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.collection.ArrayAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.IntegerConstant;
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
import java.util.*;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * This annotation instructs Byte Buddy to inject a proxy class that calls a method's super method with
 * explicit arguments. For this, the {@link Morph.Binder}
 * needs to be installed for an interface type that takes an argument of the array type {@link java.lang.Object} and
 * returns a non-array type of {@link java.lang.Object}. This is an alternative to using the
 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall} or
 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.DefaultCall} annotations which call a super
 * method using the same arguments as the intercepted method was invoked with.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Morph {

    boolean serializableProxy() default false;

    boolean defaultMethod() default false;

    Class<?> defaultTarget() default void.class;

    static class Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph>,
            MethodLookupEngine.Factory,
            MethodLookupEngine {

        private static final MethodDescription SERIALIZABLE_PROXY;

        private static final MethodDescription DEFAULT_METHOD;

        private static final MethodDescription DEFAULT_TARGET;

        static {
            MethodList methodList = new TypeDescription.ForLoadedType(Morph.class).getDeclaredMethods();
            SERIALIZABLE_PROXY = methodList.filter(named("serializableProxy")).getOnly();
            DEFAULT_METHOD = methodList.filter(named("defaultMethod")).getOnly();
            DEFAULT_TARGET = methodList.filter(named("defaultTarget")).getOnly();
        }

        private final MethodDescription forwardingMethod;

        protected Binder(MethodDescription forwardingMethod) {
            this.forwardingMethod = forwardingMethod;
        }

        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph> install(Class<?> type) {
            return install(new TypeDescription.ForLoadedType(nonNull(type)));
        }

        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph> install(TypeDescription typeDescription) {
            return new Binder(onlyMethod(nonNull(typeDescription)));
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
            MethodDescription methodDescription = methodCandidates.getOnly();
            if (!methodDescription.getReturnType().represents(Object.class)) {
                throw new IllegalArgumentException(methodDescription + " does not return an Object-type");
            } else if (methodDescription.getParameterTypes().size() != 1
                    || !methodDescription.getParameterTypes().get(0).represents(Object[].class)) {
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
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            TypeDescription parameterType = target.getParameterTypes().get(targetParameterIndex);
            if (!parameterType.equals(forwardingMethod.getDeclaringType())) {
                throw new IllegalStateException(String.format("The installed type %s for the @Morph annotation does not " +
                        "equal the annotated parameter type on %s", parameterType, target));
            }
            Instrumentation.SpecialMethodInvocation specialMethodInvocation;
            TypeDescription typeDescription = annotation.getValue(DEFAULT_TARGET, TypeDescription.class);
            if (typeDescription.represents(void.class) && !annotation.getValue(DEFAULT_METHOD, Boolean.class)) {
                specialMethodInvocation = instrumentationTarget.invokeSuper(source, Instrumentation.Target.MethodLookup.Default.EXACT);
            } else {
                specialMethodInvocation = (typeDescription.represents(void.class)
                        ? DefaultMethodLocator.Implicit.INSTANCE
                        : new DefaultMethodLocator.Explicit(typeDescription)).resolve(instrumentationTarget, source);
            }
            return specialMethodInvocation.isValid()
                    ? new MethodDelegationBinder.ParameterBinding.Anonymous(new RedirectionProxy(forwardingMethod.getDeclaringType(),
                    instrumentationTarget.getTypeDescription(),
                    specialMethodInvocation,
                    assigner,
                    annotation.getValue(SERIALIZABLE_PROXY, Boolean.class),
                    this))
                    : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
        }

        @Override
        public MethodLookupEngine make(boolean extractDefaultMethods) {
            return this;
        }

        @Override
        public Finding process(TypeDescription typeDescription) {
            return new PrecomputedFinding(typeDescription);
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

        protected static class RedirectionProxy implements AuxiliaryType, StackManipulation {

            private static final String FIELD_NAME = "target";

            private final TypeDescription forwardingType;

            private final TypeDescription instrumentedType;

            private final Instrumentation.SpecialMethodInvocation specialMethodInvocation;

            /**
             * The assigner to use.
             */
            private final Assigner assigner;

            /**
             * Determines if the generated proxy should be {@link java.io.Serializable}.
             */
            private final boolean serializableProxy;

            /**
             * The method lookup engine factory to register.
             */
            private final Factory methodLookupEngineFactory;

            protected RedirectionProxy(TypeDescription forwardingType,
                                       TypeDescription instrumentedType,
                                       Instrumentation.SpecialMethodInvocation specialMethodInvocation,
                                       Assigner assigner,
                                       boolean serializableProxy,
                                       Factory methodLookupEngineFactory) {
                this.forwardingType = forwardingType;
                this.instrumentedType = instrumentedType;
                this.specialMethodInvocation = specialMethodInvocation;
                this.assigner = assigner;
                this.serializableProxy = serializableProxy;
                this.methodLookupEngineFactory = methodLookupEngineFactory;
            }

            @Override
            public DynamicType make(String auxiliaryTypeName,
                                    ClassFileVersion classFileVersion,
                                    MethodAccessorFactory methodAccessorFactory) {
                return new ByteBuddy(classFileVersion)
                        .subclass(forwardingType, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .name(auxiliaryTypeName)
                        .modifiers(DEFAULT_TYPE_MODIFIER)
                        .methodLookupEngine(methodLookupEngineFactory)
                        .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                        .defineConstructor(specialMethodInvocation.getMethodDescription().isStatic()
                                ? Collections.<TypeDescription>emptyList()
                                : Collections.singletonList(instrumentedType))
                        .intercept(specialMethodInvocation.getMethodDescription().isStatic()
                                ? StaticFieldConstructor.INSTANCE
                                : new InstanceFieldConstructor(instrumentedType))
                        .method(isDeclaredBy(forwardingType))
                        .intercept(new MethodCall(methodAccessorFactory.registerAccessorFor(specialMethodInvocation), assigner))
                        .make();
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                TypeDescription forwardingType = instrumentationContext.register(this);
                return new Compound(
                        TypeCreation.forType(forwardingType),
                        Duplication.SINGLE,
                        MethodVariableAccess.REFERENCE.loadFromIndex(0),
                        MethodInvocation.invoke(forwardingType.getDeclaredMethods().filter(isConstructor()).getOnly())
                ).apply(methodVisitor, instrumentationContext);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                RedirectionProxy that = (RedirectionProxy) other;
                return serializableProxy == that.serializableProxy
                        && assigner.equals(that.assigner)
                        && forwardingType.equals(that.forwardingType)
                        && specialMethodInvocation.equals(that.specialMethodInvocation)
                        && methodLookupEngineFactory.equals(that.methodLookupEngineFactory);
            }

            @Override
            public int hashCode() {
                int result = forwardingType.hashCode();
                result = 31 * result + specialMethodInvocation.hashCode();
                result = 31 * result + assigner.hashCode();
                result = 31 * result + methodLookupEngineFactory.hashCode();
                result = 31 * result + (serializableProxy ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return "Morph.Binder.Redirection{" +
                        "forwardingType=" + forwardingType +
                        ", specialMethodInvocation=" + specialMethodInvocation +
                        ", assigner=" + assigner +
                        ", methodLookupEngineFactory=" + methodLookupEngineFactory +
                        ", serializableProxy=" + serializableProxy +
                        '}';
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

            private static class MethodCall implements Instrumentation {

                private final MethodDescription accessorMethod;

                private final Assigner assigner;

                private MethodCall(MethodDescription accessorMethod, Assigner assigner) {
                    this.accessorMethod = accessorMethod;
                    this.assigner = assigner;
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
                    return "Morph.Binder.Redirection.MethodCall{" +
                            "accessorMethod=" + accessorMethod +
                            ", assigner=" + assigner +
                            '}';
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
                        StackManipulation arrayReference = MethodVariableAccess.REFERENCE.loadFromIndex(1);
                        StackManipulation[] parameterLoading = new StackManipulation[accessorMethod.getParameterTypes().size()];
                        int index = 0;
                        for (TypeDescription parameterType : accessorMethod.getParameterTypes()) {
                            parameterLoading[index] = new StackManipulation.Compound(arrayReference,
                                    IntegerConstant.forValue(index),
                                    ArrayAccess.REFERENCE.load(),
                                    assigner.assign(new TypeDescription.ForLoadedType(Object.class), parameterType, true));
                            index++;
                        }
                        StackManipulation.Size stackSize = new StackManipulation.Compound(
                                accessorMethod.isStatic()
                                        ? LegalTrivial.INSTANCE
                                        : new StackManipulation.Compound(
                                        MethodVariableAccess.REFERENCE.loadFromIndex(0),
                                        FieldAccess.forField(typeDescription.getDeclaredFields().named(RedirectionProxy.FIELD_NAME)).getter()),
                                new StackManipulation.Compound(parameterLoading),
                                MethodInvocation.invoke(accessorMethod),
                                assigner.assign(accessorMethod.getReturnType(), instrumentedMethod.getReturnType(), false),
                                MethodReturn.REFERENCE
                        ).apply(methodVisitor, instrumentationContext);
                        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                    }

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

            private static class InstanceFieldConstructor implements Instrumentation {

                private final TypeDescription instrumentedType;

                private InstanceFieldConstructor(TypeDescription instrumentedType) {
                    this.instrumentedType = instrumentedType;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType.withField(RedirectionProxy.FIELD_NAME,
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
                                .named(RedirectionProxy.FIELD_NAME);
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
        }

        protected class PrecomputedFinding implements Finding {

            private final TypeDescription typeDescription;

            public PrecomputedFinding(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            @Override
            public TypeDescription getTypeDescription() {
                return typeDescription;
            }

            @Override
            public MethodList getInvokableMethods() {
                List<MethodDescription> invokableMethods = new ArrayList<MethodDescription>(2);
                invokableMethods.addAll(typeDescription.getDeclaredMethods());
                invokableMethods.add(forwardingMethod);
                return new MethodList.Explicit(invokableMethods);
            }

            @Override
            public Map<TypeDescription, Set<MethodDescription>> getInvokableDefaultMethods() {
                return Collections.emptyMap();
            }

            private Binder getBinder() {
                return Binder.this;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && typeDescription.equals(((PrecomputedFinding) other).typeDescription)
                        && Binder.this.equals(((PrecomputedFinding) other).getBinder());
            }

            @Override
            public int hashCode() {
                return typeDescription.hashCode() + 31 * Binder.this.hashCode();
            }

            @Override
            public String toString() {
                return "Pipe.Binder.PrecomputedFinding{" +
                        "binder=" + Binder.this +
                        ", typeDescription=" + typeDescription +
                        '}';
            }
        }

        /**
         * A default method locator is responsible for looking up a default method to a given source method.
         */
        protected static interface DefaultMethodLocator {

            /**
             * Locates the correct default method to a given source method.
             *
             * @param instrumentationTarget The current instrumentation target.
             * @param source                The source method for which a default method should be looked up.
             * @return A special method invocation of the default method or an illegal special method invocation,
             * if no suitable invocation could be located.
             */
            Instrumentation.SpecialMethodInvocation resolve(Instrumentation.Target instrumentationTarget,
                                                            MethodDescription source);

            /**
             * An implicit default method locator that only permits the invocation of a default method if the source
             * method itself represents a method that was defined on a default method interface.
             */
            static enum Implicit implements DefaultMethodLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Instrumentation.SpecialMethodInvocation resolve(Instrumentation.Target instrumentationTarget,
                                                                       MethodDescription source) {
                    String uniqueSignature = source.getUniqueSignature();
                    Instrumentation.SpecialMethodInvocation specialMethodInvocation = null;
                    for (TypeDescription candidate : instrumentationTarget.getTypeDescription().getInterfaces()) {
                        if (source.isSpecializableFor(candidate)) {
                            if (specialMethodInvocation != null) {
                                return Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE;
                            }
                            specialMethodInvocation = instrumentationTarget.invokeDefault(candidate, uniqueSignature);
                        }
                    }
                    return specialMethodInvocation != null
                            ? specialMethodInvocation
                            : Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE;
                }
            }

            /**
             * An explicit default method locator attempts to look up a default method in the specified interface type.
             */
            static class Explicit implements DefaultMethodLocator {

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
                public Instrumentation.SpecialMethodInvocation resolve(Instrumentation.Target instrumentationTarget,
                                                                       MethodDescription source) {
                    if (!typeDescription.isInterface()) {
                        throw new IllegalStateException(source + " method carries default method call parameter on non-interface type");
                    }
                    return instrumentationTarget.invokeDefault(typeDescription, source.getUniqueSignature());
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
                    return "Binder.DefaultCall.DefaultMethodLocator.Explicit{typeDescription=" + typeDescription + '}';
                }
            }
        }
    }
}
