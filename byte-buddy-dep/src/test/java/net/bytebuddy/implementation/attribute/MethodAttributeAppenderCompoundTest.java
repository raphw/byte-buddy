package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderCompoundTest extends AbstractMethodAttributeAppenderTest {

    @Mock
    private MethodAttributeAppender.Factory firstFactory, secondFactory;

    @Mock
    private MethodAttributeAppender first, second;

    @Mock
    private TypeDescription instrumentedType;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(firstFactory.make(instrumentedType)).thenReturn(first);
        when(secondFactory.make(instrumentedType)).thenReturn(second);
    }

    @Test
    public void testApplication() throws Exception {
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.Factory.Compound(firstFactory, secondFactory).make(instrumentedType);
        methodAttributeAppender.apply(methodVisitor, methodDescription);
        verify(first).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(first);
        verify(second).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAttributeAppender.Compound.class).apply();
    }
}
