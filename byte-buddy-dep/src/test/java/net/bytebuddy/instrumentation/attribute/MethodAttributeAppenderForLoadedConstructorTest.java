package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForLoadedConstructorTest extends AbstractMethodAttributeAppenderTest {

    private static final int PARAMETER_INDEX = 0;
    private Constructor<?> constructor;

    @Before
    public void setUp() throws Exception {
        constructor = Foo.class.getDeclaredConstructor(Object.class);
        TypeList typeList = mock(TypeList.class);
        when(methodDescription.getParameterTypes()).thenReturn(typeList);
        when(typeList.size()).thenReturn(PARAMETER_INDEX + 1);
    }

    @Test
    public void testMakeReturnsSameInstance() throws Exception {
        assertThat(new MethodAttributeAppender.ForLoadedConstructor(constructor).make(mock(TypeDescription.class)),
                is((MethodAttributeAppender) new MethodAttributeAppender.ForLoadedConstructor(constructor)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalApplicationThrowsException() throws Exception {
        when(methodDescription.getParameterTypes()).thenReturn(new TypeList.Empty());
        new MethodAttributeAppender.ForLoadedConstructor(constructor).apply(methodVisitor, methodDescription);
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

    @Test
    public void testObjectProperties() throws Exception {
        Constructor<?> first = Sample.class.getDeclaredConstructor(), second = Sample.class.getDeclaredConstructor(Void.class);
        final Iterator<Constructor<?>> iterator = Arrays.<Constructor<?>>asList(first, second).iterator();
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForLoadedConstructor.class).create(new ObjectPropertyAssertion.Creator<Constructor<?>>() {
            @Override
            public Constructor<?> create() {
                return iterator.next();
            }
        }).apply();
    }

    private static abstract class Foo {

        @Qux
        @Baz
        @QuxBaz
        protected Foo(@Qux @Baz @QuxBaz Object o) {
            /* empty */
        }
    }

    private static class Sample {

        private Sample() {
        }

        private Sample(Void v) {
        }
    }
}
