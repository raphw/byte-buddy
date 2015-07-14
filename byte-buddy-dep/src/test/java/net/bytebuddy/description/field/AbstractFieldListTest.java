package net.bytebuddy.description.field;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.AbstractFilterableListTest;
import org.junit.Test;

import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractFieldListTest<U> extends AbstractFilterableListTest<FieldDescription, FieldList, U> {

    @Test
    @SuppressWarnings("unchecked")
    public void testTokenWithoutMatcher() throws Exception {
        assertThat(asList(Collections.singletonList(getFirst())).asTokenList(), is(new ByteCodeElement.Token
                .TokenList<FieldDescription.Token>(Collections.singletonList(asElement(getFirst()).asToken()))));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTokenWithMatcher() throws Exception {
        assertThat(asList(Collections.singletonList(getFirst())).asTokenList(none()), is(new ByteCodeElement.Token
                .TokenList<FieldDescription.Token>(Collections.singletonList(asElement(getFirst()).asToken(none())))));
    }

    protected static class Foo {

        Void foo;

        Void bar;
    }
}
