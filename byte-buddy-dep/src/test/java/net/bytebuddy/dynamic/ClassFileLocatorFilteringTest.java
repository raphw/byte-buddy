package net.bytebuddy.dynamic;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClassFileLocatorFilteringTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ClassFileLocator classFileLocator;

    @Test
    public void testLocation() throws Exception {
        when(classFileLocator.locate(FOO)).thenReturn(new ClassFileLocator.Resolution.Explicit(new byte[0]));
        assertThat(new ClassFileLocator.Filtering(is(FOO), classFileLocator).locate(FOO).isResolved(), CoreMatchers.is(true));
        assertThat(new ClassFileLocator.Filtering(is(FOO), classFileLocator).locate(BAR).isResolved(), CoreMatchers.is(false));
    }

    @Test
    public void testClose() throws Exception {
        new ClassFileLocator.Filtering(is(FOO), classFileLocator).close();
        verify(classFileLocator).close();
    }
}
