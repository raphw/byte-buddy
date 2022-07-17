package net.bytebuddy.implementation.attribute;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class MethodAttributeAppenderNoOpTest extends AbstractMethodAttributeAppenderTest {

    @Test
    public void testApplication() throws Exception {
        MethodAttributeAppender.NoOp.INSTANCE.apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(MethodAttributeAppender.NoOp.INSTANCE.make(instrumentedType), is((MethodAttributeAppender) MethodAttributeAppender.NoOp.INSTANCE));
        verifyNoMoreInteractions(instrumentedType);
    }
}
