package net.bytebuddy.matcher;

import net.bytebuddy.description.NamedElement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class NameMatcherTest extends AbstractElementMatcherTest<NameMatcher<?>> {

    private static final String FOO = "foo";

    @Mock
    private NamedElement namedElement;

    @Mock
    private ElementMatcher<String> nameMatcher;

    @SuppressWarnings("unchecked")
    public NameMatcherTest() {
        super((Class<NameMatcher<?>>) (Object) NameMatcher.class, "name");
    }

    @Before
    public void setUp() throws Exception {
        when(namedElement.getSourceCodeName()).thenReturn(FOO);
    }

    @Test
    public void testMatch() throws Exception {
        when(nameMatcher.matches(FOO)).thenReturn(true);
        assertThat(new NameMatcher<NamedElement>(nameMatcher).matches(namedElement), is(true));
        verify(nameMatcher).matches(FOO);
        verifyNoMoreInteractions(nameMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(nameMatcher.matches(FOO)).thenReturn(false);
        assertThat(new NameMatcher<NamedElement>(nameMatcher).matches(namedElement), is(false));
        verify(nameMatcher).matches(FOO);
        verifyNoMoreInteractions(nameMatcher);
    }
}
