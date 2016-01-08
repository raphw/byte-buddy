package net.bytebuddy.description.type;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeListEmptyTest {

    @Test
    public void testInternalName() throws Exception {
        assertThat(new TypeList.Empty().toInternalNames(), nullValue(String[].class));
    }

    @Test
    public void testSize() throws Exception {
        assertThat(new TypeList.Empty().getStackSize(), is(0));
    }
}
