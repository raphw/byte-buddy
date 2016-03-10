package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.none;

/**
 * A latent matcher that resolves an {@link ElementMatcher} after supplying a type description.
 *
 * @param <T> The type of the matched element.
 */
public interface LatentMatcher<T> {

    /**
     * Resolves the element matcher this instance represents for the supplied type description.
     *
     * @param typeDescription The type description for which the represented matcher should be resolved.
     * @return An {@link ElementMatcher} that represents this matcher's resolved form.
     */
    ElementMatcher<? super T> resolve(TypeDescription typeDescription);

    /**
     * A latent matcher representing an already resolved {@link ElementMatcher}.
     *
     * @param <S> The type of the matched element.
     */
    class Resolved<S> implements LatentMatcher<S> {

        /**
         * The resolved matcher.
         */
        private final ElementMatcher<? super S> matcher;

        /**
         * Creates a new resolved latent matcher.
         *
         * @param matcher The resolved matcher.
         */
        public Resolved(ElementMatcher<? super S> matcher) {
            this.matcher = matcher;
        }

        @Override
        public ElementMatcher<? super S> resolve(TypeDescription typeDescription) {
            return matcher;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && matcher.equals(((Resolved) other).matcher);
        }

        @Override
        public int hashCode() {
            return matcher.hashCode();
        }

        @Override
        public String toString() {
            return "LatentMatcher.Resolved{" +
                    "matcher=" + matcher +
                    '}';
        }
    }

    /**
     * A latent matcher where the field token is being attached to the supplied type description before matching.
     */
    class ForFieldToken implements LatentMatcher<FieldDescription> {

        /**
         * A token representing the field being matched.
         */
        private final FieldDescription.Token token;

        /**
         * Creates a new latent matcher for a field token.
         *
         * @param token A token representing the field being matched.
         */
        public ForFieldToken(FieldDescription.Token token) {
            this.token = token;
        }

        @Override
        public ElementMatcher<? super FieldDescription> resolve(TypeDescription typeDescription) {
            return new ResolvedMatcher(token.asSignatureToken(typeDescription));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && token.equals(((ForFieldToken) other).token);
        }

        @Override
        public int hashCode() {
            return token.hashCode();
        }

        @Override
        public String toString() {
            return "LatentMatcher.ForFieldToken{" +
                    "token=" + token +
                    '}';
        }

        /**
         * A resolved matcher of a latent field matcher for a field token.
         */
        protected static class ResolvedMatcher implements ElementMatcher<FieldDescription> {

            /**
             * The signature token representing the matched field.
             */
            private final FieldDescription.SignatureToken signatureToken;

            /**
             * Creates a new resolved matcher.
             *
             * @param signatureToken The signature token representing the matched field.
             */
            protected ResolvedMatcher(FieldDescription.SignatureToken signatureToken) {
                this.signatureToken = signatureToken;
            }

            @Override
            public boolean matches(FieldDescription target) {
                return target.asSignatureToken().equals(signatureToken);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && signatureToken.equals(((ResolvedMatcher) other).signatureToken);
            }

            @Override
            public int hashCode() {
                return signatureToken.hashCode();
            }

            @Override
            public String toString() {
                return "LatentMatcher.ForFieldToken.ResolvedMatcher{" +
                        "signatureToken=" + signatureToken +
                        '}';
            }
        }
    }

    /**
     * A latent matcher where the method token is being attached to the supplied type description before matching.
     */
    class ForMethodToken implements LatentMatcher<MethodDescription> {

        /**
         * A token representing the method being matched.
         */
        private final MethodDescription.Token token;

        /**
         * Creates a new latent matcher for a method token.
         *
         * @param token A token representing the method being matched.
         */
        public ForMethodToken(MethodDescription.Token token) {
            this.token = token;
        }

        @Override
        public ElementMatcher<? super MethodDescription> resolve(TypeDescription typeDescription) {
            return new ResolvedMatcher(token.asSignatureToken(typeDescription));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && token.equals(((ForMethodToken) other).token);
        }

        @Override
        public int hashCode() {
            return token.hashCode();
        }

        @Override
        public String toString() {
            return "LatentMatcher.ForMethodToken{" +
                    "token=" + token +
                    '}';
        }

        /**
         * A resolved matcher of a latent method matcher for a method token.
         */
        protected static class ResolvedMatcher implements ElementMatcher<MethodDescription> {

            /**
             * The signature token representing the matched field.
             */
            private final MethodDescription.SignatureToken signatureToken;

            /**
             * Creates a new resolved matcher.
             *
             * @param signatureToken The signature token representing the matched field.
             */
            protected ResolvedMatcher(MethodDescription.SignatureToken signatureToken) {
                this.signatureToken = signatureToken;
            }

            @Override
            public boolean matches(MethodDescription target) {
                return target.asSignatureToken().equals(signatureToken);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && signatureToken.equals(((ResolvedMatcher) other).signatureToken);
            }

            @Override
            public int hashCode() {
                return signatureToken.hashCode();
            }

            @Override
            public String toString() {
                return "LatentMatcher.ForMethodToken.ResolvedMatcher{" +
                        "signatureToken=" + signatureToken +
                        '}';
            }
        }
    }

    /**
     * A compound implementation of a latent matcher. A compound matcher matches a method if at least one of the resolved matchers
     * matches the target element.
     *
     * @param <S> The type of the matched element.
     */
    class Compound<S> implements LatentMatcher<S> {

        /**
         * The matchers this compound matcher represents.
         */
        private final List<? extends LatentMatcher<? super S>> matchers;

        /**
         * Creates a new compound latent matcher.
         *
         * @param matcher The matchers this compound matcher represents.
         */
        //@SafeVarargs
        public Compound(LatentMatcher<? super S>... matcher) {
            this(Arrays.asList(matcher));
        }

        /**
         * Creates a new compound latent matcher.
         *
         * @param matchers The matchers this compound matcher represents.
         */
        public Compound(List<? extends LatentMatcher<? super S>> matchers) {
            this.matchers = matchers;
        }

        @Override
        public ElementMatcher<? super S> resolve(TypeDescription typeDescription) {
            ElementMatcher.Junction<S> matcher = none();
            for (LatentMatcher<? super S> latentMatcher : matchers) {
                matcher = matcher.or(latentMatcher.resolve(typeDescription));
            }
            return matcher;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && matchers.equals(((Compound) other).matchers);
        }

        @Override
        public int hashCode() {
            return matchers.hashCode();
        }

        @Override
        public String toString() {
            return "LatentMatcher.Compound{" +
                    "matchers=" + matchers +
                    '}';
        }
    }
}
