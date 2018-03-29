package net.bytebuddy.description.type;

import org.junit.Test;

import java.lang.reflect.Type;

import static org.mockito.Mockito.mock;

public class TypeDefinitionSortOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownType() throws Exception {
        TypeDefinition.Sort.describe(mock(Type.class));
    }
}
