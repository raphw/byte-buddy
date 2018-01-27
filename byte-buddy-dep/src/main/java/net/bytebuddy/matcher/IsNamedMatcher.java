package net.bytebuddy.matcher;

import com.google.auto.value.AutoValue;
import net.bytebuddy.description.NamedElement;

/**
 * An element matcher that matches a named element only if is explicitly named.
 *
 * @param <T> The type of the matched entity.
 */
@AutoValue
public class IsNamedMatcher<T extends NamedElement.WithOptionalName> extends ElementMatcher.Junction.AbstractBase<T> {

    @Override
    public boolean matches(T target) {
        return target.isNamed();
    }

    @Override
    public String toString() {
        return "isNamed()";
    }
}
