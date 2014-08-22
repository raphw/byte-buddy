package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.NullConstant;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.utility.ByteBuddyCommons.join;

public interface MethodFlatteningResolver {

    static final int REDEFINE_METHOD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

    static class Default implements MethodFlatteningResolver {

        private final MethodMatcher ignoredMethods;

        private final TypeDescription placeholderType;

        public Default(MethodMatcher ignoredMethods, TypeDescription placeholderType) {
            this.ignoredMethods = ignoredMethods;
            this.placeholderType = placeholderType;
        }

        @Override
        public Resolution resolve(MethodDescription methodDescription) {
            return ignoredMethods.matches(methodDescription)
                    ? new Resolution.Preserved(methodDescription)
                    : redefine(methodDescription);
        }

        private Resolution redefine(MethodDescription methodDescription) {
            return methodDescription.isConstructor()
                    ? new Resolution.ForRedefinedConstructor(new MethodDescription.Latent(methodDescription.getInternalName(),
                    methodDescription.getDeclaringType(),
                    methodDescription.getReturnType(),
                    join(methodDescription.getParameterTypes(), placeholderType),
                    REDEFINE_METHOD_MODIFIER))
                    : new Resolution.ForRedefinedMethod(new MethodDescription.Latent(methodDescription.getInternalName(),
                    methodDescription.getDeclaringType(),
                    methodDescription.getReturnType(),
                    methodDescription.getParameterTypes(),
                    REDEFINE_METHOD_MODIFIER
                            | (methodDescription.isStatic() ? Opcodes.ACC_STATIC : 0)
                            | (methodDescription.isNative() ? Opcodes.ACC_NATIVE : 0)));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && ignoredMethods.equals(((Default) other).ignoredMethods)
                    && placeholderType.equals(((Default) other).placeholderType);
        }

        @Override
        public int hashCode() {
            return 31 * ignoredMethods.hashCode() + placeholderType.hashCode();
        }

        @Override
        public String toString() {
            return "MethodFlatteningResolver.Default{" +
                    "ignoredMethods=" + ignoredMethods +
                    ", placeholderType=" + placeholderType +
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
            public boolean isRedefined() {
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

        static class ForRedefinedMethod implements Resolution {

            private final MethodDescription methodDescription;

            public ForRedefinedMethod(MethodDescription methodDescription) {
                this.methodDescription = methodDescription;
            }

            @Override
            public boolean isRedefined() {
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
                        && methodDescription.equals(((ForRedefinedMethod) other).methodDescription);
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

        static class ForRedefinedConstructor implements Resolution {

            private final MethodDescription methodDescription;

            public ForRedefinedConstructor(MethodDescription methodDescription) {
                this.methodDescription = methodDescription;
            }

            @Override
            public boolean isRedefined() {
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
                        && methodDescription.equals(((ForRedefinedConstructor) other).methodDescription);
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

        boolean isRedefined();

        MethodDescription getResolvedMethod();

        StackManipulation getAdditionalArguments();
    }

    Resolution resolve(MethodDescription methodDescription);
}
