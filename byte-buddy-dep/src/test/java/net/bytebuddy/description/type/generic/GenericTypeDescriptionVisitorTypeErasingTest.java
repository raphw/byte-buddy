package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class GenericTypeDescriptionVisitorTypeErasingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription genericTypeDescription, rawType;

    @Before
    public void setUp() throws Exception {
        when(genericTypeDescription.asRawType()).thenReturn(rawType);
    }

    @Test
    public void testGenericArray() throws Exception {
        assertThat(GenericTypeDescription.Visitor.TypeErasing.INSTANCE.onGenericArray(genericTypeDescription), is(rawType));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWildcard() throws Exception {
        GenericTypeDescription.Visitor.TypeErasing.INSTANCE.onWildcard(genericTypeDescription);
    }

    @Test
    public void testParameterized() throws Exception {
        assertThat(GenericTypeDescription.Visitor.TypeErasing.INSTANCE.onParameterizedType(genericTypeDescription), is(rawType));
    }

    @Test
    public void testTypeVariable() throws Exception {
        assertThat(GenericTypeDescription.Visitor.TypeErasing.INSTANCE.onTypeVariable(genericTypeDescription), is(rawType));
    }

    @Test
    public void testNonGeneric() throws Exception {
        assertThat(GenericTypeDescription.Visitor.TypeErasing.INSTANCE.onNonGenericType(genericTypeDescription), is(rawType));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.TypeErasing.class).apply();
    }
}