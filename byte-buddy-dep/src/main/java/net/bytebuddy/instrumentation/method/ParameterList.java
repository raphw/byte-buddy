package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.matcher.FilterableList;
import net.bytebuddy.utility.JavaMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a list of parameters of a method or a constructor.
 */
public interface ParameterList extends FilterableList<ParameterDescription, ParameterList> {

    /**
     * Transforms the list of parameters into a list of type descriptions.
     *
     * @return A list of type descriptions.
     */
    TypeList asTypeList();

    /**
     * Represents a list of parameters for an executable, i.e. a {@link java.lang.reflect.Method} or
     * {@link java.lang.reflect.Constructor}.
     */
    static class ForLoadedExecutable extends AbstractBase<ParameterDescription, ParameterList> implements ParameterList {

        /**
         * Represents the {@code java.lang.reflect.Executable}'s {@code getParameters} method.
         */
        private static final JavaMethod GET_PARAMETERS;

        /**
         * Initializes the {@link net.bytebuddy.utility.JavaMethod} instances of this class dependant on
         * whether they are available.
         */
        static {
            JavaMethod getParameters, getDeclaringExecutable;
            try {
                Class<?> executableType = Class.forName("java.lang.reflect.Executable");
                getParameters = new JavaMethod.ForLoadedMethod(executableType.getDeclaredMethod("getParameters"));
                Class<?> parameterType = Class.forName("java.lang.reflect.Parameter");
            } catch (Exception ignored) {
                getParameters = JavaMethod.ForUnavailableMethod.INSTANCE;
            }
            GET_PARAMETERS = getParameters;
        }

        /**
         * An array of the represented {@code java.lang.reflect.Parameter} instances.
         */
        private final Object[] parameter;

        /**
         * Creates a list representing a method's or a constructor's parameters.
         *
         * @param parameter The {@code java.lang.reflect.Parameter}-typed parameters to represent.
         */
        protected ForLoadedExecutable(Object[] parameter) {
            this.parameter = parameter;
        }

        /**
         * Creates a parameter list for a loaded method.
         *
         * @param method The method to represent.
         * @return A list of parameters for this method.
         */
        public static ParameterList of(Method method) {
            return GET_PARAMETERS.isInvokable()
                    ? new ForLoadedExecutable((Object[]) GET_PARAMETERS.invoke(method))
                    : new OfLegacyVmMethod(method);
        }

        /**
         * Creates a parameter list for a loaded constructor.
         *
         * @param constructor The constructor to represent.
         * @return A list of parameters for this constructor.
         */
        public static ParameterList of(Constructor<?> constructor) {
            return GET_PARAMETERS.isInvokable()
                    ? new ForLoadedExecutable((Object[]) GET_PARAMETERS.invoke(constructor))
                    : new OfLegacyVmConstructor(constructor);
        }

        @Override
        public ParameterDescription get(int index) {
            return new ParameterDescription.ForLoadedParameter(parameter[index], index);
        }

        @Override
        public int size() {
            return parameter.length;
        }

        @Override
        public TypeList asTypeList() {
            List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(parameter.length);
            for (Object aParameter : parameter) {
                Class<?> type = (Class<?>) ParameterDescription.ForLoadedParameter.GET_TYPE.invoke(aParameter);
                typeDescriptions.add(new TypeDescription.ForLoadedType(type));
            }
            return new TypeList.Explicit(typeDescriptions);
        }

        @Override
        protected ParameterList wrap(List<ParameterDescription> values) {
            return new Explicit(values);
        }

        /**
         * Represents a list of method parameters on virtual machines where the {@code java.lang.reflect.Parameter}
         * type is not available.
         */
        protected static class OfLegacyVmMethod extends FilterableList.AbstractBase<ParameterDescription, ParameterList> implements ParameterList {

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
            protected ParameterList wrap(List<ParameterDescription> values) {
                return new Explicit(values);
            }

            @Override
            public ParameterDescription get(int index) {
                return new ParameterDescription.ForLoadedParameter.OfLegacyVmMethod(method, index, parameterType[index], parameterAnnotation[index]);
            }

            @Override
            public int size() {
                return parameterType.length;
            }

            @Override
            public TypeList asTypeList() {
                return new TypeList.ForLoadedType(parameterType);
            }
        }

        /**
         * Represents a list of constructor parameters on virtual machines where the {@code java.lang.reflect.Parameter}
         * type is not available.
         */
        protected static class OfLegacyVmConstructor extends FilterableList.AbstractBase<ParameterDescription, ParameterList> implements ParameterList {

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
            protected ParameterList wrap(List<ParameterDescription> values) {
                return new Explicit(values);
            }

            @Override
            public ParameterDescription get(int index) {
                return new ParameterDescription.ForLoadedParameter.OfLegacyVmConstructor(constructor, index, parameterType[index], parameterAnnotation[index]);
            }

            @Override
            public int size() {
                return parameterType.length;
            }

            @Override
            public TypeList asTypeList() {
                return new TypeList.ForLoadedType(parameterType);
            }
        }
    }

    /**
     * A list of explicitly provided parameter descriptions.
     */
    static class Explicit extends AbstractBase<ParameterDescription, ParameterList> implements ParameterList {

        /**
         * The list of parameter descriptions that are represented by this list.
         */
        private final List<? extends ParameterDescription> parameterDescriptions;

        /**
         * Creates a new list of explicit parameter descriptions.
         *
         * @param parameterDescriptions The list of parameter descriptions that are represented by this list.
         */
        public Explicit(List<? extends ParameterDescription> parameterDescriptions) {
            this.parameterDescriptions = Collections.unmodifiableList(parameterDescriptions);
        }

        /**
         * Creates a list of method parameters from a list of type descriptions.
         *
         * @param declaringMethod The method for which this latent list should be created.
         * @param parameterTypes  A list of the parameter types.
         * @return A list describing these parameters.
         */
        public static ParameterList latent(MethodDescription declaringMethod, List<? extends TypeDescription> parameterTypes) {
            List<ParameterDescription> parameterDescriptions = new ArrayList<ParameterDescription>(parameterTypes.size());
            int index = 0, offset = declaringMethod.isStatic()
                    ? StackSize.ZERO.getSize()
                    : StackSize.SINGLE.getSize();
            for (TypeDescription parameterType : parameterTypes) {
                parameterDescriptions.add(new ParameterDescription.Latent(declaringMethod,
                        parameterType,
                        index++,
                        offset));
                offset += parameterType.getStackSize().getSize();
            }
            return new Explicit(parameterDescriptions);
        }

        @Override
        public ParameterDescription get(int index) {
            return parameterDescriptions.get(index);
        }

        @Override
        public int size() {
            return parameterDescriptions.size();
        }

        @Override
        public TypeList asTypeList() {
            List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(parameterDescriptions.size());
            for (ParameterDescription parameterDescription : parameterDescriptions) {
                typeDescriptions.add(parameterDescription.getTypeDescription());
            }
            return new TypeList.Explicit(typeDescriptions);
        }

        @Override
        protected ParameterList wrap(List<ParameterDescription> values) {
            return new Explicit(values);
        }
    }

    /**
     * An empty list of parameters.
     */
    static class Empty extends FilterableList.Empty<ParameterDescription, ParameterList> implements ParameterList {

        @Override
        public TypeList asTypeList() {
            return new TypeList.Empty();
        }
    }
}
