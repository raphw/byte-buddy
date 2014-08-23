package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.NullConstant;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.utility.ByteBuddyCommons.join;

public interface MethodFlatteningResolver {

    static final int REDEFINE_METHOD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

    static class Default implements MethodFlatteningResolver {

        private final MethodMatcher ignoredMethods;

        private final TypeDescription placeholderType;

        private final String seed;

        public Default(MethodMatcher ignoredMethods, TypeDescription placeholderType) {
            this.ignoredMethods = ignoredMethods;
            this.placeholderType = placeholderType;
            seed = RandomString.make();
        }

        @Override
        public Resolution resolve(MethodDescription methodDescription) {
            return ignoredMethods.matches(methodDescription)
                    ? new Resolution.Preserved(methodDescription)
                    : redefine(methodDescription);
        }

        private Resolution redefine(MethodDescription methodDescription) {
            return methodDescription.isConstructor()
                    ? new Resolution.ForRebasedConstructor(methodDescription, placeholderType)
                    : new Resolution.ForRebasedMethod(methodDescription, seed);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Default aDefault = (Default) other;
            return seed.equals(aDefault.seed)
                    && ignoredMethods.equals(aDefault.ignoredMethods)
                    && placeholderType.equals(aDefault.placeholderType);
        }

        @Override
        public int hashCode() {
            int result = ignoredMethods.hashCode();
            result = 31 * result + placeholderType.hashCode();
            result = 31 * result + seed.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodFlatteningResolver.Default{" +
                    "ignoredMethods=" + ignoredMethods +
                    ", placeholderType=" + placeholderType +
                    ", seed=" + seed +
                    '}';
        }
    }

    static enum NoOp implements MethodFlatteningResolver {

        INSTANCE;

        @Override
        public Resolution resolve(MethodDescription methodDescription) {
            return new Resolution.Preserved(methodDescription);
        }
    }

    static interface Resolution {

        static class Preserved implements Resolution {

            private final MethodDescription methodDescription;

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
                throw new IllegalStateException();
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
                return "MethodFlatteningResolver.Resolution.Preserved{methodDescription=" + methodDescription + '}';
            }
        }

        static class ForRebasedMethod implements Resolution {

            private static final String ORIGINAL_METHOD_NAME_SUFFIX = "original";

            private final MethodDescription methodDescription;

            public ForRebasedMethod(MethodDescription methodDescription, String seed) {
                this.methodDescription = new MethodDescription.Latent(
                        String.format("%s$%s$%s", methodDescription.getInternalName(), ORIGINAL_METHOD_NAME_SUFFIX, seed),
                        methodDescription.getDeclaringType(),
                        methodDescription.getReturnType(),
                        methodDescription.getParameterTypes(),
                        REDEFINE_METHOD_MODIFIER
                                | (methodDescription.isStatic() ? Opcodes.ACC_STATIC : 0)
                                | (methodDescription.isNative() ? Opcodes.ACC_NATIVE : 0));
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
                return "MethodFlatteningResolver.Resolution.ForRedefinedMethod{methodDescription=" + methodDescription + '}';
            }
        }

        static class ForRebasedConstructor implements Resolution {

            private final MethodDescription methodDescription;

            public ForRebasedConstructor(MethodDescription methodDescription, TypeDescription placeholderType) {
                this.methodDescription = new MethodDescription.Latent(methodDescription.getInternalName(),
                        methodDescription.getDeclaringType(),
                        methodDescription.getReturnType(),
                        join(methodDescription.getParameterTypes(), placeholderType),
                        REDEFINE_METHOD_MODIFIER);
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
                return "MethodFlatteningResolver.Resolution.ForRedefinedConstructor{methodDescription=" + methodDescription + '}';
            }
        }

        boolean isRebased();

        MethodDescription getResolvedMethod();

        StackManipulation getAdditionalArguments();
    }

    Resolution resolve(MethodDescription methodDescription);
}
