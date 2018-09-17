package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HasSuperTypeMatcherTest extends AbstractElementMatcherTest<HasSuperTypeMatcher<?>> {

    @Mock
    private ElementMatcher<? super TypeDescription.Generic> typeMatcher;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private TypeDescription.Generic superType, interfaceType, implicitInterfaceType;

    @Before
    public void setUp() throws Exception {
        when(superType.asGenericType()).thenReturn(superType);
        when(interfaceType.asGenericType()).thenReturn(interfaceType);
        when(interfaceType.asErasure()).thenReturn(mock(TypeDescription.class));
        when(implicitInterfaceType.asGenericType()).thenReturn(implicitInterfaceType);
        when(implicitInterfaceType.asErasure()).thenReturn(mock(TypeDescription.class));
        when(typeDescription.iterator()).thenReturn(Collections.<TypeDefinition>singletonList(superType).iterator());
        when(superType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(interfaceType));
        when(interfaceType.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(implicitInterfaceType));
        when(implicitInterfaceType.getInterfaces()).thenReturn(new TypeList.Generic.Empty());
    }

    @SuppressWarnings("unchecked")
    public HasSuperTypeMatcherTest() {
        super((Class<HasSuperTypeMatcher<?>>) (Object) HasSuperTypeMatcher.class, "hasSuperType");
    }

    @Test
    public void testMatchSuperClass() throws Exception {
        when(typeMatcher.matches(superType)).thenReturn(true);
        assertThat(new HasSuperTypeMatcher<TypeDescription>(typeMatcher).matches(typeDescription), is(true));
    }

    @Test
    public void testMatchSuperInterface() throws Exception {
        when(typeMatcher.matches(interfaceType)).thenReturn(true);
        assertThat(new HasSuperTypeMatcher<TypeDescription>(typeMatcher).matches(typeDescription), is(true));
    }

    @Test
    public void testMatchSuperInterfaceImplicit() throws Exception {
        when(typeMatcher.matches(implicitInterfaceType)).thenReturn(true);
        assertThat(new HasSuperTypeMatcher<TypeDescription>(typeMatcher).matches(typeDescription), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        assertThat(new HasSuperTypeMatcher<TypeDescription>(typeMatcher).matches(typeDescription), is(false));
    }
}
