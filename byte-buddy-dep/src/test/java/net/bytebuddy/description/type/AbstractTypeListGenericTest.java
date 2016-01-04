package net.bytebuddy.description.type;

import net.bytebuddy.matcher.AbstractFilterableListTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractTypeListGenericTest<U> extends AbstractFilterableListTest<TypeDescription.Generic, TypeList.Generic, U> {

    @Test
    public void testErasures() throws Exception {
        assertThat(asList(getFirst()).asErasures().size(), is(1));
        assertThat(asList(getFirst()).asErasures().getOnly(), is(asElement(getFirst()).asErasure()));
    }

    @Test
    public void testRawTypes() throws Exception {
        assertThat(asList(getFirst()).asRawTypes().size(), is(1));
        assertThat(asList(getFirst()).asRawTypes().getOnly(), is(asElement(getFirst()).asRawType()));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(asList(getFirst()).accept(TypeDescription.Generic.Visitor.NoOp.INSTANCE), is(asList(getFirst())));
    }

    @Test
    public void testStackSizeEmpty() throws Exception {
        assertThat(emptyList().getStackSize(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStackSizeNonEmpty() throws Exception {
        assertThat(asList(getFirst(), getSecond()).getStackSize(), is(2));
    }

    protected interface Foo<T> {
        /* empty */
    }

    protected interface Bar<S> {
        /* empty */
    }

    public static class Holder implements Foo<String>, Bar<Integer> {
        /* empty */
    }
}
