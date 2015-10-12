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
        when(prepend.wrapReader(FOO)).thenReturn(BAR);
        when(wrapper.wrapReader(BAR)).thenReturn(QUX);
        when(append.wrapReader(QUX)).thenReturn(BAZ);
        when(prepend.wrapWriter(FOO)).thenReturn(BAR);
        when(wrapper.wrapWriter(BAR)).thenReturn(QUX);
        when(append.wrapWriter(QUX)).thenReturn(BAZ);
    }

    @Test
    public void testWrapperChain() throws Exception {
        ClassVisitorWrapper.Chain chain = new ClassVisitorWrapper.Chain();
        chain = chain.append(wrapper).append(append).prepend(prepend);
        assertThat(chain.wrap(prependVisitor), is(resultVisitor));
        verify(prepend).wrap(prependVisitor);
        verifyNoMoreInteractions(prepend);
        verify(wrapper).wrap(wrapperVisitor);
        verifyNoMoreInteractions(wrapper);
        verify(append).wrap(appendVisitor);
        verifyNoMoreInteractions(append);
    }

    @Test
    public void testReaderHint() throws Exception {
        ClassVisitorWrapper.Chain chain = new ClassVisitorWrapper.Chain();
        chain = chain.append(wrapper).append(append).prepend(prepend);
        assertThat(chain.wrapReader(FOO), is(BAZ));
        verify(prepend).wrapReader(FOO);
        verifyNoMoreInteractions(prepend);
        verify(wrapper).wrapReader(BAR);
        verifyNoMoreInteractions(wrapper);
        verify(append).wrapReader(QUX);
        verifyNoMoreInteractions(append);
    }

    @Test
    public void testWriterHint() throws Exception {
        ClassVisitorWrapper.Chain chain = new ClassVisitorWrapper.Chain();
        chain = chain.append(wrapper).append(append).prepend(prepend);
        assertThat(chain.wrapWriter(FOO), is(BAZ));
        verify(prepend).wrapWriter(FOO);
        verifyNoMoreInteractions(prepend);
        verify(wrapper).wrapWriter(BAR);
        verifyNoMoreInteractions(wrapper);
        verify(append).wrapWriter(QUX);
        verifyNoMoreInteractions(append);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassVisitorWrapper.Chain.class).apply();
    }
}
