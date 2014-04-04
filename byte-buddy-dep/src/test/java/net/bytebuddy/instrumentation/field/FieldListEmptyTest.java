package net.bytebuddy.instrumentation.field;

import org.junit.Before;
import org.junit.Test;

import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FieldListEmptyTest {

    private static final String FOO = "foo";

    private FieldList fieldList;

    @Before
    public void setUp() throws Exception {
        fieldList = new FieldList.Empty();
    }

    @Test(expected = NoSuchElementException.class)
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
}
