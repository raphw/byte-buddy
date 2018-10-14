package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDefinition;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class ArrayTypeMatcherTest extends AbstractElementMatcherTest<ArrayTypeMatcher<?>> {

    @Mock
    private TypeDefinition typeDefinition;

    @SuppressWarnings("unchecked")
    public ArrayTypeMatcherTest() {
        super((Class<ArrayTypeMatcher<?>>) (Object) ArrayTypeMatcher.class, "isArray");
    }

    @Test
    public void testIsArray() {
        when(typeDefinition.isArray()).thenReturn(true);
        assertThat(new ArrayTypeMatcher<TypeDefinition>().matches(typeDefinition), is(true));
    }

    @Test
    public void testIsNoArray() {
        assertThat(new ArrayTypeMatcher<TypeDefinition>().matches(typeDefinition), is(false));
    }
}