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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.EqualsMethod;
import net.bytebuddy.implementation.HashCodeMethod;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.*;
import java.util.Comparator;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A build tool plugin that adds {@link Object#hashCode()} and {@link Object#equals(Object)} methods to a class if the
 * {@link Enhance} annotation is present and no explicit method declaration was added. This plugin does not need to be closed.
 */
@HashCodeAndEqualsPlugin.Enhance
public class HashCodeAndEqualsPlugin implements Plugin, Plugin.Factory, MethodAttributeAppender.Factory, MethodAttributeAppender {

    /**
     * A description of the {@link Enhance#invokeSuper()} method.
     */
    private static final MethodDescription.InDefinedShape ENHANCE_INVOKE_SUPER;

    /**
     * A description of the {@link Enhance#simpleComparisonsFirst()} method.
     */
    private static final MethodDescription.InDefinedShape ENHANCE_SIMPLE_COMPARISON_FIRST;

    /**
     * A description of the {@link Enhance#includeSyntheticFields()} method.
     */
    private static final MethodDescription.InDefinedShape ENHANCE_INCLUDE_SYNTHETIC_FIELDS;

    /**
     * A description of the {@link Enhance#permitSubclassEquality()} method.
     */
    private static final MethodDescription.InDefinedShape ENHANCE_PERMIT_SUBCLASS_EQUALITY;

    /**
     * A description of the {@link Enhance#useTypeHashConstant()} method.
     */
    private static final MethodDescription.InDefinedShape ENHANCE_USE_TYPE_HASH_CONSTANT;

    /**
     * A description of the {@link ValueHandling#value()} method.
     */
    private static final MethodDescription.InDefinedShape VALUE_HANDLING_VALUE;

    /**
     * A description of the {@link Sorted#value()} method.
     */
    private static final MethodDescription.InDefinedShape SORTED_VALUE;

    /*
     * Resolves diverse annotation properties.
     */
    static {
        MethodList<MethodDescription.InDefinedShape> enhanceMethods = TypeDescription.ForLoadedType.of(Enhance.class).getDeclaredMethods();
        ENHANCE_INVOKE_SUPER = enhanceMethods.filter(named("invokeSuper")).getOnly();
        ENHANCE_SIMPLE_COMPARISON_FIRST = enhanceMethods.filter(named("simpleComparisonsFirst")).getOnly();
        ENHANCE_INCLUDE_SYNTHETIC_FIELDS = enhanceMethods.filter(named("includeSyntheticFields")).getOnly();
        ENHANCE_PERMIT_SUBCLASS_EQUALITY = enhanceMethods.filter(named("permitSubclassEquality")).getOnly();
        ENHANCE_USE_TYPE_HASH_CONSTANT = enhanceMethods.filter(named("useTypeHashConstant")).getOnly();
        VALUE_HANDLING_VALUE = TypeDescription.ForLoadedType.of(ValueHandling.class).getDeclaredMethods().filter(named("value")).getOnly();
        SORTED_VALUE = TypeDescription.ForLoadedType.of(Sorted.class).getDeclaredMethods().filter(named("value")).getOnly();
    }

    /**
     * Defines the binary name of a runtime-visible annotation type that should be added to the parameter of the
     * {@link Object#equals(Object)} method, or {@code null} if no such name should be defined.
     */
    @MaybeNull
    @ValueHandling(ValueHandling.Sort.REVERSE_NULLABILITY)
    private final String annotationType;

    /**
     * Creates a new hash code equals plugin.
     */
    public HashCodeAndEqualsPlugin() {
        this(null);
    }

    /**
     * Creates a new hash code equals plugin.
     *
     * @param annotationType Defines the binary name of a runtime-visible annotation type that should be added to the
     *                       parameter of the {@link Object#equals(Object)} method, or {@code null} if no such name
     *                       should be defined.
     */
    public HashCodeAndEqualsPlugin(@MaybeNull String annotationType) {
        this.annotationType = annotationType;
    }

    /**
     * {@inheritDoc}
     */
    public Plugin make() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(@MaybeNull TypeDescription target) {
        return target != null && target.getDeclaredAnnotations().isAnnotationPresent(Enhance.class);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Annotation presence is required by matcher.")
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        AnnotationDescription.Loadable<Enhance> enhance = typeDescription.getDeclaredAnnotations().ofType(Enhance.class);
        if (typeDescription.getDeclaredMethods().filter(isHashCode()).isEmpty()) {
            builder = builder.method(isHashCode()).intercept(enhance.getValue(ENHANCE_INVOKE_SUPER).load(Enhance.class.getClassLoader()).resolve(Enhance.InvokeSuper.class)
                    .hashCodeMethod(typeDescription,
                            enhance.getValue(ENHANCE_USE_TYPE_HASH_CONSTANT).resolve(Boolean.class),
                            enhance.getValue(ENHANCE_PERMIT_SUBCLASS_EQUALITY).resolve(Boolean.class))
                    .withIgnoredFields(enhance.getValue(ENHANCE_INCLUDE_SYNTHETIC_FIELDS).resolve(Boolean.class)
                            ? ElementMatchers.<FieldDescription>none()
                            : ElementMatchers.<FieldDescription>isSynthetic())
                    .withIgnoredFields(new ValueMatcher(ValueHandling.Sort.IGNORE))
                    .withNonNullableFields(nonNullable(new ValueMatcher(ValueHandling.Sort.REVERSE_NULLABILITY)))
                    .withIdentityFields(isAnnotatedWith(Identity.class)));
        }
        if (typeDescription.getDeclaredMethods().filter(isEquals()).isEmpty()) {
            EqualsMethod equalsMethod = enhance.getValue(ENHANCE_INVOKE_SUPER).load(Enhance.class.getClassLoader()).resolve(Enhance.InvokeSuper.class)
                    .equalsMethod(typeDescription)
                    .withIgnoredFields(enhance.getValue(ENHANCE_INCLUDE_SYNTHETIC_FIELDS).resolve(Boolean.class)
                            ? ElementMatchers.<FieldDescription>none()
                            : ElementMatchers.<FieldDescription>isSynthetic())
                    .withIgnoredFields(new ValueMatcher(ValueHandling.Sort.IGNORE))
                    .withNonNullableFields(nonNullable(new ValueMatcher(ValueHandling.Sort.REVERSE_NULLABILITY)))
                    .withIdentityFields(isAnnotatedWith(Identity.class))
                    .withFieldOrder(AnnotationOrderComparator.INSTANCE);
            if (enhance.getValue(ENHANCE_SIMPLE_COMPARISON_FIRST).resolve(Boolean.class)) {
                equalsMethod = equalsMethod
                        .withPrimitiveTypedFieldsFirst()
                        .withEnumerationTypedFieldsFirst()
                        .withPrimitiveWrapperTypedFieldsFirst()
                        .withStringTypedFieldsFirst();
            }
            builder = builder.method(isEquals()).intercept(enhance.getValue(ENHANCE_PERMIT_SUBCLASS_EQUALITY).resolve(Boolean.class)
                    ? equalsMethod.withSubclassEquality()
                    : equalsMethod).attribute(this);
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
     * {@inheritDoc}
     */
    public MethodAttributeAppender make(TypeDescription typeDescription) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription, AnnotationValueFilter annotationValueFilter) {
        if (annotationType != null) {
            AnnotationVisitor annotationVisitor = methodVisitor.visitParameterAnnotation(0,
                    "L" + annotationType.replace('.', '/') + ";",
                    true);
            if (annotationVisitor != null) {
                annotationVisitor.visitEnd();
            }
        }
    }

    /**
     * A version of the {@link HashCodeAndEqualsPlugin} that assumes that all fields are non-nullable unless they are explicitly marked.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public static class WithNonNullableFields extends HashCodeAndEqualsPlugin {

        /**
         * Creates a new hash code equals plugin where fields are assumed nullable by default.
         */
        public WithNonNullableFields() {
            this(null);
        }

        /**
         * Creates a new hash code equals plugin where fields are assumed nullable by default.
         *
         * @param annotationType Defines the binary name of a runtime-visible annotation type that should be added to the
         *                       parameter of the {@link Object#equals(Object)} method, or {@code null} if no such name
         *                       should be defined.
         */
        public WithNonNullableFields(@MaybeNull String annotationType) {
            super(annotationType);
        }

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
         * Determines if the hash code constant should be derived of the instrumented type. If {@link Enhance#permitSubclassEquality()}
         * is set to {@code true}, this constant is derived of the declared class, otherwise the type hash is computed of the active instance.
         *
         * @return {@code true} if the hash code constant should be derived of the instrumented type.
         */
        boolean useTypeHashConstant() default true;

        /**
         * A strategy for determining the base value of a hash code or equality contract.
         */
        enum InvokeSuper {

            /**
             * Only invokes the super method's hash code and equality methods if any super class that is not {@link Object} explicitly defines such a method.
             */
            IF_DECLARED {
                @Override
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType, boolean typeHash, boolean subclassEquality) {
                    TypeDefinition typeDefinition = instrumentedType.getSuperClass();
                    while (typeDefinition != null && !typeDefinition.represents(Object.class)) {
                        if (typeDefinition.asErasure().getDeclaredAnnotations().isAnnotationPresent(Enhance.class)) {
                            return HashCodeMethod.usingSuperClassOffset();
                        }
                        MethodList<?> hashCode = typeDefinition.getDeclaredMethods().filter(isHashCode());
                        if (!hashCode.isEmpty()) {
                            return hashCode.getOnly().isAbstract()
                                    ? (typeHash ? HashCodeMethod.usingTypeHashOffset(!subclassEquality) : HashCodeMethod.usingDefaultOffset())
                                    : HashCodeMethod.usingSuperClassOffset();
                        }
                        typeDefinition = typeDefinition.getSuperClass();
                    }
                    return typeHash ? HashCodeMethod.usingTypeHashOffset(!subclassEquality) : HashCodeMethod.usingDefaultOffset();
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
                        typeDefinition = typeDefinition.getSuperClass();
                    }
                    return EqualsMethod.isolated();
                }
            },

            /**
             * Only invokes the super method's hash code and equality methods if the super class is also annotated with {@link Enhance}.
             */
            IF_ANNOTATED {
                @Override
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType, boolean typeHash, boolean subclassEquality) {
                    TypeDefinition superClass = instrumentedType.getSuperClass();
                    return superClass != null && superClass.asErasure().getDeclaredAnnotations().isAnnotationPresent(Enhance.class)
                            ? HashCodeMethod.usingSuperClassOffset()
                            : (typeHash ? HashCodeMethod.usingTypeHashOffset(!subclassEquality) : HashCodeMethod.usingDefaultOffset());
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
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType, boolean typeHash, boolean subclassEquality) {
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
                protected HashCodeMethod hashCodeMethod(TypeDescription instrumentedType, boolean typeHash, boolean subclassEquality) {
                    return typeHash ? HashCodeMethod.usingTypeHashOffset(!subclassEquality) : HashCodeMethod.usingDefaultOffset();
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
             * @param typeHash         {@code true} if the base hash should be based on the instrumented class's type.
             * @param subclassEquality {@code true} if subclasses can be equal to their base classes.
             * @return The hash code method to use.
             */
            protected abstract HashCodeMethod hashCodeMethod(TypeDescription instrumentedType, boolean typeHash, boolean subclassEquality);

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
     * Indicates that a field should be compared by identity. Hash codes are then determined by
     * {@link System#identityHashCode(Object)}. Fields that are compared by identity are implicitly null-safe.
     */
    @Documented
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Identity {
        /* empty */
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
            int leftValue = leftAnnotation == null ? Sorted.DEFAULT : leftAnnotation.getValue(SORTED_VALUE).resolve(Integer.class);
            int rightValue = rightAnnotation == null ? Sorted.DEFAULT : rightAnnotation.getValue(SORTED_VALUE).resolve(Integer.class);
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
    protected static class ValueMatcher extends ElementMatcher.Junction.ForNonNullValues<FieldDescription> {

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
        protected boolean doMatch(FieldDescription target) {
            AnnotationDescription.Loadable<ValueHandling> annotation = target.getDeclaredAnnotations().ofType(ValueHandling.class);
            return annotation != null && annotation.getValue(VALUE_HANDLING_VALUE).load(ValueHandling.class.getClassLoader()).resolve(ValueHandling.Sort.class) == sort;
        }
    }
}
