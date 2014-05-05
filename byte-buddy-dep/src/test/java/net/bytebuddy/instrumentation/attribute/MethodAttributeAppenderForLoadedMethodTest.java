package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForLoadedMethodTest extends AbstractMethodAttributeAppenderTest {

    private static final String BAR = "bar";
    private static final int PARAMETER_INDEX = 0;
    private Method method;

    @Before
    public void setUp() throws Exception {
        method = Foo.class.getDeclaredMethod(BAR, Object.class);
        TypeList typeList = mock(TypeList.class);
        when(methodDescription.getParameterTypes()).thenReturn(typeList);
        when(typeList.size()).thenReturn(PARAMETER_INDEX + 1);
    }

    @Test
    public void testLoadedFieldAttributeAppender() throws Exception {
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.ForLoadedMethod(method);
        methodAttributeAppender.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }

    private static abstract class Foo {

        @Qux
        @Baz
        @QuxBaz
        protected abstract void bar(@Qux @Baz @QuxBaz Object o);
    }
}
