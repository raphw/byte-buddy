package net.bytebuddy.description.type;

import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

public class TypeListForLoadedTypeTest {

    private TypeList typeList;

    @Before
    public void setUp() throws Exception {
        typeList = new TypeList.ForLoadedType(Object.class, Integer.class, long.class);
    }

    @Test
    public void testRetrieval() throws Exception {
        assertThat(typeList.size(), is(3));
        assertThat(typeList.get(0), equalTo((TypeDescription) new TypeDescription.ForLoadedType(Object.class)));
        assertThat(typeList.get(1), equalTo((TypeDescription) new TypeDescription.ForLoadedType(Integer.class)));
        assertThat(typeList.get(2), equalTo((TypeDescription) new TypeDescription.ForLoadedType(long.class)));
        assertThat(typeList.getStackSize(), is(4));
    }

    @Test
    public void testInternalNames() throws Exception {
        String[] internalName = typeList.toInternalNames();
        assertThat(internalName[0], is(Type.getInternalName(Object.class)));
        assertThat(internalName[1], is(Type.getInternalName(Integer.class)));
        assertThat(internalName[2], is(Type.getInternalName(long.class)));
    }

    @Test
    public void testMethodListFilter() throws Exception {
        typeList = typeList.filter(ElementMatchers.is(Object.class));
        assertThat(typeList.size(), is(1));
        assertThat(typeList.getOnly(), is((TypeDescription) new TypeDescription.ForLoadedType(Object.class)));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOnly() throws Exception {
        typeList.getOnly();
    }

    @Test
    public void testEmptyList() throws Exception {
        TypeList typeList = new TypeList.ForLoadedType();
        assertThat(typeList.isEmpty(), is(true));
        assertThat(typeList.toInternalNames(), nullValue(String[].class));
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(typeList.subList(0, 1), is((TypeList) new TypeList.ForLoadedType(Object.class)));
    }

    @Test
    public void testStackSize() throws Exception {
        assertThat(typeList.getStackSize(), is(4));
    }
}
