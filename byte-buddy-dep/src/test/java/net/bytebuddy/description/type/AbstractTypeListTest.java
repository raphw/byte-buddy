package net.bytebuddy.description.type;

import net.bytebuddy.matcher.AbstractFilterableListTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractTypeListTest<U> extends AbstractFilterableListTest<TypeDescription, TypeList, U> {

    @Test
    public void testEmptyToInternalNames() throws Exception {
        assertThat(emptyList().toInternalNames(), nullValue(String[].class));
    }

    @Test
    public void testNonEmptyToInternalNames() throws Exception {
        assertThat(asList(getFirst()).toInternalNames(), is(new String[]{asElement(getFirst()).getInternalName()}));
    }

    @Test
    public void testEmptyStackSize() throws Exception {
        assertThat(emptyList().getStackSize(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNonEmptyStackSize() throws Exception {
        assertThat(asList(getFirst(), getSecond()).getStackSize(), is(2));
    }

    protected interface Foo {
        /* empty */
    }

    protected interface Bar {
        /* empty */
    }

    public static class Holder implements Foo, Bar {
        /* empty */
    }
}
