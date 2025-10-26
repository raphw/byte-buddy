package net.bytebuddy.matcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CollectionElementMatcherTest extends AbstractElementMatcherTest<CollectionElementMatcher<?>> {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    private Iterable<Object> iterable;

    private Object element;

    @Mock
    private ElementMatcher<? super Object> elementMatcher;

    @SuppressWarnings("unchecked")
    public CollectionElementMatcherTest() {
        super((Class<CollectionElementMatcher<?>>) (Object) CollectionElementMatcher.class, "with");
    }

    @Before
    public void setUp() throws Exception {
        element = new Object();
        iterable = Arrays.asList(new Object(), element);
    }

    @Test
    public void testMatch() throws Exception {
        when(elementMatcher.matches(element)).thenReturn(true);
        assertThat(new CollectionElementMatcher<Object>(1, elementMatcher).matches(iterable), is(true));
        verify(elementMatcher).matches(element);
        verifyNoMoreInteractions(elementMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(elementMatcher.matches(element)).thenReturn(false);
        assertThat(new CollectionElementMatcher<Object>(1, elementMatcher).matches(iterable), is(false));
        verify(elementMatcher).matches(element);
        verifyNoMoreInteractions(elementMatcher);
    }

    @Test
    public void testNoMatchIndex() throws Exception {
        assertThat(new CollectionElementMatcher<Object>(2, elementMatcher).matches(iterable), is(false));
        verifyNoMoreInteractions(elementMatcher);
    }
}
