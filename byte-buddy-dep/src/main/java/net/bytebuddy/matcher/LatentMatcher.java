package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

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
     * A latent matching methods that are declared by the resolved type.
     */
    enum ForSelfDeclaredMethod implements LatentMatcher<MethodDescription> {

        /**
         * Matches any method declared by the resolved type.
         */
        DECLARED(false),

        /**
         * Matches any method not declared by the resolved type.
         */
        NOT_DECLARED(true);

        /**
         * {@code true} if the matcher is inverted.
         */
        private final boolean inverted;

        /**
         * Creates a new latent matcher for a self-declared method.
         *
         * @param inverted {@code true} if the matcher is inverted.
         */
        ForSelfDeclaredMethod(boolean inverted) {
            this.inverted = inverted;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ElementMatcher<? super MethodDescription> resolve(TypeDescription typeDescription) {
            // Casting is required by some Java 6 compilers.
            return (ElementMatcher<? super MethodDescription>) (inverted
                    ? not(isDeclaredBy(typeDescription))
                    : isDeclaredBy(typeDescription));
        }
    }

    /**
     * A latent matcher representing an already resolved {@link ElementMatcher}.
     *
     * @param <S> The type of the matched element.
     */
    @HashCodeAndEqualsPlugin.Enhance
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
    }

    /**
     * A latent matcher where the field token is being attached to the supplied type description before matching.
     */
    @HashCodeAndEqualsPlugin.Enhance
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

        /**
         * A resolved matcher of a latent field matcher for a field token.
         */
        @HashCodeAndEqualsPlugin.Enhance
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
        }
    }

    /**
     * A latent matcher where the method token is being attached to the supplied type description before matching.
     */
    @HashCodeAndEqualsPlugin.Enhance
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

        /**
         * A resolved matcher of a latent method matcher for a method token.
         */
        @HashCodeAndEqualsPlugin.Enhance
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
        }
    }

    /**
     * A matcher that computes the conjunction of all supplied latent matchers.
     *
     * @param <S> The type of the matched element.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Conjunction<S> implements LatentMatcher<S> {

        /**
         * The matchers this conjunction represents.
         */
        private final List<? extends LatentMatcher<? super S>> matchers;

        /**
         * Creates a new conjunction of latent matchers.
         *
         * @param matcher The matchers this conjunction represents.
         */
        @SuppressWarnings("unchecked") // In absence of @SafeVarargs for Java 6
        public Conjunction(LatentMatcher<? super S>... matcher) {
            this(Arrays.asList(matcher));
        }

        /**
         * Creates a new conjunction of latent matchers.
         *
         * @param matchers The matchers this conjunction represents.
         */
        public Conjunction(List<? extends LatentMatcher<? super S>> matchers) {
            this.matchers = matchers;
        }

        @Override
        public ElementMatcher<? super S> resolve(TypeDescription typeDescription) {
            ElementMatcher.Junction<S> matcher = any();
            for (LatentMatcher<? super S> latentMatcher : matchers) {
                matcher = matcher.and(latentMatcher.resolve(typeDescription));
            }
            return matcher;
        }
    }

    /**
     * A matcher that computes the disjunction of all supplied latent matchers.
     *
     * @param <S> The type of the matched element.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Disjunction<S> implements LatentMatcher<S> {

        /**
         * The matchers this disjunction represents.
         */
        private final List<? extends LatentMatcher<? super S>> matchers;

        /**
         * Creates a new disjunction of latent matchers.
         *
         * @param matcher The matchers this disjunction represents.
         */
        @SuppressWarnings("unchecked") // In absence of @SafeVarargs for Java 6
        public Disjunction(LatentMatcher<? super S>... matcher) {
            this(Arrays.asList(matcher));
        }

        /**
         * Creates a new disjunction of latent matchers.
         *
         * @param matchers The matchers this disjunction represents.
         */
        public Disjunction(List<? extends LatentMatcher<? super S>> matchers) {
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
    }
}
