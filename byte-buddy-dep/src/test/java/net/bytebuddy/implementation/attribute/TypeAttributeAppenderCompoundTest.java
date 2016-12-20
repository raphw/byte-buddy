package net.bytebuddy.implementation.attribute;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderCompoundTest extends AbstractTypeAttributeAppenderTest {

    @Mock
    private TypeAttributeAppender first, second;

    @Test
    public void testApplication() throws Exception {
        TypeAttributeAppender typeAttributeAppender = new TypeAttributeAppender.Compound(first, second);
        typeAttributeAppender.apply(classVisitor, instrumentedType, annotationValueFilter);
        verify(first).apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyNoMoreInteractions(first);
        verify(second).apply(classVisitor, instrumentedType, annotationValueFilter);
        verifyNoMoreInteractions(second);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeAttributeAppender.Compound.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Collections.singletonList(mock(TypeAttributeAppender.class));
            }
        }).apply();
    }
}
