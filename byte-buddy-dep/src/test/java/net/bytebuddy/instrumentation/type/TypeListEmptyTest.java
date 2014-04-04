package net.bytebuddy.instrumentation.type;

import org.junit.Before;
import org.junit.Test;

import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class TypeListEmptyTest {

    private TypeList typeList;

    @Before
    public void setUp() throws Exception {
        typeList = new TypeList.Empty();
    }

    @Test(expected = NoSuchElementException.class)
    public void testGet() throws Exception {
        typeList.get(0);
    }

    @Test
    public void testSize() throws Exception {
        assertThat(typeList.size(), is(0));
    }

    @Test
    public void testToInternalNames() throws Exception {
        assertThat(typeList.toInternalNames(), nullValue());
    }

    @Test
    public void testGetStackSize() throws Exception {
        assertThat(typeList.getStackSize(), is(0));
    }
}
