package com.blogspot.mydailyjava.bytebuddy.instrumentation.field;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FieldDescriptionForLoadedFieldTest {

    private static final String FOO = "foo";

    private static class Foo {

        private Object foo;
    }

    private FieldDescription fieldDescription;

    @Before
    public void setUp() throws Exception {
        fieldDescription = new FieldDescription.ForLoadedField(Foo.class.getDeclaredField(FOO));
    }

    @Test
    public void testFieldName() throws Exception {
        assertThat(fieldDescription.getName(), is(FOO));
        assertThat(fieldDescription.getInternalName(), is(FOO));
    }

    @Test
    public void testFieldType() throws Exception {
        assertThat(fieldDescription.getFieldType().represents(Object.class), is(true));
    }

    @Test
    public void testDeclaringType() throws Exception {
        assertThat(fieldDescription.getDeclaringType().represents(Foo.class), is(true));
    }
}
