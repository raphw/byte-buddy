package net.bytebuddy.description.type;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeListGenericEmptyTest {

    @Test
    public void testRawTypes() throws Exception {
        assertThat(new TypeList.Generic.Empty().asErasures().size(), is(0));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(new TypeList.Generic.Empty().accept(TypeDescription.Generic.Visitor.NoOp.INSTANCE).size(), is(0));
    }

    @Test
    public void testSize() throws Exception {
        assertThat(new TypeList.Generic.Empty().getStackSize(), is(0));
    }
}
