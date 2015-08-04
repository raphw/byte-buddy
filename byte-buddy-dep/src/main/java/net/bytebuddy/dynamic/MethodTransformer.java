package net.bytebuddy.dynamic;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;

import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

public interface MethodTransformer {

    MethodDescription transform(MethodDescription methodDescription);

    enum Retaining implements MethodTransformer {

        INSTANCE;

        @Override
        public MethodDescription transform(MethodDescription methodDescription) {
            return methodDescription;
        }

        @Override
        public String toString() {
            return "MethodTransformer.Retaining." + name();
        }
    }

    class Transforming implements MethodTransformer {

        public static MethodTransformer enforce(ModifierContributor.ForMethod... modifierTransformer) {
            return new Transforming(new Transformer.ForModifierTransformation(Arrays.asList(nonNull(modifierTransformer))));
        }

        private final Transformer transformer;

        public Transforming(Transformer transformer) {
            this.transformer = transformer;
        }

        @Override
        public MethodDescription transform(MethodDescription methodDescription) {
            return new TransformedMethod(methodDescription, transformer);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && transformer.equals(((Transforming) other).transformer);
        }

        @Override
        public int hashCode() {
            return transformer.hashCode();
        }

        @Override
        public String toString() {
            return "MethodTransformer.Transforming{" +
                    "transformer=" + transformer +
                    '}';
        }

        public interface Transformer {

            GenericTypeDescription resolveReturnType(MethodDescription methodDescription);

            ParameterList<?> resolveParameters(MethodDescription methodDescription);

            GenericTypeList resolveExceptionTypes(MethodDescription methodDescription);

            AnnotationList resolveAnnotations(MethodDescription methodDescription);

            int resolveModifiers(MethodDescription methodDescription);

            GenericTypeDescription resolveReturnType(MethodDescription.InDefinedShape methodDescription);

            ParameterList<ParameterDescription.InDefinedShape> resolveParameters(MethodDescription.InDefinedShape methodDescription);

            GenericTypeList resolveExceptionTypes(MethodDescription.InDefinedShape methodDescription);

            class AbstractBase implements Transformer {

                @Override
                public GenericTypeDescription resolveReturnType(MethodDescription methodDescription) {
                    return methodDescription.getReturnType();
                }

                @Override
                public ParameterList<?> resolveParameters(MethodDescription methodDescription) {
                    return methodDescription.getParameters();
                }

                @Override
                public GenericTypeList resolveExceptionTypes(MethodDescription methodDescription) {
                    return methodDescription.getExceptionTypes();
                }

                @Override
                public AnnotationList resolveAnnotations(MethodDescription methodDescription) {
                    return methodDescription.getDeclaredAnnotations();
                }

                @Override
                public int resolveModifiers(MethodDescription methodDescription) {
                    return methodDescription.getModifiers();
                }

                @Override
                public GenericTypeDescription resolveReturnType(MethodDescription.InDefinedShape methodDescription) {
                    return methodDescription.getReturnType();
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> resolveParameters(MethodDescription.InDefinedShape methodDescription) {
                    return methodDescription.getParameters();
                }

                @Override
                public GenericTypeList resolveExceptionTypes(MethodDescription.InDefinedShape methodDescription) {
                    return methodDescription.getExceptionTypes();
                }
            }

            class ForModifierTransformation extends AbstractBase {

                private final List<? extends ModifierContributor.ForMethod> modifierContributors;

                public ForModifierTransformation(List<? extends ModifierContributor.ForMethod> modifierContributors) {
                    this.modifierContributors = modifierContributors;
                }

                @Override
                public int resolveModifiers(MethodDescription methodDescription) {
                    int modifiers = methodDescription.getModifiers();
                    for (ModifierContributor.ForMethod modifierContributor : modifierContributors) {
                        modifiers = (modifiers & ~modifierContributor.getRange()) | modifierContributor.getMask();
                    }
                    return modifiers;
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
                    return "MethodTransformer.Altering.Transformer.ForModifierTransformation{" +
                            "modifierContributors=" + modifierContributors +
                            '}';
                }
            }
        }

        protected static class TransformedMethod extends MethodDescription.AbstractBase {

            private final MethodDescription methodDescription;

            private final Transformer transformer;

            protected TransformedMethod(MethodDescription methodDescription, Transformer transformer) {
                this.methodDescription = methodDescription;
                this.transformer = transformer;
            }

            @Override
            public GenericTypeDescription getReturnType() {
                return transformer.resolveReturnType(methodDescription);
            }

            @Override
            public ParameterList<?> getParameters() {
                return transformer.resolveParameters(methodDescription);
            }

            @Override
            public GenericTypeList getExceptionTypes() {
                return transformer.resolveExceptionTypes(methodDescription);
            }

            @Override
            public Object getDefaultValue() {
                return methodDescription.getDefaultValue();
            }

            @Override
            public GenericTypeList getTypeVariables() {
                return transformer.resolveExceptionTypes(methodDescription);
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return transformer.resolveAnnotations(methodDescription);
            }

            @Override
            public GenericTypeDescription getDeclaringType() {
                return methodDescription.getDeclaringType();
            }

            @Override
            public int getModifiers() {
                return transformer.resolveModifiers(methodDescription);
            }

            @Override
            public InDefinedShape asDefined() {
                return new InDefinedShape(methodDescription.asDefined(), transformer);
            }

            @Override
            public String getInternalName() {
                return methodDescription.getInternalName();
            }

            protected static class InDefinedShape extends MethodDescription.InDefinedShape.AbstractBase {

                private final MethodDescription.InDefinedShape methodDescription;

                private final Transformer transformer;

                public InDefinedShape(MethodDescription.InDefinedShape methodDescription, Transformer transformer) {
                    this.methodDescription = methodDescription;
                    this.transformer = transformer;
                }

                @Override
                public GenericTypeDescription getReturnType() {
                    return transformer.resolveReturnType(methodDescription);
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return transformer.resolveParameters(methodDescription);
                }

                @Override
                public GenericTypeList getExceptionTypes() {
                    return transformer.resolveExceptionTypes(methodDescription);
                }

                @Override
                public Object getDefaultValue() {
                    return methodDescription.getDefaultValue();
                }

                @Override
                public GenericTypeList getTypeVariables() {
                    return transformer.resolveExceptionTypes(methodDescription);
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return transformer.resolveAnnotations(methodDescription);
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return methodDescription.getDeclaringType();
                }

                @Override
                public int getModifiers() {
                    return transformer.resolveModifiers(methodDescription);
                }

                @Override
                public String getInternalName() {
                    return methodDescription.getInternalName();
                }
            }
        }
    }
}
