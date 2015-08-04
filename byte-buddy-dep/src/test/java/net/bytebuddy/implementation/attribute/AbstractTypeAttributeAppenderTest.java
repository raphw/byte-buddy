package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;

public abstract class AbstractTypeAttributeAppenderTest extends AbstractAttributeAppenderTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    protected ClassVisitor classVisitor;

    @Mock
    protected TypeDescription typeDescription;

    @Mock
    protected GenericTypeDescription targetType;
}
