package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.type.RecordComponentDescription;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.RecordComponentVisitor;

public abstract class AbstractRecordComponentAttributeAppenderTest extends AbstractAttributeAppenderTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    protected RecordComponentVisitor recordComponentVisitor;

    @Mock
    protected RecordComponentDescription recordComponentDescription;
}
