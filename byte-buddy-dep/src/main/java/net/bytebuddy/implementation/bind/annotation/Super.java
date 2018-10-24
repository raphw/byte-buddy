/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.auxiliary.TypeProxy;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.annotation.*;
import java.util.Arrays;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Parameters that are annotated with this annotation are assigned an instance of an auxiliary proxy type that allows calling
 * any {@code super} methods of the instrumented type where the parameter type must be a super type of the instrumented type.
 * The proxy type will be a direct subclass of the parameter's type such as for example a specific interface.
 * <p>&nbsp;</p>
 * Obviously, the proxy type must be instantiated before it is assigned to the intercepting method's parameter. For this
 * purpose, two strategies are available which can be specified by setting the {@link Super#strategy()} parameter which can
 * be assigned:
 * <ol>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Super.Instantiation#CONSTRUCTOR}:
 * A constructor call is made where {@link Super#constructorParameters()} determines the constructor's signature. Any constructor
 * parameter is assigned the parameter's default value when the constructor is called. Calling the default constructor is the
 * preconfigured strategy.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Super.Instantiation#UNSAFE}:
 * The proxy is created by making use of Java's {@link sun.reflect.ReflectionFactory} which is however not a public API which
 * is why it should be used with care. No constructor is called when this strategy is used. If this option is set, the
 * {@link Super#constructorParameters()} parameter is ignored.</li>
 * </ol>
 * Note that when for example intercepting a type {@code Foo} that implements some interface {@code Bar}, the proxy type
 * will only implement {@code Bar} and therefore extend {@link java.lang.Object} what allows for calling the default
 * constructor on the proxy. This implies that an interception by some method {@code qux(@Super Baz baz, @Super Bar bar)}
 * would cause the creation of two super call proxies, one extending {@code Baz}, the other extending {@code Bar}, give
 * that both types are super types of {@code Foo}.
 * <p>&nbsp;</p>
 * As an exception, no method calls to {@link Object#finalize()} are delegated by calling this method on the {@code super}-call
 * proxy by default. If this is absolutely necessary, this can however be enabled by setting {@link Super#ignoreFinalizer()}
 * to {@code false}.
 * <p>&nbsp;</p>
 * If a method parameter is not a super type of the instrumented type, the method with the parameter that is annotated by
 * #{@code Super} is not considered a possible delegation target.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Super {

    /**
     * Determines how the {@code super}call proxy type is instantiated.
     *
     * @return The instantiation strategy for this proxy.
     */
    Instantiation strategy() default Instantiation.CONSTRUCTOR;

    /**
     * If {@code true}, the proxy type will not implement {@code super} calls to {@link Object#finalize()} or any overridden methods.
     *
     * @return {@code false} if finalizer methods should be considered for {@code super}-call proxy type delegation.
     */
    boolean ignoreFinalizer() default true;

    /**
     * Determines if the generated proxy should be {@link java.io.Serializable}. If the annotated type
     * already is serializable, such an explicit specification is not required.
     *
     * @return {@code true} if the generated proxy should be {@link java.io.Serializable}.
     */
    boolean serializableProxy() default false;

    /**
     * Defines the parameter types of the constructor to be called for the created {@code super}-call proxy type.
     *
     * @return The parameter types of the constructor to be called.
     */
    Class<?>[] constructorParameters() default {};

    /**
     * Determines the type that is implemented by the proxy. When this value is set to its default value
     * {@code void}, the proxy is created as an instance of the parameter's type. When it is set to
     * {@link TargetType}, it is created as an instance of the generated class. Otherwise, the proxy type
     * is set to the given value.
     *
     * @return The type of the proxy or an indicator type, i.e. {@code void} or {@link TargetType}.
     */
    Class<?> proxyType() default void.class;

    /**
     * Determines the instantiation of the proxy type.
     *
     * @see net.bytebuddy.implementation.bind.annotation.Super
     */
    enum Instantiation {

        /**
         * A proxy instance is instantiated by its constructor. For the constructor's arguments, the parameters default
         * values are used. The constructor can be identified by setting {@link Super#constructorParameters()}.
         */
        CONSTRUCTOR {
            @Override
            protected StackManipulation proxyFor(TypeDescription parameterType,
                                                 Implementation.Target implementationTarget,
                                                 AnnotationDescription.Loadable<Super> annotation) {
                return new TypeProxy.ForSuperMethodByConstructor(parameterType,
                        implementationTarget,
                        Arrays.asList(annotation.getValue(CONSTRUCTOR_PARAMETERS).resolve(TypeDescription[].class)),
                        annotation.getValue(IGNORE_FINALIZER).resolve(Boolean.class),
                        annotation.getValue(SERIALIZABLE_PROXY).resolve(Boolean.class));
            }
        },

        /**
         * A proxy is instantiated by calling JVM internal methods and without calling a constructor. This strategy
         * might fail on exotic JVM implementations.
         */
        UNSAFE {
            @Override
            protected StackManipulation proxyFor(TypeDescription parameterType,
                                                 Implementation.Target implementationTarget,
                                                 AnnotationDescription.Loadable<Super> annotation) {
                return new TypeProxy.ForSuperMethodByReflectionFactory(parameterType,
                        implementationTarget,
                        annotation.getValue(IGNORE_FINALIZER).resolve(Boolean.class),
                        annotation.getValue(SERIALIZABLE_PROXY).resolve(Boolean.class));
            }
        };

        /**
         * A reference to the ignore finalizer method.
         */
        private static final MethodDescription.InDefinedShape IGNORE_FINALIZER;

        /**
         * A reference to the serializable proxy method.
         */
        private static final MethodDescription.InDefinedShape SERIALIZABLE_PROXY;

        /**
         * A reference to the constructor parameters method.
         */
        private static final MethodDescription.InDefinedShape CONSTRUCTOR_PARAMETERS;

        /*
         * Extracts method references to the annotation methods.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> annotationProperties = TypeDescription.ForLoadedType.of(Super.class).getDeclaredMethods();
            IGNORE_FINALIZER = annotationProperties.filter(named("ignoreFinalizer")).getOnly();
            SERIALIZABLE_PROXY = annotationProperties.filter(named("serializableProxy")).getOnly();
            CONSTRUCTOR_PARAMETERS = annotationProperties.filter(named("constructorParameters")).getOnly();
        }

        /**
         * Creates a stack manipulation which loads a {@code super}-call proxy onto the stack.
         *
         * @param parameterType        The type of the parameter that was annotated with
         *                             {@link net.bytebuddy.implementation.bind.annotation.Super}
         * @param implementationTarget The implementation target for the currently created type.
         * @param annotation           The annotation that caused this method call.
         * @return A stack manipulation representing this instance's instantiation strategy.
         */
        protected abstract StackManipulation proxyFor(TypeDescription parameterType,
                                                      Implementation.Target implementationTarget,
                                                      AnnotationDescription.Loadable<Super> annotation);
    }

    /**
     * A binder for handling the
     * {@link net.bytebuddy.implementation.bind.annotation.Super}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Super> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A method reference to the strategy property.
         */
        private static final MethodDescription.InDefinedShape STRATEGY;

        /**
         * A reference to the proxy type property.
         */
        private static final MethodDescription.InDefinedShape PROXY_TYPE;

        /*
         * Extracts method references of the super annotation.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> annotationProperties = TypeDescription.ForLoadedType.of(Super.class).getDeclaredMethods();
            STRATEGY = annotationProperties.filter(named("strategy")).getOnly();
            PROXY_TYPE = annotationProperties.filter(named("proxyType")).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public Class<Super> getHandledType() {
            return Super.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Super> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (target.getType().isPrimitive() || target.getType().isArray()) {
                throw new IllegalStateException(target + " uses the @Super annotation on an invalid type");
            }
            TypeDescription proxyType = TypeLocator.ForType
                    .of(annotation.getValue(PROXY_TYPE).resolve(TypeDescription.class))
                    .resolve(implementationTarget.getInstrumentedType(), target.getType());
            if (proxyType.isFinal()) {
                throw new IllegalStateException("Cannot extend final type as @Super proxy: " + proxyType);
            } else if (source.isStatic() || !implementationTarget.getInstrumentedType().isAssignableTo(proxyType)) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            } else {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(annotation
                        .getValue(STRATEGY).resolve(EnumerationDescription.class).load(Instantiation.class)
                        .proxyFor(proxyType, implementationTarget, annotation));
            }
        }

        /**
         * Locates the type which should be the base type of the created proxy.
         */
        protected interface TypeLocator {

            /**
             * Resolves the target type.
             *
             * @param instrumentedType The instrumented type.
             * @param parameterType    The type of the target parameter.
             * @return The proxy type.
             */
            TypeDescription resolve(TypeDescription instrumentedType, TypeDescription.Generic parameterType);

            /**
             * A type locator that yields the instrumented type.
             */
            enum ForInstrumentedType implements TypeLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription resolve(TypeDescription instrumentedType, TypeDescription.Generic parameterType) {
                    return instrumentedType;
                }
            }

            /**
             * A type locator that yields the target parameter's type.
             */
            enum ForParameterType implements TypeLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription resolve(TypeDescription instrumentedType, TypeDescription.Generic parameterType) {
                    TypeDescription erasure = parameterType.asErasure();
                    return erasure.equals(instrumentedType)
                            ? instrumentedType
                            : erasure;
                }
            }

            /**
             * A type locator that returns a given type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForType implements TypeLocator {

                /**
                 * The type to be returned upon resolution.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new type locator for a given type.
                 *
                 * @param typeDescription The type to be returned upon resolution.
                 */
                protected ForType(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * Resolves a type locator based upon an annotation value.
                 *
                 * @param typeDescription The annotation's value.
                 * @return The appropriate type locator.
                 */
                protected static TypeLocator of(TypeDescription typeDescription) {
                    if (typeDescription.represents(void.class)) {
                        return ForParameterType.INSTANCE;
                    } else if (typeDescription.represents(TargetType.class)) {
                        return ForInstrumentedType.INSTANCE;
                    } else if (typeDescription.isPrimitive() || typeDescription.isArray()) {
                        throw new IllegalStateException("Cannot assign proxy to " + typeDescription);
                    } else {
                        return new ForType(typeDescription);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription resolve(TypeDescription instrumentedType, TypeDescription.Generic parameterType) {
                    if (!typeDescription.isAssignableTo(parameterType.asErasure())) {
                        throw new IllegalStateException("Impossible to assign " + typeDescription + " to parameter of type " + parameterType);
                    }
                    return typeDescription;
                }
            }
        }
    }
}
