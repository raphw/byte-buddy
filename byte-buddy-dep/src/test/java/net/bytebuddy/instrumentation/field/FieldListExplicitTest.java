package net.bytebuddy.instrumentation.field;

import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FieldListExplicitTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldDescription firstFieldDescription, secondFieldDescription;

    private FieldList fieldList;

    @Before
    public void setUp() throws Exception {
        fieldList = new FieldList.Explicit(Arrays.asList(firstFieldDescription, secondFieldDescription));
    }

    @Test
    public void testFieldList() throws Exception {
        assertThat(fieldList.size(), is(2));
        assertThat(fieldList.get(0), is(firstFieldDescription));
        assertThat(fieldList.get(1), is(secondFieldDescription));
    }
// TODO
//    @Test
//    public void testFieldListNamed() throws Exception {
//        when(firstFieldDescription.getInternalName()).thenReturn(FOO);
//        when(secondFieldDescription.getInternalName()).thenReturn(BAR);
//        FieldDescription fieldDescription = fieldList.named(FOO);
//        assertThat(fieldDescription, is(firstFieldDescription));
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testNamedIllegal() throws Exception {
//        when(firstFieldDescription.getInternalName()).thenReturn(BAR);
//        when(secondFieldDescription.getInternalName()).thenReturn(BAR);
//        fieldList.named(FOO);
//    }

    @Test
    public void testSubList() throws Exception {
        assertThat(fieldList.subList(0, 1), is((FieldList) new FieldList.Explicit(Arrays.asList(firstFieldDescription))));
    }
}
