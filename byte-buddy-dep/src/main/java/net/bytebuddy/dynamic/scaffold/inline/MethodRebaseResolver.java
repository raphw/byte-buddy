package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.auxiliary.TrivialType;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * A method rebase resolver is responsible for mapping methods of an instrumented type to an alternative signature.
 * This way a method can exist in two versions within a class:
 * <ol>
 * <li>The rebased method which represents the original implementation as it is present in a class file.</li>
 * <li>An overriden method which implements user code which is still able to invoke the original, rebased method.</li>
 * </ol>
 */
public interface MethodRebaseResolver {

    /**
     * Checks if a method is eligible for rebasing and resolves this possibly rebased method.
     *
     * @param methodDescription A description of the method to resolve.
     * @return A resolution for the given method.
     */
    Resolution resolve(MethodDescription.InDefinedShape methodDescription);

    /**
     * Returns a (potentially empty) list of auxiliary types that are required by this method rebase resolver.
     *
     * @return A list of auxiliary types that are required by this method rebase resolver.
     */
    List<DynamicType> getAuxiliaryTypes();

    /**
     * A method rebase resolver that preserves any method in its original form.
     */
    enum Disabled implements MethodRebaseResolver {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolution resolve(MethodDescription.InDefinedShape methodDescription) {
            return new Resolution.Preserved(methodDescription);
        }

