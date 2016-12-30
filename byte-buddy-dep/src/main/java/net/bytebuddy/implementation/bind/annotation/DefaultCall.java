package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.auxiliary.MethodCallProxy;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;

import java.lang.annotation.*;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A parameter with this annotation is assigned a proxy for invoking a default method that fits the intercepted method.
 * If no suitable default method for the intercepted method can be identified, the target method with the annotated
 * parameter is considered to be unbindable.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DefaultCall {

    /**
     * If this parameter is not explicitly set, a parameter with the
     * {@link net.bytebuddy.implementation.bind.annotation.DefaultCall} is only bound to a
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
     * Assigns {@code null} to the parameter if it is impossible to invoke the super method or a possible dominant default method, if permitted.
     *
     * @return {@code true} if a {@code null} constant should be assigned to this parameter in case that a legal binding is impossible.
     */
    boolean nullIfImpossible() default false;

    /**
     * A binder for handling the
     * {@link net.bytebuddy.implementation.bind.annotation.DefaultCall}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<DefaultCall> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A reference to the target type method of the default call annotation.
         */
        private static final MethodDescription.InDefinedShape TARGET_TYPE;

        /**
         * A reference to the serializable proxy method of the default call annotation.
         */
        private static final MethodDescription.InDefinedShape SERIALIZABLE_PROXY;

        /**
         * A reference to the null if possible method of the default call annotation.
         */
        private static final MethodDescription.InDefinedShape NULL_IF_IMPOSSIBLE;

        /*
         * Looks up method constants of the default call annotation.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> annotationProperties = new TypeDescription.ForLoadedType(DefaultCall.class).getDeclaredMethods();
            TARGET_TYPE = annotationProperties.filter(named("targetType")).getOnly();
            SERIALIZABLE_PROXY = annotationProperties.filter(named("serializableProxy")).getOnly();
            NULL_IF_IMPOSSIBLE = annotationProperties.filter(named("nullIfImpossible")).getOnly();
        }

        @Override
        public Class<DefaultCall> getHandledType() {
            return DefaultCall.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<DefaultCall> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            TypeDescription targetType = target.getType().asErasure();
            if (!targetType.represents(Runnable.class) && !targetType.represents(Callable.class) && !targetType.represents(Object.class)) {
                throw new IllegalStateException("A default method call proxy can only be assigned to Runnable or Callable types: " + target);
            } else if (source.isConstructor()) {
                return annotation.getValue(NULL_IF_IMPOSSIBLE).resolve(Boolean.class)
                        ? new MethodDelegationBinder.ParameterBinding.Anonymous(NullConstant.INSTANCE)
                        : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            TypeDescription typeDescription = annotation.getValue(TARGET_TYPE).resolve(TypeDescription.class);
            Implementation.SpecialMethodInvocation specialMethodInvocation = (typeDescription.represents(void.class)
                    ? DefaultMethodLocator.Implicit.INSTANCE
                    : new DefaultMethodLocator.Explicit(typeDescription)).resolve(implementationTarget, source);
            StackManipulation stackManipulation;
            if (specialMethodInvocation.isValid()) {
                stackManipulation = new MethodCallProxy.AssignableSignatureCall(specialMethodInvocation, annotation.getValue(SERIALIZABLE_PROXY).resolve(Boolean.class));
            } else if (annotation.loadSilent().nullIfImpossible()) {
                stackManipulation = NullConstant.INSTANCE;
            } else {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
            return new MethodDelegationBinder.ParameterBinding.Anonymous(stackManipulation);
        }

        @Override
        public String toString() {
            return "DefaultCall.Binder." + name();
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
                    Implementation.SpecialMethodInvocation specialMethodInvocation = Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
                    for (TypeDescription candidate : implementationTarget.getInstrumentedType().getInterfaces().asErasures()) {
                        if (source.isSpecializableFor(candidate)) {
                            if (specialMethodInvocation.isValid()) {
                                return Implementation.SpecialMethodInvocation.Illegal.INSTANCE;
                            }
                            specialMethodInvocation = implementationTarget.invokeDefault(candidate, source.asSignatureToken());
                        }
                    }
                    return specialMethodInvocation;
                }

                @Override
                public String toString() {
                    return "DefaultCall.Binder.DefaultMethodLocator.Implicit." + name();
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
                public Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source) {
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
                    return "DefaultCall.Binder.DefaultMethodLocator.Explicit{typeDescription=" + typeDescription + '}';
                }
            }
        }
    }
}
