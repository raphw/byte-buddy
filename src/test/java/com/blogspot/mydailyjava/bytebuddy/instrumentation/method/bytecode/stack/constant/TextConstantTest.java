package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class TextConstantTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;


    @Test
    public void testTextValue() throws Exception {
        StackManipulation.Size size = new TextConstant(FOO).apply(methodVisitor, instrumentationContext);
         assertThat(size.getSizeImpact(), is(1));
         assertThat(size.getMaximalSize(), is(1));
         verify(methodVisitor).visitLdcInsn(FOO);
         verifyNoMoreInteractions(methodVisitor);
         verifyZeroInteractions(instrumentationContext);
    }
}
