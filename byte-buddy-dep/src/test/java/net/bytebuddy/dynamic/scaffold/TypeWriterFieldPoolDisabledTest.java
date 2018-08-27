package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class TypeWriterFieldPoolDisabledTest {

    @Test(expected = IllegalStateException.class)
    public void testCannotLookupField() {
        TypeWriter.FieldPool.Disabled.INSTANCE.target(mock(FieldDescription.class));
    }
}
