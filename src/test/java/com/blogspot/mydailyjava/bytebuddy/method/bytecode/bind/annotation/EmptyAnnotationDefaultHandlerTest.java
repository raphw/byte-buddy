package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class EmptyAnnotationDefaultHandlerTest {

    @Test(expected = NoSuchElementException.class)
    public void testEmptyIteration() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        MethodDescription left = mock(MethodDescription.class);
        MethodDescription right = mock(MethodDescription.class);
        Iterator<?> iterator = AnnotationDrivenBinder.AnnotationDefaultHandler.Empty.INSTANCE.makeIterator(typeDescription, left, right);
        assertThat(iterator.hasNext(), is(false));
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(left);
        verifyZeroInteractions(right);
        iterator.next();
    }
}
