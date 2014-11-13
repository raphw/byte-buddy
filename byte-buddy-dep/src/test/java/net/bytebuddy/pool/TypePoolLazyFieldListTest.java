package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TypePoolLazyFieldListTest {

    private FieldList fieldList;

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
        fieldList = typePool.describe(Sample.class.getName()).getDeclaredFields();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Test
    public void testFieldList() throws Exception {
        assertThat(fieldList.size(), is(2));
        assertThat(fieldList.get(0), is((FieldDescription) new FieldDescription.ForLoadedField(Sample.class.getDeclaredField("first"))));
        assertThat(fieldList.get(1), is((FieldDescription) new FieldDescription.ForLoadedField(Sample.class.getDeclaredField("second"))));
    }

    @Test
    public void testFieldListNamed() throws Exception {
        FieldDescription fieldDescription = fieldList.named("first");
        assertThat(fieldDescription, is(is((FieldDescription) new FieldDescription.ForLoadedField(Sample.class.getDeclaredField("first")))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNamedIllegal() throws Exception {
        fieldList.named("foo");
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(fieldList.subList(0, 1), is((FieldList) new FieldList.Explicit(Arrays.asList(fieldList.get(0)))));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSubListOutOfBounds() throws Exception {
        fieldList.subList(0, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListIllegal() throws Exception {
        fieldList.subList(1, 0);
    }

    public static class Sample {

        int first;

        Void second;
    }
}
