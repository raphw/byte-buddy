package net.bytebuddy.implementation.attribute;

import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

public class RecordComponentAttributeAppenderCompoundTest extends AbstractRecordComponentAttributeAppenderTest {

    @Mock
    private RecordComponentAttributeAppender first, second;

    @Test
    public void testApplication() throws Exception {
        RecordComponentAttributeAppender recordComponentAttributeAppender = new RecordComponentAttributeAppender.Compound(first, second);
        recordComponentAttributeAppender.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verify(first).apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verifyNoMoreInteractions(first);
        verify(second).apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verifyNoMoreInteractions(second);
        verifyZeroInteractions(instrumentedType);
    }
}
