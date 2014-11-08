package net.bytebuddy.instrumentation.field;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FieldListEmptyTest {

    private static final String FOO = "foo";

    private FieldList fieldList;

    @Before
    public void setUp() throws Exception {
        fieldList = new FieldList.Empty();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGet() throws Exception {
        fieldList.get(0);
    }

    @Test
    public void testSize() throws Exception {
        assertThat(fieldList.size(), is(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNamed() throws Exception {
        fieldList.named(FOO);
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(fieldList.subList(0, 0), is(fieldList));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSubListOutOfBounds() throws Exception {
        fieldList.subList(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListIllegal() throws Exception {
        fieldList.subList(1, 0);
    }
}
