package com.blogspot.mydailyjava.bytebuddy.asm;

import org.junit.Test;
import org.objectweb.asm.ClassVisitor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ClassVisitorWrapperChainTest {

    @Test
    public void testWrapperChain() throws Exception {
        ClassVisitorWrapper.Chain chain = new ClassVisitorWrapper.Chain();
        ClassVisitorWrapper first = mock(ClassVisitorWrapper.class);
        ClassVisitorWrapper prepend = mock(ClassVisitorWrapper.class);
        ClassVisitorWrapper append = mock(ClassVisitorWrapper.class);
        chain = chain.append(first).append(append).prepend(prepend);
        ClassVisitor classVisitor = mock(ClassVisitor.class);
        when(first.wrap(any(ClassVisitor.class))).thenReturn(classVisitor);
        when(prepend.wrap(any(ClassVisitor.class))).thenReturn(classVisitor);
        when(append.wrap(any(ClassVisitor.class))).thenReturn(classVisitor);
        chain.wrap(classVisitor).visitEnd();
        verify(prepend).wrap(any(ClassVisitor.class));
        verifyNoMoreInteractions(prepend);
        verify(first).wrap(any(ClassVisitor.class));
        verifyNoMoreInteractions(first);
        verify(append).wrap(any(ClassVisitor.class));
        verifyNoMoreInteractions(append);
    }
}
