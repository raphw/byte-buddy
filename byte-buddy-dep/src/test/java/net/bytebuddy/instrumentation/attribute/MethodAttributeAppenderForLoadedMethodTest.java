package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForLoadedMethodTest extends AbstractMethodAttributeAppenderTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int PARAMETER_INDEX = 0;

    private Method method;

    @Before
    public void setUp() throws Exception {
        method = Foo.class.getDeclaredMethod(BAR, Object.class);
        ParameterList parameters = mock(ParameterList.class);
        when(methodDescription.getParameters()).thenReturn(parameters);
        when(parameters.size()).thenReturn(PARAMETER_INDEX + 1);
    }

    @Test
    public void testMakeReturnsSameInstance() throws Exception {
        assertThat(new MethodAttributeAppender.ForLoadedMethod(method).make(mock(TypeDescription.class)),
                is((MethodAttributeAppender) new MethodAttributeAppender.ForLoadedMethod(method)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalApplicationThrowsException() throws Exception {
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty());
        new MethodAttributeAppender.ForLoadedMethod(method).apply(methodVisitor, methodDescription);
    }

    @Test
    public void testLoadedFieldAttributeAppender() throws Exception {
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.ForLoadedMethod(method);
        methodAttributeAppender.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Method> iterator = Arrays.asList(Sample.class.getDeclaredMethod(FOO), Sample.class.getDeclaredMethod(FOO, Void.class)).iterator();
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForLoadedMethod.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return iterator.next();
            }
        }).apply();
    }

    private static abstract class Foo {

        @Qux
        @Baz
        @QuxBaz
        protected abstract void bar(@Qux @Baz @QuxBaz Object o);
    }

    private static class Sample {

        private void foo() {
        }

        private void foo(Void v) {
        }
    }
}
