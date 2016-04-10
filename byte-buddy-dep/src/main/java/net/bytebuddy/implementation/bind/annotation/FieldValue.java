package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <p>
 * Assigns the value of a field of the instrumented type to the annotated parameter. For a binding to be valid,
 * the instrumented type must be able to access a field of the given name. Also, the parameter's type must be
 * assignable to the given field. For attempting a type casting, the {@link RuntimeType} annotation can be
 * applied to the parameter.
 * </p>
 * <p>
 * Setting {@link FieldValue#value()} is optional. If the value is not set, the field value attempts to bind a setter's
 * or getter's field if the intercepted method is an accessor method. Otherwise, the binding renders the target method
 * to be an illegal candidate for binding.
 * </p>
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 * @see net.bytebuddy.implementation.bind.annotation.RuntimeType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FieldValue {

    /**
     * The name of the field to be accessed.
     *
     * @return The name of the field.
     */
    String value() default TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFieldBinding.BEAN_PROPERTY;

    /**
     * Defines the type on which the field is declared. If this value is not set, the most specific type's field is read,
     * if two fields with the same name exist in the same type hierarchy.
     *
     * @return The type that declares the accessed field.
     */
    Class<?> declaringType() default void.class;

    /**
     * Binds a {@link FieldValue} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldValue> {

        /**
         * The singleton instance.
         */
        INSTANCE(new Delegate());

        /**
         * The annotation method that for the defining type.
         */
        private static final MethodDescription.InDefinedShape DEFINING_TYPE;

        /**
         * The annotation method for the field's name.
         */
        private static final MethodDescription.InDefinedShape FIELD_NAME;

        /*
         * Initializes the methods of the annotation that is read by this binder.
         */
        static {
            MethodList<MethodDescription.InDefinedShape> methodList = new TypeDescription.ForLoadedType(FieldValue.class).getDeclaredMethods();
            DEFINING_TYPE = methodList.filter(named("declaringType")).getOnly();
            FIELD_NAME = methodList.filter(named("value")).getOnly();
        }

        /**
         * A delegate parameter binder responsible for binding the parameter.
         */
        private final TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldValue> delegate;

        /**
         * Creates a new binder for a {@link FieldValue}.
         *
         * @param delegate A delegate parameter binder responsible for binding the parameter.
         */
        Binder(TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldValue> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Class<FieldValue> getHandledType() {
            return delegate.getHandledType();
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<FieldValue> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            return delegate.bind(annotation, source, target, implementationTarget, assigner);
        }

        @Override
        public String toString() {
            return "FieldValue.Binder." + name();
        }

        /**
         * A delegate implementation for the {@link FieldValue.Binder}.
         */
        protected static class Delegate extends ForFieldBinding<FieldValue> {

            @Override
            public Class<FieldValue> getHandledType() {
                return FieldValue.class;
            }

            @Override
            protected MethodDelegationBinder.ParameterBinding<?> bind(FieldDescription fieldDescription,
                                                                      AnnotationDescription.Loadable<FieldValue> annotation,
                                                                      MethodDescription source,
                                                                      ParameterDescription target,
                                                                      Implementation.Target implementationTarget,
                                                                      Assigner assigner) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        fieldDescription.isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.REFERENCE.loadOffset(0),
                        FieldAccess.forField(fieldDescription).getter(),
                        assigner.assign(fieldDescription.getType(), target.getType(), RuntimeType.Verifier.check(target))
                );
                return stackManipulation.isValid()
                        ? new MethodDelegationBinder.ParameterBinding.Anonymous(stackManipulation)
                        : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }

            @Override
            protected String fieldName(AnnotationDescription.Loadable<FieldValue> annotation) {
                return annotation.getValue(FIELD_NAME, String.class);
            }

            @Override
            protected TypeDescription declaringType(AnnotationDescription.Loadable<FieldValue> annotation) {
                return annotation.getValue(DEFINING_TYPE, TypeDescription.class);
            }

            @Override
            public String toString() {
                return "FieldValue.Binder.Delegate{}";
            }
        }
    }
}
