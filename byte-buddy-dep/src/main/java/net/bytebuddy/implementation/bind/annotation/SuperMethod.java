package net.bytebuddy.implementation.bind.annotation;

import com.google.auto.value.AutoValue;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
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

/**
 * A parameter with this annotation is assigned an instance of {@link Method} which invokes the super implementation of this method.
 * If such a method is not available, this annotation causes that this delegation target cannot be bound unless {@link SuperMethod#nullIfImpossible()}
 * is set to {@code true}. The method is declared as {@code public} and is invokable unless the instrumented type itself is not visible. Note that
 * requesting such a method exposes the super method to reflection.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SuperMethod {

    /**
     * Indicates if the instance assigned to this parameter should be stored in a static field for reuse.
     *
     * @return {@code true} if this method instance should be cached.
     */
    boolean cached() default true;

    /**
     * Indicates that the assigned method should attemt the invocation of an unambiguous default method if no super method is available.
     *
     * @return {@code true} if a default method should be invoked if it is not ambiguous and no super class method is available.
     */
    boolean fallbackToDefault() default true;

    /**
     * Indicates that {@code null} should be assigned to this parameter if no super method is invokable.
     *
     * @return {@code true} if {@code null} should be assigned if no valid method can be assigned.
     */
    boolean nullIfImpossible() default false;

    /**
     * A binder for the {@link SuperMethod} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<SuperMethod> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<SuperMethod> getHandledType() {
            return SuperMethod.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(final AnnotationDescription.Loadable<SuperMethod> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            if (!target.getType().asErasure().isAssignableFrom(Method.class)) {
                throw new IllegalStateException("Cannot assign Method type to " + target);
            } else if (source.isMethod()) {
                Implementation.SpecialMethodInvocation specialMethodInvocation = annotation.loadSilent().fallbackToDefault()
                        ? implementationTarget.invokeDominant(source.asSignatureToken())
                        : implementationTarget.invokeSuper(source.asSignatureToken());
                if (specialMethodInvocation.isValid()) {
                    return new MethodDelegationBinder.ParameterBinding.Anonymous(new DelegationMethod(specialMethodInvocation, annotation.loadSilent().cached()));
                } else if (annotation.loadSilent().nullIfImpossible()) {
                    return new MethodDelegationBinder.ParameterBinding.Anonymous(NullConstant.INSTANCE);
                } else {
                    return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
                }
            } else if (annotation.loadSilent().nullIfImpossible()) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(NullConstant.INSTANCE);
            } else {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
        }

        /**
         * Loads the delegation method constant onto the stack.
         */
        @AutoValue
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
             * Creates a new delegation method.
             *
             * @param specialMethodInvocation The special method invocation that represents the super method call.
             * @param cached                  {@code true} if the method constant should be cached.
             */
            protected DelegationMethod(Implementation.SpecialMethodInvocation specialMethodInvocation, boolean cached) {
                this.specialMethodInvocation = specialMethodInvocation;
                this.cached = cached;
            }

            @Override
            public boolean isValid() {
                return specialMethodInvocation.isValid();
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                StackManipulation stackManipulation = MethodConstant.forMethod(implementationContext.registerAccessorFor(specialMethodInvocation,
                        MethodAccessorFactory.AccessType.PUBLIC));
                return (cached
                        ? FieldAccess.forField(implementationContext.cache(stackManipulation, new TypeDescription.ForLoadedType(Method.class))).read()
                        : stackManipulation).apply(methodVisitor, implementationContext);
            }
        }
    }
}
