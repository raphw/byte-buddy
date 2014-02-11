package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType0;
import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class EmptyDefaultProviderTest {

    @Test(expected = NoSuchElementException.class)
    public void testEmptyIteration() throws Exception {
        InstrumentedType0 typeDescription = mock(InstrumentedType0.class);
        MethodDescription left = mock(MethodDescription.class);
        MethodDescription right = mock(MethodDescription.class);
        Iterator<?> iterator = AnnotationDrivenBinder.DefaultProvider.Empty.INSTANCE.makeIterator(typeDescription, left, right);
        assertThat(iterator.hasNext(), is(false));
        verifyZeroInteractions(typeDescription);
        verifyZeroInteractions(left);
        verifyZeroInteractions(right);
        iterator.next();
    }
}
