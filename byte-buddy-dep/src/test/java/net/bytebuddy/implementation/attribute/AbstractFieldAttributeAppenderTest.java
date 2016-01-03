package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.FieldVisitor;

public abstract class AbstractFieldAttributeAppenderTest extends AbstractAttributeAppenderTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    protected FieldVisitor fieldVisitor;

    @Mock
    protected FieldDescription fieldDescription;
}
