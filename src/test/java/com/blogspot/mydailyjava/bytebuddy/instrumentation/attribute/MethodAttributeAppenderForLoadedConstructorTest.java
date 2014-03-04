package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Constructor;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForLoadedConstructorTest extends AbstractMethodAttributeAppenderTest {


    private static final String BAR = "bar";
    private static final int PARAMETER_INDEX = 0;

    private static abstract class Foo {

        @Qux
        @Baz
        @QuxBaz
        protected Foo(@Qux @Baz @QuxBaz Object o) {
            /* empty */
        }
    }

    private Constructor<?> constructor;

    @Before
    public void setUp() throws Exception {
        constructor = Foo.class.getDeclaredConstructor(Object.class);
        TypeList typeList = mock(TypeList.class);
        when(methodDescription.getParameterTypes()).thenReturn(typeList);
        when(typeList.size()).thenReturn(PARAMETER_INDEX + 1);
    }

    @Test
    public void testLoadedFieldAttributeAppender() throws Exception {
        MethodAttributeAppender methodAttributeAppender = new MethodAttributeAppender.ForLoadedConstructor(constructor);
        methodAttributeAppender.apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }
}
