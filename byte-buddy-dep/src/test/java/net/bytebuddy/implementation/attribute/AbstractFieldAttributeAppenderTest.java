package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import org.junit.Before;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.FieldVisitor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public abstract class AbstractFieldAttributeAppenderTest extends AbstractAttributeAppenderTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    protected FieldVisitor fieldVisitor;

    @Mock
    protected FieldDescription fieldDescription;
}
