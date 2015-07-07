package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.represents;

/**
 * A method matcher that is resolved by handing over the instrumented type before the matcher is applied to a method.
 */
public interface LatentMethodMatcher {

    /**
     * Resolves the latent method matcher.
     *
     * @param instrumentedType The instrumented type.
     * @return An resolved element matcher for matching a method description.
     */
    ElementMatcher<? super MethodDescription> resolve(TypeDescription instrumentedType);

    /**
     * A latent method matcher that is already resolved.
     */
    class Resolved implements LatentMethodMatcher {

        /**
         * The resolved method matcher.
         */
        private final ElementMatcher<? super MethodDescription> methodMatcher;

        /**
         * Creates a new resolved latent method matcher.
         *
         * @param methodMatcher The resolved method matcher.
         */
        public Resolved(ElementMatcher<? super MethodDescription> methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public ElementMatcher<? super MethodDescription> resolve(TypeDescription instrumentedType) {
            return methodMatcher;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodMatcher.equals(((Resolved) other).methodMatcher);
        }

        @Override
        public int hashCode() {
            return methodMatcher.hashCode();
        }

        @Override
        public String toString() {
            return "LatentMethodMatcher.Resolved{" +
                    "methodMatcher=" + methodMatcher +
                    '}';
        }
    }

    class ForToken implements LatentMethodMatcher {

        private final MethodDescription.Token methodToken;

        public ForToken(MethodDescription.Token methodToken) {
            this.methodToken = methodToken;
        }

        @Override
        public ElementMatcher<? super MethodDescription> resolve(TypeDescription instrumentedType) {
            return represents(methodToken.accept(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(instrumentedType)));
        }
    }
}
