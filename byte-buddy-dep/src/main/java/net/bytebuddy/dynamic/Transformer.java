package net.bytebuddy.dynamic;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;

/**
 * A transformer is responsible for transforming an object into a compatible instance of the same type.
 *
 * @param <T> The type of the instance being transformed.
 */
public interface Transformer<T> {

    /**
     * Transforms the supplied target.
     *
     * @param instrumentedType The instrumented type that declares the target being transformed.
     * @param target           The target entity that is being transformed.
     * @return The transformed instance.
     */
    T transform(TypeDescription instrumentedType, T target);

    /**
     * A non-operational transformer that returns the received instance.
     */
    enum NoOp implements Transformer<Object> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * Creates a transformer in a type-safe manner.
         *
         * @param <T> The type of the transformed object.
         * @return A non-operational transformer.
         */
        @SuppressWarnings("unchecked")
        public static <T> Transformer<T> make() {
            return (Transformer<T>) INSTANCE;
        }

        @Override
        public Object transform(TypeDescription instrumentedType, Object target) {
            return target;
        }

        @Override
        public String toString() {
            return "Transformer.NoOp." + name();
        }
    }

    /**
     * A transformer for a field that delegates to another transformer that transforms a {@link net.bytebuddy.description.field.FieldDescription.Token}.
     */
    class ForField implements Transformer<FieldDescription> {

        /**
         * The token transformer to apply to a transformed field.
         */
        private final Transformer<FieldDescription.Token> transformer;

        /**
         * Creates a new simple field transformer.
         *
         * @param transformer The token transformer to apply to a transformed field.
         */
        public ForField(Transformer<FieldDescription.Token> transformer) {
            this.transformer = transformer;
        }

        /**
         * Creates a field transformer that patches the transformed field by the givien modifier contributors.
         *
         * @param modifierContributor The modifier contributors to apply.
         * @return A suitable field transformer.
         */
        public static Transformer<FieldDescription> withModifiers(ModifierContributor.ForField... modifierContributor) {
            return new ForField(new FieldModifierTransformer(Arrays.asList(modifierContributor)));
        }

        @Override
        public FieldDescription transform(TypeDescription instrumentedType, FieldDescription fieldDescription) {
            return new TransformedField(fieldDescription.getDeclaringType(), transformer.transform(instrumentedType, fieldDescription.asToken(is(instrumentedType))), fieldDescription.asDefined());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && transformer.equals(((ForField) other).transformer);
        }

        @Override
        public int hashCode() {
            return transformer.hashCode();
        }

        @Override
        public String toString() {
            return "Transformer.ForField{" +
                    "transformer=" + transformer +
                    '}';
        }

        /**
         * A transformer for a field's modifiers.
         */
        protected static class FieldModifierTransformer implements Transformer<FieldDescription.Token> {

            /**
             * The list of modifier contributors to apply onto the transformed field token.
             */
            private final List<? extends ModifierContributor.ForField> modifierContributors;

            /**
             * Creates a new field token modifier for transforming a field's modifiers.
             *
             * @param modifierContributors The list of modifier contributors to apply onto the transformed field token.
             */
            public FieldModifierTransformer(List<? extends ModifierContributor.ForField> modifierContributors) {
                this.modifierContributors = modifierContributors;
            }

            @Override
            public FieldDescription.Token transform(TypeDescription instrumentedType, FieldDescription.Token target) {
                return new FieldDescription.Token(target.getName(),
                        ModifierContributor.Resolver.of(modifierContributors).resolve(target.getModifiers()),
                        target.getType(),
                        target.getAnnotations());
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && modifierContributors.equals(((FieldModifierTransformer) other).modifierContributors);
            }

            @Override
            public int hashCode() {
                return modifierContributors.hashCode();
            }

            @Override
            public String toString() {
                return "Transformer.ForField.FieldModifierTransformer{" +
                        "modifierContributors=" + modifierContributors +
                        '}';
            }
        }

        /**
         * An implementation of a transformed field.
         */
        protected static class TransformedField extends FieldDescription.AbstractBase {

            /**
             * The field's declaring type.
             */
            private final TypeDefinition declaringType;

            /**
             * A field token representing the transformed field.
             */
            private final FieldDescription.Token token;

            /**
             * The field's defined shape.
             */
            private final FieldDescription.InDefinedShape fieldDescription;

            /**
             * Creates a new transformed field.
             *
             * @param declaringType    The field's declaring type.
             * @param token            A field token representing the transformed field.
             * @param fieldDescription The field's defined shape.
             */
            protected TransformedField(TypeDefinition declaringType,
                                       FieldDescription.Token token,
                                       FieldDescription.InDefinedShape fieldDescription) {
                this.declaringType = declaringType;
                this.token = token;
                this.fieldDescription = fieldDescription;
            }

            @Override
            public TypeDescription.Generic getType() {
                return token.getType().accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(this));
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return token.getAnnotations();
            }

            @Override
            public TypeDefinition getDeclaringType() {
                return declaringType;
            }

            @Override
            public int getModifiers() {
                return token.getModifiers();
            }

            @Override
            public InDefinedShape asDefined() {
                return fieldDescription;
            }

            @Override
            public String getName() {
                return token.getName();
            }
        }
    }

    /**
     * A transformer for a field that delegates to another transformer that transforms a {@link net.bytebuddy.description.method.MethodDescription.Token}.
     */
    class ForMethod implements Transformer<MethodDescription> {

        /**
         * The transformer to be applied.
         */
        private final Transformer<MethodDescription.Token> transformer;

        /**
         * Creates a new transforming method transformer.
         *
         * @param transformer The transformer to be applied.
         */
        public ForMethod(Transformer<MethodDescription.Token> transformer) {
            this.transformer = transformer;
        }

        /**
         * Creates a transformer that enforces the supplied modifier contributors. All ranges of each contributor is first cleared and then overridden
         * by the specified modifiers in the order they are supplied.
         *
         * @param modifierTransformer The modifier transformers in their application order.
         * @return A method transformer where each method's modifiers are adapted to the given modifiers.
         */
        public static Transformer<MethodDescription> withModifiers(ModifierContributor.ForMethod... modifierTransformer) {
            return new ForMethod(new MethodModifierTransformer(Arrays.asList(modifierTransformer)));
        }

        @Override
        public MethodDescription transform(TypeDescription instrumentedType, MethodDescription methodDescription) {
            return new TransformedMethod(methodDescription.getDeclaringType(), transformer.transform(instrumentedType, methodDescription.asToken(is(instrumentedType))), methodDescription.asDefined());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && transformer.equals(((ForMethod) other).transformer);
        }

        @Override
        public int hashCode() {
            return transformer.hashCode();
        }

        @Override
        public String toString() {
            return "Transformer.ForMethod{" +
                    "transformer=" + transformer +
                    '}';
        }

        /**
         * A transformer for a method's modifiers.
         */
        protected static class MethodModifierTransformer implements Transformer<MethodDescription.Token> {

            /**
             * The modifier contributors to apply on each transformation.
             */
            private final List<? extends ModifierContributor.ForMethod> modifierContributors;

            /**
             * Creates a new modifier transformation.
             *
             * @param modifierContributors The modifier contributors to apply on each transformation in their application order.
             */
            public MethodModifierTransformer(List<? extends ModifierContributor.ForMethod> modifierContributors) {
                this.modifierContributors = modifierContributors;
            }

            @Override
            public MethodDescription.Token transform(TypeDescription instrumentedType, MethodDescription.Token target) {
                return new MethodDescription.Token(target.getName(),
                        ModifierContributor.Resolver.of(modifierContributors).resolve(target.getModifiers()),
                        target.getTypeVariableTokens(),
                        target.getReturnType(),
                        target.getParameterTokens(),
                        target.getExceptionTypes(),
                        target.getAnnotations(),
                        target.getDefaultValue(),
                        target.getReceiverType());
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && modifierContributors.equals(((MethodModifierTransformer) other).modifierContributors);
            }

            @Override
            public int hashCode() {
                return modifierContributors.hashCode();
            }

            @Override
            public String toString() {
                return "Transformer.ForMethod.MethodModifierTransformer{" +
                        "modifierContributors=" + modifierContributors +
                        '}';
            }
        }

        /**
         * The transformed method.
         */
        protected static class TransformedMethod extends MethodDescription.AbstractBase {

            /**
             * The method's declaring type.
             */
            private final TypeDefinition declaringType;

            /**
             * The method representing the transformed method.
             */
            private final MethodDescription.Token token;

            /**
             * The defined shape of the transformed method.
             */
            private final MethodDescription.InDefinedShape methodDescription;

            /**
             * Creates a new transformed method.
             *
             * @param declaringType     The method's declaring type.
             * @param token             The method representing the transformed method.
             * @param methodDescription The defined shape of the transformed method.
             */
            protected TransformedMethod(TypeDefinition declaringType,
                                        MethodDescription.Token token,
                                        MethodDescription.InDefinedShape methodDescription) {
                this.declaringType = declaringType;
                this.token = token;
                this.methodDescription = methodDescription;
            }

            @Override
            public TypeList.Generic getTypeVariables() {
                return TypeList.Generic.ForDetachedTypes.attachVariables(this, token.getTypeVariableTokens());
            }

            @Override
            public TypeDescription.Generic getReturnType() {
                return token.getReturnType().accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(this));
            }

            @Override
            public ParameterList<?> getParameters() {
                return new TransformedParameterList();
            }

            @Override
            public TypeList.Generic getExceptionTypes() {
                return TypeList.Generic.ForDetachedTypes.attach(this, token.getExceptionTypes());
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return token.getAnnotations();
            }

            @Override
            public String getInternalName() {
                return token.getName();
            }

            @Override
            public TypeDefinition getDeclaringType() {
                return declaringType;
            }

            @Override
            public int getModifiers() {
                return token.getModifiers();
            }

            @Override
            public Object getDefaultValue() {
                return token.getDefaultValue();
            }

            @Override
            public InDefinedShape asDefined() {
                return methodDescription;
            }

            @Override
            public TypeDescription.Generic getReceiverType() {
                return methodDescription.getReceiverType();
            }

            /**
             * A parameter list representing the transformed method's parameters.
             */
            protected class TransformedParameterList extends ParameterList.AbstractBase<ParameterDescription> {

                @Override
                public ParameterDescription get(int index) {
                    return new TransformedParameter(index, token.getParameterTokens().get(index));
                }

                @Override
                public int size() {
                    return token.getParameterTokens().size();
                }
            }

            /**
             * A transformed method's parameter.
             */
            protected class TransformedParameter extends ParameterDescription.AbstractBase {

                /**
                 * The index of the transformed method.
                 */
                private final int index;

                /**
                 * The token representing the transformed method parameter's properties.
                 */
                private final ParameterDescription.Token parameterToken;

                /**
                 * Creates a transformed parameter.
                 *
                 * @param index          The index of the transformed method.
                 * @param parameterToken The token representing the transformed method parameter's properties.
                 */
                protected TransformedParameter(int index, ParameterDescription.Token parameterToken) {
                    this.index = index;
                    this.parameterToken = parameterToken;
                }

                @Override
                public TypeDescription.Generic getType() {
                    return parameterToken.getType().accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(this));
                }

                @Override
                public MethodDescription getDeclaringMethod() {
                    return TransformedMethod.this;
                }

                @Override
                public int getIndex() {
                    return index;
                }

                @Override
                public boolean isNamed() {
                    return parameterToken.getName() != null;
                }

                @Override
                public boolean hasModifiers() {
                    return parameterToken.getModifiers() != null;
                }

                @Override
                public String getName() {
                    return isNamed()
                            ? parameterToken.getName()
                            : super.getName();
                }

                @Override
                public int getModifiers() {
                    return hasModifiers()
                            ? parameterToken.getModifiers()
                            : super.getModifiers();
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return parameterToken.getAnnotations();
                }

                @Override
                public InDefinedShape asDefined() {
                    return methodDescription.getParameters().get(index);
                }
            }
        }
    }

    /**
     * A compound transformer.
     *
     * @param <S> The type of the transformed instance.
     */
    class Compound<S> implements Transformer<S> {

        /**
         * The list of transformers to apply in their application order.
         */
        private final List<? extends Transformer<S>> transformers;

        /**
         * Creates a new compound transformer.
         *
         * @param transformer The list of transformers to apply in their application order.
         */
        // @SafeVarargs
        public Compound(Transformer<S>... transformer) {
            this(Arrays.asList(transformer));
        }

        /**
         * Creates a new compound transformer.
         *
         * @param transformers The list of transformers to apply in their application order.
         */
        public Compound(List<? extends Transformer<S>> transformers) {
            this.transformers = transformers;
        }

        @Override
        public S transform(TypeDescription instrumentedType, S target) {
            for (Transformer<S> transformer : transformers) {
                target = transformer.transform(instrumentedType, target);
            }
            return target;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other != null && getClass() == other.getClass()
                    && transformers.equals(((Compound) other).transformers);
        }

        @Override
        public int hashCode() {
            return transformers.hashCode();
        }

        @Override
        public String toString() {
            return "Transformer.Compound{" +
                    "transformers=" + transformers +
                    '}';
        }
    }
}
