package net.bytebuddy.matcher;

import net.bytebuddy.description.ByteCodeElement;
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
    private ByteCodeElement byteCodeElement;

    @SuppressWarnings("unchecked")
    public DescriptorMatcherTest() {
        super((Class<DescriptorMatcher<?>>) (Object) DescriptorMatcher.class, "hasDescriptor");
    }

    @Before
    public void setUp() throws Exception {
        when(byteCodeElement.getDescriptor()).thenReturn(FOO);
    }

    @Test
    public void testMatch() throws Exception {
        when(descriptorMatcher.matches(FOO)).thenReturn(true);
        assertThat(new DescriptorMatcher<ByteCodeElement>(descriptorMatcher).matches(byteCodeElement), is(true));
        verify(descriptorMatcher).matches(FOO);
        verifyNoMoreInteractions(descriptorMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(descriptorMatcher.matches(FOO)).thenReturn(false);
        assertThat(new DescriptorMatcher<ByteCodeElement>(descriptorMatcher).matches(byteCodeElement), is(false));
        verify(descriptorMatcher).matches(FOO);
        verifyNoMoreInteractions(descriptorMatcher);
    }
}
