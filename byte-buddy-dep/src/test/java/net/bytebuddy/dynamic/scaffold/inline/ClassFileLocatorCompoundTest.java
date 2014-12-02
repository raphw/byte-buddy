package net.bytebuddy.dynamic.scaffold.inline;

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

public class ClassFileLocatorCompoundTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileLocator classFileLocator, otherClassFileLocator;
    @Mock
    private TypeDescription.BinaryRepresentation legal, illegal;

    @Before
    public void setUp() throws Exception {
        when(legal.isValid()).thenReturn(true);
    }

    @Test
    public void testApplicationOrderCallsSecond() throws Exception {
        when(classFileLocator.classFileFor(FOO)).thenReturn(illegal);
        when(otherClassFileLocator.classFileFor(FOO)).thenReturn(legal);
        assertThat(new ClassFileLocator.Compound(classFileLocator, otherClassFileLocator).classFileFor(FOO), is(legal));
        verify(classFileLocator).classFileFor(FOO);
        verifyNoMoreInteractions(classFileLocator);
        verify(otherClassFileLocator).classFileFor(FOO);
        verifyNoMoreInteractions(otherClassFileLocator);
    }

    @Test
    public void testApplicationOrderDoesNotCallSecond() throws Exception {
        when(classFileLocator.classFileFor(FOO)).thenReturn(legal);
        assertThat(new ClassFileLocator.Compound(classFileLocator, otherClassFileLocator).classFileFor(FOO), is(legal));
        verify(classFileLocator).classFileFor(FOO);
        verifyNoMoreInteractions(classFileLocator);
        verifyZeroInteractions(otherClassFileLocator);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileLocator.Compound.class).apply();
    }
}
