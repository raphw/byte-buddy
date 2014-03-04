package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.FieldVisitor;

public abstract class AbstractFieldAttributeAppenderTest extends AbstractAttributeAppenderTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    protected FieldVisitor fieldVisitor;
    @Mock
    protected FieldDescription fieldDescription;
}
