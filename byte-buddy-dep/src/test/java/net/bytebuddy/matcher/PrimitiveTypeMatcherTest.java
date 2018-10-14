package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDefinition;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class PrimitiveTypeMatcherTest extends AbstractElementMatcherTest<PrimitiveTypeMatcher<?>> {

    @Mock
    private TypeDefinition typeDefinition;

    @SuppressWarnings("unchecked")
    public PrimitiveTypeMatcherTest() {
        super((Class<PrimitiveTypeMatcher<?>>) (Object) PrimitiveTypeMatcher.class, "isPrimitive");
    }

    @Test
    public void testIsPrimitive() {
        when(typeDefinition.isPrimitive()).thenReturn(true);
        assertThat(new PrimitiveTypeMatcher<TypeDefinition>().matches(typeDefinition), is(true));
    }

    @Test
    public void testIsNotPrimitive() {
        assertThat(new PrimitiveTypeMatcher<TypeDefinition>().matches(typeDefinition), is(false));
    }
}
