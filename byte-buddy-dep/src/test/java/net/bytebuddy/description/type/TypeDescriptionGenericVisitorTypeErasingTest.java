package net.bytebuddy.description.type;

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

public class TypeDescriptionGenericVisitorTypeErasingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic typeDescription, rawType;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.asRawType()).thenReturn(rawType);
    }

    @Test
    public void testGenericArray() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.TypeErasing.INSTANCE.onGenericArray(typeDescription), is(rawType));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWildcard() throws Exception {
        TypeDescription.Generic.Visitor.TypeErasing.INSTANCE.onWildcard(typeDescription);
    }

    @Test
    public void testParameterized() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.TypeErasing.INSTANCE.onParameterizedType(typeDescription), is(rawType));
    }

    @Test
    public void testTypeVariable() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.TypeErasing.INSTANCE.onTypeVariable(typeDescription), is(rawType));
    }

    @Test
    public void testNonGeneric() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.TypeErasing.INSTANCE.onNonGenericType(typeDescription), is(rawType));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.TypeErasing.class).apply();
    }
}
