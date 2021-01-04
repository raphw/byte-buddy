/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.build;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.EqualsMethod;
import net.bytebuddy.implementation.HashCodeMethod;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.*;
import java.util.Comparator;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A build tool plugin that adds {@link Object#hashCode()} and {@link Object#equals(Object)} methods to a class if the
 * {@link Enhance} annotation is present and no explicit method declaration was added. This plugin does not need to be closed.
 */
@HashCodeAndEqualsPlugin.Enhance
public class HashCodeAndEqualsPlugin implements Plugin, Plugin.Factory {

    /**
     * {@inheritDoc}
     */
    public Plugin make() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(TypeDescription target) {
        return target.getDeclaredAnnotations().isAnnotationPresent(Enhance.class);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        Enhance enhance = typeDescription.getDeclaredAnnotations().ofType(Enhance.class).load();
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
                    .withNonNullableFields(nonNullable(new ValueMatcher(ValueHandling.Sort.REVERSE_NULLABILITY)))
                    .withFieldOrder(AnnotationOrderComparator.INSTANCE);
            if (enhance.simpleComparisonsFirst()) {
                equalsMethod = equalsMethod
                        .withPrimitiveTypedFieldsFirst()
                        .withEnumerationTypedFieldsFirst()
                        .withPrimitiveWrapperTypedFieldsFirst()
                        .withStringTypedFieldsFirst();
            }
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
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }

    /**
     * A version of the {@link HashCodeAndEqualsPlugin} that assumes that all fields are non-nullable unless they are explicitly marked.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public static class WithNonNullableFields extends HashCodeAndEqualsPlugin {

        /**
         * {@inheritDoc}
         */
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
         * Determines if fields with primitive types, then enumeration types, then primtive wrapper types and then {@link String} types
         * should be compared for equality before fields with other types. Before determining such a field order,
         * the {@link Sorted} property is always considered first if it is defined.
         *
         * @return {@code true} if fields with simple comparison methods should be compared first.
         */
        boolean simpleComparisonsFirst() default true;

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
     * Determines the sort order of fields for the equality check when implementing the {@link Object#equals(Object)} method. Any field
     * that is not annotated is considered with a value of {@link Sorted#DEFAULT} where fields with a higher value are checked for equality
     * first. This sort order is applied first after which the type order is considered if {@link Enhance#simpleComparisonsFirst()} is considered
     * as additional sort criteria.
     */
    @Documented
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Sorted {

        /**
         * The default sort weight.
         */
        int DEFAULT = 0;

        /**
         * The value for the sort order where fields with higher values are checked for equality first.
         *
         * @return The value for the sort order where fields with higher values are checked for equality first.
         */
        int value();
    }

    /**
     * A comparator that arranges fields in the order of {@link Sorted}.
     */
    protected enum AnnotationOrderComparator implements Comparator<FieldDescription.InDefinedShape> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public int compare(FieldDescription.InDefinedShape left, FieldDescription.InDefinedShape right) {
            AnnotationDescription.Loadable<Sorted> leftAnnotation = left.getDeclaredAnnotations().ofType(Sorted.class);
            AnnotationDescription.Loadable<Sorted> rightAnnotation = right.getDeclaredAnnotations().ofType(Sorted.class);
            int leftValue = leftAnnotation == null ? Sorted.DEFAULT : leftAnnotation.load().value();
            int rightValue = rightAnnotation == null ? Sorted.DEFAULT : rightAnnotation.load().value();
            if (leftValue > rightValue) {
                return -1;
            } else if (leftValue < rightValue) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * An element matcher for a {@link ValueHandling} annotation.
     */
    @HashCodeAndEqualsPlugin.Enhance
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

        /**
         * {@inheritDoc}
         */
        public boolean matches(FieldDescription target) {
            AnnotationDescription.Loadable<ValueHandling> annotation = target.getDeclaredAnnotations().ofType(ValueHandling.class);
            return annotation != null && annotation.load().value() == sort;
        }
    }
}
