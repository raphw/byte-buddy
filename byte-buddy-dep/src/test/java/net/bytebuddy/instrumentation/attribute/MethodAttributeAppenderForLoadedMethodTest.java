package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForLoadedMethodTest extends AbstractMethodAttributeAppenderTest {

    private static final String BAR = "bar", TO_STRING = "toString";
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

    @Test
    public void testHashCodeEquals() throws Exception {
        Method otherMethod = Object.class.getDeclaredMethod(TO_STRING);
        assertThat(new MethodAttributeAppender.ForLoadedMethod(method).hashCode(),
                is(new MethodAttributeAppender.ForLoadedMethod(method).hashCode()));
        assertThat(new MethodAttributeAppender.ForLoadedMethod(method),
                is(new MethodAttributeAppender.ForLoadedMethod(method)));
        assertThat(new MethodAttributeAppender.ForLoadedMethod(method).hashCode(),
                not(is(new MethodAttributeAppender.ForLoadedMethod(otherMethod).hashCode())));
        assertThat(new MethodAttributeAppender.ForLoadedMethod(method),
                not(is(new MethodAttributeAppender.ForLoadedMethod(otherMethod))));
    }

    private static abstract class Foo {

        @Qux
        @Baz
        @QuxBaz
        protected abstract void bar(@Qux @Baz @QuxBaz Object o);
    }
}
