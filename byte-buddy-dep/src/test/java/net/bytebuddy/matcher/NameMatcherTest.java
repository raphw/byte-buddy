package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.ByteCodeElement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class NameMatcherTest extends AbstractElementMatcherTest<NameMatcher<?>> {

    private static final String FOO = "foo";

    @SuppressWarnings("unchecked")
    public NameMatcherTest() {
        super((Class<NameMatcher<?>>) (Object) NameMatcher.class, "name");
    }

    @Mock
    private ByteCodeElement byteCodeElement;

    @Mock
    private ElementMatcher<String> nameMatcher;

    @Before
    public void setUp() throws Exception {
        when(byteCodeElement.getSourceCodeName()).thenReturn(FOO);
    }

    @Test
    public void testMatch() throws Exception {
        when(nameMatcher.matches(FOO)).thenReturn(true);
        assertThat(new NameMatcher<ByteCodeElement>(nameMatcher).matches(byteCodeElement), is(true));
        verify(nameMatcher).matches(FOO);
        verifyNoMoreInteractions(nameMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(nameMatcher.matches(FOO)).thenReturn(false);
        assertThat(new NameMatcher<ByteCodeElement>(nameMatcher).matches(byteCodeElement), is(false));
        verify(nameMatcher).matches(FOO);
        verifyNoMoreInteractions(nameMatcher);
    }
}
