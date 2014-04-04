package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;

public abstract class AbstractTypeAttributeAppenderTest extends AbstractAttributeAppenderTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    protected ClassVisitor classVisitor;
    @Mock
    protected TypeDescription typeDescription;
}
