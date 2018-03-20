package net.bytebuddy.build;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.EqualsMethod;
import net.bytebuddy.implementation.HashCodeMethod;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A build tool plugin that adds {@link Object#hashCode()} and {@link Object#equals(Object)} methods to a class if the
 * {@link Enhance} annotation is present and no explicit method declaration was added.
 */
@EqualsAndHashCode
public class HashCodeAndEqualsPlugin implements Plugin {

    @Override
    public boolean matches(TypeDescription target) {
        return target.getDeclaredAnnotations().isAnnotationPresent(Enhance.class);
    }

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
        Enhance enhance = typeDescription.getDeclaredAnnotations().ofType(Enhance.class).loadSilent();
        if (typeDescription.getDeclaredMethods().filter(isHashCode()).isEmpty()) {
            builder = builder.method(isHashCode()).intercept(enhance.invokeSuper()
                    .hashCodeMethod(typeDescription)
                    .withIgnoredFields(enhance.includeSyntheticFields()
                            ? ElementMatchers.<FieldDescription>none()
                            : ElementMatchers.<FieldDescription>isSynthetic())
                    .withIgnoredFields(new ValueMatcher(ValueHandling.Sort.IGNORE))
                    .withNonNullableFields(nonNullable(new ValueMatcher(ValueHandling.Sort.REVERSE_NULLABILITY))));
        }
        if (typeDescription.getDeclaredMethods().filter(isEquals()).isEmpty()) {
            EqualsMethod equalsMethod = enhance.invokeSuper()
                    .equalsMethod(typeDescription)
                    .withIgnoredFields(enhance.includeSyntheticFields()
                            ? ElementMatchers.<FieldDescription>none()
                            : ElementMatchers.<FieldDescription>isSynthetic())
                    .withIgnoredFields(new ValueMatcher(ValueHandling.Sort.IGNORE))
                    .withNonNullableFields(nonNullable(new ValueMatcher(ValueHandling.Sort.REVERSE_NULLABILITY)));
            builder = builder.method(isEquals()).intercept(enhance.permitSubclassEquality() ? equalsMethod.withSubclassEquality() : equalsMethod);
        }
        return builder;
    }

    /**
     * Resolves the matcher to identify non-nullable fields.
     *
     * @param matcher The matcher that identifies fields that are either nullable or non-nullable.
     * @return The actual matcher to identify non-nullable fields.
     */
    protected ElementMatcher<FieldDescription> nonNullable(ElementMatcher<FieldDescription> matcher) {
        return matcher;
    }

    /**
     * A version of the {@link HashCodeAndEqualsPlugin} that assumes that all fields are non-nullable unless they are explicitly marked.
     */
    @EqualsAndHashCode(callSuper = true)
    public static class WithNonNullableFields extends HashCodeAndEqualsPlugin {

        @Override
        protected ElementMatcher<FieldDescription> nonNullable(ElementMatcher<FieldDescription> matcher) {
            return not(matcher);
        }
    }

    /**
     * Instructs the {@link HashCodeAndEqualsPlugin} to generate {@link Object#hashCode()} and {@link Object#equals(Object)} for the annotated
     * class unless these methods are already declared explicitly.
     */
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enhance {

        /**
         * Determines the base value of any added method, i.e. if hash code or equality is based on the super type or not.
         *
         * @return A strategy for determining the base value.
         */
        InvokeSuper invokeSuper() default InvokeSuper.IF_DECLARED;

        /**
         * Determines if synthetic fields should be included in the hash code and equality contract.
         *
         * @return {@code true} if synthetic fields should be included.
         */
        boolean includeSyntheticFields() default false;

        /**
         * Determines if instances subclasses of the instrumented type are accepted upon an equality check.
         *
         * @return {@code true} if instances subclasses of the instrumented type are accepted upon an equality check.
         */
        boolean permitSubclassEquality() default false;

        /**
         * A strategy for determining the base value of a hash code or equality contract.
         */
        enum InvokeSuper {

            /**
             * Only invokes the super method's hash code and equality methods if any super class that is not {@link Object} explicitly defines such a method.
             */
            IF_DECLARED {
                @Override
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType) {
                    TypeDefinition typeDefinition = instrumentedType.getSuperClass();
                    while (typeDefinition != null && !typeDefinition.represents(Object.class)) {
                        if (typeDefinition.asErasure().getDeclaredAnnotations().isAnnotationPresent(Enhance.class)) {
                            return HashCodeMethod.usingSuperClassOffset();
                        }
                        MethodList<?> hashCode = typeDefinition.getDeclaredMethods().filter(isHashCode());
                        if (!hashCode.isEmpty()) {
                            return hashCode.getOnly().isAbstract()
                                    ? HashCodeMethod.usingDefaultOffset()
                                    : HashCodeMethod.usingSuperClassOffset();
                        }
                        typeDefinition = typeDefinition.getSuperClass();
                    }
                    return HashCodeMethod.usingDefaultOffset();
                }

                @Override
                protected EqualsMethod equalsMethod(TypeDescription instrumentedType) {
                    TypeDefinition typeDefinition = instrumentedType.getSuperClass();
                    while (typeDefinition != null && !typeDefinition.represents(Object.class)) {
                        if (typeDefinition.asErasure().getDeclaredAnnotations().isAnnotationPresent(Enhance.class)) {
                            return EqualsMethod.requiringSuperClassEquality();
                        }
                        MethodList<?> hashCode = typeDefinition.getDeclaredMethods().filter(isHashCode());
                        if (!hashCode.isEmpty()) {
                            return hashCode.getOnly().isAbstract()
                                    ? EqualsMethod.isolated()
                                    : EqualsMethod.requiringSuperClassEquality();
                        }
                        typeDefinition = typeDefinition.getSuperClass().asErasure();
                    }
                    return EqualsMethod.isolated();
                }
            },

            /**
             * Only invokes the super method's hash code and equality methods if the super class is also annotated with {@link Enhance}.
             */
            IF_ANNOTATED {
                @Override
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType) {
                    TypeDefinition superClass = instrumentedType.getSuperClass();
                    return superClass != null && superClass.asErasure().getDeclaredAnnotations().isAnnotationPresent(Enhance.class)
                            ? HashCodeMethod.usingSuperClassOffset()
                            : HashCodeMethod.usingDefaultOffset();
                }

                @Override
                protected EqualsMethod equalsMethod(TypeDescription instrumentedType) {
                    TypeDefinition superClass = instrumentedType.getSuperClass();
                    return superClass != null && superClass.asErasure().getDeclaredAnnotations().isAnnotationPresent(Enhance.class)
                            ? EqualsMethod.requiringSuperClassEquality()
                            : EqualsMethod.isolated();
                }
            },

            /**
             * Always invokes the super class's hash code and equality methods.
             */
            ALWAYS {
                @Override
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType) {
                    return HashCodeMethod.usingSuperClassOffset();
                }

                @Override
                protected EqualsMethod equalsMethod(TypeDescription instrumentedType) {
                    return EqualsMethod.requiringSuperClassEquality();
                }
            },

            /**
             * Never invokes the super class's hash code and equality methods.
             */
            NEVER {
                @Override
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType) {
                    return HashCodeMethod.usingDefaultOffset();
                }

                @Override
                protected EqualsMethod equalsMethod(TypeDescription instrumentedType) {
                    return EqualsMethod.isolated();
                }
            };

            /**
             * Resolves the hash code method to use.
             *
             * @param instrumentedType The instrumented type.
             * @return The hash code method to use.
             */
            protected abstract HashCodeMethod hashCodeMethod(TypeDescription instrumentedType);

            /**
             * Resolves the equals method to use.
             *
             * @param instrumentedType The instrumented type.
             * @return The equals method to use.
             */
            protected abstract EqualsMethod equalsMethod(TypeDescription instrumentedType);
        }
    }

    /**
     * Determines how a field should be used within generated hash code and equality methods.
     */
    @Documented
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ValueHandling {

        /**
         * Determines the handling of the annotated field.
         *
         * @return The handling of the annotated field.
         */
        Sort value();

        /**
         * Determines how a field should be handled.
         */
        enum Sort {

            /**
             * Excludes the field from hash code and equality methods.
             */
            IGNORE,

            /**
             * Reverses the nullability of the field, i.e. assumes this field to be non-null or {@code null} if {@link WithNonNullableFields} is used.
             */
            REVERSE_NULLABILITY
        }
    }

    /**
     * An element matcher for a {@link ValueHandling} annotation.
     */
    @EqualsAndHashCode
    protected static class ValueMatcher implements ElementMatcher<FieldDescription> {

        /**
         * The matched value.
         */
        private final ValueHandling.Sort sort;

        /**
         * Creates a new value matcher.
         *
         * @param sort The matched value.
         */
        protected ValueMatcher(ValueHandling.Sort sort) {
            this.sort = sort;
        }

        @Override
        public boolean matches(FieldDescription target) {
            AnnotationDescription.Loadable<ValueHandling> annotation = target.getDeclaredAnnotations().ofType(ValueHandling.class);
            return annotation != null && annotation.loadSilent().value() == sort;
        }
    }
}
