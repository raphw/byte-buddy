package net.bytebuddy.asm;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassVisitorWrapperChainTest {

    private static final int FOO = 1, BAR = 2, QUX = 3, BAZ = 4;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassVisitorWrapper wrapper, prepend, append;

    @Mock
    private ClassVisitor wrapperVisitor, prependVisitor, appendVisitor, resultVisitor;

    @Before
    public void setUp() throws Exception {
        when(prepend.wrap(prependVisitor)).thenReturn(wrapperVisitor);
        when(wrapper.wrap(wrapperVisitor)).thenReturn(appendVisitor);
        when(append.wrap(appendVisitor)).thenReturn(resultVisitor);
        when(prepend.mergeReader(FOO)).thenReturn(BAR);
        when(wrapper.mergeReader(BAR)).thenReturn(QUX);
        when(append.mergeReader(QUX)).thenReturn(BAZ);
        when(prepend.mergeWriter(FOO)).thenReturn(BAR);
        when(wrapper.mergeWriter(BAR)).thenReturn(QUX);
        when(append.mergeWriter(QUX)).thenReturn(BAZ);
    }

    @Test
    public void testWrapperChain() throws Exception {
        ClassVisitorWrapper.Chain chain = new ClassVisitorWrapper.Chain(prepend, wrapper, append);
        assertThat(chain.wrap(prependVisitor), is(resultVisitor));
        verify(prepend).wrap(prependVisitor);
        verifyNoMoreInteractions(prepend);
        verify(wrapper).wrap(wrapperVisitor);
        verifyNoMoreInteractions(wrapper);
        verify(append).wrap(appendVisitor);
        verifyNoMoreInteractions(append);
    }

    @Test
    public void testReaderFlags() throws Exception {
        ClassVisitorWrapper.Chain chain = new ClassVisitorWrapper.Chain(prepend, wrapper, append);
        assertThat(chain.mergeReader(FOO), is(BAZ));
        verify(prepend).mergeReader(FOO);
        verifyNoMoreInteractions(prepend);
        verify(wrapper).mergeReader(BAR);
        verifyNoMoreInteractions(wrapper);
        verify(append).mergeReader(QUX);
        verifyNoMoreInteractions(append);
    }

    @Test
    public void testWriterFlags() throws Exception {
        ClassVisitorWrapper.Chain chain = new ClassVisitorWrapper.Chain(prepend, wrapper, append);
        assertThat(chain.mergeWriter(FOO), is(BAZ));
        verify(prepend).mergeWriter(FOO);
        verifyNoMoreInteractions(prepend);
        verify(wrapper).mergeWriter(BAR);
        verifyNoMoreInteractions(wrapper);
        verify(append).mergeWriter(QUX);
        verifyNoMoreInteractions(append);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassVisitorWrapper.Chain.class).apply();
    }
}
