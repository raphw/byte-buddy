package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Assigns the value of a field of the instrumented type to the annotated parameter. For a binding to be valid,
 * the instrumented type must be able to access a field of the given name. Also, the parameter's type must be
 * assignable to the given field. For attempting a type casting, the {@link RuntimeType} annotation can be
 * applied to the parameter.
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
    String value();

    /**
     * Defines the type on which the field is declared. If this value is not set, the most specific type's field is read,
     * if two fields with the same name exist in the same type hierarchy.
     *
     * @return The type that declares the accessed field.
     */
    Class<?> definingType() default void.class;

    /**
     * Binds a {@link FieldValue} annotation.
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldValue> {

        /**
         * The singleton instance.
         */
        INSTANCE;

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
            DEFINING_TYPE = methodList.filter(named("definingType")).getOnly();
            FIELD_NAME = methodList.filter(named("value")).getOnly();
        }

        @Override
        public Class<FieldValue> getHandledType() {
            return FieldValue.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<FieldValue> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            FieldLocator.Resolution resolution = FieldLocator
                    .of(annotation.getValue(DEFINING_TYPE, TypeDescription.class), implementationTarget.getTypeDescription())
                    .resolve(annotation.getValue(FIELD_NAME, String.class), source.isStatic());
            if (resolution.isResolved()) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        resolution.getFieldDescription().isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.REFERENCE.loadOffset(0),
                        FieldAccess.forField(resolution.getFieldDescription()).getter(),
                        assigner.assign(resolution.getFieldDescription().getType().asErasure(),
                                target.getType().asErasure(),
                                RuntimeType.Verifier.check(target))
                );
                return stackManipulation.isValid()
                        ? new MethodDelegationBinder.ParameterBinding.Anonymous(stackManipulation)
                        : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            } else {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            }
        }

        @Override
        public String toString() {
            return "FieldValue.Binder." + name();
        }

        /**
         * Responsible for locating a field of the instrumented type.
         */
        protected abstract static class FieldLocator {

            /**
             * Creates a field locator for the given type and instrumented type.
             *
             * @param typeDescription  The type which is supposed to define the field or {@code void} if a type should be located
             *                         in the hierarchy.
             * @param instrumentedType The instrumented type from which the field is accessed.
             * @return The field locator for the given field.
             */
            protected static FieldLocator of(TypeDescription typeDescription, TypeDescription instrumentedType) {
                return typeDescription.represents(void.class)
                        ? new ForFieldInHierarchy(instrumentedType)
                        : ForSpecificType.of(typeDescription, instrumentedType);
            }

            /**
             * Attempts to locate a type for a given field.
             *
             * @param fieldName    The name of the field.
             * @param staticMethod {@code} true if the field is accessed from a static method.
             * @return The resolution for the requested lookup.
             */
            protected abstract Resolution resolve(String fieldName, boolean staticMethod);

            /**
             * A resolution of a field locator.
             */
            protected interface Resolution {

                /**
                 * Returns the located field description if available or throws an exception if this method is called for an
                 * unresolved resolution.
                 *
                 * @return The located field.
                 */
                FieldDescription getFieldDescription();

                /**
                 * Returns {@code true} if a field was successfully located.
                 *
                 * @return {@code true} if a field was successfully located.
                 */
                boolean isResolved();

                /**
                 * A canonical implementation of an unresolved field resolution.
                 */
                enum Unresolved implements Resolution {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public FieldDescription getFieldDescription() {
                        throw new IllegalStateException("Cannot resolve field for unresolved lookup");
                    }

                    @Override
                    public boolean isResolved() {
                        return false;
                    }

                    @Override
                    public String toString() {
                        return "FieldValue.Binder.FieldLocator.Resolution.Unresolved." + name();
                    }
                }

                /**
                 * A successfully resolved field resolution.
                 */
                class Resolved implements Resolution {

                    /**
                     * The resolved field.
                     */
                    private final FieldDescription fieldDescription;

                    /**
                     * Creates a successful field resolution.
                     *
                     * @param fieldDescription The resolved field.
                     */
                    public Resolved(FieldDescription fieldDescription) {
                        this.fieldDescription = fieldDescription;
                    }

                    @Override
                    public FieldDescription getFieldDescription() {
                        return fieldDescription;
                    }

                    @Override
                    public boolean isResolved() {
                        return true;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (!(other instanceof Resolved)) return false;
                        Resolved resolved = (Resolved) other;
                        return fieldDescription.equals(resolved.fieldDescription);
                    }

                    @Override
                    public int hashCode() {
                        return fieldDescription.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "FieldValue.Binder.FieldLocator.Resolution.Resolved{" +
                                "fieldDescription=" + fieldDescription +
                                '}';
                    }
                }
            }

            /**
             * Attempts to locate a field within a type's hierarchy.
             */
            protected static class ForFieldInHierarchy extends FieldLocator {

                /**
                 * The instrumented type which defines the field.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * Creates a new field locator for locating a field in the instrumented type's class hierarchy.
                 *
                 * @param instrumentedType The instrumented type
                 */
                protected ForFieldInHierarchy(TypeDescription instrumentedType) {
                    this.instrumentedType = instrumentedType;
                }

                @Override
                protected Resolution resolve(String fieldName, boolean staticMethod) {
                    for (GenericTypeDescription currentType : instrumentedType) {
                        FieldList<?> fieldList = currentType.getDeclaredFields().filter(named(fieldName));
                        if (!fieldList.isEmpty() && fieldList.getOnly().isVisibleTo(instrumentedType) && (!staticMethod || fieldList.getOnly().isStatic())) {
                            return new Resolution.Resolved(fieldList.getOnly());
                        }
                    }
                    return Resolution.Unresolved.INSTANCE;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof ForFieldInHierarchy)) return false;
                    ForFieldInHierarchy that = (ForFieldInHierarchy) other;
                    return instrumentedType.equals(that.instrumentedType);
                }

                @Override
                public int hashCode() {
                    return instrumentedType.hashCode();
                }

                @Override
                public String toString() {
                    return "FieldValue.Binder.FieldLocator.ForFieldInHierarchy{" +
                            "instrumentedType=" + instrumentedType +
                            '}';
                }
            }

            /**
             * Locates a field only within a given type.
             */
            protected static class ForSpecificType extends FieldLocator {

                /**
                 * The type which is supposed to define the field.
                 */
                private final TypeDescription typeDescription;

                /**
                 * The instrumented type which is accessing the field.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * Creates a new field locator for looking up a field within a specific type.
                 *
                 * @param typeDescription  The type which is supposed to define the field.
                 * @param instrumentedType The instrumented type which is accessing the field.
                 */
                protected ForSpecificType(TypeDescription typeDescription, TypeDescription instrumentedType) {
                    this.typeDescription = typeDescription;
                    this.instrumentedType = instrumentedType;
                }

                /**
                 * Creates a field locator that locates a field within the given type only if that type is within the
                 * instrumented type's type hierarchy.
                 *
                 * @param typeDescription  The given type to locate a field within.
                 * @param instrumentedType The instrumented type.
                 * @return An appropriate field locator.
                 */
                protected static FieldLocator of(TypeDescription typeDescription, TypeDescription instrumentedType) {
                    if (typeDescription.isInterface() || typeDescription.isPrimitive() || typeDescription.isArray()) {
                        throw new IllegalStateException(typeDescription + " is not capable of declaring a field");
                    }
                    return instrumentedType.isAssignableTo(typeDescription)
                            ? new ForSpecificType(typeDescription, instrumentedType)
                            : new Impossible();
                }

                @Override
                protected Resolution resolve(String fieldName, boolean staticMethod) {
                    FieldList<?> fieldList = typeDescription.getDeclaredFields().filter(named(fieldName));
                    return fieldList.isEmpty() || !fieldList.getOnly().isVisibleTo(instrumentedType) || (staticMethod && !fieldList.getOnly().isStatic())
                            ? Resolution.Unresolved.INSTANCE
                            : new Resolution.Resolved(fieldList.getOnly());
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof ForSpecificType)) return false;
                    ForSpecificType that = (ForSpecificType) other;
                    return typeDescription.equals(that.typeDescription)
                            && instrumentedType.equals(that.instrumentedType);
                }

                @Override
                public int hashCode() {
                    int result = typeDescription.hashCode();
                    result = 31 * result + instrumentedType.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "FieldValue.Binder.FieldLocator.ForSpecificType{" +
                            "typeDescription=" + typeDescription +
                            ", instrumentedType=" + instrumentedType +
                            '}';
                }
            }

            /**
             * A field locator that never locates a field.
             */
            protected static class Impossible extends FieldLocator {

                @Override
                protected Resolution resolve(String fieldName, boolean staticMethod) {
                    return Resolution.Unresolved.INSTANCE;
                }

                @Override
                public int hashCode() {
                    return 31;
                }

                @Override
                public boolean equals(Object other) {
                    return other != null && other.getClass() == getClass();
                }

                @Override
                public String toString() {
                    return "FieldValue.Binder.FieldLocator.Impossible{}";
                }
            }
        }
    }
}
