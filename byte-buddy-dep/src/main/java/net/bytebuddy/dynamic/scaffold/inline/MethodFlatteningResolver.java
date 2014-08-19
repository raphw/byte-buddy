package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.utility.ByteBuddyCommons.join;

public interface MethodFlatteningResolver {

    static final int REDEFINE_METHOD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

    static class Default implements MethodFlatteningResolver {

        private final Map<MethodDescription, Resolution> resolutions;

        public Default(List<MethodDescription> methodDescriptions,
                       MethodMatcher ignoredMethods,
                       TypeDescription placeholderType) {
            resolutions = new HashMap<MethodDescription, Resolution>(methodDescriptions.size());
            for (MethodDescription methodDescription : methodDescriptions) {
                resolutions.put(methodDescription, ignoredMethods.matches(methodDescription)
                        ? new Resolution.Preserved(methodDescription)
                        : new Resolution.Redefined(redefine(methodDescription, placeholderType)));
            }
        }

        private static MethodDescription redefine(MethodDescription methodDescription, TypeDescription placeholderType) {
            return methodDescription.isConstructor()
                    ? new MethodDescription.Latent(methodDescription.getInternalName(),
                    methodDescription.getDeclaringType(),
                    methodDescription.getReturnType(),
                    join(methodDescription.getParameterTypes(), placeholderType),
                    REDEFINE_METHOD_MODIFIER)
                    : new MethodDescription.Latent(methodDescription.getInternalName(),
                    methodDescription.getDeclaringType(),
                    methodDescription.getReturnType(),
                    methodDescription.getParameterTypes(),
                    REDEFINE_METHOD_MODIFIER
                            | (methodDescription.isStatic() ? Opcodes.ACC_STATIC : 0)
                            | (methodDescription.isNative() ? Opcodes.ACC_NATIVE : 0));
        }

        @Override
        public Resolution resolve(MethodDescription methodDescription) {
            return resolutions.get(methodDescription);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && resolutions.equals(((Default) other).resolutions);
        }

        @Override
        public int hashCode() {
            return resolutions.hashCode();
        }

        @Override
        public String toString() {
            return "MethodFlatteningResolver.Default{resolutions=" + resolutions + '}';
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

        static class Redefined implements Resolution {

            private final MethodDescription methodDescription;

            public Redefined(MethodDescription methodDescription) {
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
                return "MethodFlatteningResolver.Resolution.Redefined{methodDescription=" + methodDescription + '}';
            }
        }

        boolean isRedefined();

        MethodDescription getResolvedMethod();
    }

    Resolution resolve(MethodDescription methodDescription);
}
