package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TypePoolExplicitTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    private TypePool typePool;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private TypePool parent;

    @Before
    public void setUp() throws Exception {
        when(parent.describe(anyString())).thenReturn(new TypePool.Resolution.Illegal(BAR));
        typePool = new TypePool.Explicit(parent, Collections.singletonMap(FOO, typeDescription));
    }

    @Test
    public void testSuccessfulLookup() throws Exception {
        TypePool.Resolution resolution = typePool.describe(FOO);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(typeDescription));
        verify(parent).describe(FOO);
        verifyNoMoreInteractions(parent);
    }

    @Test
    public void testFailedLookup() throws Exception {
        TypePool.Resolution resolution = typePool.describe(BAR);
        assertThat(resolution.isResolved(), is(false));
        verify(parent).describe(BAR);
        verifyNoMoreInteractions(parent);
    }

    @Test
    public void testDelegation() throws Exception {
        TypePool.Resolution resolution = mock(TypePool.Resolution.class);
        when(resolution.isResolved()).thenReturn(true);
        when(parent.describe(BAR)).thenReturn(resolution);
        assertThat(typePool.describe(BAR), sameInstance(resolution));
        verify(parent).describe(BAR);
        verifyNoMoreInteractions(parent);
    }
}
