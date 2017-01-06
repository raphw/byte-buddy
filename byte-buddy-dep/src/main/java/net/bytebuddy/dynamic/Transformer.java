package net.bytebuddy.dynamic;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;

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
    }

    /**
     * A transformer for a field that delegates to another transformer that transforms a {@link net.bytebuddy.description.field.FieldDescription.Token}.
     */
    @EqualsAndHashCode
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
            return new TransformedField(instrumentedType,
                    fieldDescription.getDeclaringType(),
                    transformer.transform(instrumentedType, fieldDescription.asToken(none())),
                    fieldDescription.asDefined());
        }

        /**
         * A transformer for a field's modifiers.
         */
        @EqualsAndHashCode
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
        }

        /**
         * An implementation of a transformed field.
         */
        protected static class TransformedField extends FieldDescription.AbstractBase {

            /**
             * The instrumented type for which this field is transformed.
             */
            private final TypeDescription instrumentedType;

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
             * @param instrumentedType The instrumented type for which this field is transformed.
             * @param declaringType    The field's declaring type.
             * @param token            A field token representing the transformed field.
             * @param fieldDescription The field's defined shape.
             */
            protected TransformedField(TypeDescription instrumentedType,
                                       TypeDefinition declaringType,
                                       Token token,
                                       InDefinedShape fieldDescription) {
                this.instrumentedType = instrumentedType;
                this.declaringType = declaringType;
                this.token = token;
                this.fieldDescription = fieldDescription;
            }

            @Override
            public TypeDescription.Generic getType() {
                return token.getType().accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(instrumentedType));
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
    @EqualsAndHashCode
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
            return new TransformedMethod(instrumentedType,
                    methodDescription.getDeclaringType(),
                    transformer.transform(instrumentedType, methodDescription.asToken(none())),
                    methodDescription.asDefined());
        }

        /**
         * A transformer for a method's modifiers.
         */
        @EqualsAndHashCode
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
        }

        /**
         * The transformed method.
         */
        protected static class TransformedMethod extends MethodDescription.AbstractBase {

            /**
             * The instrumented type for which this method is transformed.
             */
            private final TypeDescription instrumentedType;

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
             * @param instrumentedType  The instrumented type for which this method is transformed.
             * @param declaringType     The method's declaring type.
             * @param token             The method representing the transformed method.
             * @param methodDescription The defined shape of the transformed method.
             */
            protected TransformedMethod(TypeDescription instrumentedType,
                                        TypeDefinition declaringType,
                                        Token token,
                                        InDefinedShape methodDescription) {
                this.instrumentedType = instrumentedType;
                this.declaringType = declaringType;
                this.token = token;
                this.methodDescription = methodDescription;
            }

            @Override
            public TypeList.Generic getTypeVariables() {
                return new TypeList.Generic.ForDetachedTypes.OfTypeVariables(this, token.getTypeVariableTokens(), new AttachmentVisitor());
            }

            @Override
            public TypeDescription.Generic getReturnType() {
                return token.getReturnType().accept(new AttachmentVisitor());
            }

            @Override
            public ParameterList<?> getParameters() {
                return new TransformedParameterList();
            }

            @Override
            public TypeList.Generic getExceptionTypes() {
                return new TypeList.Generic.ForDetachedTypes(token.getExceptionTypes(), new AttachmentVisitor());
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
            public AnnotationValue<?, ?> getDefaultValue() {
                return token.getDefaultValue();
            }

            @Override
            public InDefinedShape asDefined() {
                return methodDescription;
            }

            @Override
            public TypeDescription.Generic getReceiverType() {
                TypeDescription.Generic receiverType = token.getReceiverType();
                return receiverType == null
                        ? TypeDescription.Generic.UNDEFINED
                        : receiverType.accept(new AttachmentVisitor());
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
                    return parameterToken.getType().accept(new AttachmentVisitor());
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

            /**
             * A visitor that attaches type variables based on the transformed method's type variables and the instrumented type. Binding type
             * variables directly for this method is not possible as type variables are already resolved for the instrumented type such
             * that it is required to bind variables for the instrumented type directly.
             */
            protected class AttachmentVisitor extends TypeDescription.Generic.Visitor.Substitutor.WithoutTypeSubstitution {

                @Override
                public TypeDescription.Generic onTypeVariable(TypeDescription.Generic typeVariable) {
                    TypeList.Generic candidates = getTypeVariables().filter(named(typeVariable.getSymbol()));
                    TypeDescription.Generic attached = candidates.isEmpty()
                            ? instrumentedType.findVariable(typeVariable.getSymbol())
                            : candidates.getOnly();
                    if (attached == null) {
                        throw new IllegalArgumentException("Cannot attach undefined variable: " + typeVariable);
                    } else {
                        return new TypeDescription.Generic.OfTypeVariable.WithAnnotationOverlay(attached, typeVariable.getDeclaredAnnotations());
                    }
                }

                @Override
                public int hashCode() {
                    return TransformedMethod.this.hashCode();
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || (other instanceof AttachmentVisitor && ((AttachmentVisitor) other).getOuter().equals(TransformedMethod.this));
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private TransformedMethod getOuter() {
                    return TransformedMethod.this;
                }
            }
        }
    }

    /**
     * A compound transformer.
     *
     * @param <S> The type of the transformed instance.
     */
    @EqualsAndHashCode
    class Compound<S> implements Transformer<S> {

        /**
         * The list of transformers to apply in their application order.
         */
        private final List<Transformer<S>> transformers;

        /**
         * Creates a new compound transformer.
         *
         * @param transformer The list of transformers to apply in their application order.
         */
        @SuppressWarnings("unchecked") // In absence of @SafeVarargs for Java 6
        public Compound(Transformer<S>... transformer) {
            this(Arrays.asList(transformer));
        }

        /**
         * Creates a new compound transformer.
         *
         * @param transformers The list of transformers to apply in their application order.
         */
        public Compound(List<? extends Transformer<S>> transformers) {
            this.transformers = new ArrayList<Transformer<S>>();
            for (Transformer<S> transformer : transformers) {
                if (transformer instanceof Compound) {
                    this.transformers.addAll(((Compound<S>) transformer).transformers);
                } else if (!(transformer instanceof NoOp)) {
                    this.transformers.add(transformer);
                }
            }
        }

        @Override
        public S transform(TypeDescription instrumentedType, S target) {
            for (Transformer<S> transformer : transformers) {
                target = transformer.transform(instrumentedType, target);
            }
            return target;
        }
    }
}
