package net.bytebuddy.description.type;

import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
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
        assertThat(asList(Collections.<U>emptyList()).toInternalNames(), nullValue(String[].class));
    }

    @Test
    public void testNonEmptyToInternalNames() throws Exception {
        assertThat(asList(Collections.singletonList(getFirst())).toInternalNames(), is(new String[]{asElement(getFirst()).getInternalName()}));
    }

    @Test
    public void testEmptyStackSize() throws Exception {
        assertThat(asList(Collections.<U>emptyList()).getStackSize(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNonEmptyStackSize() throws Exception {
        assertThat(asList(Arrays.asList(getFirst(), getSecond())).getStackSize(), is(2));
    }

    @Test
    public void testGenericTypes() throws Exception {
        assertThat(asList(Collections.singletonList(getFirst())).asGenericTypes().size(), is(1));
        assertThat(asList(Collections.singletonList(getFirst())).asGenericTypes().getOnly(), is((GenericTypeDescription) asElement(getFirst())));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(asList(Collections.singletonList(getFirst())).accept(GenericTypeDescription.Visitor.NoOp.INSTANCE),
                is((GenericTypeList) new GenericTypeList.Explicit(asList(Collections.singletonList(getFirst())))));
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
