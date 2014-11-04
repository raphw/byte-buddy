package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
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
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import org.objectweb.asm.MethodVisitor;

import java.io.Serializable;
import java.util.*;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

public @interface Morph {

    boolean serializableProxy() default false;

    static class Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph>,
            MethodLookupEngine.Factory,
            MethodLookupEngine {

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
            } else if (methodDescription.getParameterTypes().size() != 2
                    || !methodDescription.getParameterTypes().get(0).represents(Object.class)
                    || methodDescription.getParameterTypes().get(1).represents(Object[].class)) {
                throw new IllegalArgumentException(methodDescription + " does not take two arguments of type Object and Object[]");
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
            } else if (source.isStatic()) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(new Redirection(forwardingMethod.getDeclaringType(),
                    source,
                    assigner,
                    annotation.load().serializableProxy(),
                    this));
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

        protected static class Redirection implements AuxiliaryType, StackManipulation {

            /**
             * The type that declares the method for forwarding a method invocation.
             */
            private final TypeDescription forwardingType;

            /**
             * The method that is to be forwarded.
             */
            private final MethodDescription sourceMethod;

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

            /**
             * Creates a new redirection.
             *
             * @param forwardingType            The type that declares the method for forwarding a method invocation.
             * @param sourceMethod              The method that is to be forwarded.
             * @param assigner                  The assigner to use.
             * @param serializableProxy         Determines if the generated proxy should be {@link java.io.Serializable}.
             * @param methodLookupEngineFactory The method lookup engine factory to register.
             */
            protected Redirection(TypeDescription forwardingType,
                                  MethodDescription sourceMethod,
                                  Assigner assigner,
                                  boolean serializableProxy,
                                  Factory methodLookupEngineFactory) {
                this.forwardingType = forwardingType;
                this.sourceMethod = sourceMethod;
                this.assigner = assigner;
                this.serializableProxy = serializableProxy;
                this.methodLookupEngineFactory = methodLookupEngineFactory;
            }

            @Override
            public DynamicType make(String auxiliaryTypeName,
                                    ClassFileVersion classFileVersion,
                                    MethodAccessorFactory methodAccessorFactory) {
                return new ByteBuddy(classFileVersion)
                        .subclass(forwardingType, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                        .name(auxiliaryTypeName)
                        .modifiers(DEFAULT_TYPE_MODIFIER)
                        .methodLookupEngine(methodLookupEngineFactory)
                        .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                        .method(isDeclaredBy(forwardingType))
                        .intercept(new MethodCall(sourceMethod, assigner))
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
                        MethodVariableAccess.loadArguments(sourceMethod),
                        MethodInvocation.invoke(forwardingType.getDeclaredMethods().filter(isConstructor()).getOnly())
                ).apply(methodVisitor, instrumentationContext);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Redirection that = (Redirection) other;
                return serializableProxy == that.serializableProxy
                        && assigner.equals(that.assigner)
                        && forwardingType.equals(that.forwardingType)
                        && sourceMethod.equals(that.sourceMethod);
            }

            @Override
            public int hashCode() {
                int result = forwardingType.hashCode();
                result = 31 * result + sourceMethod.hashCode();
                result = 31 * result + assigner.hashCode();
                result = 31 * result + (serializableProxy ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return "Morph.Binder.Redirection{" +
                        "forwardingType=" + forwardingType +
                        ", sourceMethod=" + sourceMethod +
                        ", assigner=" + assigner +
                        ", serializableProxy=" + serializableProxy +
                        '}';
            }

            private static class MethodCall implements Instrumentation, ByteCodeAppender {

                /**
                 * The method that is invoked by the implemented method.
                 */
                private final MethodDescription morphMethod;

                /**
                 * The assigner to be used for invoking the forwarded method.
                 */
                private final Assigner assigner;

                /**
                 * Creates a new method call instrumentation.
                 *
                 * @param morphMethod The method that is invoked by the implemented method.
                 * @param assigner    The assigner to be used for invoking the forwarded method.
                 */
                private MethodCall(MethodDescription morphMethod, Assigner assigner) {
                    this.morphMethod = morphMethod;
                    this.assigner = assigner;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public ByteCodeAppender appender(Target instrumentationTarget) {
                    return this;
                }

                @Override
                public boolean appendsCode() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor,
                                  Context instrumentationContext,
                                  MethodDescription instrumentedMethod) {
                    StackManipulation arrayReference = MethodVariableAccess.REFERENCE.loadFromIndex(2);
                    StackManipulation[] parameterLoading = new StackManipulation[morphMethod.getParameterTypes().size()];
                    int index = 0;
                    for (TypeDescription parameterType : morphMethod.getParameterTypes()) {
                        parameterLoading[index] = new StackManipulation.Compound(arrayReference,
                                IntegerConstant.forValue(index),
                                ArrayAccess.REFERENCE.load(),
                                assigner.assign(new TypeDescription.ForLoadedType(Object.class), parameterType, true));
                        index++;
                    }
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.REFERENCE.loadFromIndex(1),
                            assigner.assign(new TypeDescription.ForLoadedType(Object.class), morphMethod.getDeclaringType(), true),
                            new StackManipulation.Compound(parameterLoading),
                            MethodInvocation.invoke(morphMethod),
                            assigner.assign(morphMethod.getReturnType(), instrumentedMethod.getReturnType(), false),
                            MethodReturn.REFERENCE
                    ).apply(methodVisitor, instrumentationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && morphMethod.equals(((MethodCall) other).morphMethod)
                            && assigner.equals(((MethodCall) other).assigner);
                }

                @Override
                public int hashCode() {
                    return morphMethod.hashCode() + 31 * assigner.hashCode();
                }

                @Override
                public String toString() {
                    return "Morph.Binder.Redirection.MethodCall{" +
                            "morphMethod=" + morphMethod +
                            ", assigner=" + assigner +
                            '}';
                }
            }
        }

        private class PrecomputedFinding implements Finding {

            /**
             * The type which was looked up. This type should be the instrumented type itself and therefore defines
             * the constructor of the instrumented type.
             */
            private final TypeDescription typeDescription;

            /**
             * Creates a precomputed finding.
             *
             * @param typeDescription The type which was looked up. This type should be the instrumented type itself
             *                        and therefore defines the constructor of the instrumented type.
             */
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

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
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
    }
}
