package net.bytebuddy.description.type.generic;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GenericTypeListEmptyTest {

    @Test
    public void testRawTypes() throws Exception {
        assertThat(new GenericTypeList.Empty().asErasures().size(), is(0));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(new GenericTypeList.Empty().accept(GenericTypeDescription.Visitor.NoOp.INSTANCE).size(), is(0));
    }

    @Test
    public void testSize() throws Exception {
        assertThat(new GenericTypeList.Empty().getStackSize(), is(0));
    }
}
