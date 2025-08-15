/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.description.method;

import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
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

        /**
         * {@inheritDoc}
         */
        public boolean hasExplicitMetaData() {
            // cache the size and make sure to avoid iterators here
            // this pattern reduces the number of allocations and also the CPU usage
            int size = size();
            for (int i = 0; i < size; i++) {
                ParameterDescription parameterDescription = get(i);
                if (!parameterDescription.isNamed() || !parameterDescription.hasModifiers()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeElement.Token.TokenList<ParameterDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher) {
            // cache the size and make sure to avoid iterators here
            // this pattern reduces the number of allocations and also the CPU usage
            int size = size();
            List<ParameterDescription.Token> tokens = new ArrayList<ParameterDescription.Token>(size);
            for (int i = 0; i < size; i++) {
                tokens.add(get(i).asToken(matcher));
            }
            return new ByteCodeElement.Token.TokenList<ParameterDescription.Token>(tokens);
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic asTypeList() {
            // cache the size and make sure to avoid iterators here
            // this pattern reduces the number of allocations and also the CPU usage
            int size = size();
            List<TypeDescription.Generic> types = new ArrayList<TypeDescription.Generic>(size);
            for (int i = 0; i < size; i++) {
                types.add(get(i).getType());
            }
            return new TypeList.Generic.Explicit(types);
        }

        /**
         * {@inheritDoc}
         */
        public ParameterList<ParameterDescription.InDefinedShape> asDefined() {
            // cache the size and make sure to avoid iterators here
            // this pattern reduces the number of allocations and also the CPU usage
            int size = size();
            List<ParameterDescription.InDefinedShape> declaredForms = new ArrayList<ParameterDescription.InDefinedShape>(size);
            for (int i = 0; i < size; i++) {
                declaredForms.add(get(i).asDefined());
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
        protected static final Executable EXECUTABLE = doPrivileged(JavaDispatcher.of(Executable.class));

        /**
         * The executable for which a parameter list is represented.
         */
        protected final T executable;

        /**
         * The number of parameters of this executable.
         * <p>
         * It is important to cache it as calling getParameterCount() via the dispatcher has a high cost.
         */
        protected final int size;

        /**
         * The parameter annotation source to query.
         */
        protected final ParameterDescription.ForLoadedParameter.ParameterAnnotationSource parameterAnnotationSource;

        /**
         * Creates a new description for a loaded executable.
         *
         * @param executable                The executable for which a parameter list is represented.
         * @param parameterAnnotationSource The parameter annotation source to query.
         */
        protected ForLoadedExecutable(T executable, ParameterDescription.ForLoadedParameter.ParameterAnnotationSource parameterAnnotationSource) {
            this.executable = executable;
            this.size = EXECUTABLE.getParameterCount(executable);
            this.parameterAnnotationSource = parameterAnnotationSource;
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
        }

        /**
         * Creates a new list that describes the parameters of the given {@link Constructor}.
         *
         * @param constructor The constructor for which the parameters should be described.
         * @return A list describing the constructor's parameters.
         */
        public static ParameterList<ParameterDescription.InDefinedShape> of(Constructor<?> constructor) {
            return of(constructor, new ParameterDescription.ForLoadedParameter.ParameterAnnotationSource.ForLoadedConstructor(constructor));
        }

        /**
         * Creates a new list that describes the parameters of the given {@link Constructor}.
         *
         * @param constructor               The constructor for which the parameters should be described.
         * @param parameterAnnotationSource The parameter annotation source to query.
         * @return A list describing the constructor's parameters.
         */
        public static ParameterList<ParameterDescription.InDefinedShape> of(Constructor<?> constructor,
                                                                            ParameterDescription.ForLoadedParameter.ParameterAnnotationSource parameterAnnotationSource) {
            return EXECUTABLE.isInstance(constructor)
                    ? new OfConstructor(constructor, parameterAnnotationSource)
                    : new OfLegacyVmConstructor(constructor, parameterAnnotationSource);
        }

        /**
         * Creates a new list that describes the parameters of the given {@link Method}.
         *
         * @param method The method for which the parameters should be described.
         * @return A list describing the method's parameters.
         */
        public static ParameterList<ParameterDescription.InDefinedShape> of(Method method) {
            return of(method, new ParameterDescription.ForLoadedParameter.ParameterAnnotationSource.ForLoadedMethod(method));
        }

        /**
         * Creates a new list that describes the parameters of the given {@link Method}.
         *
         * @param method                    The method for which the parameters should be described.
         * @param parameterAnnotationSource The parameter annotation source to query.
         * @return A list describing the method's parameters.
         */
        public static ParameterList<ParameterDescription.InDefinedShape> of(Method method,
                                                                            ParameterDescription.ForLoadedParameter.ParameterAnnotationSource parameterAnnotationSource) {
            return EXECUTABLE.isInstance(method)
                    ? new OfMethod(method, parameterAnnotationSource)
                    : new OfLegacyVmMethod(method, parameterAnnotationSource);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return size;
        }

        /**
         * A dispatcher for creating descriptions of parameter lists and for evaluating the size of an {@code java.lang.reflect.Executable}'s parameters.
         */
        @JavaDispatcher.Proxied("java.lang.reflect.Executable")
        protected interface Executable {

            /**
             * Checks if the supplied instance is a {@code java.lang.reflect.Executable}.
             *
             * @param value The value to check for being a {@code java.lang.reflect.Executable}.
             * @return {@code true} if the supplied instance is a {@code java.lang.reflect.Executable}.
             */
            @JavaDispatcher.Instance
            boolean isInstance(Object value);

            /**
             * Returns the amount of parameters of a given executable..
             *
             * @param executable The executable for which the amount of parameters should be found.
             * @return The amount of parameters of the given executable.
             */
            int getParameterCount(Object executable);

            /**
             * Returns the parameters of an executable.
             *
             * @param value The executable to introspect.
             * @return An array of the parameters of the supplied executable.
             */
            Object[] getParameters(Object value);
        }

        /**
         * Describes the list of {@link Constructor} parameters on a modern VM.
         */
        protected static class OfConstructor extends ForLoadedExecutable<Constructor<?>> {

            /**
             * Creates a new description of the parameters of a constructor.
             *
             * @param constructor               The constructor that is represented by this instance.
             * @param parameterAnnotationSource The parameter annotation source to query.
             */
            protected OfConstructor(Constructor<?> constructor, ParameterDescription.ForLoadedParameter.ParameterAnnotationSource parameterAnnotationSource) {
                super(constructor, parameterAnnotationSource);
            }

            /**
             * {@inheritDoc}
             */
            public ParameterDescription.InDefinedShape get(int index) {
                return new ParameterDescription.ForLoadedParameter.OfConstructor(executable, index, parameterAnnotationSource);
            }
        }

        /**
         * Describes the list of {@link Method} parameters on a modern VM.
         */
        protected static class OfMethod extends ForLoadedExecutable<Method> {

            /**
             * Creates a new description of the parameters of a method.
             *
             * @param method                    The method that is represented by this instance.
             * @param parameterAnnotationSource The parameter annotation source to query.
             */
            protected OfMethod(Method method, ParameterDescription.ForLoadedParameter.ParameterAnnotationSource parameterAnnotationSource) {
                super(method, parameterAnnotationSource);
            }

            /**
             * {@inheritDoc}
             */
            public ParameterDescription.InDefinedShape get(int index) {
                return new ParameterDescription.ForLoadedParameter.OfMethod(executable, index, parameterAnnotationSource);
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
             * The parameter annotation source to query.
             */
            private final ParameterDescription.ForLoadedParameter.ParameterAnnotationSource parameterAnnotationSource;

            /**
             * Creates a legacy representation of a constructor's parameters.
             *
             * @param constructor               The constructor to represent.
             * @param parameterAnnotationSource The parameter annotation source to query.
             */
            protected OfLegacyVmConstructor(Constructor<?> constructor, ParameterDescription.ForLoadedParameter.ParameterAnnotationSource parameterAnnotationSource) {
                this.constructor = constructor;
                this.parameterType = constructor.getParameterTypes();
                this.parameterAnnotationSource = parameterAnnotationSource;
            }

            /**
             * {@inheritDoc}
             */
            public ParameterDescription.InDefinedShape get(int index) {
                return new ParameterDescription.ForLoadedParameter.OfLegacyVmConstructor(constructor, index, parameterType, parameterAnnotationSource);
            }

            /**
             * {@inheritDoc}
             */
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
             * The parameter annotation source to query.
             */
            private final ParameterDescription.ForLoadedParameter.ParameterAnnotationSource parameterAnnotationSource;

            /**
             * Creates a legacy representation of a method's parameters.
             *
             * @param method                    The method to represent.
             * @param parameterAnnotationSource The parameter annotation source to query.
             */
            protected OfLegacyVmMethod(Method method, ParameterDescription.ForLoadedParameter.ParameterAnnotationSource parameterAnnotationSource) {
                this.method = method;
                this.parameterType = method.getParameterTypes();
                this.parameterAnnotationSource = parameterAnnotationSource;
            }

            /**
             * {@inheritDoc}
             */
            public ParameterDescription.InDefinedShape get(int index) {
                return new ParameterDescription.ForLoadedParameter.OfLegacyVmMethod(method, index, parameterType, parameterAnnotationSource);
            }

            /**
             * {@inheritDoc}
             */
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

        /**
         * {@inheritDoc}
         */
        public S get(int index) {
            return parameterDescriptions.get(index);
        }

        /**
         * {@inheritDoc}
         */
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
             * @param typeDefinition    A list of detached types representing the parameters.
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

            /**
             * {@inheritDoc}
             */
            public ParameterDescription.InDefinedShape get(int index) {
                int offset = methodDescription.isStatic() ? 0 : 1;
                for (int previous = 0; previous < index; previous++) {
                    offset += typeDefinitions.get(previous).getStackSize().getSize();
                }
                return new ParameterDescription.Latent(methodDescription, typeDefinitions.get(index).asGenericType(), index, offset);
            }

            /**
             * {@inheritDoc}
             */
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

        /**
         * {@inheritDoc}
         */
        public ParameterDescription.InDefinedShape get(int index) {
            int offset = declaringMethod.isStatic() ? 0 : 1;
            for (ParameterDescription.Token token : tokens.subList(0, index)) {
                offset += token.getType().getStackSize().getSize();
            }
            return new ParameterDescription.Latent(declaringMethod, tokens.get(index), index, offset);
        }

        /**
         * {@inheritDoc}
         */
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
         * The number of parameters.
         */
        private final int size;

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
            this.size = parameterDescriptions.size();
            this.visitor = visitor;
        }

        /**
         * {@inheritDoc}
         */
        public ParameterDescription.InGenericShape get(int index) {
            return new ParameterDescription.TypeSubstituting(declaringMethod, parameterDescriptions.get(index), visitor);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return size;
        }
    }

    /**
     * An empty list of parameters.
     *
     * @param <S> The type of parameter descriptions represented by this list.
     */
    class Empty<S extends ParameterDescription> extends FilterableList.Empty<S, ParameterList<S>> implements ParameterList<S> {

        /**
         * {@inheritDoc}
         */
        public boolean hasExplicitMetaData() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic asTypeList() {
            return new TypeList.Generic.Empty();
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeElement.Token.TokenList<ParameterDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher) {
            return new ByteCodeElement.Token.TokenList<ParameterDescription.Token>();
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public ParameterList<ParameterDescription.InDefinedShape> asDefined() {
            return (ParameterList<ParameterDescription.InDefinedShape>) this;
        }
    }
}
