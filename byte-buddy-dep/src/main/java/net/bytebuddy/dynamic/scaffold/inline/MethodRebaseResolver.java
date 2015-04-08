package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.NullConstant;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import net.bytebuddy.instrumentation.type.auxiliary.TrivialType;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
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
     * The modifier that is used for rebased methods.
     */
    int REBASED_METHOD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

    /**
     * Checks if a method is eligible for rebasing and resolves this possibly rebased method.
     *
     * @param methodDescription A description of the method to resolve.
     * @return A resolution for the given method.
     */
    Resolution resolve(MethodDescription methodDescription);

    List<DynamicType> getAuxiliaryTypes();

    /**
     * A method name transformer provides a unique mapping of a method's name to an alternative name.
     *
     * @see MethodRebaseResolver
     */
    interface MethodNameTransformer {

        /**
         * Transforms a method's name to an alternative name. For a given argument, this mapper must always provide
         * the same return value.
         *
         * @param originalName The original name.
         * @return The alternative name.
         */
        String transform(String originalName);

        /**
         * A method name transformer that adds a fixed suffix to an original method name, separated by a {@code $}.
         */
        class Suffixing implements MethodNameTransformer {

            /**
             * The default suffix to add to an original method name.
             */
            private static final String DEFAULT_SUFFIX = "original";

            /**
             * The suffix to append to a method name.
             */
            private final String suffix;

            /**
             * Creates a new suffixing method name transformer which adds a default suffix.
             */
            public Suffixing() {
                this(DEFAULT_SUFFIX);
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
            public String transform(String originalName) {
                return String.format("%s$%s", originalName, suffix);
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
            public String transform(String originalName) {
                return String.format("%s%s", prefix, originalName);
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
        MethodDescription getResolvedMethod();

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
            private final MethodDescription methodDescription;

            /**
             * Creates a new {@link MethodRebaseResolver.Resolution} for
             * a non-rebased method.
             *
             * @param methodDescription The preserved method.
             */
            public Preserved(MethodDescription methodDescription) {
                this.methodDescription = methodDescription;
            }

            @Override
            public boolean isRebased() {
                return false;
            }

            @Override
            public MethodDescription getResolvedMethod() {
                return methodDescription;
            }

            @Override
            public StackManipulation getAdditionalArguments() {
                throw new IllegalStateException("A non-rebased method never requires additional arguments");
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
            private final MethodDescription methodDescription;

            /**
             * Creates a {@link MethodRebaseResolver.Resolution} for a
             * rebased method.
             *
             * @param methodDescription     The original method that should be rebased.
             * @param methodNameTransformer A transformer for renaming a rebased method.
             */
            public ForRebasedMethod(MethodDescription methodDescription, MethodNameTransformer methodNameTransformer) {
                this.methodDescription = new MethodDescription.Latent(
                        methodNameTransformer.transform(methodDescription.getInternalName()),
                        methodDescription.getDeclaringType(),
                        methodDescription.getReturnType(),
                        methodDescription.getParameters().asTypeList(),
                        REBASED_METHOD_MODIFIER
                                | (methodDescription.isStatic() ? Opcodes.ACC_STATIC : 0)
                                | (methodDescription.isNative() ? Opcodes.ACC_NATIVE : 0),
                        methodDescription.getExceptionTypes());
            }

            @Override
            public boolean isRebased() {
                return true;
            }

            @Override
            public MethodDescription getResolvedMethod() {
                return methodDescription;
            }

            @Override
            public StackManipulation getAdditionalArguments() {
                return StackManipulation.LegalTrivial.INSTANCE;
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
        }

        /**
         * A {@link MethodRebaseResolver.Resolution} of a rebased constructor.
         */
        class ForRebasedConstructor implements Resolution {

            /**
             * The rebased constructor.
             */
            private final MethodDescription methodDescription;

            /**
             * Creates a {@link MethodRebaseResolver.Resolution} for a
             * rebased method.
             *
             * @param methodDescription The constructor to rebase.
             * @param placeholderType   A placeholder type which is added to a rebased constructor.
             */
            public ForRebasedConstructor(MethodDescription methodDescription, TypeDescription placeholderType) {
                this.methodDescription = new MethodDescription.Latent(methodDescription.getInternalName(),
                        methodDescription.getDeclaringType(),
                        methodDescription.getReturnType(),
                        join(methodDescription.getParameters().asTypeList(), placeholderType),
                        REBASED_METHOD_MODIFIER,
                        methodDescription.getExceptionTypes());
            }

            @Override
            public boolean isRebased() {
                return true;
            }

            @Override
            public MethodDescription getResolvedMethod() {
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
        }
    }

    /**
     * A method rebase resolver that preserves any method in its original form.
     */
    enum Forbidden implements MethodRebaseResolver {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolution resolve(MethodDescription methodDescription) {
            return new Resolution.Preserved(methodDescription);
        }

        @Override
        public List<DynamicType> getAuxiliaryTypes() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "MethodRebaseResolver.NoOp." + name();
        }

    }

    abstract class AbstractBase implements MethodRebaseResolver {

        protected final Set<? extends MethodDescription> instrumentedMethods;

        protected AbstractBase(Set<? extends MethodDescription> instrumentedMethods) {
            this.instrumentedMethods = instrumentedMethods;
        }

        @Override
        public Resolution resolve(MethodDescription methodDescription) {
            return instrumentedMethods.contains(methodDescription)
                    ? rebase(methodDescription)
                    : new Resolution.Preserved(methodDescription);
        }

        protected abstract Resolution rebase(MethodDescription methodDescription);
    }

    class MethodsOnly extends AbstractBase {

        protected static MethodRebaseResolver of(MethodList instrumentedMethods, MethodNameTransformer methodNameTransformer) {
            return new MethodsOnly(new HashSet<MethodDescription>(instrumentedMethods), methodNameTransformer);
        }

        private final MethodNameTransformer methodNameTransformer;

        public MethodsOnly(Set<? extends MethodDescription> instrumentedMethods, MethodNameTransformer methodNameTransformer) {
            super(instrumentedMethods);
            this.methodNameTransformer = methodNameTransformer;
        }

        @Override
        protected Resolution rebase(MethodDescription methodDescription) {
            if (methodDescription.isConstructor()) {
                throw new IllegalArgumentException();
            }
            return new Resolution.ForRebasedMethod(methodDescription, methodNameTransformer);
        }

        @Override
        public List<DynamicType> getAuxiliaryTypes() {
            return Collections.emptyList();
        }
    }

    class Enabled extends AbstractBase {

        public static MethodRebaseResolver make(MethodList instrumentedMethods,
                                                TypeDescription instrumentedType,
                                                ClassFileVersion classFileVersion,
                                                AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                MethodNameTransformer methodNameTransformer) {
            return instrumentedMethods.filter(isConstructor()).isEmpty()
                    ? MethodsOnly.of(instrumentedMethods, methodNameTransformer)
                    : of(instrumentedMethods, TrivialType.INSTANCE.make(auxiliaryTypeNamingStrategy.name(TrivialType.INSTANCE, instrumentedType),
                            classFileVersion,
                            AuxiliaryType.MethodAccessorFactory.Illegal.INSTANCE), methodNameTransformer);
        }

        protected static MethodRebaseResolver of(MethodList instrumentedMethods, DynamicType placeholderType, MethodNameTransformer methodNameTransformer) {
            return new Enabled(new HashSet<MethodDescription>(instrumentedMethods), placeholderType, methodNameTransformer);
        }

        private final DynamicType placeholderType;

        private final MethodNameTransformer methodNameTransformer;

        public Enabled(Set<? extends MethodDescription> instrumentedMethods,
                       DynamicType placeholderType,
                       MethodNameTransformer methodNameTransformer) {
            super(instrumentedMethods);
            this.placeholderType = placeholderType;
            this.methodNameTransformer = methodNameTransformer;
        }

        @Override
        protected Resolution rebase(MethodDescription methodDescription) {
            return methodDescription.isConstructor()
                    ? new Resolution.ForRebasedConstructor(methodDescription, placeholderType.getTypeDescription())
                    : new Resolution.ForRebasedMethod(methodDescription, methodNameTransformer);
        }

        @Override
        public List<DynamicType> getAuxiliaryTypes() {
            return Collections.singletonList(placeholderType);
        }
    }
}
