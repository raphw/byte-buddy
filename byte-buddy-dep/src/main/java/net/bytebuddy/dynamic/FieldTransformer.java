package net.bytebuddy.dynamic;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;

/**
 * A field transformer allows to transform a field prior to its definition. This way, previously defined fields
 * can be substituted by a different field description. It is the responsibility of the field transformer that
 * the substitute field remains compatible to the substituted field.
 */
public interface FieldTransformer {

    /**
     * Transforms a field. The transformed field is <b>not</b> validated by Byte Buddy and it is the responsibility
     * of the transformer to assure the validity of the transformation.
     *
     * @param instrumentedType The instrumented type.
     * @param fieldDescription The field to be transformed.
     * @return The transformed field.
     */
    FieldDescription transform(TypeDescription instrumentedType, FieldDescription fieldDescription);

    /**
     * A field transformer that returns the original field.
     */
    enum NoOp implements FieldTransformer {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public FieldDescription transform(TypeDescription instrumentedType, FieldDescription fieldDescription) {
            return fieldDescription;
        }

        @Override
        public String toString() {
            return "FieldTransformer.NoOp." + name();
        }
    }

    /**
     * A simple implementation of a field transformer.
     */
    class Simple implements FieldTransformer {

        /**
         * The token transformer to apply to a transformed field.
         */
        private final TokenTransformer tokenTransformer;

        /**
         * Creates a new simple field transformer.
         *
         * @param tokenTransformer The token transformer to apply to a transformed field.
         */
        public Simple(TokenTransformer tokenTransformer) {
            this.tokenTransformer = tokenTransformer;
        }

        /**
         * Creates a field transformer that patches the transformed field by the givien modifier contributors.
         *
         * @param modifierContributor The modifier contributors to apply.
         * @return A suitable field transformer.
         */
        public static FieldTransformer withModifiers(ModifierContributor.ForField... modifierContributor) {
            return new Simple(new TokenTransformer.ForModifierTransformation(Arrays.asList(modifierContributor)));
        }

        @Override
        public FieldDescription transform(TypeDescription instrumentedType, FieldDescription fieldDescription) {
            return new TransformedField(fieldDescription.getDeclaringType(),
                    tokenTransformer.transform(fieldDescription.asToken(is(instrumentedType))), fieldDescription.asDefined());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && tokenTransformer.equals(((Simple) other).tokenTransformer);
        }

        @Override
        public int hashCode() {
            return tokenTransformer.hashCode();
        }

        @Override
        public String toString() {
            return "FieldTransformer.Simple{" +
                    "tokenTransformer=" + tokenTransformer +
                    '}';
        }

        /**
         * A transformer for a field token where the resulting token is used to represent the transformed field.
         */
        public interface TokenTransformer {

            /**
             * Transforms a field token.
             *
             * @param token The original token that is being transformed.
             * @return The transformed field token.
             */
            FieldDescription.Token transform(FieldDescription.Token token);

            /**
             * A token transformer that transforms a field's modifier.
             */
            class ForModifierTransformation implements TokenTransformer {

                /**
                 * The list of modifier contributors to apply onto the transformed field token.
                 */
                private final List<? extends ModifierContributor.ForField> modifierContributors;

                /**
                 * Creates a new field token modifier for transforming a field's modifiers.
                 *
                 * @param modifierContributors The list of modifier contributors to apply onto the transformed field token.
                 */
                public ForModifierTransformation(List<? extends ModifierContributor.ForField> modifierContributors) {
                    this.modifierContributors = modifierContributors;
                }

                @Override
                public FieldDescription.Token transform(FieldDescription.Token token) {
                    return new FieldDescription.Token(token.getName(),
                            ModifierContributor.Resolver.of(modifierContributors).resolve(token.getModifiers()),
                            token.getType(),
                            token.getAnnotations());
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
                    return "FieldTransformer.Simple.TokenTransformer.ForModifierTransformation{" +
                            "modifierContributors=" + modifierContributors +
                            '}';
                }
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
     * A compound field transformer that applies a list of given transformers in the given order.
     */
    class Compound implements FieldTransformer {

        /**
         * The field transformers represented by this compound transformer in their application order.
         */
        private final List<? extends FieldTransformer> fieldTransformers;

        /**
         * Creates a new compound transformer.
         *
         * @param fieldTransformer The field transformers represented by this compound transformer in their application order.
         */
        public Compound(FieldTransformer... fieldTransformer) {
            this(Arrays.asList(fieldTransformer));
        }

        /**
         * Creates a new compound transformer.
         *
         * @param fieldTransformers The field transformers represented by this compound transformer in their application order.
         */
        public Compound(List<? extends FieldTransformer> fieldTransformers) {
            this.fieldTransformers = fieldTransformers;
        }

        @Override
        public FieldDescription transform(TypeDescription instrumentedType, FieldDescription fieldDescription) {
            FieldDescription transformed = fieldDescription;
            for (FieldTransformer fieldTransformer : fieldTransformers) {
                transformed = fieldTransformer.transform(instrumentedType, transformed);
            }
            return transformed;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other != null && getClass() == other.getClass()
                    && fieldTransformers.equals(((Compound) other).fieldTransformers);
        }

        @Override
        public int hashCode() {
            return fieldTransformers.hashCode();
        }

        @Override
        public String toString() {
            return "FieldTransformer.Compound{" +
                    "fieldTransformers=" + fieldTransformers +
                    '}';
        }
    }

}
