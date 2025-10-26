package net.bytebuddy.description.type;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorReifyingTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic generic;

    @Test
    public void testInitiatingParameterizedType() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Reifying.INITIATING.onParameterizedType(generic), sameInstance(generic));
    }

    @Test
    public void testInitiatingGenerifiedNonGenericType() throws Exception {
        when(generic.asErasure()).thenReturn(TypeDescription.ForLoadedType.of(Object.class));
        assertThat(TypeDescription.Generic.Visitor.Reifying.INITIATING.onNonGenericType(generic), sameInstance(generic));
    }

    @Test
    public void testInitiatingNonGenerifiedNonGenericType() throws Exception {
        when(generic.asErasure()).thenReturn(TypeDescription.ForLoadedType.of(Foo.class));
        assertThat(TypeDescription.Generic.Visitor.Reifying.INITIATING.onNonGenericType(generic), not(sameInstance(generic)));
        assertThat(TypeDescription.Generic.Visitor.Reifying.INITIATING.onNonGenericType(generic).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitiatingTypeVariable() throws Exception {
        TypeDescription.Generic.Visitor.Reifying.INITIATING.onTypeVariable(generic);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitiatingGenericArray() throws Exception {
        TypeDescription.Generic.Visitor.Reifying.INITIATING.onGenericArray(generic);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitiatingWildcard() throws Exception {
        TypeDescription.Generic.Visitor.Reifying.INITIATING.onWildcard(generic);
    }

    @Test
    public void testInheritingParameterizedType() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Reifying.INHERITING.onParameterizedType(generic), not(sameInstance(generic)));
    }

    @Test
    public void testInheritingGenerifiedNonGenericType() throws Exception {
        when(generic.asErasure()).thenReturn(TypeDescription.ForLoadedType.of(Object.class));
        assertThat(TypeDescription.Generic.Visitor.Reifying.INHERITING.onNonGenericType(generic), sameInstance(generic));
    }

    @Test
    public void testInheritingNonGenerifiedNonGenericType() throws Exception {
        when(generic.asErasure()).thenReturn(TypeDescription.ForLoadedType.of(Foo.class));
        assertThat(TypeDescription.Generic.Visitor.Reifying.INHERITING.onNonGenericType(generic), not(sameInstance(generic)));
        assertThat(TypeDescription.Generic.Visitor.Reifying.INHERITING.onNonGenericType(generic).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInheritingTypeVariable() throws Exception {
        TypeDescription.Generic.Visitor.Reifying.INHERITING.onTypeVariable(generic);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInheritingGenericArray() throws Exception {
        TypeDescription.Generic.Visitor.Reifying.INHERITING.onGenericArray(generic);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInheritingWildcard() throws Exception {
        TypeDescription.Generic.Visitor.Reifying.INHERITING.onWildcard(generic);
    }

    private static class Foo<T> {
        /* empty */
    }
}
