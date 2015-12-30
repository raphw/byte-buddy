package net.bytebuddy.dynamic;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

import java.util.Arrays;
import java.util.List;

public interface Transformer<T extends ByteCodeElement> {

    T transform(TypeDescription instrumentedType, T byteCodeElement);

    /**
     * A method transformer that returns the original method.
     */
    enum NoOp implements Transformer<ByteCodeElement> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @SuppressWarnings("unchecked")
        public static <S extends ByteCodeElement> Transformer<S> make() {
            return (Transformer<S>) INSTANCE;
        }

        @Override
        public ByteCodeElement transform(TypeDescription instrumentedType, ByteCodeElement byteCodeElement) {
            return byteCodeElement;
        }

        @Override
        public String toString() {
            return "Transformer.NoOp." + name();
        }
    }

    /**
     * A method transformer that modifies method properties by applying a {@link Simple.Transformer}.
     */
    class ForMethodDescription implements Transformer<MethodDescription> {

        /**
         * The transformer to be applied.
         */
        private final Engine engine;

        /**
         * Creates a new transforming method transformer.
         *
         * @param engine The engine to be applied.
         */
        public ForMethodDescription(Engine engine) {
            this.engine = engine;
        }

        /**
         * Creates a transformer that enforces the supplied modifier contributors. All ranges of each contributor is first cleared and then overridden
         * by the specified modifiers in the order they are supplied.
         *
         * @param modifierTransformer The modifier transformers in their application order.
         * @return A method transformer where each method's modifiers are adapted to the given modifiers.
         */
        public static Transformer<MethodDescription> withModifiers(ModifierContributor.ForMethod... modifierTransformer) {
            return new ForMethodDescription(new Engine.ForModifierTransformation(Arrays.asList(modifierTransformer)));
        }

        @Override
        public MethodDescription transform(TypeDescription instrumentedType, MethodDescription methodDescription) {
            return new TransformedMethod(methodDescription.getDeclaringType(), engine.transform(methodDescription.asToken()), methodDescription.asDefined());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && engine.equals(((ForMethodDescription) other).engine);
        }

        @Override
        public int hashCode() {
            return engine.hashCode();
        }

        @Override
        public String toString() {
            return "Transformer.Simple{" +
                    "engine=" + engine +
                    '}';
        }

        /**
         * A transformer is responsible for transforming a method token into its transformed form.
         */
        public interface Engine {

            /**
             * Transforms a method token.
             *
             * @param token The original method's token.
             * @return The transformed method token.
             */
            MethodDescription.Token transform(MethodDescription.Token token);

            /**
             * A transformation for a modifier transformation.
             */
            class ForModifierTransformation implements Engine {

                /**
                 * The modifier contributors to apply on each transformation.
                 */
                private final List<? extends ModifierContributor.ForMethod> modifierContributors;

                /**
                 * Creates a new modifier transformation.
                 *
                 * @param modifierContributors The modifier contributors to apply on each transformation in their application order.
                 */
                public ForModifierTransformation(List<? extends ModifierContributor.ForMethod> modifierContributors) {
                    this.modifierContributors = modifierContributors;
                }

                @Override
                public MethodDescription.Token transform(MethodDescription.Token token) {
                    int modifiers = token.getModifiers();
                    for (ModifierContributor.ForMethod modifierContributor : modifierContributors) {
                        modifiers = (modifiers & ~modifierContributor.getRange()) | modifierContributor.getMask();
                    }
                    return new MethodDescription.Token(token.getName(),
                            modifiers,
                            token.getTypeVariables(),
                            token.getReturnType(),
                            token.getParameterTokens(),
                            token.getExceptionTypes(),
                            token.getAnnotations(),
                            token.getDefaultValue());
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && modifierContributors.equals(((ForModifierTransformation) other).modifierContributors);
                }

                @Override
                public int hashCode() {
                    return modifierContributors.hashCode();
                }

                @Override
                public String toString() {
                    return "Transformer.Simple.Engine.ForModifierTransformation{" +
                            "modifierContributors=" + modifierContributors +
                            '}';
                }
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
            private final MethodDescription.InDefinedShape definedShape;

            /**
             * Creates a new transformed method.
             *
             * @param declaringType The method's declaring type.
             * @param token   The method representing the transformed method.
             * @param definedShape  The defined shape of the transformed method.
             */
            protected TransformedMethod(TypeDefinition declaringType,
                                        MethodDescription.Token token,
                                        MethodDescription.InDefinedShape definedShape) {
                this.declaringType = declaringType;
                this.token = token;
                this.definedShape = definedShape;
            }

            @Override
            public TypeList.Generic getTypeVariables() {
                return TypeList.Generic.ForDetachedTypes.attach(this, token.getTypeVariables());
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
                return definedShape;
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
                    return definedShape.getParameters().get(index);
                }
            }
        }
    }

    /**
     * A method transformer that applies several method transformers in a row.
     */
    class Compound<S extends ByteCodeElement> implements Transformer<S> {

        /**
         * The method transformers in their application order.
         */
        private final List<? extends Transformer<S>> methodTransformers;

        /**
         * Creates a new compound method transformer.
         *
         * @param methodTransformer The method transformers in their application order.
         */
        public Compound(Transformer<S>... methodTransformer) {
            this(Arrays.asList(methodTransformer));
        }

        /**
         * Creates a new compound method transformer.
         *
         * @param methodTransformers The method transformers in their application order.
         */
        public Compound(List<? extends Transformer<S>> methodTransformers) {
            this.methodTransformers = methodTransformers;
        }

        @Override
        public S transform(TypeDescription instrumentedType, S byteCodeElement) {
            S transformed = byteCodeElement;
            for (Transformer<S> transformer : methodTransformers) {
                transformed = transformer.transform(instrumentedType, transformed);
            }
            return transformed;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other != null && getClass() == other.getClass()
                    && methodTransformers.equals(((Compound) other).methodTransformers);
        }

        @Override
        public int hashCode() {
            return methodTransformers.hashCode();
        }

        @Override
        public String toString() {
            return "Transformer.Compound{" +
                    "methodTransformers=" + methodTransformers +
                    '}';
        }
    }

}
