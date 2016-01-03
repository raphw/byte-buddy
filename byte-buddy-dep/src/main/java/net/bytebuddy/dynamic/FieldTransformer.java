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

public interface FieldTransformer {

    FieldDescription transform(TypeDescription instrumentedType, FieldDescription fieldDescription);

    enum NoOp implements FieldTransformer {

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

    class Simple implements FieldTransformer {

        private final TokenTransformer tokenTransformer;

        public Simple(TokenTransformer tokenTransformer) {
            this.tokenTransformer = tokenTransformer;
        }

        public static FieldTransformer withModifiers(ModifierContributor.ForField... modifierContributor) {
            return new Simple(new TokenTransformer.ForModifierTransformation(Arrays.asList(modifierContributor)));
        }

        @Override
        public FieldDescription transform(TypeDescription instrumentedType, FieldDescription fieldDescription) {
            return new TransformedField(fieldDescription.getDeclaringType(), tokenTransformer.transform(fieldDescription.asToken()), fieldDescription.asDefined());
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
            return "MethodTransformer.Simple{" +
                    "transformer=" + tokenTransformer +
                    '}';
        }

        public interface TokenTransformer {

            FieldDescription.Token transform(FieldDescription.Token token);

            class ForModifierTransformation implements TokenTransformer {

                private final List<? extends ModifierContributor.ForField> modifierContributors;

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
                    return "MethodTransformer.Simple.Transformer.ForModifierTransformation{" +
                            "modifierContributors=" + modifierContributors +
                            '}';
                }
            }
        }

        protected static class TransformedField extends FieldDescription.AbstractBase {

            private final TypeDefinition declaringType;

            private final FieldDescription.Token token;

            private final FieldDescription.InDefinedShape fieldDescription;

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

    class Compound implements FieldTransformer {

        private final List<? extends FieldTransformer> fieldTransformers;

        public Compound(FieldTransformer... fieldTransformer) {
            this(Arrays.asList(fieldTransformer));
        }

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
            return "MethodTransformer.Compound{" +
                    "fieldTransformers=" + fieldTransformers +
                    '}';
        }
    }

}
