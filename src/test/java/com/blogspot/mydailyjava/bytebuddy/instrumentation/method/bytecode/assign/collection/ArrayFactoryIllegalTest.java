package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.collection;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArrayFactoryIllegalTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArrayCreation() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.isArray()).thenReturn(false);
        ArrayFactory.of(typeDescription);
    }
}
