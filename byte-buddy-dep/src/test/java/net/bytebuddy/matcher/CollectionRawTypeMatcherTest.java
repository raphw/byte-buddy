package net.bytebuddy.matcher;


import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class CollectionRawTypeMatcherTest extends AbstractElementMatcherTest<CollectionRawTypeMatcher<?>> {

    @Mock
    private ElementMatcher<? super Iterable<? extends GenericTypeDescription>> matcher;

    @Mock
    private GenericTypeDescription first, second;

    @Mock
    private TypeDescription firstRaw, secondRaw;

    @SuppressWarnings("unchecked")
    public CollectionRawTypeMatcherTest() {
        super((Class<CollectionRawTypeMatcher<?>>) (Object) CollectionRawTypeMatcher.class, "rawTypes");
    }

    @Before
    public void setUp() throws Exception {
        when(first.asErasure()).thenReturn(firstRaw);
        when(second.asErasure()).thenReturn(secondRaw);
    }

    @Test
    public void testMatch() throws Exception {
        when(matcher.matches(Arrays.asList(firstRaw, secondRaw))).thenReturn(true);
        assertThat(new CollectionRawTypeMatcher<Iterable<GenericTypeDescription>>(matcher).matches(Arrays.asList(first, second)), is(true));
        verify(matcher).matches(Arrays.asList(firstRaw, secondRaw));
        verifyNoMoreInteractions(matcher);
        verify(first).asErasure();
        verifyNoMoreInteractions(first);
        verify(second).asErasure();
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testNoMatch() throws Exception {
        assertThat(new CollectionRawTypeMatcher<Iterable<GenericTypeDescription>>(matcher).matches(Arrays.asList(first, second)), is(false));
        verify(matcher).matches(Arrays.asList(firstRaw, secondRaw));
        verifyNoMoreInteractions(matcher);
        verify(first).asErasure();
        verifyNoMoreInteractions(first);
        verify(second).asErasure();
        verifyNoMoreInteractions(second);
    }
}
