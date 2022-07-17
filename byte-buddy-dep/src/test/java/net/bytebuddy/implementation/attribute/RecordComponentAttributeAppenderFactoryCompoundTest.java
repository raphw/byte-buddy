package net.bytebuddy.implementation.attribute;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

public class RecordComponentAttributeAppenderFactoryCompoundTest extends AbstractRecordComponentAttributeAppenderTest {

    @Mock
    private RecordComponentAttributeAppender.Factory firstFactory, secondFactory;

    @Mock
    private RecordComponentAttributeAppender first, second;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(firstFactory.make(instrumentedType)).thenReturn(first);
        when(secondFactory.make(instrumentedType)).thenReturn(second);
    }

    @Test
    public void testApplication() throws Exception {
        RecordComponentAttributeAppender recordComponentAttributeAppender = new RecordComponentAttributeAppender.Factory.Compound(firstFactory, secondFactory).make(instrumentedType);
        recordComponentAttributeAppender.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verify(first).apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verifyNoMoreInteractions(first);
        verify(second).apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verifyNoMoreInteractions(second);
        verifyNoMoreInteractions(instrumentedType);
    }
}
