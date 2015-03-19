package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.method.MethodDescription;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

public class AbstractMethodAttributeAppenderTest extends AbstractAttributeAppenderTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    protected MethodVisitor methodVisitor;

    @Mock
    protected MethodDescription methodDescription;
}
