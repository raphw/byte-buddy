package net.bytebuddy.matcher;

import net.bytebuddy.description.NamedElement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DescriptorMatcherTest extends AbstractElementMatcherTest<DescriptorMatcher<?>> {

    private static final String FOO = "foo";

    @Mock
    private ElementMatcher<String> descriptorMatcher;

    @Mock
    private NamedElement.WithDescriptor namedElement;

    @SuppressWarnings("unchecked")
    public DescriptorMatcherTest() {
        super((Class<DescriptorMatcher<?>>) (Object) DescriptorMatcher.class, "hasDescriptor");
    }

    @Before
    public void setUp() throws Exception {
        when(namedElement.getDescriptor()).thenReturn(FOO);
    }

    @Test
    public void testMatch() throws Exception {
        when(descriptorMatcher.matches(FOO)).thenReturn(true);
        assertThat(new DescriptorMatcher<NamedElement.WithDescriptor>(descriptorMatcher).matches(namedElement), is(true));
        verify(descriptorMatcher).matches(FOO);
        verifyNoMoreInteractions(descriptorMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(descriptorMatcher.matches(FOO)).thenReturn(false);
        assertThat(new DescriptorMatcher<NamedElement.WithDescriptor>(descriptorMatcher).matches(namedElement), is(false));
        verify(descriptorMatcher).matches(FOO);
        verifyNoMoreInteractions(descriptorMatcher);
    }
}
