package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForLoadedMethodTest extends AbstractMethodAttributeAppenderTest {

    private static final String FOO = "foo", BAR = "bar";
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

        private void foo (Void v) {
        }
    }
}
