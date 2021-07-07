package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
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
 * This annotation instructs Byte Buddy to inject a proxy class that calls a method's super method with 此注释指示Byte Buddy注入代理类，该代理类使用显式参数调用方法的super方法
 * explicit arguments. For this, the {@link Morph.Binder}
 * needs to be installed for an interface type that takes an argument of the array type {@link java.lang.Object} and  为此，需要为接口类型安装{@link Morph.Binder}，该接口类型接受数组类型 {@link java.lang.Object} 的参数并返回非数组类型的 {@link java.lang.Object}
 * returns a non-array type of {@link java.lang.Object}. This is an alternative to using the
 * {@link net.bytebuddy.implementation.bind.annotation.SuperCall} or
 * {@link net.bytebuddy.implementation.bind.annotation.DefaultCall} annotations which call a super
 * method using the same arguments as the intercepted method was invoked with. 这是使用{@link net.bytebuddy.implementation.bind.annotation.SuperCall} 或 {@link net.bytebuddy.implementation.bind.annotation.DefaultCall} 注解的替代方法，这些注解使用与被调用的方法
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
     * A binder for the {@link net.bytebuddy.implementation.bind.annotation.Morph} annotation. {@link net.bytebuddy.implementation.bind.annotation.Morph}注解绑定器
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph> {

        /**
         * A reference to the serializable proxy method. 对可序列化代理方法的引用
         */
        private static final MethodDescription.InDefinedShape SERIALIZABLE_PROXY;

        /**
         * A reference to the default method method. 对默认方法的引用
         */
        private static final MethodDescription.InDefinedShape DEFAULT_METHOD;

        /**
         * A reference to the default target method. 对默认目标方法的引用
         */
        private static final MethodDescription.InDefinedShape DEFAULT_TARGET;

        /*
         * Looks up references for all annotation properties of the morph annotation. 查找 @Morph 注解的所有注解属性的方法引用
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methodList = TypeDescription.ForLoadedType.of(Morph.class).getDeclaredMethods();
            SERIALIZABLE_PROXY = methodList.filter(named("serializableProxy")).getOnly();
            DEFAULT_METHOD = methodList.filter(named("defaultMethod")).getOnly();
            DEFAULT_TARGET = methodList.filter(named("defaultTarget")).getOnly();
        }

        /**
         * The method which is overridden for generating the proxy class. 用于生成代理类的方法被重写
         */
        private final MethodDescription forwardingMethod;

        /**
         * Creates a new binder.
         *
         * @param forwardingMethod The method which is overridden for generating the proxy class. 用于生成代理类的方法被重写
         */
        protected Binder(MethodDescription forwardingMethod) {
            this.forwardingMethod = forwardingMethod;
        }

        /**
         * Installs a given type for use on a {@link net.bytebuddy.implementation.bind.annotation.Morph}
         * annotation. The given type must be an interface without any super interfaces and a single method which
         * maps an {@link java.lang.Object} array to a {@link java.lang.Object} type. The use of generics is
         * permitted. 安装给定类型以用于 {@link net.bytebuddy.implementation.bind.annotation.Morph} 注解。 给定的类型必须是没有任何超级接口的接口，并且必须是将 {@link java.lang.Object} 数组映射到 {@link java.lang.Object} 类型的单个方法。 允许使用泛型
         *
         * @param type The type to install.
         * @return A binder for the {@link net.bytebuddy.implementation.bind.annotation.Morph}
         * annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph> install(Class<?> type) {
            return install(TypeDescription.ForLoadedType.of(type));
        }

        /**
         * Installs a given type for use on a {@link net.bytebuddy.implementation.bind.annotation.Morph} 安装给定类型以用于{@link net.bytebuddy.implementation.bind.annotation.Morph}注解
         * annotation. The given type must be an interface without any super interfaces and a single method which
         * maps an {@link java.lang.Object} array to a {@link java.lang.Object} type. The use of generics is
         * permitted.
         *
         * @param typeDescription The type to install. 要安装的类型
         * @return A binder for the {@link net.bytebuddy.implementation.bind.annotation.Morph} {@link net.bytebuddy.implementation.bind.annotation.Morph}注解的绑定器
         * annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<Morph> install(TypeDescription typeDescription) {
            return new Binder(onlyMethod(typeDescription));
        }

        /**
         * Extracts the only method of a given type and validates to fit the constraints of the morph annotation. 提取给定类型的唯一方法，并进行验证以适合变形注解的约束
         *
         * @param typeDescription The type to extract the method from. 从中提取方法的类型
         * @return The only method after validation. 验证后的唯一方法
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
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
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

        /**
         * A default method locator is responsible for looking up a default method to a given source method. 默认方法定位器负责查找给定源方法的默认方法
         */
        protected interface DefaultMethodLocator {

            /**
             * Locates the correct default method to a given source method. 将正确的默认方法定位到给定的源方法
             *
             * @param implementationTarget The current implementation target.
             * @param source               The source method for which a default method should be looked up.
             * @return A special method invocation of the default method or an illegal special method invocation,
             * if no suitable invocation could be located. 默认方法的特殊方法调用或非法的特殊方法调用(如果找不到合适的调用)
             */
            Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget,
                                                           MethodDescription source);

            /**
             * An implicit default method locator that only permits the invocation of a default method if the source
             * method itself represents a method that was defined on a default method interface. 一种隐式默认方法定位器，仅当源方法本身表示在默认方法接口上定义的方法时，才允许调用默认方法
             */
            enum Implicit implements DefaultMethodLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source) {
                    return implementationTarget.invokeDefault(source.asSignatureToken());
                }
            }

            /**
             * An explicit default method locator attempts to look up a default method in the specified interface type. 显式默认方法定位器尝试在指定的接口类型中查找默认方法
             */
            @HashCodeAndEqualsPlugin.Enhance
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
                public Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source) {
                    if (!typeDescription.isInterface()) {
                        throw new IllegalStateException(source + " method carries default method call parameter on non-interface type");
                    }
                    return implementationTarget.invokeDefault(source.asSignatureToken(), typeDescription);
                }
            }
        }

        /**
         * A proxy that implements the installed interface in order to allow for a morphed super method invocation. 实现已安装接口以便允许变形的超级方法调用的代理
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class RedirectionProxy implements AuxiliaryType, StackManipulation {

            /**
             * The name of the field that carries an instance for invoking a super method on. 字段的名称，该字段包含用于在上调用超级方法的实例
             */
            protected static final String FIELD_NAME = "target";

            /**
             * The interface type that is implemented by the generated proxy. 由生成的代理实现的接口类型
             */
            private final TypeDescription morphingType;

            /**
             * The type that is instrumented on which the super method is invoked. 在其上调用超级方法的插桩类型
             */
            private final TypeDescription instrumentedType;

            /**
             * The special method invocation to be executed by the morphing type via an accessor on the
             * instrumented type.  变形类型通过插桩类型上的访问器执行的特殊方法调用
             */
            private final Implementation.SpecialMethodInvocation specialMethodInvocation;

            /**
             * The assigner to use.
             */
            private final Assigner assigner;

            /**
             * Determines if the generated proxy should be {@link java.io.Serializable}. 确定生成的代理是否应为{@link java.io.Serializable}.
             */
            private final boolean serializableProxy;

            /**
             * Creates a new redirection proxy.
             *
             * @param morphingType            The interface type that is implemented by the generated proxy.            由生成的代理实现的接口类型
             * @param instrumentedType        The type that is instrumented on which the super method is invoked.       在其上调用超级方法的插桩类型
             * @param specialMethodInvocation The special method invocation to be executed by the morphing type via
             *                                an accessor on the instrumented type.                     变形类型通过插桩类型上的访问器执行的特殊方法调用
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
                        .with(TypeValidation.DISABLED)
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

            /**
             * Creates an instance of the proxy when instrumenting a static method. 在插桩静态方法时创建代理实例
             */
            protected enum StaticFieldConstructor implements Implementation {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * A reference of the {@link Object} type default constructor. {@link Object}类型默认构造函数的引用
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
            }

            /**
             * Creates an instance of the proxy when instrumenting an instance method. 检测实例方法时，创建代理的实例
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class InstanceFieldConstructor implements Implementation {

                /**
                 * The instrumented type.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * Creates a new instance field constructor implementation. 创建新的实例字段构造函数实现
                 *
                 * @param instrumentedType The instrumented type. 插桩类型
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

                /**
                 * The byte code appender that implements the constructor. 实现构造函数的字节码附加器
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class Appender implements ByteCodeAppender {

                    /**
                     * The field that carries the instance on which the super method is invoked. 包含在其上调用super方法的实例的字段
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
                }
            }

            /**
             * Implements a the method call of the morphing method. 实现变形方法的方法调用
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class MethodCall implements Implementation {

                /**
                 * The accessor method to invoke from the proxy's method. 要从代理的方法调用的访问器方法
                 */
                private final MethodDescription accessorMethod;

                /**
                 * The assigner to be used.
                 */
                private final Assigner assigner;

                /**
                 * Creates a new method call implementation for a proxy method.
                 *
                 * @param accessorMethod The accessor method to invoke from the proxy's method. 要从代理的方法调用的访问器方法
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

                /**
                 * The byte code appender to implement the method. 实现方法的字节码附加器
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
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
                }
            }
        }
    }
}
