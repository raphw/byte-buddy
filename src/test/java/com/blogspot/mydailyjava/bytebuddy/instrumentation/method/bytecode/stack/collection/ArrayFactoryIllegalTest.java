package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.collection;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.test.MockitoRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ArrayFactoryIllegalTest {

    @Rule
    public TestRule mockRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @After
    public void tearDown() throws Exception {
        verify(typeDescription).isArray();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArrayCreation() throws Exception {
        when(typeDescription.isArray()).thenReturn(false);
        ArrayFactory.of(typeDescription);
    }
}
