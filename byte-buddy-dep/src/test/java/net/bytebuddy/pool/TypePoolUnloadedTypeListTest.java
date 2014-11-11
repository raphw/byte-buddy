package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TypePoolUnloadedTypeListTest {

    private TypeList typeList;

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
        typeList = typePool.describe(Sample.class.getName()).getInterfaces();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Test
    public void testFieldList() throws Exception {
        assertThat(typeList.size(), is(2));
        assertThat(typeList.get(0), is((TypeDescription) new TypeDescription.ForLoadedType(Serializable.class)));
        assertThat(typeList.get(1), is((TypeDescription) new TypeDescription.ForLoadedType(Runnable.class)));
    }

    @Test
    public void testToInternalName() throws Exception {
        assertThat(typeList.toInternalNames(), is(new String[]{Type.getInternalName(Serializable.class),
                Type.getInternalName(Runnable.class)}));
    }

    @Test
    public void testEmptyList() throws Exception {
        assertThat(typeList.subList(0, 0).toInternalNames(), nullValue(String[].class));
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(typeList.subList(0, 1), is((TypeList) new TypeList.ForLoadedType(Serializable.class)));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSubListOutOfBounds() throws Exception {
        typeList.subList(0, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListIllegal() throws Exception {
        typeList.subList(1, 0);
    }

    public static abstract class Sample implements Serializable, Runnable {
    }
}
