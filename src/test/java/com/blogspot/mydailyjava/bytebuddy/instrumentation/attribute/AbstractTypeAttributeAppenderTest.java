package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;

public abstract class AbstractTypeAttributeAppenderTest extends AbstractAttributeAppenderTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    protected ClassVisitor classVisitor;
    @Mock
    protected TypeDescription typeDescription;
}
