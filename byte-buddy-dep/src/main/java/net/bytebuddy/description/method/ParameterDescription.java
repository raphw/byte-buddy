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
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.none;

/**
 * Description of the parameter of a Java method or constructor.
 */
public interface ParameterDescription extends AnnotatedCodeElement,
        NamedElement.WithRuntimeName,
        ModifierReviewable,
        ByteCodeElement.TypeDependant<ParameterDescription.InDefinedShape, ParameterDescription.Token> {

    /**
     * The prefix for names of an unnamed parameter.
     */
    String NAME_PREFIX = "arg";

    /**
     * Returns the type of this parameter.
     *
     * @return The type of this parameter.
     */
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

    interface InDefinedShape extends ParameterDescription {

        @Override
        MethodDescription.InDefinedShape getDeclaringMethod();

        abstract class AbstractBase extends ParameterDescription.AbstractBase implements InDefinedShape {

            @Override
            public InDefinedShape asDefined() {
                return this;
            }
        }
    }

    /**
     * A base implementation of a method parameter description.
     */
    abstract class AbstractBase extends ModifierReviewable.AbstractBase implements ParameterDescription {

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
            return asToken(none());
        }

        @Override
        public Token asToken(ElementMatcher<? super GenericTypeDescription> targetTypeMatcher) {
            return new Token(getType().accept(new GenericTypeDescription.Visitor.Substitutor.ForDetachment(targetTypeMatcher)),
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
    class ForLoadedParameter extends InDefinedShape.AbstractBase {

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
        public MethodDescription.InDefinedShape getDeclaringMethod() {
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
        protected static class OfLegacyVmMethod extends InDefinedShape.AbstractBase {

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
                return new TypeDescription.LazyProjection.OfLoadedParameter.OfLegacyVmMethod(method, index, parameterType);
            }

            @Override
            public MethodDescription.InDefinedShape getDeclaringMethod() {
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
        protected static class OfLegacyVmConstructor extends InDefinedShape.AbstractBase {

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
                return new TypeDescription.LazyProjection.OfLoadedParameter.OfLegacyVmConstructor(constructor, index, parameterType);
            }

            @Override
            public MethodDescription.InDefinedShape getDeclaringMethod() {
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
    class Latent extends InDefinedShape.AbstractBase {

        /**
         * The method that is declaring the parameter.
         */
        private final MethodDescription.InDefinedShape declaringMethod;

        /**
         * The type of the parameter.
         */
        private final GenericTypeDescription parameterType;

        /**
         * The annotations of the parameter.
         */
        private final List<? extends AnnotationDescription> declaredAnnotations;

        /**
         * The name of the parameter or {@code null} if no name is explicitly defined.
         */
        private final String name;

        /**
         * The modifiers of the parameter or {@code null} if no modifiers are explicitly defined.
         */
        private final Integer modifiers;

        /**
         * The index of the parameter.
         */
        private final int index;

        /**
         * The parameter's offset in the local method variables array.
         */
        private final int offset;

        /**
         * Creates a latent parameter description. All provided types are attached to this instance before they are returned.
         *
         * @param declaringMethod The method that is declaring the parameter.
         * @param token           The token describing the shape of the parameter.
         * @param index           The index of the parameter.
         * @param offset          The parameter's offset in the local method variables array.
         */
        public Latent(MethodDescription.InDefinedShape declaringMethod, Token token, int index, int offset) {
            this(declaringMethod,
                    token.getType(),
                    token.getAnnotations(),
                    token.getName(),
                    token.getModifiers(),
                    index,
                    offset);
        }

        /**
         * Creates a new latent parameter descriptions for a parameter without explicit meta data or annotations.
         *
         * @param declaringMethod The method declaring this parameter.
         * @param parameterType   The type of the parameter.
         * @param index           The index of the parameter.
         * @param offset          The offset of the parameter.
         */
        public Latent(MethodDescription.InDefinedShape declaringMethod,
                      GenericTypeDescription parameterType,
                      int index,
                      int offset) {
            this(declaringMethod,
                    parameterType,
                    Collections.<AnnotationDescription>emptyList(),
                    Token.NO_NAME,
                    Token.NO_MODIFIERS,
                    index,
                    offset);
        }

        /**
         * Creates a latent parameter description. All provided types are attached to this instance before they are returned.
         *
         * @param declaringMethod     The method that is declaring the parameter.
         * @param parameterType       The parameter's type.
         * @param declaredAnnotations The annotations of the parameter.
         * @param name                The name of the parameter or {@code null} if no name is explicitly defined.
         * @param modifiers           The modifiers of the parameter or {@code null} if no modifiers are explicitly defined.
         * @param index               The index of the parameter.
         * @param offset              The parameter's offset in the local method variables array.
         */
        public Latent(MethodDescription.InDefinedShape declaringMethod,
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
            return parameterType.accept(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(this));
        }

        @Override
        public MethodDescription.InDefinedShape getDeclaringMethod() {
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

    /**
     * <p>
     * A parameter description that represents a given parameter but with a substituted parameter type.
     * </p>
     * <p>
     * <b>Note</b>: The supplied visitor must assure to not substitute
     * </p>
     */
    class TypeSubstituting extends AbstractBase {

        /**
         * The method that declares this type-substituted parameter.
         */
        private final MethodDescription declaringMethod;

        /**
         * The represented parameter.
         */
        private final ParameterDescription parameterDescription;

        /**
         * A visitor that is applied to the parameter type.
         */
        private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor;

        /**
         * Creates a new type substituting parameter.
         *
         * @param declaringMethod      The method that declares this type-substituted parameter.
         * @param parameterDescription The represented parameter.
         * @param visitor              A visitor that is applied to the parameter type.
         */
        public TypeSubstituting(MethodDescription declaringMethod,
                                ParameterDescription parameterDescription,
                                GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
            this.declaringMethod = declaringMethod;
            this.parameterDescription = parameterDescription;
            this.visitor = visitor;
        }

        @Override
        public GenericTypeDescription getType() {
            return parameterDescription.getType().accept(visitor);
        }

        @Override
        public MethodDescription getDeclaringMethod() {
            return declaringMethod;
        }

        @Override
        public int getIndex() {
            return parameterDescription.getIndex();
        }

        @Override
        public boolean isNamed() {
            return parameterDescription.isNamed();
        }

        @Override
        public boolean hasModifiers() {
            return parameterDescription.hasModifiers();
        }

        @Override
        public int getOffset() {
            return parameterDescription.getOffset();
        }

        @Override
        public String getName() {
            return parameterDescription.getName();
        }

        @Override
        public int getModifiers() {
            return parameterDescription.getModifiers();
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return parameterDescription.getDeclaredAnnotations();
        }

        @Override
        public InDefinedShape asDefined() {
            return parameterDescription.asDefined();
        }
    }

    /**
     * A token that describes the shape of a method parameter. A parameter token is equal to another parameter token if
     * their explicit names are explicitly defined and equal or if the token is of the same identity.
     */
    class Token implements ByteCodeElement.Token<Token> {

        /**
         * Indicator for a method parameter without an explicit name.
         */
        public static final String NO_NAME = null;

        /**
         * Indicator for a method parameter without explicit modifiers.
         */
        public static final Integer NO_MODIFIERS = null;

        /**
         * The type of the represented parameter.
         */
        private final GenericTypeDescription typeDescription;

        /**
         * A list of parameter annotations.
         */
        private final List<? extends AnnotationDescription> annotationDescriptions;

        /**
         * The name of the parameter or {@code null} if no explicit name is defined.
         */
        private final String name;

        /**
         * The modifiers of the parameter or {@code null} if no explicit modifiers is defined.
         */
        private final Integer modifiers;

        /**
         * Creates a new parameter token without an explicit name, an explicit modifier or annotations.
         *
         * @param typeDescription The type of the represented parameter.
         */
        public Token(GenericTypeDescription typeDescription) {
            this(typeDescription, Collections.<AnnotationDescription>emptyList());
        }

        /**
         * Creates a new parameter token without an explicit name or an explicit modifier.
         *
         * @param typeDescription        The type of the represented parameter.
         * @param annotationDescriptions The annotations of the parameter.
         */
        public Token(GenericTypeDescription typeDescription, List<? extends AnnotationDescription> annotationDescriptions) {
            this(typeDescription, annotationDescriptions, NO_NAME, NO_MODIFIERS);
        }

        /**
         * Creates a new parameter token.
         *
         * @param typeDescription        The type of the represented parameter.
         * @param annotationDescriptions The annotations of the parameter.
         * @param name                   The name of the parameter or {@code null} if no explicit name is defined.
         * @param modifiers              The modifiers of the parameter or {@code null} if no explicit modifiers is defined.
         */
        public Token(GenericTypeDescription typeDescription,
                     List<? extends AnnotationDescription> annotationDescriptions,
                     String name,
                     Integer modifiers) {
            this.typeDescription = typeDescription;
            this.annotationDescriptions = annotationDescriptions;
            this.name = name;
            this.modifiers = modifiers;
        }

        /**
         * Returns the type of the represented method parameter.
         *
         * @return The type of the represented method parameter.
         */
        public GenericTypeDescription getType() {
            return typeDescription;
        }

        /**
         * Returns the annotations of the represented method parameter.
         *
         * @return The annotations of the represented method parameter.
         */
        public AnnotationList getAnnotations() {
            return new AnnotationList.Explicit(annotationDescriptions);
        }

        /**
         * Returns the name of the represented method parameter.
         *
         * @return The name of the parameter or {@code null} if no explicit name is defined.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the modifiers of the represented method parameter.
         *
         * @return The modifiers of the parameter or {@code null} if no explicit modifiers is defined.
         */
        public Integer getModifiers() {
            return modifiers;
        }

        @Override
        public Token accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
            return new Token(getType().accept(visitor),
                    getAnnotations(),
                    getName(),
                    getModifiers());
        }

        @Override
        public boolean isIdenticalTo(Token token) {
            return getType().equals(token.getType())
                    && getAnnotations().equals(token.getAnnotations())
                    && ((getName() == null && token.getName() == null)
                    || (getName() != null && token.getName() != null && (getName().equals(token.getName()))))
                    && ((getModifiers() == null && token.getModifiers() == null)
                    || (getModifiers() != null && token.getModifiers() != null && (getModifiers().equals(token.getModifiers()))));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Token)) return false;
            String name = getName();
            return name != null && name.equals(((Token) other).getName());

        }

        @Override
        public int hashCode() {
            return name != null
                    ? name.hashCode()
                    : super.hashCode();
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

        /**
         * A list of types represented as a list of parameter tokens.
         */
        public static class TypeList extends AbstractList<Token> {

            /**
             * The list of types to represent as parameter tokens.
             */
            private final List<? extends GenericTypeDescription> typeDescriptions;

            /**
             * Creates a new list of types that represent parameters.
             *
             * @param typeDescriptions The types to represent.
             */
            public TypeList(List<? extends GenericTypeDescription> typeDescriptions) {
                this.typeDescriptions = typeDescriptions;
            }

            @Override
            public Token get(int index) {
                return new Token(typeDescriptions.get(index));
            }

            @Override
            public int size() {
                return typeDescriptions.size();
            }
        }
    }
}
