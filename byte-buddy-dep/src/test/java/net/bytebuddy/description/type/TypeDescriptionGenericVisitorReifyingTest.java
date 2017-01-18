package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorReifyingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic generic;

    @Test
    public void testParameterizedType() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Reifying.INSTANCE.onParameterizedType(generic), is(generic));
    }

    @Test
    public void testGenerifiedNonGenericType() throws Exception {
        when(generic.asErasure()).thenReturn(TypeDescription.OBJECT);
        assertThat(TypeDescription.Generic.Visitor.Reifying.INSTANCE.onNonGenericType(generic), is(generic));
    }

    @Test
    public void testNonGenerifiedNonGenericType() throws Exception {
        when(generic.asErasure()).thenReturn(new TypeDescription.ForLoadedType(Foo.class));
        assertThat(TypeDescription.Generic.Visitor.Reifying.INSTANCE.onNonGenericType(generic).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeVariable() throws Exception {
        TypeDescription.Generic.Visitor.Reifying.INSTANCE.onTypeVariable(generic);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenericArray() throws Exception {
        TypeDescription.Generic.Visitor.Reifying.INSTANCE.onGenericArray(generic);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWildcard() throws Exception {
        TypeDescription.Generic.Visitor.Reifying.INSTANCE.onWildcard(generic);
    }

    private static class Foo<T> {
        /* empty */
    }
}
