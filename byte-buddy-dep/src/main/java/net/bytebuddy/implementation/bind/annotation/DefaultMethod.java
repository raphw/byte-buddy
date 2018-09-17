package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.MethodConstant;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.*;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A parameter with this annotation is assigned an instance of {@link Method} which invokes a default method implementation of this method.
 * If such a method is not available, this annotation causes that this delegation target cannot be bound unless {@link DefaultMethod#nullIfImpossible()}
 * is set to {@code true}. The method is declared as {@code public} and is invokable unless the instrumented type itself is not visible. Note that
 * requesting such a method exposes the super method to reflection.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DefaultMethod {

    /**
     * Indicates if the instance assigned to this parameter should be stored in a static field for reuse.
     *
     * @return {@code true} if this method instance should be cached.
     */
    boolean cached() default true;

    /**
     * Indicates if the instance assigned to this parameter should be looked up using an {@link java.security.AccessController}.
     *
     * @return {@code true} if this method should be looked up using an {@link java.security.AccessController}.
     */
    boolean privileged() default false;

    /**
     * Specifies an explicit type that declares the default method to invoke.
     *
     * @return The type declaring the method to invoke or {@link TargetType} to indicate that the instrumented method declared the method.
     */
    Class<?> targetType() default void.class;

    /**
     * Indicates that {@code null} should be assigned to this parameter if no default method is invokable.
     *
     * @return {@code true} if {@code null} should be assigned if no valid method can be assigned.
     */
    boolean nullIfImpossible() default false;

    /**
     * A binder for the {@link DefaultMethod} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<DefaultMethod> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * The {@link DefaultMethod#cached()} property.
         */
        private static final MethodDescription.InDefinedShape CACHED;

        /**
         * The {@link DefaultMethod#privileged()} property.
         */
        private static final MethodDescription.InDefinedShape PRIVILEGED;

        /**
         * The {@link DefaultMethod#targetType()} property.
         */
        private static final MethodDescription.InDefinedShape TARGET_TYPE;

        /**
         * The {@link DefaultMethod#nullIfImpossible()} property.
         */
        private static final MethodDescription.InDefinedShape NULL_IF_IMPOSSIBLE;

        /*
         * Locates method constants for properties of the default method annotation.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methodList = TypeDescription.ForLoadedType.of(DefaultMethod.class).getDeclaredMethods();
            CACHED = methodList.filter(named("cached")).getOnly();
            PRIVILEGED = methodList.filter(named("privileged")).getOnly();
            TARGET_TYPE = methodList.filter(named("targetType")).getOnly();
            NULL_IF_IMPOSSIBLE = methodList.filter(named("nullIfImpossible")).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public Class<DefaultMethod> getHandledType() {
            return DefaultMethod.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(final AnnotationDescription.Loadable<DefaultMethod> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().asErasure().isAssignableFrom(Method.class)) {
                throw new IllegalStateException("Cannot assign Method type to " + target);
            } else if (source.isMethod()) {
                TypeDescription typeDescription = annotation.getValue(TARGET_TYPE).resolve(TypeDescription.class);
                Implementation.SpecialMethodInvocation specialMethodInvocation = (typeDescription.represents(void.class)
                        ? MethodLocator.ForImplicitType.INSTANCE
                        : new MethodLocator.ForExplicitType(typeDescription)).resolve(implementationTarget, source);
                if (specialMethodInvocation.isValid()) {
                    return new MethodDelegationBinder.ParameterBinding.Anonymous(new DelegationMethod(specialMethodInvocation,
                            annotation.getValue(CACHED).resolve(Boolean.class),
                            annotation.getValue(PRIVILEGED).resolve(Boolean.class)));
                } else if (annotation.getValue(NULL_IF_IMPOSSIBLE).resolve(Boolean.class)) {
                    return new MethodDelegationBinder.ParameterBinding.Anonymous(NullConstant.INSTANCE);
                } else {
                    return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
                }
            } else if (annotation.getValue(NULL_IF_IMPOSSIBLE).resolve(Boolean.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(NullConstant.INSTANCE);
            } else {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
        }

        /**
         * A method locator is responsible for creating the super method call.
         */
        protected interface MethodLocator {

            /**
             * Resolves the special method invocation to this target.
             *
             * @param implementationTarget The implementation target.
             * @param source               The method being instrumented.
             * @return A special method invocation that represents the super call of this binding.
             */
            Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source);

            /**
             * A method locator for an implicit target type.
             */
            enum ForImplicitType implements MethodLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source) {
                    return implementationTarget.invokeDefault(source.asSignatureToken());
                }
            }

            /**
             * A method locator for an explicit target type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForExplicitType implements MethodLocator {

                /**
                 * The explicit target type.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a method locator for an explicit target.
                 *
                 * @param typeDescription The explicit target type.
                 */
                protected ForExplicitType(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Implementation.SpecialMethodInvocation resolve(Implementation.Target implementationTarget, MethodDescription source) {
                    if (!typeDescription.isInterface()) {
                        throw new IllegalStateException(source + " method carries default method call parameter on non-interface type");
                    }
                    return implementationTarget.invokeDefault(source.asSignatureToken(), TargetType.resolve(typeDescription, implementationTarget.getInstrumentedType()));
                }
            }
        }

        /**
         * Loads the delegation method constant onto the stack.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class DelegationMethod implements StackManipulation {

            /**
             * The special method invocation that represents the super method call.
             */
            private final Implementation.SpecialMethodInvocation specialMethodInvocation;

            /**
             * {@code true} if the method constant should be cached.
             */
            private final boolean cached;

            /**
             * {@code true} if the method should be looked up using an {@link java.security.AccessController}.
             */
            private final boolean privileged;

            /**
             * Creates a new delegation method.
             *
             * @param specialMethodInvocation The special method invocation that represents the super method call.
             * @param cached                  {@code true} if the method constant should be cached.
             * @param privileged              {@code true} if the method should be looked up using an {@link java.security.AccessController}.
             */
            protected DelegationMethod(Implementation.SpecialMethodInvocation specialMethodInvocation, boolean cached, boolean privileged) {
                this.specialMethodInvocation = specialMethodInvocation;
                this.cached = cached;
                this.privileged = privileged;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isValid() {
                return specialMethodInvocation.isValid();
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                StackManipulation methodConstant = privileged
                        ? MethodConstant.ofPrivileged(implementationContext.registerAccessorFor(specialMethodInvocation, MethodAccessorFactory.AccessType.PUBLIC))
                        : MethodConstant.of(implementationContext.registerAccessorFor(specialMethodInvocation, MethodAccessorFactory.AccessType.PUBLIC));
                return (cached
                        ? FieldAccess.forField(implementationContext.cache(methodConstant, TypeDescription.ForLoadedType.of(Method.class))).read()
                        : methodConstant).apply(methodVisitor, implementationContext);
            }
        }
    }
}
