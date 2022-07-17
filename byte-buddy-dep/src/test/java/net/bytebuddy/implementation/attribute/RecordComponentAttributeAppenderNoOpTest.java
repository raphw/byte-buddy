package net.bytebuddy.implementation.attribute;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class RecordComponentAttributeAppenderNoOpTest extends AbstractRecordComponentAttributeAppenderTest {

    @Test
    public void testApplication() throws Exception {
        RecordComponentAttributeAppender.NoOp.INSTANCE.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
        verifyNoMoreInteractions(recordComponentDescription);
        verifyNoMoreInteractions(recordComponentDescription);
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(RecordComponentAttributeAppender.NoOp.INSTANCE.make(instrumentedType), sameInstance((RecordComponentAttributeAppender) RecordComponentAttributeAppender.NoOp.INSTANCE));
        verifyNoMoreInteractions(instrumentedType);
    }
}
