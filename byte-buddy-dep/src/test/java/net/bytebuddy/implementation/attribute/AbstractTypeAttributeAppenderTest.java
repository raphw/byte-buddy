package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.junit.Before;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;

import static org.mockito.Mockito.when;

public abstract class AbstractTypeAttributeAppenderTest extends AbstractAttributeAppenderTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    protected ClassVisitor classVisitor;

    @Mock
    protected TypeDescription typeDescription;

    @Mock
    protected GenericTypeDescription genericTypeDescription;

    @Mock
    protected GenericTypeDescription targetType;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(typeDescription.asGenericType()).thenReturn(genericTypeDescription);
    }
}
