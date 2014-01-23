package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class NextUnboundAsDefaultHandlerTest {

    private TypeDescription typeDescription;
    private MethodDescription source;
    private MethodDescription target;

    @Before
    public void setUp() throws Exception {
        typeDescription = mock(TypeDescription.class);
        source = mock(MethodDescription.class);
        target = mock(MethodDescription.class);
    }

    @Test
    public void testFullyUnannotated() throws Exception {
        when(source.getParameterTypes()).thenReturn(new Class<?>[] {Void.class, Void.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[0][0]);
        Iterator<Argument> iterator = Argument.NextUnboundAsDefaultHandler.INSTANCE.makeIterator(typeDescription, source, target);
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().value(), is(0));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().value(), is(1));
        assertThat(iterator.hasNext(), is(false));
        verify(source, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verifyZeroInteractions(typeDescription);
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoval() throws Exception {
        when(source.getParameterTypes()).thenReturn(new Class<?>[] {Void.class, Void.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[0][0]);
        Iterator<Argument> iterator = Argument.NextUnboundAsDefaultHandler.INSTANCE.makeIterator(typeDescription, source, target);
        assertThat(iterator.hasNext(), is(true));
        iterator.remove();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().value(), is(1));
        assertThat(iterator.hasNext(), is(false));
        iterator.remove();
    }

    @Test
    public void testPartlyAnnotatedOrdered() throws Exception {
        Argument indexZeroArgument = mock(Argument.class);
        when(indexZeroArgument.value()).thenReturn(0);
        doReturn(Argument.class).when(indexZeroArgument).annotationType();
        when(source.getParameterTypes()).thenReturn(new Class<?>[] {Void.class, Void.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[][] {{indexZeroArgument}, {}});
        Iterator<Argument> iterator = Argument.NextUnboundAsDefaultHandler.INSTANCE.makeIterator(typeDescription, source, target);
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().value(), is(1));
        assertThat(iterator.hasNext(), is(false));
        verify(source, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testPartlyAnnotatedUnordered() throws Exception {
        Argument indexOneArgument = mock(Argument.class);
        when(indexOneArgument.value()).thenReturn(1);
        doReturn(Argument.class).when(indexOneArgument).annotationType();
        when(source.getParameterTypes()).thenReturn(new Class<?>[] {Void.class, Void.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[][] {{indexOneArgument}, {}});
        Iterator<Argument> iterator = Argument.NextUnboundAsDefaultHandler.INSTANCE.makeIterator(typeDescription, source, target);
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().value(), is(0));
        assertThat(iterator.hasNext(), is(false));
        verify(source, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testFullyAnnotatedUnordered() throws Exception {
        Argument indexZeroArgument = mock(Argument.class);
        when(indexZeroArgument.value()).thenReturn(0);
        doReturn(Argument.class).when(indexZeroArgument).annotationType();
        Argument indexOneArgument = mock(Argument.class);
        when(indexOneArgument.value()).thenReturn(1);
        doReturn(Argument.class).when(indexOneArgument).annotationType();
        when(source.getParameterTypes()).thenReturn(new Class<?>[] {Void.class, Void.class});
        when(target.getParameterAnnotations()).thenReturn(new Annotation[][] {{indexOneArgument}, {indexZeroArgument}});
        Iterator<Argument> iterator = Argument.NextUnboundAsDefaultHandler.INSTANCE.makeIterator(typeDescription, source, target);
        assertThat(iterator.hasNext(), is(false));
        verify(source, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verifyZeroInteractions(typeDescription);
    }
}
