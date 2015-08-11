package net.bytebuddy.description.type.generic;

import net.bytebuddy.matcher.AbstractFilterableListTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractGenericTypeListTest<U> extends AbstractFilterableListTest<GenericTypeDescription, GenericTypeList, U> {

    @Test
    public void testRawTypes() throws Exception {
        assertThat(asList(Collections.singletonList(getFirst())).asErasures().size(), is(1));
        assertThat(asList(Collections.singletonList(getFirst())).asErasures().getOnly(), is(asElement(getFirst()).asErasure()));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(asList(Collections.singletonList(getFirst())).accept(GenericTypeDescription.Visitor.NoOp.INSTANCE),
                is(asList(Collections.singletonList(getFirst()))));
    }

    @Test
    public void testStackSizeEmpty() throws Exception {
        assertThat(asList(Collections.<U>emptyList()).getStackSize(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStackSizeNonEmpty() throws Exception {
        assertThat(asList(Arrays.asList(getFirst(), getSecond())).getStackSize(), is(2));
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
