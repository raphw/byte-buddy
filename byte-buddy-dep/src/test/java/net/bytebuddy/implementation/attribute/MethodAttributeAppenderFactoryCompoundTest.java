package net.bytebuddy.implementation.attribute;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MethodAttributeAppenderFactoryCompoundTest extends AbstractMethodAttributeAppenderTest {

    @Mock
    private MethodAttributeAppender.Factory firstFactory, secondFactory;

    @Mock
    private MethodAttributeAppender first, second;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(firstFactory.make(instrumentedType)).thenReturn(first);
        when(secondFactory.make(instrumentedType)).thenReturn(second);
    }

    @Test
    public void testApplication() throws Exception {
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.Factory.Compound(firstFactory, secondFactory).make(instrumentedType);
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(first).apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyNoMoreInteractions(first);
        verify(second).apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyNoMoreInteractions(second);
        verifyNoMoreInteractions(instrumentedType);
    }
}
