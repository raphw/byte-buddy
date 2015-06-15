package net.bytebuddy.description.method;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotatedCodeElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.utility.JavaMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Description of the parameter of a Java method or constructor.
 */
public interface ParameterDescription extends AnnotatedCodeElement, NamedElement.WithRuntimeName, ModifierReviewable {

    /**
     * The prefix for names of an unnamed parameter.
     */
    String NAME_PREFIX = "arg";

    GenericTypeDescription getType();

    /**
     * Returns the method that declares this parameter.
     *
     * @return The method that declares this parameter.
     */
    MethodDescription getDeclaringMethod();

    /**
     * Returns this parameter's index.
     *
     * @return The index of this parameter.
     */
    int getIndex();

    /**
     * Checks if this parameter has an explicit name. A parameter without an explicit name is named implicitly by
     * {@code argX} with {@code X} denoting the zero-based index.
     *
     * @return {@code true} if the parameter has an explicit name.
     */
    boolean isNamed();

    /**
     * Checks if this parameter has an explicit modifier. A parameter without a modifier is simply treated as
     * if it had a modifier of zero.
     *
     * @return {@code true} if this parameter defines explicit modifiers.
     */
    boolean hasModifiers();

    /**
     * Returns the offset to the parameter value within the local method variable.
     *
     * @return The offset of this parameter's value.
     */
    int getOffset();

    Token asToken();

    /**
     * A base implementation of a method parameter description.
     */
    abstract class AbstractParameterDescription extends AbstractModifierReviewable implements ParameterDescription {

        @Override
        public String getName() {
            return NAME_PREFIX.concat(String.valueOf(getIndex()));
        }

        @Override
        public String getInternalName() {
            return getName();
        }

        @Override
        public String getSourceCodeName() {
            return isNamed()
                    ? getName()
                    : EMPTY_NAME;
        }

        @Override
        public int getModifiers() {
            return EMPTY_MASK;
        }

        @Override
        public int getOffset() {
            TypeList parameterType = getDeclaringMethod().getParameters().asTypeList().asRawTypes();
            int offset = getDeclaringMethod().isStatic()
                    ? StackSize.ZERO.getSize()
                    : StackSize.SINGLE.getSize();
            for (int i = 0; i < getIndex(); i++) {
                offset += parameterType.get(i).getStackSize().getSize();
            }
            return offset;
        }

