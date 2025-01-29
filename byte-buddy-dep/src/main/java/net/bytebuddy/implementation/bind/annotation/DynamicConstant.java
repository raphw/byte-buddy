package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.JavaConstantValue;
import net.bytebuddy.implementation.bytecode.member.Invokedynamic;
import net.bytebuddy.utility.JavaConstant;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <p>
 * Binds a dynamic constant to the annotated parameter. The constant is either bound by using constantdynamic
 * or invokedynamic.
 * </p>
 * <p>
 * <b>Important</b>: Don't confuse this annotation with {@link net.bytebuddy.asm.Advice.DynamicConstant} or
 * {@link net.bytebuddy.asm.MemberSubstitution.DynamicConstant}. This annotation should be used with
 * {@link net.bytebuddy.implementation.MethodDelegation} only.
 * </p>
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DynamicConstant {

    /**
     * Returns the name of the dynamic constant that is supplied to the bootstrap method.
     *
     * @return The name of the dynamic constant that is supplied to the bootstrap method.
     */
    String name() default JavaConstant.Dynamic.DEFAULT_NAME;

    /**
     * Returns the type of the bootstrap method handle to resolve.
     *
     * @return The type of the bootstrap method handle to resolve.
     */
    JavaConstant.MethodHandle.HandleType bootstrapType();

    /**
     * Returns the owner type of the bootstrap method handle, or {@code void}, to represent the instrumented type.
     *
     * @return The owner type of the bootstrap method handle, or {@code void}, to represent the instrumented type.
     */
    Class<?> bootstrapOwner() default void.class;

    /**
     * Returns the name of the bootstrap method handle.
     *
     * @return The name of the bootstrap method handle.
     */
    String bootstrapName();

    /**
     * Returns the return type of the bootstrap method handle.
     *
     * @return The return type of the bootstrap method handle.
     */
    Class<?> bootstrapReturnType();

    /**
     * Returns the parameter types of the bootstrap method handle.
     *
     * @return The parameter types of the bootstrap method handle.
     */
    Class<?>[] bootstrapParameterTypes();

    /**
     * Returns {@code true} if invokedynamic should be used to bind the annotated parameter.
     *
     * @return {@code true} if invokedynamic should be used to bind the annotated parameter.
     */
    boolean invokedynamic() default false;
    
    /**
     * A binder for handling the
     * {@link net.bytebuddy.implementation.bind.annotation.DynamicConstant}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<DynamicConstant> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * The {@link DynamicConstant#name()} method.
         */
        private static final MethodDescription.InDefinedShape NAME;

        /**
         * The {@link DynamicConstant#bootstrapType()} method.
         */
        private static final MethodDescription.InDefinedShape BOOTSTRAP_TYPE;

        /**
         * The {@link DynamicConstant#bootstrapOwner()} method.
         */
        private static final MethodDescription.InDefinedShape BOOTSTRAP_OWNER;

        /**
         * The {@link DynamicConstant#bootstrapName()} method.
         */
        private static final MethodDescription.InDefinedShape BOOTSTRAP_NAME;

        /**
         * The {@link DynamicConstant#bootstrapReturnType()} method.
         */
        private static final MethodDescription.InDefinedShape BOOTSTRAP_RETURN_TYPE;

        /**
         * The {@link DynamicConstant#bootstrapParameterTypes()} method.
         */
        private static final MethodDescription.InDefinedShape BOOTSTRAP_PARAMETER_TYPES;

        /**
         * The {@link DynamicConstant#invokedynamic()} method.
         */
        private static final MethodDescription.InDefinedShape INVOKEDYNAMIC;

        /*
         * Resolves all annotation properties.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methods = TypeDescription.ForLoadedType.of(DynamicConstant.class).getDeclaredMethods();
            NAME = methods.filter(named("name")).getOnly();
            BOOTSTRAP_TYPE = methods.filter(named("bootstrapType")).getOnly();
            BOOTSTRAP_OWNER = methods.filter(named("bootstrapOwner")).getOnly();
            BOOTSTRAP_NAME = methods.filter(named("bootstrapName")).getOnly();
            BOOTSTRAP_RETURN_TYPE = methods.filter(named("bootstrapReturnType")).getOnly();
            BOOTSTRAP_PARAMETER_TYPES = methods.filter(named("bootstrapParameterTypes")).getOnly();
            INVOKEDYNAMIC = methods.filter(named("invokedynamic")).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public Class<DynamicConstant> getHandledType() {
            return DynamicConstant.class;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<DynamicConstant> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner,
                                                               Assigner.Typing typing) {
            TypeDescription bootstrapOwner = annotation.getValue(BOOTSTRAP_OWNER).resolve(TypeDescription.class);
            if (annotation.getValue(INVOKEDYNAMIC).resolve(Boolean.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new Invokedynamic(
                        annotation.getValue(NAME).resolve(String.class),
                        JavaConstant.MethodType.of(target.getType().asErasure()),
                        new JavaConstant.MethodHandle(
                                annotation.getValue(BOOTSTRAP_TYPE).resolve(EnumerationDescription.class).load(JavaConstant.MethodHandle.HandleType.class),
                                bootstrapOwner.represents(void.class) ? implementationTarget.getInstrumentedType() : bootstrapOwner,
                                annotation.getValue(BOOTSTRAP_NAME).resolve(String.class),
                                annotation.getValue(BOOTSTRAP_RETURN_TYPE).resolve(TypeDescription.class),
                                Arrays.asList(annotation.getValue(BOOTSTRAP_PARAMETER_TYPES).resolve(TypeDescription[].class))),
                        Collections.<JavaConstant>emptyList()));
            } else {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new JavaConstantValue(new JavaConstant.Dynamic(
                        annotation.getValue(NAME).resolve(String.class),
                        target.getType().asErasure(),
                        new JavaConstant.MethodHandle(
                                annotation.getValue(BOOTSTRAP_TYPE).resolve(EnumerationDescription.class).load(JavaConstant.MethodHandle.HandleType.class),
                                bootstrapOwner.represents(void.class) ? implementationTarget.getInstrumentedType() : bootstrapOwner,
                                annotation.getValue(BOOTSTRAP_NAME).resolve(String.class),
                                annotation.getValue(BOOTSTRAP_RETURN_TYPE).resolve(TypeDescription.class),
                                Arrays.asList(annotation.getValue(BOOTSTRAP_PARAMETER_TYPES).resolve(TypeDescription[].class))),
                        Collections.<JavaConstant>emptyList())));
            }
        }
    }
}