        @Override
        public List<DynamicType> getAuxiliaryTypes() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "MethodRebaseResolver.Disabled." + name();
        }

    }

    /**
     * A method name transformer provides a unique mapping of a method's name to an alternative name.
     *
     * @see MethodRebaseResolver
     */
    interface MethodNameTransformer {

        /**
         * Transforms a method's name to an alternative name. This name must not be equal to any existing method of the
         * created class.
         *
         * @param methodDescription The original method.
         * @return The alternative name.
         */
        String transform(MethodDescription methodDescription);

        /**
         * A method name transformer that adds a fixed suffix to an original method name, separated by a {@code $}.
         */
        class Suffixing implements MethodNameTransformer {

            /**
             * The default suffix to add to an original method name.
             */
            private static final String DEFAULT_SUFFIX = "original$";

            /**
             * The suffix to append to a method name.
             */
            private final String suffix;

            /**
             * Creates a new suffixing method name transformer which adds a default suffix with a random name component.
             *
             * @return A method name transformer that adds a randomized suffix to the original method name.
             */
            public static MethodNameTransformer withRandomSuffix() {
                return new Suffixing(DEFAULT_SUFFIX + RandomString.make());
            }

            /**
             * Creates a new suffixing method name transformer.
             *
             * @param suffix The suffix to add to the method name before the seed.
             */
            public Suffixing(String suffix) {
                this.suffix = suffix;
            }

            @Override
            public String transform(MethodDescription methodDescription) {
                return String.format("%s$%s", methodDescription.getInternalName(), suffix);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && suffix.equals(((Suffixing) other).suffix);
            }

            @Override
            public int hashCode() {
                return suffix.hashCode();
            }

            @Override
            public String toString() {
                return "MethodRebaseResolver.MethodNameTransformer.Suffixing{" +
                        "suffix='" + suffix + '\'' +
                        '}';
            }
        }

        /**
         * A method name transformer that adds a fixed prefix to an original method name.
         */
        class Prefixing implements MethodNameTransformer {

            /**
             * The default prefix to add to an original method name.
             */
            private static final String DEFAULT_PREFIX = "original";

            /**
             * The prefix that is appended.
             */
            private final String prefix;

            /**
             * Creates a new prefixing method name transformer using a default prefix.
             */
            public Prefixing() {
                this(DEFAULT_PREFIX);
            }

            /**
             * Creates a new prefixing method name transformer.
             *
             * @param prefix The prefix being used.
             */
            public Prefixing(String prefix) {
                this.prefix = prefix;
            }

            @Override
            public String transform(MethodDescription methodDescription) {
                return String.format("%s%s", prefix, methodDescription.getInternalName());
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && prefix.equals(((Prefixing) other).prefix);
            }

            @Override
            public int hashCode() {
                return prefix.hashCode();
            }

            @Override
            public String toString() {
                return "MethodRebaseResolver.MethodNameTransformer.Prefixing{" +
                        "prefix='" + prefix + '\'' +
                        '}';
            }
        }
    }

    /**
     * A resolution for a method that was checked by a {@link MethodRebaseResolver}.
     */
    interface Resolution {

        /**
         * Checks if this resolution represents a rebased method.
         *
         * @return {@code true} if this resolution requires to rebase a method.
         */
        boolean isRebased();

        /**
         * Returns the resolved method if this resolution represents a rebased method or the original method.
         *
         * @return The resolved method if this resolution represents a rebased method or the original method.
         */
        MethodDescription.InDefinedShape getResolvedMethod();

        /**
         * A rebased method might require additional arguments in order to create a distinct signature. The
         * stack manipulation that is returned from this method loads these arguments onto the operand stack. For
         * a non-rebased method, this method throws an {@link java.lang.IllegalArgumentException}.
         *
         * @return A stack manipulation that loaded the additional arguments onto the stack, if any.
         */
        StackManipulation getAdditionalArguments();

        /**
         * A {@link MethodRebaseResolver.Resolution} of a non-rebased method.
         */
        class Preserved implements Resolution {

            /**
             * The preserved method.
             */
            private final MethodDescription.InDefinedShape methodDescription;

            /**
             * Creates a new {@link MethodRebaseResolver.Resolution} for
             * a non-rebased method.
             *
             * @param methodDescription The preserved method.
             */
            public Preserved(MethodDescription.InDefinedShape methodDescription) {
                this.methodDescription = methodDescription;
            }

            @Override
            public boolean isRebased() {
                return false;
            }

            @Override
            public MethodDescription.InDefinedShape getResolvedMethod() {
                return methodDescription;
            }

            @Override
            public StackManipulation getAdditionalArguments() {
                throw new IllegalStateException("Cannot process additional arguments for non-rebased method: " + methodDescription);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && methodDescription.equals(((Preserved) other).methodDescription);
            }

            @Override
            public int hashCode() {
                return methodDescription.hashCode();
            }

            @Override
            public String toString() {
                return "MethodRebaseResolver.Resolution.Preserved{methodDescription=" + methodDescription + '}';
            }
        }

        /**
         * A {@link MethodRebaseResolver.Resolution} of a rebased method.
         */
        class ForRebasedMethod implements Resolution {

            /**
             * The rebased method.
             */
            private final MethodDescription.InDefinedShape methodDescription;

            /**
             * Creates a resolution for a rebased method.
             *
             * @param methodDescription The rebased method.
             */
            protected ForRebasedMethod(MethodDescription.InDefinedShape methodDescription) {
                this.methodDescription = methodDescription;
            }

            /**
             * Resolves a rebasement for the provided method.
             *
             * @param methodDescription     The method to be rebased.
             * @param methodNameTransformer The transformer to use for renaming the method.
             * @return A resolution for rebasing the provided method.
             */
            public static Resolution of(MethodDescription.InDefinedShape methodDescription, MethodNameTransformer methodNameTransformer) {
                return new ForRebasedMethod(new RebasedMethod(methodDescription, methodNameTransformer));
            }

            @Override
            public boolean isRebased() {
                return true;
            }

            @Override
            public MethodDescription.InDefinedShape getResolvedMethod() {
                return methodDescription;
            }

            @Override
            public StackManipulation getAdditionalArguments() {
                return StackManipulation.Trivial.INSTANCE;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && methodDescription.equals(((ForRebasedMethod) other).methodDescription);
            }

            @Override
            public int hashCode() {
                return methodDescription.hashCode();
            }

            @Override
            public String toString() {
                return "MethodRebaseResolver.Resolution.ForRebasedMethod{methodDescription=" + methodDescription + '}';
            }

            /**
             * A description of a rebased method.
             */
            protected static class RebasedMethod extends MethodDescription.InDefinedShape.AbstractBase {

                /**
                 * The method that is being rebased.
                 */
                private final InDefinedShape methodDescription;

                /**
                 * The transformer to use for renaming the method.
                 */
                private final MethodNameTransformer methodNameTransformer;

                /**
                 * Creates a new rebased method.
                 *
                 * @param methodDescription     The method that is being rebased.
                 * @param methodNameTransformer The transformer to use for renaming the method.
                 */
                protected RebasedMethod(InDefinedShape methodDescription, MethodNameTransformer methodNameTransformer) {
                    this.methodDescription = methodDescription;
                    this.methodNameTransformer = methodNameTransformer;
                }

                @Override
                public GenericTypeDescription getReturnType() {
                    return methodDescription.getReturnType().asErasure();
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Explicit.ForTypes(this, methodDescription.getParameters().asTypeList().asErasures());
                }

                @Override
                public GenericTypeList getExceptionTypes() {
                    return methodDescription.getExceptionTypes().asErasures().asGenericTypes();
                }

                @Override
                public Object getDefaultValue() {
                    return MethodDescription.NO_DEFAULT_VALUE;
                }

                @Override
                public GenericTypeList getTypeVariables() {
                    return new GenericTypeList.Empty();
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return methodDescription.getDeclaringType();
                }

                @Override
                public int getModifiers() {
                    return Opcodes.ACC_SYNTHETIC
                            | (methodDescription.isStatic() ? Opcodes.ACC_STATIC : EMPTY_MASK)
                            | (methodDescription.isNative() ? Opcodes.ACC_NATIVE : EMPTY_MASK)
                            | (methodDescription.getDeclaringType().isClassType() ? Opcodes.ACC_PRIVATE : Opcodes.ACC_PUBLIC);
                }

                @Override
                public String getInternalName() {
                    return methodNameTransformer.transform(methodDescription);
                }
            }
        }

        /**
         * A {@link MethodRebaseResolver.Resolution} of a rebased constructor.
         */
        class ForRebasedConstructor implements Resolution {

            /**
             * The rebased constructor.
             */
            private final MethodDescription.InDefinedShape methodDescription;

            /**
             * Creates a new resolution for a rebased constructor.
             *
             * @param methodDescription The rebased constructor.
             */
            protected ForRebasedConstructor(MethodDescription.InDefinedShape methodDescription) {
                this.methodDescription = methodDescription;
            }

            /**
             * Resolves a constructor rebasement.
             *
             * @param methodDescription The constructor to rebase.
             * @param placeholderType   The placeholder type to use to distinguish the constructor's signature.
             * @return A resolution of the provided constructor.
             */
            public static Resolution of(MethodDescription.InDefinedShape methodDescription, TypeDescription placeholderType) {
                return new ForRebasedConstructor(new RebasedConstructor(methodDescription, placeholderType));
            }

            @Override
            public boolean isRebased() {
                return true;
            }

            @Override
            public MethodDescription.InDefinedShape getResolvedMethod() {
                return methodDescription;
            }

            @Override
            public StackManipulation getAdditionalArguments() {
                return NullConstant.INSTANCE;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && methodDescription.equals(((ForRebasedConstructor) other).methodDescription);
            }

            @Override
            public int hashCode() {
                return methodDescription.hashCode();
            }

            @Override
            public String toString() {
                return "MethodRebaseResolver.Resolution.ForRebasedConstructor{methodDescription=" + methodDescription + '}';
            }

            /**
             * An description of a rebased constructor.
             */
            protected static class RebasedConstructor extends MethodDescription.InDefinedShape.AbstractBase {

                /**
                 * The constructor that is rebased.
                 */
                private final InDefinedShape methodDescription;

                /**
                 * The placeholder type that is used to distinguish the constructor's signature.
                 */
                private final TypeDescription placeholderType;

                /**
                 * Creates a new rebased constructor.
                 *
                 * @param methodDescription The constructor that is rebased.
                 * @param placeholderType   The placeholder type that is used to distinguish the constructor's signature.
                 */
                protected RebasedConstructor(InDefinedShape methodDescription, TypeDescription placeholderType) {
                    this.methodDescription = methodDescription;
                    this.placeholderType = placeholderType;
                }

                @Override
                public GenericTypeDescription getReturnType() {
                    return TypeDescription.VOID;
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Explicit.ForTypes(this, join(methodDescription.getParameters().asTypeList().asErasures(), placeholderType));
                }

                @Override
                public GenericTypeList getExceptionTypes() {
                    return methodDescription.getExceptionTypes().asErasures().asGenericTypes();
                }

                @Override
                public Object getDefaultValue() {
                    return MethodDescription.NO_DEFAULT_VALUE;
                }

                @Override
                public GenericTypeList getTypeVariables() {
                    return new GenericTypeList.Empty();
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return methodDescription.getDeclaringType();
                }

                @Override
                public int getModifiers() {
                    return Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE;
                }

                @Override
                public String getInternalName() {
                    return MethodDescription.CONSTRUCTOR_INTERNAL_NAME;
                }
            }
        }
    }

    /**
     * A default implementation of a method rebase resolver.
     */
    class Default implements MethodRebaseResolver {

        /**
         * A mapping of rebased methods to their existing resolutions.
         */
        private final Map<MethodDescription.InDefinedShape, Resolution> resolutions;

        /**
         * A list of dynamic types that need to be appended to the created type in order to allow for the rebasement.
         */
        private final List<DynamicType> dynamicTypes;

        /**
         * Creates a new default method rebased resolver.
         *
         * @param resolutions  A mapping of rebased methods to their existing resolutions.
         * @param dynamicTypes A list of dynamic types that need to be appended to the created type in order to allow for the rebasement.
         */
        protected Default(Map<MethodDescription.InDefinedShape, Resolution> resolutions, List<DynamicType> dynamicTypes) {
            this.resolutions = resolutions;
            this.dynamicTypes = dynamicTypes;
        }

        /**
         * Creates a new method rebase resolver.
         *
         * @param instrumentedType            The instrumented type.
         * @param rebaseableMethods           The methods that are possible to rebase.
         * @param classFileVersion            The class file version for the instrumentation.
         * @param auxiliaryTypeNamingStrategy The naming strategy for naming a potential auxiliary type.
         * @param methodNameTransformer       A transformer for method names.
         * @return A method rebase resolver that is capable of rebasing any of the provided methods.
         */
        public static MethodRebaseResolver make(TypeDescription instrumentedType,
                                                MethodList<MethodDescription.InDefinedShape> rebaseableMethods,
                                                ClassFileVersion classFileVersion,
                                                AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                MethodNameTransformer methodNameTransformer) {
            DynamicType placeholderType = null;
            Map<MethodDescription.InDefinedShape, Resolution> resolutions = new HashMap<MethodDescription.InDefinedShape, Resolution>(rebaseableMethods.size());
            for (MethodDescription.InDefinedShape instrumentedMethod : rebaseableMethods) {
                Resolution resolution;
                if (instrumentedMethod.isConstructor()) {
                    if (placeholderType == null) {
                        placeholderType = TrivialType.INSTANCE.make(auxiliaryTypeNamingStrategy.name(instrumentedType),
                                classFileVersion,
                                AuxiliaryType.MethodAccessorFactory.Illegal.INSTANCE);
                    }
                    resolution = Resolution.ForRebasedConstructor.of(instrumentedMethod, placeholderType.getTypeDescription());
                } else {
                    resolution = Resolution.ForRebasedMethod.of(instrumentedMethod, methodNameTransformer);
                }
                resolutions.put(instrumentedMethod, resolution);
            }
            return placeholderType == null
                    ? new Default(resolutions, Collections.<DynamicType>emptyList())
                    : new Default(resolutions, Collections.singletonList(placeholderType));
        }

        @Override
        public Resolution resolve(MethodDescription.InDefinedShape methodDescription) {
            Resolution resolution = resolutions.get(methodDescription);
            return resolution == null
                    ? new Resolution.Preserved(methodDescription)
                    : resolution;
        }

        @Override
        public List<DynamicType> getAuxiliaryTypes() {
            return dynamicTypes;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && resolutions.equals(((Default) other).resolutions)
                    && dynamicTypes.equals(((Default) other).dynamicTypes);
        }

        @Override
        public int hashCode() {
            return 31 * resolutions.hashCode() + dynamicTypes.hashCode();
        }

        @Override
        public String toString() {
            return "MethodRebaseResolver.Default{" +
                    "resolutions=" + resolutions +
                    ", dynamicTypes=" + dynamicTypes +
                    '}';
        }
    }
}
