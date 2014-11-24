package net.bytebuddy.matcher;

import org.junit.Test;
import org.mockito.Mock;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class CollectionSizeMatcherTest extends AbstractElementMatcherTest<CollectionSizeMatcher<?>> {

    @SuppressWarnings("unchecked")
    public CollectionSizeMatcherTest() {
        super((Class<CollectionSizeMatcher<?>>) (Object) CollectionSizeMatcher.class, "ofSize");
    }

    @Mock
    private Collection<?> collection;

    @Test
    public void testMatch() throws Exception {
        when(collection.size()).thenReturn(1);
        assertThat(new CollectionSizeMatcher<Collection<?>>(1).matches(collection), is(true));
        verify(collection).size();
        verifyNoMoreInteractions(collection);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(collection.size()).thenReturn(0);
        assertThat(new CollectionSizeMatcher<Collection<?>>(1).matches(collection), is(false));
        verify(collection).size();
        verifyNoMoreInteractions(collection);
    }
}
