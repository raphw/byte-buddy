package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.auxiliary.TypeProxy;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Parameters that are annotated with this annotation are assigned an instance of an auxiliary proxy type that allows calling
 * any default method of an interface of the instrumented type where the parameter type must be an interface that is
 * directly implemented by the instrumented type. The generated proxy will directly implement the parameter's
 * interface. If the interface of the annotation is not implemented by the instrumented type, the method with this
 * parameter is not considered as a binding target.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Default {

    /**
     * Determines if the generated proxy should be {@link java.io.Serializable}. If the annotated type
     * already is serializable, such an explicit specification is not required.
     *
     * @return {@code true} if the generated proxy should be {@link java.io.Serializable}.
     */
    boolean serializableProxy() default false;

    /**
     * Determines the type that is implemented by the proxy. When this value is set to its default value
     * {@code void}, the proxy is created as an instance of the parameter's type. It is <b>not</b> possible to
     * set the value of this property to {@link net.bytebuddy.dynamic.TargetType} as a interface cannot implement itself.
     *
     * @return The type of the proxy or an indicator type, i.e. {@code void}.
     */
    Class<?> proxyType() default void.class;

    /**
     * A binder for the {@link net.bytebuddy.implementation.bind.annotation.Default} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Default> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A method reference to the serializable proxy property.
         */
        private static final MethodDescription.InDefinedShape SERIALIZABLE_PROXY;

        /**
         * A method reference to the proxy type property.
         */
        private static final MethodDescription.InDefinedShape PROXY_TYPE;

        /*
         * Extracts method references of the default annotation.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> annotationProperties = new TypeDescription.ForLoadedType(Default.class).getDeclaredMethods();
            SERIALIZABLE_PROXY = annotationProperties.filter(named("serializableProxy")).getOnly();
            PROXY_TYPE = annotationProperties.filter(named("proxyType")).getOnly();
        }

        @Override
        public Class<Default> getHandledType() {
            return Default.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Default> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            TypeDescription proxyType = TypeLocator.ForType.of(annotation.getValue(PROXY_TYPE, TypeDescription.class)).resolve(target.getType());
            if (!proxyType.isInterface()) {
                throw new IllegalStateException(target + " uses the @Default annotation on an invalid type");
            }
            if (source.isStatic() || !implementationTarget.getTypeDescription().getInterfaces().asErasures().contains(proxyType)) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            } else {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new TypeProxy.ForDefaultMethod(proxyType,
                        implementationTarget,
                        annotation.getValue(SERIALIZABLE_PROXY, Boolean.class)));
            }
        }

        @Override
        public String toString() {
            return "Default.Binder." + name();
        }

        /**
         * Locates the type which should be the base type of the created proxy.
         */
        protected interface TypeLocator {

            /**
             * Resolves the target type.
             *
             * @param parameterType The type of the target parameter.
             * @return The proxy type.
             */
            TypeDescription resolve(GenericTypeDescription parameterType);

            /**
             * A type locator that yields the target parameter's type.
             */
            enum ForParameterType implements TypeLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public TypeDescription resolve(GenericTypeDescription parameterType) {
                    return parameterType.asErasure();
                }

                @Override
                public String toString() {
                    return "Default.Binder.TypeLocator.ForParameterType." + name();
                }
            }

            /**
             * A type locator that returns a given type.
             */
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
                    } else if (!typeDescription.isInterface()) {
                        throw new IllegalStateException("Cannot assign proxy to " + typeDescription);
                    } else {
                        return new ForType(typeDescription);
                    }
                }

                @Override
                public TypeDescription resolve(GenericTypeDescription parameterType) {
                    if (!typeDescription.isAssignableTo(parameterType.asErasure())) {
                        throw new IllegalStateException("Impossible to assign " + typeDescription + " to parameter of type " + parameterType);
                    }
                    return typeDescription;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ForType forType = (ForType) other;
                    return typeDescription.equals(forType.typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "Default.Binder.TypeLocator.ForType{" +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }
        }
    }
}

