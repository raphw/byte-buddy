package net.bytebuddy.description.type;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorTypeGeneralizingTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic typeDescription, rawType;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.asRawType()).thenReturn(rawType);
    }

    @Test
    public void testGenericArray() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onGenericArray(typeDescription),
                is(TypeDefinition.Sort.describe(Object.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWildcard() throws Exception {
        TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onWildcard(typeDescription);
    }

    @Test
    public void testParameterized() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onParameterizedType(typeDescription),
                is(TypeDefinition.Sort.describe(Object.class)));
    }

    @Test
    public void testTypeVariable() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onTypeVariable(typeDescription),
                is(TypeDefinition.Sort.describe(Object.class)));
    }

    @Test
    public void testNonGeneric() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onNonGenericType(typeDescription),
                is(TypeDefinition.Sort.describe(Object.class)));
    }

    @Test
    public void testNonGenericPrimitive() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onNonGenericType(TypeDefinition.Sort.describe(int.class)),
                is(TypeDefinition.Sort.describe(int.class)));
    }
}
