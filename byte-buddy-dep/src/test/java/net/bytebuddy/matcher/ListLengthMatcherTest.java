package net.bytebuddy.matcher;

import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ListLengthMatcherTest extends AbstractElementMatcherTest<ListLengthMatcher<?>> {

    @SuppressWarnings("unchecked")
    public ListLengthMatcherTest() {
        super((Class<ListLengthMatcher<?>>) (Object) ListLengthMatcher.class, "length");
    }

    @Mock
    private List<?> list;

    @Test
    public void testMatch() throws Exception {
        when(list.size()).thenReturn(1);
        assertThat(new ListLengthMatcher<List<?>>(1).matches(list), is(true));
        verify(list).size();
        verifyNoMoreInteractions(list);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(list.size()).thenReturn(0);
        assertThat(new ListLengthMatcher<List<?>>(1).matches(list), is(false));
        verify(list).size();
        verifyNoMoreInteractions(list);
    }
}
