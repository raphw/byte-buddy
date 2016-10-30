package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class FieldAccessorFieldNameExtractorForBeanPropertyTest {

    private static final String FOO = "foo", FOO_CAPITAL = "Foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testGetterMethod() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("get" + FOO_CAPITAL);
        assertThat(FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(methodDescription), is(FOO));
    }

    @Test
    public void testSetterMethod() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("set" + FOO_CAPITAL);
        assertThat(FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(methodDescription), is(FOO));
    }

    @Test
    public void testGetterMethodBooleanPrefix() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("is" + FOO_CAPITAL);
        assertThat(FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(methodDescription), is(FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyGetter() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("get");
        FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptySetter() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("set");
        FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyGetterBooleanPrefix() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("is");
        FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalName() throws Exception {
        when(methodDescription.getInternalName()).thenReturn(FOO);
        FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(methodDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAccessor.FieldNameExtractor.ForBeanProperty.class).apply();
    }
}
