package net.bytebuddy.matcher;

import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class CollectionSizeMatcherTest extends AbstractElementMatcherTest<CollectionSizeMatcher<?>> {

    @Mock
    private Iterable<Object> collection;

    @SuppressWarnings("unchecked")
    public CollectionSizeMatcherTest() {
        super((Class<CollectionSizeMatcher<?>>) (Object) CollectionSizeMatcher.class, "ofSize");
    }

    @Test
    public void testMatch() throws Exception {
        when(collection.iterator()).thenReturn(Collections.singletonList(new Object()).iterator());
        assertThat(new CollectionSizeMatcher<Iterable<?>>(1).matches(collection), is(true));
        verify(collection).iterator();
        verifyNoMoreInteractions(collection);
    }

    @Test
    public void testMatchCollection() throws Exception {
        assertThat(new CollectionSizeMatcher<Iterable<?>>(1).matches(Collections.singletonList(new Object())), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        when(collection.iterator()).thenReturn(Collections.emptyList().iterator());
        assertThat(new CollectionSizeMatcher<Iterable<?>>(1).matches(collection), is(false));
        verify(collection).iterator();
        verifyNoMoreInteractions(collection);
    }

    @Test
    public void testNoMatchCollection() throws Exception {
        assertThat(new CollectionSizeMatcher<Iterable<?>>(0).matches(Collections.singletonList(new Object())), is(false));
    }
}
