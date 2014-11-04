package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
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
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import net.bytebuddy.modifier.Visibility;
import org.objectweb.asm.MethodVisitor;

import java.io.Serializable;
import java.lang.annotation.*;
import java.util.*;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * A target method parameter that is annotated with this annotation allows to forward an intercepted method
 * invocation to another instance. The instance to which a method call is forwarded must be of the most specific
 * type that declares the intercepted method on the intercepted type.
 * <p>&nbsp;</p>
 * Unfortunately, before Java 8, the Java Class Library does not define any interface type which takes a single
 * {@link java.lang.Object} type and returns another {@link java.lang.Object} type. For this reason, a
 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe.Binder} needs to be installed explicitly
 * and registered on a {@link net.bytebuddy.instrumentation.MethodDelegation}. The installed type is allowed to be an
 * interface without any super types that declares a single method which maps an {@link java.lang.Object} type to
 * a another {@link java.lang.Object} type as a result value. It is however not prohibited to use generics in the
 * process. The following example demonstrates how the {@code @Pipe} annotation can be installed on a user type.
 * As a preparation, one needs to define a type for which the {@code @Pipe} implements the forwarding behavior:
 * <pre>
 * interface Forwarder&lt;T, S&gt; {
 *   T forwardTo(S s);
 * }
 * </pre>
 * Based on this type, one can now implement an interceptor:
 * <pre>
 * class Interceptor {
 *   private final Foo foo;
 *
 *   public Interceptor(Foo foo) {
 *     this.foo = foo;
 *   }
 *
 *   public String intercept(@Pipe Forwarder&lt;String, Foo&gt; forwarder) {
 *     return forwarder.forwardTo(foo);
 *   }
 * }
 * </pre>
 * Using both of these types, one can now install the
 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe.Binder} and register it on a
 * {@link net.bytebuddy.instrumentation.MethodDelegation}:
 * <pre>
 * MethodDelegation
 *   .to(new Interceptor(new Foo()))
 *   .appendParameterBinder(Pipe.Binder.install(ForwardingType.class))
 * </pre>
 *
 * @see net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe.Binder
 * @see net.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Pipe {

    /**
     * Determines if the generated proxy should be {@link java.io.Serializable}.
     *
     * @return {@code true} if the generated proxy should be {@link java.io.Serializable}.
     */
    boolean serializableProxy() default false;

    /**
     * A {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.TargetMethodAnnotationDrivenBinder.ParameterBinder}
     * for binding the {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe} annotation.
     */
    static class Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe>,
            MethodLookupEngine.Factory,
            MethodLookupEngine {

        /**
         * The method which implements the behavior of forwarding a method invocation. This method needs to define
         * a single non-static method with an {@link java.lang.Object} to {@link java.lang.Object} mapping.
         */
        private final MethodDescription forwardingMethod;

        /**
         * Creates a new binder. This constructor is not doing any validation of the forwarding method and its
         * declaring type. Such validation is normally performed by the
         * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe.Binder#install(Class)}
         * method.
         *
         * @param forwardingMethod The method which implements the behavior of forwarding a method invocation. This
         *                         method needs to define a single non-static method with an {@link java.lang.Object}
         *                         to {@link java.lang.Object} mapping.
         */
        protected Binder(MethodDescription forwardingMethod) {
            this.forwardingMethod = forwardingMethod;
        }

        /**
         * Installs a given type for use on a {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe}
         * annotation. The given type must be an interface without any super interfaces and a single method which
         * maps an {@link java.lang.Object} type to another {@link java.lang.Object} type. The use of generics is
         * permitted.
         *
         * @param type The type to install.
         * @return A binder for the {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe}
         * annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> install(Class<?> type) {
            return install(new TypeDescription.ForLoadedType(nonNull(type)));
        }

        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Pipe> install(TypeDescription typeDescription) {
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
            methodCandidates = methodCandidates.filter(takesArguments(Object.class).and(returns(Object.class)));
            if (methodCandidates.size() != 1) {
                throw new IllegalArgumentException(typeDescription + " must declare exactly one method with an " +
                        "Object-typed return type and an Object-typed return value");
            }
            return methodCandidates.getOnly();
        }

        @Override
        public Class<Pipe> getHandledType() {
            return Pipe.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Pipe> annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            TypeDescription parameterType = target.getParameterTypes().get(targetParameterIndex);
            if (!parameterType.equals(forwardingMethod.getDeclaringType())) {
                throw new IllegalStateException(String.format("The installed type %s for the @Pipe annotation does not " +
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
            return "Pipe.Binder{forwardingMethod=" + forwardingMethod + '}';
        }

        /**
         * An auxiliary type for performing the redirection of a method invocation as requested by the
         * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe} annotation.
         */
        protected static class Redirection implements AuxiliaryType, StackManipulation {

            /**
             * The prefix for naming fields to store method arguments.
             */
            private static final String FIELD_NAME_PREFIX = "argument";

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

            /**
             * Extracts all parameters of a method to fields.
             *
             * @param methodDescription The method to extract the parameters from.
             * @return A linked hash map of field names to the types of these fields representing all parameters of the
             * given method.
             */
            private static LinkedHashMap<String, TypeDescription> extractFields(MethodDescription methodDescription) {
                TypeList parameterTypes = methodDescription.getParameterTypes();
                LinkedHashMap<String, TypeDescription> typeDescriptions = new LinkedHashMap<String, TypeDescription>(parameterTypes.size());
                int currentIndex = 0;
                for (TypeDescription parameterType : parameterTypes) {
                    typeDescriptions.put(fieldName(currentIndex++), parameterType);
                }
                return typeDescriptions;
            }

            /**
             * Creates a new field name.
             *
             * @param index The index of the field.
             * @return The field name that corresponds to the index.
             */
            private static String fieldName(int index) {
                return String.format("%s%d", FIELD_NAME_PREFIX, index);
            }

            @Override
            public DynamicType make(String auxiliaryTypeName,
                                    ClassFileVersion classFileVersion,
                                    MethodAccessorFactory methodAccessorFactory) {
                LinkedHashMap<String, TypeDescription> parameterFields = extractFields(sourceMethod);
                DynamicType.Builder<?> builder = new ByteBuddy(classFileVersion)
                        .subclass(forwardingType, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .name(auxiliaryTypeName)
                        .modifiers(DEFAULT_TYPE_MODIFIER)
                        .methodLookupEngine(methodLookupEngineFactory)
                        .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                        .method(isDeclaredBy(forwardingType))
                        .intercept(new MethodCall(sourceMethod, assigner))
                        .defineConstructor(new ArrayList<TypeDescription>(parameterFields.values()))
                        .intercept(ConstructorCall.INSTANCE);
                for (Map.Entry<String, TypeDescription> field : parameterFields.entrySet()) {
                    builder = builder.defineField(field.getKey(), field.getValue(), Visibility.PRIVATE);
                }
                return builder.make();
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
                return "Pipe.Binder.Redirection{" +
                        "forwardingType=" + forwardingType +
                        ", sourceMethod=" + sourceMethod +
                        ", assigner=" + assigner +
                        ", serializableProxy=" + serializableProxy +
                        '}';
            }

            /**
             * The instrumentation to implement a
             * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe.Binder.Redirection}'s
             * constructor.
             */
            private static enum ConstructorCall implements Instrumentation {

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
                private ConstructorCall() {
                    this.objectTypeDefaultConstructor = new TypeDescription.ForLoadedType(Object.class)
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
                    return new Appender(instrumentationTarget.getTypeDescription());
                }

                /**
                 * The appender for implementing the
                 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe.Binder.Redirection.ConstructorCall}.
                 */
                private class Appender implements ByteCodeAppender {

                    /**
                     * The instrumented type being created.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * Creates a new appender.
                     *
                     * @param instrumentedType The instrumented type that is being created.
                     */
                    private Appender(TypeDescription instrumentedType) {
                        this.instrumentedType = instrumentedType;
                    }

                    @Override
                    public boolean appendsCode() {
                        return true;
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
                        StackManipulation thisReference = MethodVariableAccess.forType(instrumentedMethod.getDeclaringType()).loadFromIndex(0);
                        FieldList fieldList = instrumentedType.getDeclaredFields();
                        StackManipulation[] fieldLoading = new StackManipulation[fieldList.size()];
                        int index = 0;
                        for (FieldDescription fieldDescription : fieldList) {
                            fieldLoading[index] = new StackManipulation.Compound(
                                    thisReference,
                                    MethodVariableAccess.forType(fieldDescription.getFieldType())
                                            .loadFromIndex(instrumentedMethod.getParameterOffset(index)),
                                    FieldAccess.forField(fieldDescription).putter()
                            );
                            index++;
                        }
                        StackManipulation.Size stackSize = new StackManipulation.Compound(
                                thisReference,
                                MethodInvocation.invoke(objectTypeDefaultConstructor),
                                new StackManipulation.Compound(fieldLoading),
                                MethodReturn.VOID
                        ).apply(methodVisitor, instrumentationContext);
                        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && instrumentedType.equals(((Appender) other).instrumentedType);
                    }

                    @Override
                    public int hashCode() {
                        return instrumentedType.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "Pipe.Binder.Redirection.ConstructorCall.Appender{instrumentedType=" + instrumentedType + '}';
                    }
                }
            }

            /**
             * The instrumentation to implement a
             * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe.Binder.Redirection}'s
             * forwarding method.
             */
            private static class MethodCall implements Instrumentation {

                /**
                 * The method that is invoked by the implemented method.
                 */
                private final MethodDescription redirectedMethod;

                /**
                 * The assigner to be used for invoking the forwarded method.
                 */
                private final Assigner assigner;

                /**
                 * Creates a new method call instrumentation.
                 *
                 * @param redirectedMethod The method that is invoked by the implemented method.
                 * @param assigner         The assigner to be used for invoking the forwarded method.
                 */
                private MethodCall(MethodDescription redirectedMethod, Assigner assigner) {
                    this.redirectedMethod = redirectedMethod;
                    this.assigner = assigner;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public ByteCodeAppender appender(Target instrumentationTarget) {
                    return new Appender(instrumentationTarget.getTypeDescription());
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && redirectedMethod.equals(((MethodCall) other).redirectedMethod)
                            && assigner.equals(((MethodCall) other).assigner);
                }

                @Override
                public int hashCode() {
                    return redirectedMethod.hashCode() + 31 * assigner.hashCode();
                }

                @Override
                public String toString() {
                    return "Pipe.Binder.Redirection.MethodCall{" +
                            "redirectedMethod=" + redirectedMethod +
                            ", assigner=" + assigner +
                            '}';
                }

                /**
                 * The appender for implementing the
                 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe.Binder.Redirection.MethodCall}.
                 */
                private class Appender implements ByteCodeAppender {

                    /**
                     * The instrumented type that is implemented.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * Creates a new appender.
                     *
                     * @param instrumentedType The instrumented type to be implemented.
                     */
                    private Appender(TypeDescription instrumentedType) {
                        this.instrumentedType = instrumentedType;
                    }

                    @Override
                    public boolean appendsCode() {
                        return true;
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor,
                                      Context instrumentationContext,
                                      MethodDescription instrumentedMethod) {
                        StackManipulation thisReference = MethodVariableAccess.forType(instrumentedType).loadFromIndex(0);
                        FieldList fieldList = instrumentedType.getDeclaredFields();
                        StackManipulation[] fieldLoading = new StackManipulation[fieldList.size()];
                        int index = 0;
                        for (FieldDescription fieldDescription : fieldList) {
                            fieldLoading[index++] = new StackManipulation.Compound(thisReference, FieldAccess.forField(fieldDescription).getter());
                        }
                        StackManipulation.Size stackSize = new StackManipulation.Compound(
                                MethodVariableAccess.REFERENCE.loadFromIndex(1),
                                assigner.assign(new TypeDescription.ForLoadedType(Object.class), redirectedMethod.getDeclaringType(), true),
                                new StackManipulation.Compound(fieldLoading),
                                MethodInvocation.invoke(redirectedMethod),
                                assigner.assign(redirectedMethod.getReturnType(), instrumentedMethod.getReturnType(), false),
                                MethodReturn.ANY_REFERENCE
                        ).apply(methodVisitor, instrumentationContext);
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
                                && instrumentedType.equals(((Appender) other).instrumentedType)
                                && MethodCall.this.equals(((Appender) other).getMethodCall());
                    }

                    @Override
                    public int hashCode() {
                        return 31 * MethodCall.this.hashCode() + instrumentedType.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "Pipe.Binder.Redirection.MethodCall.Appender{" +
                                "methodCall=" + MethodCall.this +
                                ", instrumentedType=" + instrumentedType +
                                '}';
                    }
                }
            }
        }

        /**
         * A precomputed finding for an installed type of a
         * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe.Binder}. By using this precomputed
         * result, method look-ups can be avoided.
         */
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
