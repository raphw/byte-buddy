package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.type.RecordComponentDescription;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class TypeWriterRecordComponentPoolDisabledTest {

    @Test(expected = IllegalStateException.class)
    public void testCannotLookupRecordComponent() {
        TypeWriter.RecordComponentPool.Disabled.INSTANCE.target(mock(RecordComponentDescription.class));
    }
}
