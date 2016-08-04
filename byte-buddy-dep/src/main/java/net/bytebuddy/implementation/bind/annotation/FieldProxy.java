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
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.annotation.*;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Using this annotation it is possible to access fields by getter and setter types. Before this annotation can be
 * used, it needs to be installed with two types. The getter type must be defined in a single-method interface
 * with a single method that returns an {@link java.lang.Object} type and takes no arguments. The getter interface
 * must similarly return {@code void} and take a single {@link java.lang.Object} argument. After installing these
 * interfaces with the {@link FieldProxy.Binder}, this
 * binder needs to be registered with a {@link net.bytebuddy.implementation.MethodDelegation} before it can be used.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FieldProxy {

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
    String value() default TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFieldBinding.BEAN_PROPERTY;

    /**
     * Determines which type defines the field that is to be accessed. If this property is not set, the most field
     * that is defined highest in the type hierarchy is accessed.
     *
     * @return The type that defines the accessed field.
     */
    Class<?> declaringType() default void.class;

    /**
     * A binder for the {@link FieldProxy} annotation.
     */
    class Binder extends TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFieldBinding<FieldProxy> {

        /**
         * A reference to the method that declares the field annotation's defining type property.
         */
        private static final MethodDescription.InDefinedShape DEFINING_TYPE;

        /**
         * A reference to the method that declares the field annotation's field name property.
         */
        private static final MethodDescription.InDefinedShape FIELD_NAME;

        /**
         * A reference to the method that declares the field annotation's serializable proxy property.
         */
        private static final MethodDescription.InDefinedShape SERIALIZABLE_PROXY;

        /*
         * Fetches a reference to all annotation properties.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methodList = new TypeDescription.ForLoadedType(FieldProxy.class).getDeclaredMethods();
            DEFINING_TYPE = methodList.filter(named("declaringType")).getOnly();
            FIELD_NAME = methodList.filter(named("value")).getOnly();
            SERIALIZABLE_PROXY = methodList.filter(named("serializableProxy")).getOnly();
        }

        /**
         * Creates a binder by installing a single proxy type where annotating a parameter with {@link FieldProxy} allows
         * getting and setting values for a given field.
         *
         * @param type A type which declares exactly one abstract getter and an abstract setter for the {@link Object}
         *             type. The type is allowed to be generic.
         * @return A binder for the {@link FieldProxy} annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldProxy> install(Class<?> type) {
            return install(new TypeDescription.ForLoadedType(type));
        }

        /**
         * Creates a binder by installing a single proxy type where annotating a parameter with {@link FieldProxy} allows
         * getting and setting values for a given field.
         *
         * @param typeDescription A type which declares exactly one abstract getter and an abstract setter for the {@link Object}
         *                        type. The type is allowed to be generic.
         * @return A binder for the {@link FieldProxy} annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldProxy> install(TypeDescription typeDescription) {
            if (!typeDescription.isInterface()) {
                throw new IllegalArgumentException(typeDescription + " is not an interface");
            } else if (!typeDescription.getInterfaces().isEmpty()) {
                throw new IllegalArgumentException(typeDescription + " must not extend other interfaces");
            } else if (!typeDescription.isPublic()) {
                throw new IllegalArgumentException(typeDescription + " is not public");
            }
            MethodList<MethodDescription.InDefinedShape> methodCandidates = typeDescription.getDeclaredMethods().filter(isAbstract());
            if (methodCandidates.size() != 2) {
                throw new IllegalArgumentException(typeDescription + " does not declare exactly two non-abstract methods");
            }
            MethodList<MethodDescription.InDefinedShape> getterCandidates = methodCandidates.filter(isGetter(Object.class));
            if (getterCandidates.size() != 1) {
                throw new IllegalArgumentException(typeDescription + " does not declare a getter with an Object type");
            }
            MethodList<MethodDescription.InDefinedShape> setterCandidates = methodCandidates.filter(isSetter(Object.class));
            if (setterCandidates.size() != 1) {
                throw new IllegalArgumentException(typeDescription + " does not declare a setter with an Object type");
            }
            return new Binder(typeDescription, getterCandidates.getOnly(), setterCandidates.getOnly());
        }

        /**
         * Creates a binder by installing two proxy types which are implemented by this binder if a field getter
         * or a field setter is requested by using the
         * {@link FieldProxy} annotation.
         *
         * @param getterType The type which should be used for getter proxies. The type must
         *                   represent an interface which defines a single method which returns an
         *                   {@link java.lang.Object} return type and does not take any arguments. The use of generics
         *                   is permitted.
         * @param setterType The type which should be uses for setter proxies. The type must
         *                   represent an interface which defines a single method which returns {@code void}
         *                   and takes a single {@link java.lang.Object}-typed argument. The use of generics
         *                   is permitted.
         * @return A binder for the {@link FieldProxy} annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldProxy> install(Class<?> getterType, Class<?> setterType) {
            return install(new TypeDescription.ForLoadedType(getterType), new TypeDescription.ForLoadedType(setterType));
        }

        /**
         * Creates a binder by installing two proxy types which are implemented by this binder if a field getter
         * or a field setter is requested by using the
         * {@link FieldProxy} annotation.
         *
         * @param getterType The type which should be used for getter proxies. The type must
         *                   represent an interface which defines a single method which returns an
         *                   {@link java.lang.Object} return type and does not take any arguments. The use of generics
         *                   is permitted.
         * @param setterType The type which should be uses for setter proxies. The type must
         *                   represent an interface which defines a single method which returns {@code void}
         *                   and takes a single {@link java.lang.Object}-typed argument. The use of generics
         *                   is permitted.
         * @return A binder for the {@link FieldProxy} annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldProxy> install(TypeDescription getterType, TypeDescription setterType) {
            MethodDescription.InDefinedShape getterMethod = onlyMethod(getterType);
            if (!getterMethod.getReturnType().asErasure().represents(Object.class)) {
                throw new IllegalArgumentException(getterMethod + " must take a single Object-typed parameter");
            } else if (getterMethod.getParameters().size() != 0) {
                throw new IllegalArgumentException(getterMethod + " must not declare parameters");
            }
            MethodDescription.InDefinedShape setterMethod = onlyMethod(setterType);
            if (!setterMethod.getReturnType().asErasure().represents(void.class)) {
                throw new IllegalArgumentException(setterMethod + " must return void");
            } else if (setterMethod.getParameters().size() != 1 || !setterMethod.getParameters().get(0).getType().asErasure().represents(Object.class)) {
                throw new IllegalArgumentException(setterMethod + " must declare a single Object-typed parameters");
            }
            return new Binder(getterMethod, setterMethod);
        }

        /**
         * Extracts the only method from a given type description which is validated for the required properties for
         * using the type as a proxy base type.
         *
         * @param typeDescription The type description to evaluate.
         * @return The only method which was found to be compatible to the proxy requirements.
         */
        private static MethodDescription.InDefinedShape onlyMethod(TypeDescription typeDescription) {
            if (!typeDescription.isInterface()) {
                throw new IllegalArgumentException(typeDescription + " is not an interface");
            } else if (!typeDescription.getInterfaces().isEmpty()) {
                throw new IllegalArgumentException(typeDescription + " must not extend other interfaces");
            } else if (!typeDescription.isPublic()) {
                throw new IllegalArgumentException(typeDescription + " is not public");
            }
            MethodList<MethodDescription.InDefinedShape> methodCandidates = typeDescription.getDeclaredMethods().filter(isAbstract());
            if (methodCandidates.size() != 1) {
                throw new IllegalArgumentException(typeDescription + " must declare exactly one abstract method");
            }
            return methodCandidates.getOnly();
        }

        /**
         * The field resolver factory to apply by this binder.
         */
        private final FieldResolver.Factory fieldResolverFactory;

        protected Binder(MethodDescription.InDefinedShape getterMethod, MethodDescription.InDefinedShape setterMethod) {
            this(new FieldResolver.Factory.Simplex(getterMethod, setterMethod));
        }

        protected Binder(TypeDescription proxyType, MethodDescription.InDefinedShape getterMethod, MethodDescription.InDefinedShape setterMethod) {
            this(new FieldResolver.Factory.Duplex(proxyType, getterMethod, setterMethod));
        }

        /**
         * Creates a new binder for a {@link FieldProxy}.
         *
         * @param fieldResolverFactory The field resolver factory to apply by this binder.
         */
        protected Binder(FieldResolver.Factory fieldResolverFactory) {
            this.fieldResolverFactory = fieldResolverFactory;
        }

        @Override
        public Class<FieldProxy> getHandledType() {
            return FieldProxy.class;
        }

        @Override
        protected String fieldName(AnnotationDescription.Loadable<FieldProxy> annotation) {
            return annotation.getValue(FIELD_NAME, String.class);
        }

        @Override
        protected TypeDescription declaringType(AnnotationDescription.Loadable<FieldProxy> annotation) {
            return annotation.getValue(DEFINING_TYPE, TypeDescription.class);
        }

        @Override
        protected MethodDelegationBinder.ParameterBinding<?> bind(FieldDescription fieldDescription,
                                                                  AnnotationDescription.Loadable<FieldProxy> annotation,
                                                                  MethodDescription source,
                                                                  ParameterDescription target,
                                                                  Implementation.Target implementationTarget,
                                                                  Assigner assigner) {
            FieldResolver fieldResolver = fieldResolverFactory.resolve(target.getType().asErasure(), fieldDescription);
            if (fieldResolver.isResolved()) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new AccessorProxy(fieldDescription,
                        implementationTarget.getInstrumentedType(),
                        fieldResolver,
                        assigner,
                        annotation.getValue(SERIALIZABLE_PROXY, Boolean.class)));
            } else {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            Binder binder = (Binder) object;
            return fieldResolverFactory.equals(binder.fieldResolverFactory);
        }

        @Override
        public int hashCode() {
            return fieldResolverFactory.hashCode();
        }

        @Override
        public String toString() {
            return "FieldProxy.Binder{" +
                    "fieldResolverFactory=" + fieldResolverFactory +
                    '}';
        }

        /**
         * A resolver for creating an instrumentation for a field access.
         */
        protected interface FieldResolver {

            /**
             * Returns {@code true} if the field access can be establised.
             *
             * @return {@code true} if the field access can be establised.
             */
            boolean isResolved();

            /**
             * Returns the type of the field access proxy.
             *
             * @return The type of the field access proxy.
             */
            TypeDescription getProxyType();

            /**
             * Applies this field resolver to a dynamic type.
             *
             * @param builder               The dynamic type builder to use.
             * @param fieldDescription      The accessed field.
             * @param assigner              The assigner to use.
             * @param methodAccessorFactory The method accessor factory to use.
             * @return The builder for creating the field accessor proxy type.
             */
            DynamicType.Builder<?> apply(DynamicType.Builder<?> builder,
                                         FieldDescription fieldDescription,
                                         Assigner assigner,
                                         AuxiliaryType.MethodAccessorFactory methodAccessorFactory);

            /**
             * A factory for creating a field resolver.
             */
            interface Factory {

                /**
                 * Creates a field resolver.
                 *
                 * @param parameterType    The type of the annotated parameter.
                 * @param fieldDescription The field being proxied.
                 * @return An appropriate field resolver.
                 */
                FieldResolver resolve(TypeDescription parameterType, FieldDescription fieldDescription);

                /**
                 * A duplex factory for a type that both sets and gets a field value.
                 */
                class Duplex implements Factory {

                    /**
                     * The type of the accessor proxy.
                     */
                    private final TypeDescription proxyType;

                    /**
                     * The getter method.
                     */
                    private final MethodDescription.InDefinedShape getterMethod;

                    /**
                     * The setter method.
                     */
                    private final MethodDescription.InDefinedShape setterMethod;

                    /**
                     * Creates a new duplex factory.
                     *
                     * @param proxyType    The type of the accessor proxy.
                     * @param getterMethod The getter method.
                     * @param setterMethod The setter method.
                     */
                    protected Duplex(TypeDescription proxyType,
                                     MethodDescription.InDefinedShape getterMethod,
                                     MethodDescription.InDefinedShape setterMethod) {
                        this.proxyType = proxyType;
                        this.getterMethod = getterMethod;
                        this.setterMethod = setterMethod;
                    }

                    @Override
                    public FieldResolver resolve(TypeDescription parameterType, FieldDescription fieldDescription) {
                        if (parameterType.equals(proxyType)) {
                            return new ForGetterSetterPair(proxyType, getterMethod, setterMethod);
                        } else {
                            throw new IllegalStateException("Cannot use @FieldProxy on a non-installed type");
                        }
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        Duplex duplex = (Duplex) object;
                        return proxyType.equals(duplex.proxyType)
                                && getterMethod.equals(duplex.getterMethod)
                                && setterMethod.equals(duplex.setterMethod);
                    }

                    @Override
                    public int hashCode() {
                        int result = proxyType.hashCode();
                        result = 31 * result + getterMethod.hashCode();
                        result = 31 * result + setterMethod.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "FieldProxy.Binder.FieldResolver.Factory.Duplex{" +
                                "proxyType=" + proxyType +
                                ", getterMethod=" + getterMethod +
                                ", setterMethod=" + setterMethod +
                                '}';
                    }
                }

                /**
                 * A simplex factory where field getters and setters both have their own type.
                 */
                class Simplex implements Factory {

                    /**
                     * The getter method.
                     */
                    private final MethodDescription.InDefinedShape getterMethod;

                    /**
                     * The setter method.
                     */
                    private final MethodDescription.InDefinedShape setterMethod;

                    /**
                     * Creates a simplex factory.
                     *
                     * @param getterMethod The getter method.
                     * @param setterMethod The setter method.
                     */
                    protected Simplex(MethodDescription.InDefinedShape getterMethod, MethodDescription.InDefinedShape setterMethod) {
                        this.getterMethod = getterMethod;
                        this.setterMethod = setterMethod;
                    }

                    @Override
                    public FieldResolver resolve(TypeDescription parameterType, FieldDescription fieldDescription) {
                        if (parameterType.equals(getterMethod.getDeclaringType())) {
                            return new ForGetter(getterMethod);
                        } else if (parameterType.equals(setterMethod.getDeclaringType())) {
                            return fieldDescription.isFinal()
                                    ? Unresolved.INSTANCE
                                    : new ForSetter(setterMethod);
                        } else {
                            throw new IllegalStateException("Cannot use @FieldProxy on a non-installed type");
                        }
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        Simplex simplex = (Simplex) object;
                        return getterMethod.equals(simplex.getterMethod) && setterMethod.equals(simplex.setterMethod);
                    }

                    @Override
                    public int hashCode() {
                        int result = getterMethod.hashCode();
                        result = 31 * result + setterMethod.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "FieldProxy.Binder.FieldResolver.Factory.Simplex{" +
                                "getterMethod=" + getterMethod +
                                ", setterMethod=" + setterMethod +
                                '}';
                    }
                }
            }

            /**
             * An unresolved field resolver.
             */
            enum Unresolved implements FieldResolver {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public boolean isResolved() {
                    return false;
                }

                @Override
                public TypeDescription getProxyType() {
                    throw new IllegalStateException("Cannot read type for unresolved field resolver");
                }

                @Override
                public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder,
                                                    FieldDescription fieldDescription,
                                                    Assigner assigner,
                                                    AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                    throw new IllegalStateException("Cannot apply unresolved field resolver");
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.FieldResolver.Unresolved." + name();
                }
            }

            /**
             * A field resolver for a getter accessor.
             */
            class ForGetter implements FieldResolver {

                /**
                 * The getter method.
                 */
                private final MethodDescription.InDefinedShape getterMethod;

                /**
                 * Creates a new getter field resolver.
                 *
                 * @param getterMethod The getter method.
                 */
                protected ForGetter(MethodDescription.InDefinedShape getterMethod) {
                    this.getterMethod = getterMethod;
                }

                @Override
                public boolean isResolved() {
                    return true;
                }

                @Override
                public TypeDescription getProxyType() {
                    return getterMethod.getDeclaringType();
                }

                @Override
                public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder,
                                                    FieldDescription fieldDescription,
                                                    Assigner assigner,
                                                    AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                    return builder.method(is(getterMethod)).intercept(new FieldGetter(fieldDescription, assigner, methodAccessorFactory));
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    ForGetter forGetter = (ForGetter) object;
                    return getterMethod.equals(forGetter.getterMethod);
                }

                @Override
                public int hashCode() {
                    return getterMethod.hashCode();
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.FieldResolver.ForGetter{" +
                            "getterMethod=" + getterMethod +
                            '}';
                }
            }

            /**
             * A field resolver for a setter accessor.
             */
            class ForSetter implements FieldResolver {

                /**
                 * The setter method.
                 */
                private final MethodDescription.InDefinedShape setterMethod;

                /**
                 * Creates a new field resolver for a setter accessor.
                 *
                 * @param setterMethod The setter method.
                 */
                protected ForSetter(MethodDescription.InDefinedShape setterMethod) {
                    this.setterMethod = setterMethod;
                }

                @Override
                public boolean isResolved() {
                    return true;
                }

                @Override
                public TypeDescription getProxyType() {
                    return setterMethod.getDeclaringType();
                }

                @Override
                public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder,
                                                    FieldDescription fieldDescription,
                                                    Assigner assigner,
                                                    AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                    return builder.method(is(setterMethod)).intercept(new FieldSetter(fieldDescription, assigner, methodAccessorFactory));
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    ForSetter forSetter = (ForSetter) object;
                    return setterMethod.equals(forSetter.setterMethod);
                }

                @Override
                public int hashCode() {
                    return setterMethod.hashCode();
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.FieldResolver.ForSetter{" +
                            "setterMethod=" + setterMethod +
                            '}';
                }
            }

            /**
             * A field resolver for an accessor that both gets and sets a field value.
             */
            class ForGetterSetterPair implements FieldResolver {

                /**
                 * The type of the accessor proxy.
                 */
                private final TypeDescription proxyType;

                /**
                 * The getter method.
                 */
                private final MethodDescription.InDefinedShape getterMethod;

                /**
                 * The setter method.
                 */
                private final MethodDescription.InDefinedShape setterMethod;

                /**
                 * Creates a new field resolver for an accessor that both gets and sets a field value.
                 *
                 * @param proxyType    The type of the accessor proxy.
                 * @param getterMethod The getter method.
                 * @param setterMethod The setter method.
                 */
                protected ForGetterSetterPair(TypeDescription proxyType,
                                              MethodDescription.InDefinedShape getterMethod,
                                              MethodDescription.InDefinedShape setterMethod) {
                    this.proxyType = proxyType;
                    this.getterMethod = getterMethod;
                    this.setterMethod = setterMethod;
                }

                @Override
                public boolean isResolved() {
                    return true;
                }

                @Override
                public TypeDescription getProxyType() {
                    return proxyType;
                }

                @Override
                public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder,
                                                    FieldDescription fieldDescription,
                                                    Assigner assigner,
                                                    AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                    return builder
                            .method(is(getterMethod)).intercept(new FieldGetter(fieldDescription, assigner, methodAccessorFactory))
                            .method(is(setterMethod)).intercept(fieldDescription.isFinal()
                                    ? ExceptionMethod.throwing(UnsupportedOperationException.class, "Cannot set final field " + fieldDescription)
                                    : new FieldSetter(fieldDescription, assigner, methodAccessorFactory));
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    ForGetterSetterPair that = (ForGetterSetterPair) object;
                    return proxyType.equals(that.proxyType)
                            && getterMethod.equals(that.getterMethod)
                            && setterMethod.equals(that.setterMethod);
                }

                @Override
                public int hashCode() {
                    int result = proxyType.hashCode();
                    result = 31 * result + getterMethod.hashCode();
                    result = 31 * result + setterMethod.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.FieldResolver.ForGetterSetterPair{" +
                            "proxyType=" + proxyType +
                            ", getterMethod=" + getterMethod +
                            ", setterMethod=" + setterMethod +
                            '}';
                }
            }
        }

        /**
         * Represents an implementation for implementing a proxy type constructor when a static field is accessed.
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
                objectTypeDefaultConstructor = TypeDescription.OBJECT.getDeclaredMethods().filter(isConstructor()).getOnly();
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public ByteCodeAppender appender(Target implementationTarget) {
                return new ByteCodeAppender.Simple(MethodVariableAccess.REFERENCE.loadOffset(0),
                        MethodInvocation.invoke(objectTypeDefaultConstructor),
                        MethodReturn.VOID);
            }

            @Override
            public String toString() {
                return "FieldProxy.Binder.StaticFieldConstructor." + name();
            }
        }

        /**
         * Represents an implementation for implementing a proxy type constructor when a non-static field is accessed.
         */
        protected static class InstanceFieldConstructor implements Implementation {

            /**
             * The instrumented type from which a field is to be accessed.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates a new implementation for implementing a field accessor proxy's constructor when accessing
             * a non-static field.
             *
             * @param instrumentedType The instrumented type from which a field is to be accessed.
             */
            protected InstanceFieldConstructor(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(new FieldDescription.Token(AccessorProxy.FIELD_NAME,
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
                return "FieldProxy.Binder.InstanceFieldConstructor{" +
                        "instrumentedType=" + instrumentedType +
                        '}';
            }

            /**
             * An appender for implementing an
             * {@link FieldProxy.Binder.InstanceFieldConstructor}.
             */
            protected static class Appender implements ByteCodeAppender {

                /**
                 * The field to be set within the constructor.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new appender.
                 *
                 * @param implementationTarget The implementation target of the current implementation.
                 */
                protected Appender(Target implementationTarget) {
                    fieldDescription = implementationTarget.getInstrumentedType()
                            .getDeclaredFields()
                            .filter((named(AccessorProxy.FIELD_NAME)))
                            .getOnly();
                }

                @Override
                public Size apply(MethodVisitor methodVisitor,
                                  Context implementationContext,
                                  MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.REFERENCE.loadOffset(0),
                            MethodInvocation.invoke(StaticFieldConstructor.INSTANCE.objectTypeDefaultConstructor),
                            MethodVariableAccess.allArgumentsOf(instrumentedMethod.asDefined()).prependThisReference(),
                            FieldAccess.forField(fieldDescription).putter(),
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
                    return "FieldProxy.Binder.InstanceFieldConstructor.Appender{" +
                            "fieldDescription=" + fieldDescription +
                            '}';
                }
            }
        }

        /**
         * Implementation for a getter method.
         */
        protected static class FieldGetter implements Implementation {

            /**
             * The field that is being accessed.
             */
            private final FieldDescription fieldDescription;

            /**
             * The assigner to use.
             */
            private final Assigner assigner;

            /**
             * The accessed type's method accessor factory.
             */
            private final AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

            /**
             * Creates a new getter implementation.
             *
             * @param fieldDescription      The field that is being accessed.
             * @param assigner              The assigner to use.
             * @param methodAccessorFactory The accessed type's method accessor factory.
             */
            protected FieldGetter(FieldDescription fieldDescription,
                                  Assigner assigner,
                                  AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                this.fieldDescription = fieldDescription;
                this.assigner = assigner;
                this.methodAccessorFactory = methodAccessorFactory;
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
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                FieldGetter that = (FieldGetter) object;
                return fieldDescription.equals(that.fieldDescription)
                        && assigner.equals(that.assigner)
                        && methodAccessorFactory.equals(that.methodAccessorFactory);
            }

            @Override
            public int hashCode() {
                int result = fieldDescription.hashCode();
                result = 31 * result + assigner.hashCode();
                result = 31 * result + methodAccessorFactory.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "FieldProxy.Binder.FieldGetter{" +
                        "fieldDescription=" + fieldDescription +
                        ", assigner=" + assigner +
                        ", methodAccessorFactory=" + methodAccessorFactory +
                        '}';
            }

            /**
             * A byte code appender for a getter method.
             */
            protected class Appender implements ByteCodeAppender {

                /**
                 * The generated accessor type.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new appender for a setter method.
                 *
                 * @param implementationTarget The implementation target of the current instrumentation.
                 */
                protected Appender(Target implementationTarget) {
                    typeDescription = implementationTarget.getInstrumentedType();
                }

                @Override
                public Size apply(MethodVisitor methodVisitor,
                                  Context implementationContext,
                                  MethodDescription instrumentedMethod) {
                    MethodDescription getterMethod = methodAccessorFactory.registerGetterFor(fieldDescription);
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            fieldDescription.isStatic()
                                    ? StackManipulation.Trivial.INSTANCE
                                    : new StackManipulation.Compound(
                                    MethodVariableAccess.REFERENCE.loadOffset(0),
                                    FieldAccess.forField(typeDescription.getDeclaredFields()
                                            .filter((named(AccessorProxy.FIELD_NAME))).getOnly()).getter()),
                            MethodInvocation.invoke(getterMethod),
                            assigner.assign(getterMethod.getReturnType(), instrumentedMethod.getReturnType(), Assigner.Typing.DYNAMIC),
                            MethodReturn.returning(instrumentedMethod.getReturnType().asErasure())
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private FieldGetter getOuter() {
                    return FieldGetter.this;
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    Appender appender = (Appender) object;
                    return typeDescription.equals(appender.typeDescription) && FieldGetter.this.equals(appender.getOuter());
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode() + 31 * FieldGetter.this.hashCode();
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.FieldGetter.Appender{" +
                            "outer=" + FieldGetter.this +
                            ", typeDescription=" + typeDescription +
                            '}';
                }
            }
        }

        /**
         * Implementation for a setter method.
         */
        protected static class FieldSetter implements Implementation {

            /**
             * The field that is being accessed.
             */
            private final FieldDescription fieldDescription;

            /**
             * The assigner to use.
             */
            private final Assigner assigner;

            /**
             * The accessed type's method accessor factory.
             */
            private final AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

            /**
             * Creates a new setter implementation.
             *
             * @param fieldDescription      The field that is being accessed.
             * @param assigner              The assigner to use.
             * @param methodAccessorFactory The accessed type's method accessor factory.
             */
            protected FieldSetter(FieldDescription fieldDescription,
                                  Assigner assigner,
                                  AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                this.fieldDescription = fieldDescription;
                this.assigner = assigner;
                this.methodAccessorFactory = methodAccessorFactory;
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
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                FieldSetter that = (FieldSetter) object;
                return fieldDescription.equals(that.fieldDescription)
                        && assigner.equals(that.assigner)
                        && methodAccessorFactory.equals(that.methodAccessorFactory);
            }

            @Override
            public int hashCode() {
                int result = fieldDescription.hashCode();
                result = 31 * result + assigner.hashCode();
                result = 31 * result + methodAccessorFactory.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "FieldProxy.Binder.FieldSetter{" +
                        "fieldDescription=" + fieldDescription +
                        ", assigner=" + assigner +
                        ", methodAccessorFactory=" + methodAccessorFactory +
                        '}';
            }

            /**
             * A byte code appender for a setter method.
             */
            protected class Appender implements ByteCodeAppender {

                /**
                 * The generated accessor type.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new appender for a setter method.
                 *
                 * @param implementationTarget The implementation target of the current instrumentation.
                 */
                protected Appender(Target implementationTarget) {
                    typeDescription = implementationTarget.getInstrumentedType();
                }

                @Override
                public Size apply(MethodVisitor methodVisitor,
                                  Context implementationContext,
                                  MethodDescription instrumentedMethod) {
                    TypeDescription.Generic parameterType = instrumentedMethod.getParameters().get(0).getType();
                    MethodDescription setterMethod = methodAccessorFactory.registerSetterFor(fieldDescription);
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            fieldDescription.isStatic()
                                    ? StackManipulation.Trivial.INSTANCE
                                    : new StackManipulation.Compound(
                                    MethodVariableAccess.REFERENCE.loadOffset(0),
                                    FieldAccess.forField(typeDescription.getDeclaredFields()
                                            .filter((named(AccessorProxy.FIELD_NAME))).getOnly()).getter()),
                            MethodVariableAccess.of(parameterType).loadOffset(1),
                            assigner.assign(parameterType, setterMethod.getParameters().get(0).getType(), Assigner.Typing.DYNAMIC),
                            MethodInvocation.invoke(setterMethod),
                            MethodReturn.VOID
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private FieldSetter getOuter() {
                    return FieldSetter.this;
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    Appender appender = (Appender) object;
                    return typeDescription.equals(appender.typeDescription) && FieldSetter.this.equals(appender.getOuter());
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode() + 31 * FieldSetter.this.hashCode();
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.FieldSetter.Appender{" +
                            "outer=" + FieldSetter.this +
                            ", typeDescription=" + typeDescription +
                            '}';
                }
            }
        }

        /**
         * A proxy type for accessing a field either by a getter or a setter.
         */
        protected class AccessorProxy implements AuxiliaryType, StackManipulation {

            /**
             * The name of the field that stores the accessed instance if any.
             */
            protected static final String FIELD_NAME = "instance";

            /**
             * The field that is being accessed.
             */
            private final FieldDescription fieldDescription;

            /**
             * The type which is accessed.
             */
            private final TypeDescription instrumentedType;

            /**
             * The field resolver to use.
             */
            private final FieldResolver fieldResolver;

            /**
             * The assigner to use.
             */
            private final Assigner assigner;

            /**
             * {@code true} if the generated proxy should be serializable.
             */
            private final boolean serializableProxy;

            /**
             * @param fieldDescription  The field that is being accessed.
             * @param instrumentedType  The type which is accessed.
             * @param fieldResolver     The field resolver to use.
             * @param assigner          The assigner to use.
             * @param serializableProxy {@code true} if the generated proxy should be serializable.
             */
            protected AccessorProxy(FieldDescription fieldDescription,
                                    TypeDescription instrumentedType,
                                    FieldResolver fieldResolver,
                                    Assigner assigner,
                                    boolean serializableProxy) {
                this.fieldDescription = fieldDescription;
                this.instrumentedType = instrumentedType;
                this.fieldResolver = fieldResolver;
                this.assigner = assigner;
                this.serializableProxy = serializableProxy;
            }

            @Override
            public DynamicType make(String auxiliaryTypeName,
                                    ClassFileVersion classFileVersion,
                                    MethodAccessorFactory methodAccessorFactory) {
                return fieldResolver.apply(new ByteBuddy(classFileVersion)
                        .subclass(fieldResolver.getProxyType(), ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .name(auxiliaryTypeName)
                        .modifiers(DEFAULT_TYPE_MODIFIER)
                        .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                        .defineConstructor().withParameters(fieldDescription.isStatic()
                                ? Collections.<TypeDescription>emptyList()
                                : Collections.singletonList(instrumentedType))
                        .intercept(fieldDescription.isStatic()
                                ? StaticFieldConstructor.INSTANCE
                                : new InstanceFieldConstructor(instrumentedType)), fieldDescription, assigner, methodAccessorFactory).make();
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                TypeDescription auxiliaryType = implementationContext.register(this);
                return new Compound(
                        TypeCreation.of(auxiliaryType),
                        Duplication.SINGLE,
                        fieldDescription.isStatic()
                                ? Trivial.INSTANCE
                                : MethodVariableAccess.REFERENCE.loadOffset(0),
                        MethodInvocation.invoke(auxiliaryType.getDeclaredMethods().filter(isConstructor()).getOnly())
                ).apply(methodVisitor, implementationContext);
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private Binder getOuter() {
                return Binder.this;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                AccessorProxy that = (AccessorProxy) object;
                return serializableProxy == that.serializableProxy
                        && fieldDescription.equals(that.fieldDescription)
                        && instrumentedType.equals(that.instrumentedType)
                        && fieldResolver.equals(that.fieldResolver)
                        && assigner.equals(that.assigner)
                        && Binder.this.equals(that.getOuter());
            }

            @Override
            public int hashCode() {
                int result = fieldDescription.hashCode();
                result = 31 * result + Binder.this.hashCode();
                result = 31 * result + instrumentedType.hashCode();
                result = 31 * result + fieldResolver.hashCode();
                result = 31 * result + assigner.hashCode();
                result = 31 * result + (serializableProxy ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return "FieldProxy.Binder.AccessorProxy{" +
                        "outer=" + Binder.this +
                        ", fieldDescription=" + fieldDescription +
                        ", instrumentedType=" + instrumentedType +
                        ", fieldResolver=" + fieldResolver +
                        ", assigner=" + assigner +
                        ", serializableProxy=" + serializableProxy +
                        '}';
            }
        }
    }
}
