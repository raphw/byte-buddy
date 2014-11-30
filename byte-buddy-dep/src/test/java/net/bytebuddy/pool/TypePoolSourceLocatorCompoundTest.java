package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypePoolSourceLocatorCompoundTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypePool.SourceLocator first, second;

    @Mock
    private TypeDescription.BinaryRepresentation legalBinary, illegalBinary;


    @Before
    public void setUp() throws Exception {
        when(first.locate(any(String.class))).thenReturn(illegalBinary);
        when(first.locate(FOO)).thenReturn(legalBinary);
        when(second.locate(any(String.class))).thenReturn(illegalBinary);
        when(second.locate(BAR)).thenReturn(legalBinary);
        when(legalBinary.isValid()).thenReturn(true);
        when(illegalBinary.isValid()).thenReturn(false);
    }

    @Test
    public void testCompoundNoMatch() throws Exception {
        TypePool.SourceLocator sourceLocator = new TypePool.SourceLocator.Compound(first, second);
        assertThat(sourceLocator.locate(QUX).isValid(), is(false));
        verify(first).locate(QUX);
        verifyNoMoreInteractions(first);
        verify(second).locate(QUX);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundAppliesFirst() throws Exception {
        TypePool.SourceLocator sourceLocator = new TypePool.SourceLocator.Compound(first, second);
        assertThat(sourceLocator.locate(FOO).isValid(), is(true));
        verify(first).locate(FOO);
        verifyNoMoreInteractions(first);
        verifyZeroInteractions(second);
    }

    @Test
    public void testCompoundAppliesSecond() throws Exception {
        TypePool.SourceLocator sourceLocator = new TypePool.SourceLocator.Compound(first, second);
        assertThat(sourceLocator.locate(BAR).isValid(), is(true));
        verify(first).locate(BAR);
        verifyNoMoreInteractions(first);
        verify(second).locate(BAR);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundCompetition() throws Exception {
        when(first.locate(BAR)).thenReturn(legalBinary);
        TypePool.SourceLocator sourceLocator = new TypePool.SourceLocator.Compound(first, second);
        assertThat(sourceLocator.locate(BAR).isValid(), is(true));
        verify(first).locate(BAR);
        verifyNoMoreInteractions(first);
        verifyZeroInteractions(second);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.SourceLocator.Compound.class).apply();
    }
}
