package net.bytebuddy.description.method;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.AbstractFilterableListTest;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Test;

import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractMethodListTest<U, V extends MethodDescription> extends AbstractFilterableListTest<V, MethodList<V>, U> {

    @Test
    public void testTokenWithMatcher() throws Exception {
        assertThat(asList(getFirst()).asTokenList(none()),
                is(new ByteCodeElement.Token.TokenList<MethodDescription.Token>(asElement(getFirst()).asToken(none()))));
    }

    @Test
    public void testSignatureToken() throws Exception {
        assertThat(asList(getFirst()).asSignatureTokenList(),
                is(Collections.singletonList(asElement(getFirst()).asSignatureToken())));
    }

    @Test
    public void testSignatureTokenWithMatcher() throws Exception {
        assertThat(asList(getFirst()).asSignatureTokenList(none(), TypeDescription.ForLoadedType.of(Foo.class)),
                is(Collections.singletonList(asElement(getFirst()).asToken(none()).asSignatureToken(TypeDescription.ForLoadedType.of(Foo.class)))));
    }

    @Test
    public void testAsDefined() throws Exception {
        assertThat(asList(getFirst()).asDefined(), is(Collections.singletonList(asElement(getFirst()).asDefined())));
    }

    public abstract static class Foo {

        abstract void foo();

        abstract void bar();
    }
}
