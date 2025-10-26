package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TypePoolLazyFacadeTest {

    private static final String FOO = "foo";

    private static final int MODIFIERS = 42;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypePool typePool;

    @Mock
    private TypePool.Resolution resolution;

    @Mock
    private TypeDescription typeDescription;

    @Before
    public void setUp() throws Exception {
        when(typePool.describe(FOO)).thenReturn(resolution);
        when(resolution.isResolved()).thenReturn(true);
        when(resolution.resolve()).thenReturn(typeDescription);
        when(typeDescription.getModifiers()).thenReturn(MODIFIERS);
    }

    @Test
    public void testDoesNotQueryActualTypePoolForName() throws Exception {
        TypePool typePool = new TypePool.LazyFacade(this.typePool);
        assertThat(typePool.describe(FOO).resolve().getName(), is(FOO));
        verifyNoMoreInteractions(this.typePool);
    }

    @Test
    public void testDoesQueryActualTypePoolForResolution() throws Exception {
        TypePool typePool = new TypePool.LazyFacade(this.typePool);
        assertThat(typePool.describe(FOO).isResolved(), is(true));
        verify(this.typePool).describe(FOO);
        verifyNoMoreInteractions(this.typePool);
        verify(resolution).isResolved();
        verifyNoMoreInteractions(resolution);
    }

    @Test
    public void testDoesQueryActualTypePoolForNonNameProperty() throws Exception {
        TypePool typePool = new TypePool.LazyFacade(this.typePool);
        assertThat(typePool.describe(FOO).resolve().getModifiers(), is(MODIFIERS));
        verify(this.typePool).describe(FOO);
        verifyNoMoreInteractions(this.typePool);
        verify(resolution).resolve();
        verifyNoMoreInteractions(resolution);
        verify(typeDescription).getModifiers();
        verifyNoMoreInteractions(typeDescription);
    }
}
