package net.bytebuddy.instrumentation.type;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class TypeListEmptyTest {

    private TypeList typeList;

    @Before
    public void setUp() throws Exception {
        typeList = new TypeList.Empty();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGet() throws Exception {
        typeList.get(0);
    }

    @Test
    public void testSize() throws Exception {
        assertThat(typeList.size(), is(0));
    }

    @Test
    public void testToInternalNames() throws Exception {
        assertThat(typeList.toInternalNames(), nullValue(String[].class));
    }

    @Test
    public void testGetStackSize() throws Exception {
        assertThat(typeList.getStackSize(), is(0));
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(typeList.subList(0, 0), is(typeList));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSubListOutOfBounds() throws Exception {
        typeList.subList(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListIllegal() throws Exception {
        typeList.subList(1, 0);
    }

    @Test
    public void testStackSize() throws Exception {
        assertThat(typeList.getStackSize(), is(0));
    }
}
