package net.bytebuddy.instrumentation.field;

import org.junit.Before;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FieldListForLoadedFieldTest {

    private static final String FOO = "foo", BAR = "bar";
    private FieldList fieldList;

    @Before
    public void setUp() throws Exception {
        fieldList = new FieldList.ForLoadedField(Foo.class.getDeclaredFields());
    }

    @Test
    public void testFieldList() throws Exception {
        assertThat(fieldList.size(), is(2));
        assertThat(fieldList.get(0).getInternalName(), is(FOO));
        assertThat(fieldList.get(1).getInternalName(), is(BAR));
    }

    @Test
    public void testMethodListFilter() throws Exception {
        fieldList = fieldList.filter(named(FOO));
        assertThat(fieldList.size(), is(1));
        assertThat(fieldList.getOnly(), is((FieldDescription) new FieldDescription.ForLoadedField(Foo.class.getDeclaredField(FOO))));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOnly() throws Exception {
        fieldList.getOnly();
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(fieldList.subList(0, 1), is((FieldList) new FieldList.ForLoadedField(Foo.class.getDeclaredField(FOO))));
    }

    private static class Foo {

        private Object foo;

        private Object bar;
    }
}
