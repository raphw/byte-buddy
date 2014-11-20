package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy;

import java.lang.annotation.*;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A parameter with this annotation is assigned a proxy for invoking a default method that fits the intercepted method.
 * If no suitable default method for the intercepted method can be identified, the target method with the annotated
 * parameter is considered to be unbindable.
 *
 * @see net.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DefaultCall {

    /**
     * If this parameter is not explicitly set, a parameter with the
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.DefaultCall} is only bound to a
     * source method if this source method directly represents an unambiguous, invokable default method. On the other
     * hand, if a method is not defined unambiguously by an interface, not setting this parameter will exclude
     * the target method with the annotated parameter from a binding to the source method.
     * <p>&nbsp;</p>
     * If this parameter is however set to an explicit interface type, a default method is always invoked on this given
     * type as long as this type defines a method with a compatible signature. If this is not the case, the target
     * method with the annotated parameter is not longer considered as a possible binding candidate of a source method.
     *
     * @return The target interface that a default method invocation is to be defined upon. If no such explicit target
     * is set, this parameter should not be defined as the predefined {@code void} type encodes an implicit resolution.
     */
    Class<?> targetType() default void.class;

    /**
     * Determines if the generated proxy should be {@link java.io.Serializable}.
     *
     * @return {@code true} if the generated proxy should be {@link java.io.Serializable}.
     */
    boolean serializableProxy() default false;

    /**
     * A binder for handling the
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.DefaultCall}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<DefaultCall> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A reference to the target type method of the default call annotation.
         */
        private static final MethodDescription TARGET_TYPE;

        /**
         * A reference to the serializable proxy method of the default call annotation.
         */
        private static final MethodDescription SERIALIZABLE_PROXY;

        /**
         * Finds references to the methods of the default call annotation.
         */
        static {
            MethodList annotationProperties = new TypeDescription.ForLoadedType(DefaultCall.class).getDeclaredMethods();
            TARGET_TYPE = annotationProperties.filter(named("targetType")).getOnly();
            SERIALIZABLE_PROXY = annotationProperties.filter(named("serializableProxy")).getOnly();
        }

        @Override
        public Class<DefaultCall> getHandledType() {
            return DefaultCall.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<DefaultCall> annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            TypeDescription targetType = target.getParameterTypes().get(targetParameterIndex);
            if (!targetType.represents(Runnable.class) && !targetType.represents(Callable.class) && !targetType.represents(Object.class)) {
                throw new IllegalStateException("A default method call proxy can only be assigned to Runnable or Callable types: " + target);
            }
            TypeDescription typeDescription = annotation.getValue(TARGET_TYPE, TypeDescription.class);
            Instrumentation.SpecialMethodInvocation specialMethodInvocation = (typeDescription.represents(void.class)
                    ? DefaultMethodLocator.Implicit.INSTANCE
                    : new DefaultMethodLocator.Explicit(typeDescription)).resolve(instrumentationTarget, source);
            return specialMethodInvocation.isValid()
                    ? new MethodDelegationBinder.ParameterBinding.Anonymous(new MethodCallProxy
                    .AssignableSignatureCall(specialMethodInvocation, annotation.getValue(SERIALIZABLE_PROXY, Boolean.class)))
                    : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
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
                    return "DefaultCall.Binder.DefaultMethodLocator.Explicit{typeDescription=" + typeDescription + '}';
                }
            }
        }
    }
}
