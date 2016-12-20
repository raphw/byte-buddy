package net.bytebuddy.implementation.attribute;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class FieldAttributeAppenderFactoryCompoundTest extends AbstractFieldAttributeAppenderTest {

    @Mock
    private FieldAttributeAppender.Factory firstFactory, secondFactory;

    @Mock
    private FieldAttributeAppender first, second;

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
        fieldAttributeAppender.apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verify(first).apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verifyNoMoreInteractions(first);
        verify(second).apply(fieldVisitor, fieldDescription, annotationValueFilter);
        verifyNoMoreInteractions(second);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAttributeAppender.Factory.Compound.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Collections.singletonList(mock(FieldAttributeAppender.Factory.class));
            }
        }).apply();
    }
}
