package net.bytebuddy.dynamic;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
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

        /**
         * {@inheritDoc}
         */
        public Object transform(TypeDescription instrumentedType, Object target) {
            return target;
        }
    }

    /**
     * A transformer for a field that delegates to another transformer that transforms a {@link net.bytebuddy.description.field.FieldDescription.Token}.
     */
    @HashCodeAndEqualsPlugin.Enhance
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
         * Creates a field transformer that patches the transformed field by the given modifier contributors.
         *
         * @param modifierContributor The modifier contributors to apply.
         * @return A suitable field transformer.
         */
        public static Transformer<FieldDescription> withModifiers(ModifierContributor.ForField... modifierContributor) {
            return withModifiers(Arrays.asList(modifierContributor));
        }

        /**
         * Creates a field transformer that patches the transformed field by the given modifier contributors.
         *
         * @param modifierContributors The modifier contributors to apply.
         * @return A suitable field transformer.
         */
        public static Transformer<FieldDescription> withModifiers(List<? extends ModifierContributor.ForField> modifierContributors) {
            return new ForField(new FieldModifierTransformer(ModifierContributor.Resolver.of(modifierContributors)));
        }

        /**
         * {@inheritDoc}
         */
        public FieldDescription transform(TypeDescription instrumentedType, FieldDescription fieldDescription) {
            return new TransformedField(instrumentedType,
                    fieldDescription.getDeclaringType(),
                    transformer.transform(instrumentedType, fieldDescription.asToken(none())),
                    fieldDescription.asDefined());
        }

        /**
         * A transformer for a field's modifiers.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class FieldModifierTransformer implements Transformer<FieldDescription.Token> {

            /**
             * The resolver to apply for transforming the modifiers of a field.
             */
            private final ModifierContributor.Resolver<ModifierContributor.ForField> resolver;

            /**
             * Creates a new field token modifier for transforming a field's modifiers.
             *
             * @param resolver The resolver to apply for transforming the modifiers of a field.
             */
            protected FieldModifierTransformer(ModifierContributor.Resolver<ModifierContributor.ForField> resolver) {
                this.resolver = resolver;
            }

            /**
             * {@inheritDoc}
             */
            public FieldDescription.Token transform(TypeDescription instrumentedType, FieldDescription.Token target) {
                return new FieldDescription.Token(target.getName(),
                        resolver.resolve(target.getModifiers()),
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

            /**
             * {@inheritDoc}
             */
            public TypeDescription.Generic getType() {
                return token.getType().accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(instrumentedType));
            }

            /**
             * {@inheritDoc}
             */
            public AnnotationList getDeclaredAnnotations() {
                return token.getAnnotations();
            }

            /**
             * {@inheritDoc}
             */
            public TypeDefinition getDeclaringType() {
                return declaringType;
            }

            /**
             * {@inheritDoc}
             */
            public int getModifiers() {
                return token.getModifiers();
            }

            /**
             * {@inheritDoc}
             */
            public InDefinedShape asDefined() {
                return fieldDescription;
            }

            /**
             * {@inheritDoc}
             */
            public String getName() {
                return token.getName();
            }
        }
    }

    /**
     * A transformer for a field that delegates to another transformer that transforms a {@link net.bytebuddy.description.method.MethodDescription.Token}.
     */
    @HashCodeAndEqualsPlugin.Enhance
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
         * @param modifierContributor The modifier transformers in their application order.
         * @return A method transformer where each method's modifiers are adapted to the given modifiers.
         */
        public static Transformer<MethodDescription> withModifiers(ModifierContributor.ForMethod... modifierContributor) {
            return withModifiers(Arrays.asList(modifierContributor));
        }

        /**
         * Creates a transformer that enforces the supplied modifier contributors. All ranges of each contributor is first cleared and then overridden
         * by the specified modifiers in the order they are supplied.
         *
         * @param modifierContributors The modifier contributors in their application order.
         * @return A method transformer where each method's modifiers are adapted to the given modifiers.
         */
        public static Transformer<MethodDescription> withModifiers(List<? extends ModifierContributor.ForMethod> modifierContributors) {
            return new ForMethod(new MethodModifierTransformer(ModifierContributor.Resolver.of(modifierContributors)));
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription transform(TypeDescription instrumentedType, MethodDescription methodDescription) {
            return new TransformedMethod(instrumentedType,
                    methodDescription.getDeclaringType(),
                    transformer.transform(instrumentedType, methodDescription.asToken(none())),
                    methodDescription.asDefined());
        }

        /**
         * A transformer for a method's modifiers.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class MethodModifierTransformer implements Transformer<MethodDescription.Token> {

            /**
             * The resolver to apply onto the method's modifiers.
             */
            private final ModifierContributor.Resolver<ModifierContributor.ForMethod> resolver;

            /**
             * Creates a new modifier transformation.
             *
             * @param resolver The resolver to apply onto the method's modifiers.
             */
            protected MethodModifierTransformer(ModifierContributor.Resolver<ModifierContributor.ForMethod> resolver) {
                this.resolver = resolver;
            }

            /**
             * {@inheritDoc}
             */
            public MethodDescription.Token transform(TypeDescription instrumentedType, MethodDescription.Token target) {
                return new MethodDescription.Token(target.getName(),
                        resolver.resolve(target.getModifiers()),
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

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getTypeVariables() {
                return new TypeList.Generic.ForDetachedTypes.OfTypeVariables(this, token.getTypeVariableTokens(), new AttachmentVisitor());
            }

            /**
             * {@inheritDoc}
             */
            public TypeDescription.Generic getReturnType() {
                return token.getReturnType().accept(new AttachmentVisitor());
            }

            /**
             * {@inheritDoc}
             */
            public ParameterList<?> getParameters() {
                return new TransformedParameterList();
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getExceptionTypes() {
                return new TypeList.Generic.ForDetachedTypes(token.getExceptionTypes(), new AttachmentVisitor());
            }

            /**
             * {@inheritDoc}
             */
            public AnnotationList getDeclaredAnnotations() {
                return token.getAnnotations();
            }

            /**
             * {@inheritDoc}
             */
            public String getInternalName() {
                return token.getName();
            }

            /**
             * {@inheritDoc}
             */
            public TypeDefinition getDeclaringType() {
                return declaringType;
            }

            /**
             * {@inheritDoc}
             */
            public int getModifiers() {
                return token.getModifiers();
            }

            /**
             * {@inheritDoc}
             */
            public AnnotationValue<?, ?> getDefaultValue() {
                return token.getDefaultValue();
            }

            /**
             * {@inheritDoc}
             */
            public InDefinedShape asDefined() {
                return methodDescription;
            }

            /**
             * {@inheritDoc}
             */
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

                /**
                 * {@inheritDoc}
                 */
                public ParameterDescription get(int index) {
                    return new TransformedParameter(index, token.getParameterTokens().get(index));
                }

                /**
                 * {@inheritDoc}
                 */
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

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription.Generic getType() {
                    return parameterToken.getType().accept(new AttachmentVisitor());
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDescription getDeclaringMethod() {
                    return TransformedMethod.this;
                }

                /**
                 * {@inheritDoc}
                 */
                public int getIndex() {
                    return index;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isNamed() {
                    return parameterToken.getName() != null;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean hasModifiers() {
                    return parameterToken.getModifiers() != null;
                }

                /**
                 * {@inheritDoc}
                 */
                public String getName() {
                    return isNamed()
                            ? parameterToken.getName()
                            : super.getName();
                }

                /**
                 * {@inheritDoc}
                 */
                public int getModifiers() {
                    return hasModifiers()
                            ? parameterToken.getModifiers()
                            : super.getModifiers();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return parameterToken.getAnnotations();
                }

                /**
                 * {@inheritDoc}
                 */
                public InDefinedShape asDefined() {
                    return methodDescription.getParameters().get(index);
                }
            }

            /**
             * A visitor that attaches type variables based on the transformed method's type variables and the instrumented type. Binding type
             * variables directly for this method is not possible as type variables are already resolved for the instrumented type such
             * that it is required to bind variables for the instrumented type directly.
             */
            @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
            protected class AttachmentVisitor extends TypeDescription.Generic.Visitor.Substitutor.WithoutTypeSubstitution {

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription.Generic onTypeVariable(TypeDescription.Generic typeVariable) {
                    TypeList.Generic candidates = getTypeVariables().filter(named(typeVariable.getSymbol()));
                    TypeDescription.Generic attached = candidates.isEmpty()
                            ? instrumentedType.findVariable(typeVariable.getSymbol())
                            : candidates.getOnly();
                    if (attached == null) {
                        throw new IllegalArgumentException("Cannot attach undefined variable: " + typeVariable);
                    } else {
                        return new TypeDescription.Generic.OfTypeVariable.WithAnnotationOverlay(attached, typeVariable);
                    }
                }
            }
        }
    }

    /**
     * A compound transformer.
     *
     * @param <S> The type of the transformed instance.
     */
    @HashCodeAndEqualsPlugin.Enhance
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
        @SuppressWarnings("unchecked") // In absence of @SafeVarargs
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

        /**
         * {@inheritDoc}
         */
        public S transform(TypeDescription instrumentedType, S target) {
            for (Transformer<S> transformer : transformers) {
                target = transformer.transform(instrumentedType, target);
            }
            return target;
        }
    }
}
