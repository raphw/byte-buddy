package net.bytebuddy.asm;

import net.bytebuddy.description.type.TypeDescription;
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

public class AsmVisitorWrapperCompoundTest {

    private static final int FOO = 1, BAR = 2, QUX = 3, BAZ = 4, FLAGS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private AsmVisitorWrapper wrapper, prepend, append;

    @Mock
    private ClassVisitor wrapperVisitor, prependVisitor, appendVisitor, resultVisitor;

    @Before
    public void setUp() throws Exception {
        when(prepend.wrap(instrumentedType, prependVisitor, FLAGS, FLAGS * 2)).thenReturn(wrapperVisitor);
        when(wrapper.wrap(instrumentedType, wrapperVisitor, FLAGS, FLAGS * 2)).thenReturn(appendVisitor);
        when(append.wrap(instrumentedType, appendVisitor, FLAGS, FLAGS * 2)).thenReturn(resultVisitor);
        when(prepend.mergeReader(FOO)).thenReturn(BAR);
        when(wrapper.mergeReader(BAR)).thenReturn(QUX);
        when(append.mergeReader(QUX)).thenReturn(BAZ);
        when(prepend.mergeWriter(FOO)).thenReturn(BAR);
        when(wrapper.mergeWriter(BAR)).thenReturn(QUX);
        when(append.mergeWriter(QUX)).thenReturn(BAZ);
    }

    @Test
    public void testWrapperChain() throws Exception {
        AsmVisitorWrapper.Compound compound = new AsmVisitorWrapper.Compound(prepend, wrapper, append);
        assertThat(compound.wrap(instrumentedType, prependVisitor, FLAGS, FLAGS * 2), is(resultVisitor));
        verify(prepend).wrap(instrumentedType, prependVisitor, FLAGS, FLAGS * 2);
        verifyNoMoreInteractions(prepend);
        verify(wrapper).wrap(instrumentedType, wrapperVisitor, FLAGS, FLAGS * 2);
        verifyNoMoreInteractions(wrapper);
        verify(append).wrap(instrumentedType, appendVisitor, FLAGS, FLAGS * 2);
        verifyNoMoreInteractions(append);
    }

    @Test
    public void testReaderFlags() throws Exception {
        AsmVisitorWrapper.Compound compound = new AsmVisitorWrapper.Compound(prepend, wrapper, append);
        assertThat(compound.mergeReader(FOO), is(BAZ));
        verify(prepend).mergeReader(FOO);
        verifyNoMoreInteractions(prepend);
        verify(wrapper).mergeReader(BAR);
        verifyNoMoreInteractions(wrapper);
        verify(append).mergeReader(QUX);
        verifyNoMoreInteractions(append);
    }

    @Test
    public void testWriterFlags() throws Exception {
        AsmVisitorWrapper.Compound compound = new AsmVisitorWrapper.Compound(prepend, wrapper, append);
        assertThat(compound.mergeWriter(FOO), is(BAZ));
        verify(prepend).mergeWriter(FOO);
        verifyNoMoreInteractions(prepend);
        verify(wrapper).mergeWriter(BAR);
        verifyNoMoreInteractions(wrapper);
        verify(append).mergeWriter(QUX);
        verifyNoMoreInteractions(append);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AsmVisitorWrapper.Compound.class).apply();
    }
}
