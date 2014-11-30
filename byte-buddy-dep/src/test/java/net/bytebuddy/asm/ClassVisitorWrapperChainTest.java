package net.bytebuddy.asm;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ClassVisitorWrapperChainTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassVisitorWrapper first, prepend, append;
    @Mock
    private ClassVisitor classVisitor;

    @Before
    public void setUp() throws Exception {
        when(first.wrap(any(ClassVisitor.class))).thenReturn(classVisitor);
        when(prepend.wrap(any(ClassVisitor.class))).thenReturn(classVisitor);
        when(append.wrap(any(ClassVisitor.class))).thenReturn(classVisitor);
    }

    @Test
    public void testWrapperChain() throws Exception {
        ClassVisitorWrapper.Chain chain = new ClassVisitorWrapper.Chain();
        chain = chain.append(first).append(append).prepend(prepend);
        chain.wrap(classVisitor);
    }

    @After
    public void tearDown() throws Exception {
        verify(prepend).wrap(classVisitor);
        verifyNoMoreInteractions(prepend);
        verify(first).wrap(classVisitor);
        verifyNoMoreInteractions(first);
        verify(append).wrap(classVisitor);
        verifyNoMoreInteractions(append);
    }
}
