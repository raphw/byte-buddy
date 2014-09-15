package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.HashCodeEqualsTester;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassFileLocatorCompoundTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileLocator classFileLocator, otherClassFileLocator;
    @Mock
    private TypeDescription typeDescription;
    @Mock
    private InputStream inputStream;

    @Test
    public void testApplicationOrderCallsSecond() throws Exception {
        when(otherClassFileLocator.classFileFor(typeDescription)).thenReturn(inputStream);
        assertThat(new ClassFileLocator.Compound(classFileLocator, otherClassFileLocator).classFileFor(typeDescription), is(inputStream));
        verify(classFileLocator).classFileFor(typeDescription);
        verifyNoMoreInteractions(classFileLocator);
        verify(otherClassFileLocator).classFileFor(typeDescription);
        verifyNoMoreInteractions(otherClassFileLocator);
    }

    @Test
    public void testApplicationOrderDoesNotCallSecond() throws Exception {
        when(classFileLocator.classFileFor(typeDescription)).thenReturn(inputStream);
        assertThat(new ClassFileLocator.Compound(classFileLocator, otherClassFileLocator).classFileFor(typeDescription), is(inputStream));
        verify(classFileLocator).classFileFor(typeDescription);
        verifyNoMoreInteractions(classFileLocator);
        verifyZeroInteractions(otherClassFileLocator);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(ClassFileLocator.Compound.class).apply();
    }
}
