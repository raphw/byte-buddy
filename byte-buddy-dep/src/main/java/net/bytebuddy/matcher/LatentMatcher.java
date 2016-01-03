package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

import static net.bytebuddy.matcher.ElementMatchers.representedBy;

public interface LatentMatcher<T> {

    ElementMatcher<? super T> resolve(TypeDescription instrumentedType);

    class Resolved<S> implements LatentMatcher<S> {

        private final ElementMatcher<? super S> matcher;

        public Resolved(ElementMatcher<? super S> matcher) {
            this.matcher = matcher;
        }

        @Override
        public ElementMatcher<? super S> resolve(TypeDescription instrumentedType) {
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
            return "Resolved.Resolved{" +
                    "matcher=" + matcher +
                    '}';
        }
    }

    class ForFieldToken implements LatentMatcher<FieldDescription> {

        private final FieldDescription.Token token;

        public ForFieldToken(FieldDescription.Token token) {
            this.token = token;
        }

        @Override
        public ElementMatcher<? super FieldDescription> resolve(TypeDescription instrumentedType) {
            // Casting required for JDK 6.
            return (ElementMatcher<? super FieldDescription>) representedBy(token.accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(instrumentedType)));
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
    }

    class ForMethodToken implements LatentMatcher<MethodDescription> {

        private final MethodDescription.Token token;

        public ForMethodToken(MethodDescription.Token token) {
            this.token = token;
        }

        @Override
        public ElementMatcher<? super MethodDescription> resolve(TypeDescription instrumentedType) {
            // Casting required for JDK 6.
            return (ElementMatcher<? super MethodDescription>) representedBy(token.accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(instrumentedType)));
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
    }

}
