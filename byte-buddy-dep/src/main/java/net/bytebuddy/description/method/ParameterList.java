package net.bytebuddy.description.method;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.implementation.bytecode.StackSize;
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

    GenericTypeList asTypeList();

    /**
     * Checks if all parameters in this list define both an explicit name and an explicit modifier.
     *
     * @return {@code true} if all parameters in this list define both an explicit name and an explicit modifier.
     */
    boolean hasExplicitMetaData();

    /**
     * An base implementation for a {@link ParameterList}.
     */
    abstract class AbstractBase extends FilterableList.AbstractBase<ParameterDescription, ParameterList> implements ParameterList {

        @Override
        public boolean hasExplicitMetaData() {
            for (ParameterDescription parameterDescription : this) {
                if (!parameterDescription.isNamed() || !parameterDescription.hasModifiers()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Represents a list of parameters for an executable, i.e. a {@link java.lang.reflect.Method} or
     * {@link java.lang.reflect.Constructor}.
     */
    class ForLoadedExecutable extends AbstractBase {

        /**
         * Represents the {@code java.lang.reflect.Executable}'s {@code getParameters} method.
         */
        private static final JavaMethod GET_PARAMETERS;

        /*
         * Initializes the {@link net.bytebuddy.utility.JavaMethod} instances of this class dependant on
         * whether they are available.
         */
        static {
            JavaMethod getParameters;
            try {
                Class<?> executableType = Class.forName("java.lang.reflect.Executable");
                getParameters = new JavaMethod.ForLoadedMethod(executableType.getDeclaredMethod("getParameters"));
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
        public GenericTypeList asTypeList() {
            List<GenericTypeDescription> types = new ArrayList<GenericTypeDescription>(parameter.length);
            for (Object aParameter : parameter) {
                types.add(new GenericTypeDescription.LazyProjection.OfLoadedParameter(aParameter));
            }
            return new GenericTypeList.Explicit(types);
        }

        @Override
        protected ParameterList wrap(List<ParameterDescription> values) {
            return new Explicit(values);
        }

        /**
         * Represents a list of method parameters on virtual machines where the {@code java.lang.reflect.Parameter}
         * type is not available.
         */
        protected static class OfLegacyVmMethod extends ParameterList.AbstractBase {

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
            public GenericTypeList asTypeList() {
                List<GenericTypeDescription> types = new ArrayList<GenericTypeDescription>(parameterType.length);
                for (int index = 0; index < parameterType.length; index++) {
                    types.add(new GenericTypeDescription.LazyProjection.OfLegacyVmMethodParameter(method, index, parameterType[index]));
                }
                return new GenericTypeList.Explicit(types);
            }
        }

        /**
         * Represents a list of constructor parameters on virtual machines where the {@code java.lang.reflect.Parameter}
         * type is not available.
         */
        protected static class OfLegacyVmConstructor extends ParameterList.AbstractBase {

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
            public GenericTypeList asTypeList() {
                List<GenericTypeDescription> types = new ArrayList<GenericTypeDescription>(parameterType.length);
                for (int index = 0; index < parameterType.length; index++) {
                    types.add(new GenericTypeDescription.LazyProjection.OfLegacyVmConstructorParameter(constructor, index, parameterType[index]));
                }
                return new GenericTypeList.Explicit(types);
            }
        }
    }

    /**
     * A list of explicitly provided parameter descriptions.
     */
    class Explicit extends AbstractBase {

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
        public static ParameterList latent(MethodDescription declaringMethod, List<? extends GenericTypeDescription> parameterTypes) {
            List<ParameterDescription> parameterDescriptions = new ArrayList<ParameterDescription>(parameterTypes.size());
            int index = 0, offset = declaringMethod.isStatic()
                    ? StackSize.ZERO.getSize()
                    : StackSize.SINGLE.getSize();
            for (GenericTypeDescription parameterType : parameterTypes) {
                parameterDescriptions.add(new ParameterDescription.Latent(declaringMethod,
                        parameterType,
                        Collections.<AnnotationDescription>emptyList(),
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
        public GenericTypeList asTypeList() {
            List<GenericTypeDescription> types = new ArrayList<GenericTypeDescription>(parameterDescriptions.size());
            for (ParameterDescription parameterDescription : parameterDescriptions) {
                types.add(parameterDescription.getType());
            }
            return new GenericTypeList.Explicit(types);
        }

        @Override
        protected ParameterList wrap(List<ParameterDescription> values) {
            return new Explicit(values);
        }
    }

    /**
     * An empty list of parameters.
     */
    class Empty extends FilterableList.Empty<ParameterDescription, ParameterList> implements ParameterList {

        @Override
        public boolean hasExplicitMetaData() {
            return true;
        }

        @Override
        public GenericTypeList asTypeList() {
            return new GenericTypeList.Empty();
        }
    }
}
