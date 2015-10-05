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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class GenericTypeDescriptionVisitorTypeErasingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription genericTypeDescription;

    @Mock
    private TypeDescription typeDescription;

    @Before
    public void setUp() throws Exception {
        when(genericTypeDescription.asErasure()).thenReturn(typeDescription);
    }

    @Test
    public void testGenericArray() throws Exception {
        assertThat(GenericTypeDescription.Visitor.TypeErasing.INSTANCE.onGenericArray(genericTypeDescription), is(typeDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWildcard() throws Exception {
        GenericTypeDescription.Visitor.TypeErasing.INSTANCE.onWildcard(genericTypeDescription);
    }

    @Test
    public void testParameterized() throws Exception {
        assertThat(GenericTypeDescription.Visitor.TypeErasing.INSTANCE.onParameterizedType(genericTypeDescription), is(typeDescription));
    }

    @Test
    public void testTypeVariable() throws Exception {
        assertThat(GenericTypeDescription.Visitor.TypeErasing.INSTANCE.onTypeVariable(genericTypeDescription), is(typeDescription));
    }

    @Test
    public void testNonGeneric() throws Exception {
        assertThat(GenericTypeDescription.Visitor.TypeErasing.INSTANCE.onNonGenericType(genericTypeDescription), is(typeDescription));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.TypeErasing.class).apply();
    }
}