        @Override
        public Token asToken() {
            return new Token(getType().accept(new GenericTypeDescription.Visitor.Substitutor.ForDetachment(getDeclaringMethod().getDeclaringType())),
                    getDeclaredAnnotations(),
                    isNamed()
                            ? getName()
                            : Token.NO_NAME,
                    hasModifiers()
                            ? (Integer) getModifiers()
                            : Token.NO_MODIFIERS);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof ParameterDescription)) {
                return false;
            }
            ParameterDescription parameterDescription = (ParameterDescription) other;
            return getDeclaringMethod().equals(parameterDescription.getDeclaringMethod())
                    && getIndex() == parameterDescription.getIndex();
        }

        @Override
        public int hashCode() {
            return getDeclaringMethod().hashCode() ^ getIndex();
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(Modifier.toString(getModifiers()));
            if (getModifiers() != EMPTY_MASK) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(isVarArgs()
                    ? getType().asRawType().getName().replaceFirst("\\[\\]$", "...")
                    : getType().asRawType().getName());
            return stringBuilder.append(' ').append(getName()).toString();
        }
    }

    /**
     * Description of a loaded parameter, represented by a Java 8 {@code java.lang.reflect.Parameter}.
     */
    class ForLoadedParameter extends AbstractParameterDescription {

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s {@code getType} method.
         */
        protected static final JavaMethod GET_TYPE;

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s {@code getName} method.
         */
        private static final JavaMethod GET_NAME;

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s
         * {@code getDeclaringExecutable} method.
         */
        private static final JavaMethod GET_DECLARING_EXECUTABLE;

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s {@code isNamePresent} method.
         */
        private static final JavaMethod IS_NAME_PRESENT;

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s {@code getModifiers} method.
         */
        private static final JavaMethod GET_MODIFIERS;

        /**
         * Java method representation for the {@code java.lang.reflect.Parameter}'s {@code getDeclaredAnnotations}
         * method.
         */
        private static final JavaMethod GET_DECLARED_ANNOTATIONS;

        /*
         * Initializes the {@link net.bytebuddy.utility.JavaMethod} instances of this class dependant on
         * whether they are available.
         */
        static {
            JavaMethod getName, getDeclaringExecutable, isNamePresent, getModifiers, getDeclaredAnnotations, getType;
            try {
                Class<?> parameterType = Class.forName("java.lang.reflect.Parameter");
                getName = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getName"));
                getDeclaringExecutable = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getDeclaringExecutable"));
                isNamePresent = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("isNamePresent"));
                getModifiers = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getModifiers"));
                getDeclaredAnnotations = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getDeclaredAnnotations"));
                getType = new JavaMethod.ForLoadedMethod(parameterType.getDeclaredMethod("getType"));
            } catch (Exception ignored) {
                getName = JavaMethod.ForUnavailableMethod.INSTANCE;
                getDeclaringExecutable = JavaMethod.ForUnavailableMethod.INSTANCE;
                isNamePresent = JavaMethod.ForUnavailableMethod.INSTANCE;
                getModifiers = JavaMethod.ForUnavailableMethod.INSTANCE;
                getDeclaredAnnotations = JavaMethod.ForUnavailableMethod.INSTANCE;
                getType = JavaMethod.ForUnavailableMethod.INSTANCE;
            }
            GET_NAME = getName;
            GET_DECLARING_EXECUTABLE = getDeclaringExecutable;
            IS_NAME_PRESENT = isNamePresent;
            GET_MODIFIERS = getModifiers;
            GET_DECLARED_ANNOTATIONS = getDeclaredAnnotations;
            GET_TYPE = getType;
        }

        /**
         * An instance of {@code java.lang.reflect.Parameter}.
         */
        private final Object parameter;

        /**
         * The parameter's index.
         */
        private final int index;

        /**
         * Creates a representation of a loaded parameter.
         *
         * @param parameter An instance of {@code java.lang.reflect.Parameter}.
         * @param index     The parameter's index.
         */
        protected ForLoadedParameter(Object parameter, int index) {
            this.parameter = parameter;
            this.index = index;
        }

        @Override
        public GenericTypeDescription getType() {
            return new GenericTypeDescription.LazyProjection.OfLoadedParameter(parameter);
        }

        @Override
        public MethodDescription getDeclaringMethod() {
            Object executable = GET_DECLARING_EXECUTABLE.invoke(parameter);
            if (executable instanceof Method) {
                return new MethodDescription.ForLoadedMethod((Method) executable);
            } else if (executable instanceof Constructor) {
                return new MethodDescription.ForLoadedConstructor((Constructor<?>) executable);
            } else {
                throw new IllegalStateException("Unknown executable type: " + executable);
            }
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotation((Annotation[]) GET_DECLARED_ANNOTATIONS.invoke(parameter));
        }

        @Override
        public String getName() {
            return (String) GET_NAME.invoke(parameter);
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isNamed() {
            return (Boolean) IS_NAME_PRESENT.invoke(parameter);
        }

        @Override
        public int getModifiers() {
            return (Integer) GET_MODIFIERS.invoke(parameter);
        }

        @Override
        public boolean hasModifiers() {
            // Rational: If a parameter is not named despite the information being attached,
            // it is synthetic, i.e. it has non-default modifiers.
            return isNamed() || getModifiers() != EMPTY_MASK;
        }

        /**
         * Description of a loaded method's parameter on a virtual machine where {@code java.lang.reflect.Parameter}
         * is not available.
         */
        protected static class OfLegacyVmMethod extends AbstractParameterDescription {

            /**
             * The method that declares this parameter.
             */
            private final Method method;

            /**
             * The index of this parameter.
             */
            private final int index;

            /**
             * The type of this parameter.
             */
            private final Class<?> parameterType;

            /**
             * The annotations of this parameter.
             */
            private final Annotation[] parameterAnnotation;

            /**
             * Creates a legacy representation of a method's parameter.
             *
             * @param method              The method that declares this parameter.
             * @param index               The index of this parameter.
             * @param parameterType       The type of this parameter.
             * @param parameterAnnotation The annotations of this parameter.
             */
            protected OfLegacyVmMethod(Method method, int index, Class<?> parameterType, Annotation[] parameterAnnotation) {
                this.method = method;
                this.index = index;
                this.parameterType = parameterType;
                this.parameterAnnotation = parameterAnnotation;
            }

            @Override
            public GenericTypeDescription getType() {
                return new TypeDescription.LazyProjection.OfLegacyVmMethodParameter(method, index, parameterType);
            }

            @Override
            public MethodDescription getDeclaringMethod() {
                return new MethodDescription.ForLoadedMethod(method);
            }

            @Override
            public int getIndex() {
                return index;
            }

            @Override
            public boolean isNamed() {
                return false;
            }

            @Override
            public boolean hasModifiers() {
                return false;
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.ForLoadedAnnotation(parameterAnnotation);
            }
        }

        /**
         * Description of a loaded constructor's parameter on a virtual machine where {@code java.lang.reflect.Parameter}
         * is not available.
         */
        protected static class OfLegacyVmConstructor extends AbstractParameterDescription {

            /**
             * The method that declares this parameter.
             */
            private final Constructor<?> constructor;

            /**
             * The index of this parameter.
             */
            private final int index;

            /**
             * The type of this parameter.
             */
            private final Class<?> parameterType;

            /**
             * The annotations of this parameter.
             */
            private final Annotation[] parameterAnnotation;

            /**
             * Creates a legacy representation of a method's parameter.
             *
             * @param constructor         The constructor that declares this parameter.
             * @param index               The index of this parameter.
             * @param parameterType       The type of this parameter.
             * @param parameterAnnotation The annotations of this parameter.
             */
            protected OfLegacyVmConstructor(Constructor<?> constructor, int index, Class<?> parameterType, Annotation[] parameterAnnotation) {
                this.constructor = constructor;
                this.index = index;
                this.parameterType = parameterType;
                this.parameterAnnotation = parameterAnnotation;
            }

            @Override
            public GenericTypeDescription getType() {
                return new TypeDescription.LazyProjection.OfLegacyVmConstructorParameter(constructor, index, parameterType);
            }

            @Override
            public MethodDescription getDeclaringMethod() {
                return new MethodDescription.ForLoadedConstructor(constructor);
            }

            @Override
            public int getIndex() {
                return index;
            }

            @Override
            public boolean isNamed() {
                return false;
            }

            @Override
            public boolean hasModifiers() {
                return false;
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.ForLoadedAnnotation(parameterAnnotation);
            }
        }
    }

    /**
     * A latent description of a parameter that is not attached to a method or constructor.
     */
    class Latent extends AbstractParameterDescription {

        /**
         * The method that is declaring the parameter.
         */
        private final MethodDescription declaringMethod;

        /**
         * The type of the parameter.
         */
        private final GenericTypeDescription parameterType;

        private final List<? extends AnnotationDescription> declaredAnnotations;

        private final String name;

        private final Integer modifiers;

        /**
         * The index of the parameter.
         */
        private final int index;

        /**
         * The parameter's offset in the local method variables array.
         */
        private final int offset;

        public Latent(MethodDescription declaringMethod, Token token, int index, int offset) {
            this(declaringMethod,
                    token.getType(),
                    token.getAnnotations(),
                    token.getName(),
                    token.getModifiers(),
                    index,
                    offset);
        }

        public Latent(MethodDescription declaringMethod,
                      GenericTypeDescription parameterType,
                      List<? extends AnnotationDescription> declaredAnnotations,
                      String name,
                      Integer modifiers,
                      int index,
                      int offset) {
            this.declaringMethod = declaringMethod;
            this.parameterType = parameterType;
            this.declaredAnnotations = declaredAnnotations;
            this.name = name;
            this.modifiers = modifiers;
            this.index = index;
            this.offset = offset;
        }

        @Override
        public GenericTypeDescription getType() {
            return parameterType;
        }

        @Override
        public MethodDescription getDeclaringMethod() {
            return declaringMethod;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public boolean isNamed() {
            return name != null;
        }

        @Override
        public boolean hasModifiers() {
            return modifiers != null;
        }

        @Override
        public String getName() {
            return isNamed()
                    ? name
                    : super.getName();
        }

        @Override
        public int getModifiers() {
            return hasModifiers()
                    ? modifiers
                    : super.getModifiers();
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(declaredAnnotations);
        }
    }

    class Token implements ByteCodeElement.Token<Token> {

        public static final String NO_NAME = null;

        public static final Integer NO_MODIFIERS = null;

        public static List<Token> asList(List<? extends GenericTypeDescription> typeDescriptions) {
            List<Token> tokens = new ArrayList<Token>(typeDescriptions.size());
            for (GenericTypeDescription typeDescription : typeDescriptions) {
                tokens.add(new Token(typeDescription));
            }
            return tokens;
        }

        private final GenericTypeDescription typeDescription;

        private final List<? extends AnnotationDescription> annotationDescriptions;

        private final String name;

        private final Integer modifiers;

        public Token(GenericTypeDescription typeDescription) {
            this(typeDescription, Collections.<AnnotationDescription>emptyList());
        }

        public Token(GenericTypeDescription typeDescription, List<? extends AnnotationDescription> annotationDescriptions) {
            this(typeDescription, annotationDescriptions, NO_NAME, NO_MODIFIERS);
        }

        public Token(GenericTypeDescription typeDescription,
                     List<? extends AnnotationDescription> annotationDescriptions,
                     String name,
                     Integer modifiers) {
            this.typeDescription = typeDescription;
            this.annotationDescriptions = annotationDescriptions;
            this.name = name;
            this.modifiers = modifiers;
        }

        public GenericTypeDescription getType() {
            return typeDescription;
        }

        public AnnotationList getAnnotations() {
            return new AnnotationList.Explicit(annotationDescriptions);
        }

        public String getName() {
            return name;
        }

        public Integer getModifiers() {
            return modifiers;
        }

        @Override
        public Token accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
            return new Token(getType().accept(visitor),
                    annotationDescriptions, // Field access to avoid nesting of lists.
                    getName(),
                    getModifiers());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Token)) return false;
            Token token = (Token) other;
            return !(name != null ? !name.equals(token.name) : token.name != null);

        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "ParameterDescription.Token{" +
                    "typeDescription=" + typeDescription +
                    ", annotationDescriptions=" + annotationDescriptions +
                    ", name='" + name + '\'' +
                    ", modifiers=" + modifiers +
                    '}';
        }
    }
}
