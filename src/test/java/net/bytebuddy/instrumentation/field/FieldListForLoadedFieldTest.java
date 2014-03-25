package net.bytebuddy.instrumentation.field;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FieldListForLoadedFieldTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static class Foo {

        private Object foo;

        private Object bar;
    }

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
    public void testFieldListNamed() throws Exception {
        assertThat(fieldList.named(FOO).getInternalName(), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNamedIllegal() throws Exception {
        fieldList.named(QUX);
    }
}
