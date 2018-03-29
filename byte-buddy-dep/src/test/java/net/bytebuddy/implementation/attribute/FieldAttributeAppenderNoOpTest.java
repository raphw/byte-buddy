package net.bytebuddy.implementation.attribute;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

public class FieldAttributeAppenderNoOpTest extends AbstractFieldAttributeAppenderTest {

    @Test
    public void testApplication() throws Exception {
        FieldAttributeAppender.NoOp.INSTANCE.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verifyZeroInteractions(fieldVisitor);
        verifyZeroInteractions(fieldDescription);
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(FieldAttributeAppender.NoOp.INSTANCE.make(instrumentedType), sameInstance((FieldAttributeAppender) FieldAttributeAppender.NoOp.INSTANCE));
        verifyZeroInteractions(instrumentedType);
    }
}
