package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldLocatorNoOpTest {

    private static final String FOO = "foo";

    @Test
    public void testCannotLocateWithoutType() throws Exception {
        assertThat(FieldLocator.NoOp.INSTANCE.locate(FOO).isResolved(), is(false));
    }

    @Test
    public void testCannotLocateWithType() throws Exception {
        assertThat(FieldLocator.NoOp.INSTANCE.locate(FOO, TypeDescription.OBJECT).isResolved(), is(false));
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(FieldLocator.NoOp.INSTANCE.make(TypeDescription.OBJECT), is((FieldLocator) FieldLocator.NoOp.INSTANCE));
    }
}
