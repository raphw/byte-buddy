package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.method.MethodDescription;

/**
 * Matches a method description's signature token against another matcher.
 *
 * @param <T> The type of the matched entity.
 */
@EqualsAndHashCode(callSuper = false)
public class SignatureTokenMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to match the signature token against.
     */
    private final ElementMatcher<? super MethodDescription.SignatureToken> matcher;

    /**
     * Creates a new signature token matcher.
     *
     * @param matcher The matcher to match the signature token against.
     */
    public SignatureTokenMatcher(ElementMatcher<? super MethodDescription.SignatureToken> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
        return matcher.matches(target.asSignatureToken());
    }

    @Override
    public String toString() {
        return "signature(" + matcher + ")";
    }
}
