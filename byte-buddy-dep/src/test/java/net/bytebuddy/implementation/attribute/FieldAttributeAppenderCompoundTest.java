package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

public class FieldAttributeAppenderCompoundTest extends AbstractFieldAttributeAppenderTest {

    @Mock
    private FieldAttributeAppender.Factory firstFactory, secondFactory;

    @Mock
    private FieldAttributeAppender first, second;

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
        FieldAttributeAppender fieldAttributeAppender = new FieldAttributeAppender.Factory.Compound(firstFactory, secondFactory).make(instrumentedType);
        fieldAttributeAppender.apply(fieldVisitor, fieldDescription);
        verify(first).apply(fieldVisitor, fieldDescription);
        verifyNoMoreInteractions(first);
        verify(second).apply(fieldVisitor, fieldDescription);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAttributeAppender.Compound.class).apply();
    }
}
