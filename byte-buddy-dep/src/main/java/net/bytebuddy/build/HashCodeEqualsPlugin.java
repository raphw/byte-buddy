package net.bytebuddy.build;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.HashCodeMethod;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.isHashCode;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * A build tool plugin that adds {@link Object#hashCode()} and {@link Object#equals(Object)} methods to a class if the
 * {@link Enhance} annotation is present and no explicit method declaration was added.
 */
public class HashCodeEqualsPlugin implements Plugin {

    @Override
    public boolean matches(TypeDescription target) {
        return target.getDeclaredAnnotations().isAnnotationPresent(Enhance.class);
    }

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
        if (typeDescription.getDeclaredMethods().filter(isHashCode()).isEmpty()) {
            Enhance enhance = typeDescription.getDeclaredAnnotations().ofType(Enhance.class).loadSilent();
            return builder.method(isHashCode()).intercept(enhance.invokeSuper()
                    .hashCodeMethod(typeDescription)
                    .withIgnoredFields(enhance.includeSyntheticFields()
                            ? ElementMatchers.<FieldDescription>none()
                            : ElementMatchers.<FieldDescription>isSynthetic())
                    .withIgnoredFields(new ValueMatcher(ValueHandling.Sort.IGNORE))
                    .withNonNullableFields(nonNullable(new ValueMatcher(ValueHandling.Sort.REVERSE_NULLABILITY))));
        } else {
            return builder;
        }
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
     * A version of the {@link HashCodeEqualsPlugin} that assumes that all fields are non-nullable unless they are explicitly marked.
     */
    public static class WithNonNullableFields extends HashCodeEqualsPlugin {

        @Override
        protected ElementMatcher<FieldDescription> nonNullable(ElementMatcher<FieldDescription> matcher) {
            return not(matcher);
        }
    }

    /**
     * Instructs the {@link HashCodeEqualsPlugin} to generate {@link Object#hashCode()} and {@link Object#equals(Object)} for the annotated
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
        InvokeSuper invokeSuper() default InvokeSuper.IF_ANNOTATED;

        /**
         * Determines if synthetic fields should be included in the hash code and equality contract.
         *
         * @return {@code true} if synthetic fields should be included.
         */
        boolean includeSyntheticFields() default false;

        /**
         * A strategy for determining the base value of a hash code or equality contract.
         */
        enum InvokeSuper {

            /**
             * Only invokes the super method's hash code and equality methods if the super class is also annotated with {@link Enhance}.
             */
            IF_ANNOTATED {
                @Override
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType) {
                    return instrumentedType.getSuperClass().getDeclaredAnnotations().isAnnotationPresent(Enhance.class)
                            ? HashCodeMethod.usingSuperClassOffset()
                            : HashCodeMethod.usingDefaultOffset();
                }
            },

            /**
             * Only invokes the super method's hash code and equality methods if any super class that is not {@link Object} explicitly defines such a method.
             */
            IF_DECLARED {
                @Override
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType) {
                    while (!instrumentedType.represents(Object.class)) {
                        if (!instrumentedType.getDeclaredMethods().filter(isHashCode()).isEmpty()) {
                            return HashCodeMethod.usingSuperClassOffset();
                        }
                        instrumentedType = instrumentedType.getSuperClass().asErasure();
                    }
                    return HashCodeMethod.usingDefaultOffset();
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
            },

            /**
             * Never invokes the super class's hash code and equality methods.
             */
            NEVER {
                @Override
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType) {
                    return HashCodeMethod.usingDefaultOffset();
                }
            };

            /**
             * Resolves the hash code method to use.
             *
             * @param instrumentedType The instrumented type.
             * @return The hash code method to use.
             */
            protected abstract HashCodeMethod hashCodeMethod(TypeDescription instrumentedType);
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
