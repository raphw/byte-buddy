package net.bytebuddy.description.method;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a list of parameters of a method or a constructor.
 *
 * @param <T> The type of parameter descriptions represented by this list.
 */
public interface ParameterList<T extends ParameterDescription> extends FilterableList<T, ParameterList<T>> {

    /**
     * Transforms this list of parameters into a list of the types of the represented parameters.
     *
     * @return A list of types representing the parameters of this list.
     */
    TypeList.Generic asTypeList();

    /**
     * Transforms the list of parameter descriptions into a list of detached tokens. All types that are matched by the provided
     * target type matcher are substituted by {@link net.bytebuddy.dynamic.TargetType}.
     *
     * @param matcher A matcher that indicates type substitution.
     * @return The transformed token list.
     */
    ByteCodeElement.Token.TokenList<ParameterDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher);

    /**
     * Returns this list of these parameter descriptions resolved to their defined shape.
     *
     * @return A list of parameters in their defined shape.
     */
    ParameterList<ParameterDescription.InDefinedShape> asDefined();

    /**
     * Checks if all parameters in this list define both an explicit name and an explicit modifier.
     *
     * @return {@code true} if all parameters in this list define both an explicit name and an explicit modifier.
     */
    boolean hasExplicitMetaData();

    /**
     * An base implementation for a {@link ParameterList}.
     *
     * @param <S> The type of parameter descriptions represented by this list.
     */
    abstract class AbstractBase<S extends ParameterDescription> extends FilterableList.AbstractBase<S, ParameterList<S>> implements ParameterList<S> {

        @Override
        public boolean hasExplicitMetaData() {
            for (ParameterDescription parameterDescription : this) {
                if (!parameterDescription.isNamed() || !parameterDescription.hasModifiers()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ByteCodeElement.Token.TokenList<ParameterDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher) {
            List<ParameterDescription.Token> tokens = new ArrayList<ParameterDescription.Token>(size());
            for (ParameterDescription parameterDescription : this) {
                tokens.add(parameterDescription.asToken(matcher));
            }
            return new ByteCodeElement.Token.TokenList<ParameterDescription.Token>(tokens);
        }

        @Override
        public TypeList.Generic asTypeList() {
            List<TypeDescription.Generic> types = new ArrayList<TypeDescription.Generic>(size());
            for (ParameterDescription parameterDescription : this) {
                types.add(parameterDescription.getType());
            }
            return new TypeList.Generic.Explicit(types);
        }

        @Override
        public ParameterList<ParameterDescription.InDefinedShape> asDefined() {
            List<ParameterDescription.InDefinedShape> declaredForms = new ArrayList<ParameterDescription.InDefinedShape>(size());
            for (ParameterDescription parameterDescription : this) {
                declaredForms.add(parameterDescription.asDefined());
            }
            return new Explicit<ParameterDescription.InDefinedShape>(declaredForms);
        }

        @Override
        protected ParameterList<S> wrap(List<S> values) {
            return new Explicit<S>(values);
        }
    }

    /**
     * Represents a list of parameters for an executable, i.e. a {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor}.
     *
     * @param <T> The type of the {@code java.lang.reflect.Executable} that this list represents.
     */
    abstract class ForLoadedExecutable<T> extends AbstractBase<ParameterDescription.InDefinedShape> {

        /**
         * The dispatcher used creating parameter list instances and for accessing {@code java.lang.reflect.Executable} instances.
         */
        private static final Dispatcher DISPATCHER;

        /*
         * Creates a dispatcher for a loaded parameter if the type is available for the running JVM.
         */
        static {
            Dispatcher dispatcher;
            try {
                dispatcher = new Dispatcher.ForJava8CapableVm(Class.forName("java.lang.reflect.Executable").getDeclaredMethod("getParameterCount"));
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception ignored) {
                dispatcher = Dispatcher.ForLegacyVm.INSTANCE;
            }
            DISPATCHER = dispatcher;
        }

        /**
         * The executable for which a parameter list is represented.
         */
        protected final T executable;

        /**
         * Creates a new description for a loaded executable.
         *
         * @param executable The executable for which a parameter list is represented.
         */
        protected ForLoadedExecutable(T executable) {
            this.executable = executable;
        }

        /**
         * Creates a new list that describes the parameters of the given {@link Constructor}.
         *
         * @param constructor The constructor for which the parameters should be described.
         * @return A list describing the constructor's parameters.
         */
        public static ParameterList<ParameterDescription.InDefinedShape> of(Constructor<?> constructor) {
            return DISPATCHER.describe(constructor);
        }

        /**
         * Creates a new list that describes the parameters of the given {@link Method}.
         *
         * @param method The method for which the parameters should be described.
         * @return A list describing the method's parameters.
         */
        public static ParameterList<ParameterDescription.InDefinedShape> of(Method method) {
            return DISPATCHER.describe(method);
        }

        @Override
        public int size() {
            return DISPATCHER.getParameterCount(executable);
        }

        /**
         * A dispatcher for creating descriptions of parameter lists and for evaluating the size of an {@code java.lang.reflect.Executable}'s parameters.
         */
        protected interface Dispatcher {

            /**
             * Returns the amount of parameters of a given executable..
             *
             * @param executable The executable for which the amount of parameters should be found.
             * @return The amount of parameters of the given executable.
             */
            int getParameterCount(Object executable);

            /**
             * Describes a {@link Constructor}'s parameters of the given VM.
             *
             * @param constructor The constructor for which the parameters should be described.
             * @return A list describing the constructor's parameters.
             */
            ParameterList<ParameterDescription.InDefinedShape> describe(Constructor<?> constructor);

            /**
             * Describes a {@link Method}'s parameters of the given VM.
             *
             * @param method The method for which the parameters should be described.
             * @return A list describing the method's parameters.
             */
            ParameterList<ParameterDescription.InDefinedShape> describe(Method method);

            /**
             * A dispatcher for a legacy VM that does not support the {@code java.lang.reflect.Parameter} type.
             */
            enum ForLegacyVm implements Dispatcher {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public int getParameterCount(Object executable) {
                    throw new IllegalStateException("Cannot dispatch method for java.lang.reflect.Executable");
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> describe(Constructor<?> constructor) {
                    return new OfLegacyVmConstructor(constructor);
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> describe(Method method) {
                    return new OfLegacyVmMethod(method);
                }

                @Override
                public String toString() {
                    return "ParameterList.ForLoadedExecutable.Dispatcher.ForLegacyVm." + name();
                }
            }

            /**
             * A dispatcher for a legacy VM that does support the {@code java.lang.reflect.Parameter} type.
             */
            class ForJava8CapableVm implements Dispatcher {

                /**
                 * The {@code java.lang.reflect.Executable#getParameterCount()} method.
                 */
                private final Method getParameterCount;

                /**
                 * Creates a new dispatcher for a modern VM.
                 *
                 * @param getParameterCount The {@code java.lang.reflect.Executable#getParameterCount()} method.
                 */
                protected ForJava8CapableVm(Method getParameterCount) {
                    this.getParameterCount = getParameterCount;
                }

                @Override
                public int getParameterCount(Object executable) {
                    try {
                        return (Integer) getParameterCount.invoke(executable);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.Parameter#getModifiers", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.Parameter#getModifiers", exception.getCause());
                    }
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> describe(Constructor<?> constructor) {
                    return new OfConstructor(constructor);
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> describe(Method method) {
                    return new OfMethod(method);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && getParameterCount.equals(((ForJava8CapableVm) other).getParameterCount);
                }

                @Override
                public int hashCode() {
                    return getParameterCount.hashCode();
                }

                @Override
                public String toString() {
                    return "ParameterList.ForLoadedExecutable.Dispatcher.ForJava8CapableVm{" +
                            "getParameterCount=" + getParameterCount +
                            '}';
                }
            }
        }

        /**
         * Describes the list of {@link Constructor} parameters on a modern VM.
         */
        protected static class OfConstructor extends ForLoadedExecutable<Constructor<?>> {

            /**
             * Creates a new description of the parameters of a constructor.
             *
             * @param constructor The constructor that is represented by this instance.
             */
            protected OfConstructor(Constructor<?> constructor) {
                super(constructor);
            }

            @Override
            public ParameterDescription.InDefinedShape get(int index) {
                return new ParameterDescription.ForLoadedParameter.OfConstructor(executable, index);
            }
        }

        /**
         * Describes the list of {@link Method} parameters on a modern VM.
         */
        protected static class OfMethod extends ForLoadedExecutable<Method> {

            /**
             * Creates a new description of the parameters of a method.
             *
             * @param method The method that is represented by this instance.
             */
            protected OfMethod(Method method) {
                super(method);
            }

            @Override
            public ParameterDescription.InDefinedShape get(int index) {
                return new ParameterDescription.ForLoadedParameter.OfMethod(executable, index);
            }
        }

        /**
         * Represents a list of constructor parameters on virtual machines where the {@code java.lang.reflect.Parameter}
         * type is not available.
         */
        protected static class OfLegacyVmConstructor extends ParameterList.AbstractBase<ParameterDescription.InDefinedShape> {

            /**
             * The represented constructor.
             */
            private final Constructor<?> constructor;

            /**
             * An array of this method's parameter types.
             */
            private final Class<?>[] parameterType;

            /**
             * An array of all parameter annotations of the represented method.
             */
            private final Annotation[][] parameterAnnotation;

            /**
             * Creates a legacy representation of a constructor's parameters.
             *
             * @param constructor The constructor to represent.
             */
            public OfLegacyVmConstructor(Constructor<?> constructor) {
                this.constructor = constructor;
                this.parameterType = constructor.getParameterTypes();
                this.parameterAnnotation = constructor.getParameterAnnotations();
            }

            @Override
            public ParameterDescription.InDefinedShape get(int index) {
                return new ParameterDescription.ForLoadedParameter.OfLegacyVmConstructor(constructor, index, parameterType, parameterAnnotation);
            }

            @Override
            public int size() {
                return parameterType.length;
            }
        }

        /**
         * Represents a list of method parameters on virtual machines where the {@code java.lang.reflect.Parameter}
         * type is not available.
         */
        protected static class OfLegacyVmMethod extends ParameterList.AbstractBase<ParameterDescription.InDefinedShape> {

            /**
             * The represented method.
             */
            private final Method method;

            /**
             * An array of this method's parameter types.
             */
            private final Class<?>[] parameterType;

            /**
             * An array of all parameter annotations of the represented method.
             */
            private final Annotation[][] parameterAnnotation;

            /**
             * Creates a legacy representation of a method's parameters.
             *
             * @param method The method to represent.
             */
            protected OfLegacyVmMethod(Method method) {
                this.method = method;
                this.parameterType = method.getParameterTypes();
                this.parameterAnnotation = method.getParameterAnnotations();
            }

            @Override
            public ParameterDescription.InDefinedShape get(int index) {
                return new ParameterDescription.ForLoadedParameter.OfLegacyVmMethod(method, index, parameterType, parameterAnnotation);
            }

            @Override
            public int size() {
                return parameterType.length;
            }
        }
    }

    /**
     * A list of explicitly provided parameter descriptions.
     *
     * @param <S> The type of parameter descriptions represented by this list.
     */
    class Explicit<S extends ParameterDescription> extends AbstractBase<S> {

        /**
         * The list of parameter descriptions that are represented by this list.
         */
        private final List<? extends S> parameterDescriptions;

        /**
         * Creates a new list of explicit parameter descriptions.
         *
         * @param parameterDescription The list of parameter descriptions that are represented by this list.
         */
        @SuppressWarnings("unchecked")
        public Explicit(S... parameterDescription) {
            this(Arrays.asList(parameterDescription));
        }

        /**
         * Creates a new list of explicit parameter descriptions.
         *
         * @param parameterDescriptions The list of parameter descriptions that are represented by this list.
         */
        public Explicit(List<? extends S> parameterDescriptions) {
            this.parameterDescriptions = parameterDescriptions;
        }

        @Override
        public S get(int index) {
            return parameterDescriptions.get(index);
        }

        @Override
        public int size() {
            return parameterDescriptions.size();
        }

        /**
         * A parameter list representing parameters without meta data or annotations.
         */
        public static class ForTypes extends ParameterList.AbstractBase<ParameterDescription.InDefinedShape> {

            /**
             * The method description that declares the parameters.
             */
            private final MethodDescription.InDefinedShape methodDescription;

            /**
             * A list of detached types representing the parameters.
             */
            private final List<? extends TypeDefinition> typeDefinitions;

            /**
             * Creates a new parameter type list.
             *
             * @param methodDescription The method description that declares the parameters.
             * @param typeDefinition   A list of detached types representing the parameters.
             */
            public ForTypes(MethodDescription.InDefinedShape methodDescription, TypeDefinition... typeDefinition) {
                this(methodDescription, Arrays.asList(typeDefinition));
            }

            /**
             * Creates a new parameter type list.
             *
             * @param methodDescription The method description that declares the parameters.
             * @param typeDefinitions   A list of detached types representing the parameters.
             */
            public ForTypes(MethodDescription.InDefinedShape methodDescription, List<? extends TypeDefinition> typeDefinitions) {
                this.methodDescription = methodDescription;
                this.typeDefinitions = typeDefinitions;
            }

            @Override
            public ParameterDescription.InDefinedShape get(int index) {
                int offset = methodDescription.isStatic() ? 0 : 1;
                for (TypeDefinition typeDefinition : typeDefinitions.subList(0, index)) {
                    offset += typeDefinition.getStackSize().getSize();
                }
                return new ParameterDescription.Latent(methodDescription, typeDefinitions.get(index).asGenericType(), index, offset);
            }

            @Override
            public int size() {
                return typeDefinitions.size();
            }
        }
    }

    /**
     * A list of parameter descriptions for a list of detached tokens. For the returned parameter, each token is attached to its parameter representation.
     */
    class ForTokens extends AbstractBase<ParameterDescription.InDefinedShape> {

        /**
         * The method that is declaring the represented token.
         */
        private final MethodDescription.InDefinedShape declaringMethod;

        /**
         * The list of tokens to represent.
         */
        private final List<? extends ParameterDescription.Token> tokens;

        /**
         * Creates a new parameter list for the provided tokens.
         *
         * @param declaringMethod The method that is declaring the represented token.
         * @param tokens          The list of tokens to represent.
         */
        public ForTokens(MethodDescription.InDefinedShape declaringMethod, List<? extends ParameterDescription.Token> tokens) {
            this.declaringMethod = declaringMethod;
            this.tokens = tokens;
        }

        @Override
        public ParameterDescription.InDefinedShape get(int index) {
            int offset = declaringMethod.isStatic() ? 0 : 1;
            for (ParameterDescription.Token token : tokens.subList(0, index)) {
                offset += token.getType().getStackSize().getSize();
            }
            return new ParameterDescription.Latent(declaringMethod, tokens.get(index), index, offset);
        }

        @Override
        public int size() {
            return tokens.size();
        }
    }

    /**
     * A list of parameter descriptions that yields {@link net.bytebuddy.description.method.ParameterDescription.TypeSubstituting}.
     */
    class TypeSubstituting extends AbstractBase<ParameterDescription.InGenericShape> {

        /**
         * The method that is declaring the transformed parameters.
         */
        private final MethodDescription.InGenericShape declaringMethod;

        /**
         * The untransformed parameters that are represented by this list.
         */
        private final List<? extends ParameterDescription> parameterDescriptions;

        /**
         * The visitor to apply to the parameter types before returning them.
         */
        private final TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

        /**
         * Creates a new type substituting parameter list.
         *
         * @param declaringMethod       The method that is declaring the transformed parameters.
         * @param parameterDescriptions The untransformed parameters that are represented by this list.
         * @param visitor               The visitor to apply to the parameter types before returning them.
         */
        public TypeSubstituting(MethodDescription.InGenericShape declaringMethod,
                                List<? extends ParameterDescription> parameterDescriptions,
                                TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
            this.declaringMethod = declaringMethod;
            this.parameterDescriptions = parameterDescriptions;
            this.visitor = visitor;
        }

        @Override
        public ParameterDescription.InGenericShape get(int index) {
            return new ParameterDescription.TypeSubstituting(declaringMethod, parameterDescriptions.get(index), visitor);
        }

        @Override
        public int size() {
            return parameterDescriptions.size();
        }
    }

    /**
     * An empty list of parameters.
     *
     * @param <S> The type of parameter descriptions represented by this list.
     */
    class Empty<S extends ParameterDescription> extends FilterableList.Empty<S, ParameterList<S>> implements ParameterList<S> {

        @Override
        public boolean hasExplicitMetaData() {
            return true;
        }

        @Override
        public TypeList.Generic asTypeList() {
            return new TypeList.Generic.Empty();
        }

        @Override
        public ByteCodeElement.Token.TokenList<ParameterDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher) {
            return new ByteCodeElement.Token.TokenList<ParameterDescription.Token>();
        }

        @Override
        @SuppressWarnings("unchecked")
        public ParameterList<ParameterDescription.InDefinedShape> asDefined() {
            return (ParameterList<ParameterDescription.InDefinedShape>) this;
        }
    }
}
