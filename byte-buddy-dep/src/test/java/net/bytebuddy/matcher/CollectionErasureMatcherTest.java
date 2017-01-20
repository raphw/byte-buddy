package net.bytebuddy.matcher;


import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class CollectionErasureMatcherTest extends AbstractElementMatcherTest<CollectionErasureMatcher<?>> {

    @Mock
    private ElementMatcher<? super Iterable<? extends TypeDefinition>> matcher;

    @Mock
    private TypeDefinition first, second, other;

    @Mock
    private TypeDescription firstRaw, secondRaw;

    @SuppressWarnings("unchecked")
    public CollectionErasureMatcherTest() {
        super((Class<CollectionErasureMatcher<?>>) (Object) CollectionErasureMatcher.class, "erasures");
    }

    @Before
    public void setUp() throws Exception {
        when(first.asErasure()).thenReturn(firstRaw);
        when(second.asErasure()).thenReturn(secondRaw);
    }

    @Test
    public void testMatch() throws Exception {
        when(matcher.matches(Arrays.asList(firstRaw, secondRaw))).thenReturn(true);
        assertThat(new CollectionErasureMatcher<Iterable<TypeDefinition>>(matcher).matches(Arrays.asList(first, second)), is(true));
        verify(matcher).matches(Arrays.asList(firstRaw, secondRaw));
        verifyNoMoreInteractions(matcher);
        verify(first).asErasure();
        verifyNoMoreInteractions(first);
        verify(second).asErasure();
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testNoMatch() throws Exception {
        assertThat(new CollectionErasureMatcher<Iterable<TypeDefinition>>(matcher).matches(Arrays.asList(first, second)), is(false));
        verify(matcher).matches(Arrays.asList(firstRaw, secondRaw));
        verifyNoMoreInteractions(matcher);
        verify(first).asErasure();
        verifyNoMoreInteractions(first);
        verify(second).asErasure();
        verifyNoMoreInteractions(second);
    }
}
