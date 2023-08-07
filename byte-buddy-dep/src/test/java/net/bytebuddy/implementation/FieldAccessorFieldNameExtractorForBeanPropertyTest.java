package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class FieldAccessorFieldNameExtractorForBeanPropertyTest {

    private static final String FOO = "foo", FOO_CAPITAL = "Foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testGetterMethod() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("get" + FOO_CAPITAL);
        assertThat(FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(methodDescription), is(FOO));
    }
    @Test
    public void testGetterMethodCapitalized() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("get" + FOO_CAPITAL);
        assertThat(FieldAccessor.FieldNameExtractor.ForBeanProperty.CAPITALIZED.resolve(methodDescription), is(FOO_CAPITAL));
    }

    @Test
    public void testSetterMethod() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("set" + FOO_CAPITAL);
        assertThat(FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(methodDescription), is(FOO));
    }

    @Test
    public void testSetterMethodCapitalized() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("set" + FOO_CAPITAL);
        assertThat(FieldAccessor.FieldNameExtractor.ForBeanProperty.CAPITALIZED.resolve(methodDescription), is(FOO_CAPITAL));
    }

    @Test
    public void testGetterMethodBooleanPrefix() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("is" + FOO_CAPITAL);
        assertThat(FieldAccessor.FieldNameExtractor.ForBeanProperty.INSTANCE.resolve(methodDescription), is(FOO));
    }

    @Test
    public void testGetterMethodBooleanPrefixCapitalized() throws Exception {
        when(methodDescription.getInternalName()).thenReturn("is" + FOO_CAPITAL);
        assertThat(FieldAccessor.FieldNameExtractor.ForBeanProperty.CAPITALIZED.resolve(methodDescription), is(FOO_CAPITAL));
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
}
