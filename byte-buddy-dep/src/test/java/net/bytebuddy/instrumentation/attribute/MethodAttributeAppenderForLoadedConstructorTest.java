package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Constructor;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForLoadedConstructorTest extends AbstractMethodAttributeAppenderTest {

    private static final String BAR = "bar";
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
    public void testHashCodeEquals() throws Exception {
        Constructor<?> otherConstructor = Object.class.getDeclaredConstructor();
        assertThat(new MethodAttributeAppender.ForLoadedConstructor(constructor).hashCode(),
                is(new MethodAttributeAppender.ForLoadedConstructor(constructor).hashCode()));
        assertThat(new MethodAttributeAppender.ForLoadedConstructor(constructor),
                is(new MethodAttributeAppender.ForLoadedConstructor(constructor)));
        assertThat(new MethodAttributeAppender.ForLoadedConstructor(constructor).hashCode(),
                not(is(new MethodAttributeAppender.ForLoadedConstructor(otherConstructor).hashCode())));
        assertThat(new MethodAttributeAppender.ForLoadedConstructor(constructor),
                not(is(new MethodAttributeAppender.ForLoadedConstructor(otherConstructor))));
    }

    private static abstract class Foo {

        @Qux
        @Baz
        @QuxBaz
        protected Foo(@Qux @Baz @QuxBaz Object o) {
            /* empty */
        }
    }
}
