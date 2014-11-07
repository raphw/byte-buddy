package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.NullConstant;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.Opcodes;

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
    static final int REBASED_METHOD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

    /**
     * Checks if a method is eligible for rebasing and resolves this possibly rebased method.
     *
     * @param methodDescription A description of the method to resolve.
     * @return A resolution for the given method.
     */
    Resolution resolve(MethodDescription methodDescription);

    /**
     * A method rebase resolver that preserves any method in its original form.
     */
    static enum NoOp implements MethodRebaseResolver {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolution resolve(MethodDescription methodDescription) {
            return new Resolution.Preserved(methodDescription);
        }
    }

    /**
     * A method name transformer provides a unique mapping of a method's name to an alternative name.
     *
     * @see MethodRebaseResolver
     */
    static interface MethodNameTransformer {

        /**
         * Transforms a method's name to an alternative name. For a given argument, this mapper must always provide
         * the same return value.
         *
         * @param originalName The original name.
         * @return The alternative name.
         */
        String transform(String originalName);

        /**
         * A method name transformer that adds a fixed suffix to an original method name.
         */
        static class Suffixing implements MethodNameTransformer {

            /**
             * The default suffix to add to an original method name.
             */
            private static final String DEFAULT_SUFFIX = "original";

            /**
             * The suffix to append to a method name.
             */
            private final String suffix;

            /**
             * The seed to append to a method name.
             */
            private final String seed;

            /**
             * Creates a new suffixing method name transformer which adds a default suffix and a random seed.
             *
             * @param randomString A provider for a random seed.
             */
            public Suffixing(RandomString randomString) {
                this(randomString, DEFAULT_SUFFIX);
            }

            /**
             * Creates a new suffixing method name transformer.
             *
             * @param randomString A provider for a random seed.
             * @param suffix       The suffix to add to the method name before the seed.
             */
            public Suffixing(RandomString randomString, String suffix) {
                this.suffix = suffix;
                seed = randomString.nextString();
            }

            @Override
            public String transform(String originalName) {
                return String.format("%s$%s$%s", originalName, suffix, seed);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && seed.equals(((Suffixing) other).seed) && suffix.equals(((Suffixing) other).suffix);
            }

            @Override
            public int hashCode() {
                return 31 * suffix.hashCode() + seed.hashCode();
            }

            @Override
            public String toString() {
                return "MethodRebaseResolver.MethodNameTransformer.Suffixing{" +
                        "suffix='" + suffix + '\'' +
                        ", seed='" + seed + '\'' +
                        '}';
            }
        }
    }

    /**
     * A resolution for a method that was checked by a {@link MethodRebaseResolver}.
     */
    static interface Resolution {

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
        static class Preserved implements Resolution {

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
        static class ForRebasedMethod implements Resolution {

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
                        methodDescription.getParameterTypes(),
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
        static class ForRebasedConstructor implements Resolution {

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
                        join(methodDescription.getParameterTypes(), placeholderType),
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
     * A default implementation of a {@link MethodRebaseResolver} which
     * renames rebased methods and adds an additional constructor placeholder parameter to constructors. Ignored
     * methods are never rebased.
     */
    static class Default implements MethodRebaseResolver {

        /**
         * Ignored methods which are never rebased.
         */
        private final MethodMatcher ignoredMethods;

        /**
         * A placeholder type which is added to a rebased constructor.
         */
        private final TypeDescription placeholderType;

        /**
         * A transformer for renaming a rebased method.
         */
        private final MethodNameTransformer methodNameTransformer;

        /**
         * Creates a default method rebase resolver.
         *
         * @param ignoredMethods        Ignored methods which are never rebased.
         * @param placeholderType       A placeholder type which is added to a rebased constructor.
         * @param methodNameTransformer A transformer for renaming a rebased method.
         */
        public Default(MethodMatcher ignoredMethods,
                       TypeDescription placeholderType,
                       MethodNameTransformer methodNameTransformer) {
            this.ignoredMethods = ignoredMethods;
            this.placeholderType = placeholderType;
            this.methodNameTransformer = methodNameTransformer;
        }

        @Override
        public Resolution resolve(MethodDescription methodDescription) {
            return ignoredMethods.matches(methodDescription)
                    ? new Resolution.Preserved(methodDescription)
                    : rebase(methodDescription);
        }

        /**
         * Resolves a rebase method of a given method.
         *
         * @param methodDescription The method to rebase.
         * @return The resolution for the given method.
         */
        private Resolution rebase(MethodDescription methodDescription) {
            return methodDescription.isConstructor()
                    ? new Resolution.ForRebasedConstructor(methodDescription, placeholderType)
                    : new Resolution.ForRebasedMethod(methodDescription, methodNameTransformer);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Default aDefault = (Default) other;
            return ignoredMethods.equals(aDefault.ignoredMethods)
                    && placeholderType.equals(aDefault.placeholderType)
                    && methodNameTransformer.equals(aDefault.methodNameTransformer);
        }

        @Override
        public int hashCode() {
            int result = ignoredMethods.hashCode();
            result = 31 * result + placeholderType.hashCode();
            result = 31 * result + methodNameTransformer.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodRebaseResolver.Default{" +
                    "ignoredMethods=" + ignoredMethods +
                    ", placeholderType=" + placeholderType +
                    ", methodNameTransformer=" + methodNameTransformer +
                    '}';
        }
    }
}
