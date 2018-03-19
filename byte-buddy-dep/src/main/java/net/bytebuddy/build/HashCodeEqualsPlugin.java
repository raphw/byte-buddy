package net.bytebuddy.build;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.HashCodeMethod;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.isHashCode;
import static net.bytebuddy.matcher.ElementMatchers.not;

public class HashCodeEqualsPlugin implements Plugin {

    @Override
    public boolean matches(TypeDescription target) {
        return target.getDeclaredAnnotations().isAnnotationPresent(Enhance.class);
    }

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
        if (typeDescription.getDeclaredMethods().filter(isHashCode()).isEmpty()) {
            return builder.method(isHashCode()).intercept(typeDescription.getDeclaredAnnotations()
                    .ofType(Enhance.class)
                    .loadSilent()
                    .value()
                    .resolve(typeDescription)
                    .withIgnoredFields(new ValueMatcher(ValueHandling.Sort.IGNORE))
                    .withNonNullableFields(nonNullable(new ValueMatcher(ValueHandling.Sort.REVERSE_NULLABILITY))));
        } else {
            return builder;
        }
    }

    protected ElementMatcher<FieldDescription> nonNullable(ElementMatcher<FieldDescription> matcher) {
        return matcher;
    }

    public static class WithNonNullableFields extends HashCodeEqualsPlugin {

        @Override
        protected ElementMatcher<FieldDescription> nonNullable(ElementMatcher<FieldDescription> matcher) {
            return not(matcher);
        }
    }

    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enhance {

        InvokeSuper value() default InvokeSuper.IF_ANNOTATED;

        enum InvokeSuper {

            IF_DECLARED {
                @Override
                protected HashCodeMethod resolve(TypeDescription typeDescription) {
                    while (!typeDescription.represents(Object.class)) {
                        if (!typeDescription.getDeclaredMethods().filter(isHashCode()).isEmpty()) {
                            return HashCodeMethod.invokingSuperMethod();
                        }
                        typeDescription = typeDescription.getSuperClass().asErasure();
                    }
                    return HashCodeMethod.usingDefaultOffset();
                }
            },

            IF_ANNOTATED {
                @Override
                protected HashCodeMethod resolve(TypeDescription typeDescription) {
                    return typeDescription.getSuperClass().getDeclaredAnnotations().isAnnotationPresent(Enhance.class)
                            ? HashCodeMethod.invokingSuperMethod()
                            : HashCodeMethod.usingDefaultOffset();
                }
            },

            ALWAYS {
                @Override
                protected HashCodeMethod resolve(TypeDescription typeDescription) {
                    return HashCodeMethod.invokingSuperMethod();
                }
            },

            NEVER {
                @Override
                protected HashCodeMethod resolve(TypeDescription typeDescription) {
                    return HashCodeMethod.usingDefaultOffset();
                }
            };

            protected abstract HashCodeMethod resolve(TypeDescription typeDescription);
        }
    }

    @Documented
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ValueHandling {

        Sort value();

        enum Sort {

            IGNORE,

            REVERSE_NULLABILITY
        }
    }

    private static class ValueMatcher implements ElementMatcher<FieldDescription> {

        private final ValueHandling.Sort sort;

        private ValueMatcher(ValueHandling.Sort sort) {
            this.sort = sort;
        }

        @Override
        public boolean matches(FieldDescription target) {
            AnnotationDescription.Loadable<ValueHandling> annotation = target.getDeclaredAnnotations().ofType(ValueHandling.class);
            return annotation != null && annotation.loadSilent().value() == sort;
        }
    }
}
