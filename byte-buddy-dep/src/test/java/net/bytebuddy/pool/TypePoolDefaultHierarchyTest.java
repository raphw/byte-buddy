package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TypePoolDefaultHierarchyTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypePool parentPool;

    @Mock
    private TypePool.CacheProvider cacheProvider;

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private TypePool.Resolution resolution;

    @Test
    public void testParentFirst() throws Exception {
        TypePool typePool = new TypePool.Default(cacheProvider, classFileLocator, TypePool.Default.ReaderMode.FAST, parentPool);
        when(parentPool.describe(FOO)).thenReturn(resolution);
        when(resolution.isResolved()).thenReturn(true);
        assertThat(typePool.describe(FOO), is(resolution));
        verifyNoMoreInteractions(cacheProvider);
        verifyNoMoreInteractions(classFileLocator);
        verify(parentPool).describe(FOO);
        verifyNoMoreInteractions(parentPool);
        verify(resolution).isResolved();
        verifyNoMoreInteractions(resolution);
    }

    @Test
    @SuppressWarnings("cast")
    public void testChildSecond() throws Exception {
        TypePool typePool = new TypePool.Default(cacheProvider, classFileLocator, TypePool.Default.ReaderMode.FAST, parentPool);
        when(parentPool.describe(FOO)).thenReturn(resolution);
        when(resolution.isResolved()).thenReturn(false);
        when(classFileLocator.locate(FOO)).thenReturn(new ClassFileLocator.Resolution.Explicit(ClassFileLocator.ForClassLoader.read(Foo.class)));
        when(cacheProvider.register(eq(FOO), any(TypePool.Resolution.class))).then(new Answer<TypePool.Resolution>() {
            public TypePool.Resolution answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (TypePool.Resolution) invocationOnMock.getArguments()[1];
            }
        });
        TypePool.Resolution resolution = typePool.describe(FOO);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is((TypeDescription) TypeDescription.ForLoadedType.of(Foo.class)));
        verify(cacheProvider).find(FOO);
        verify(cacheProvider).register(FOO, resolution);
        verifyNoMoreInteractions(cacheProvider);
        verify(classFileLocator).locate(FOO);
        verifyNoMoreInteractions(classFileLocator);
        verify(parentPool).describe(FOO);
        verifyNoMoreInteractions(parentPool);
        verify(this.resolution).isResolved();
        verifyNoMoreInteractions(this.resolution);
    }

    @Test
    public void testClear() throws Exception {
        TypePool typePool = new TypePool.Default(cacheProvider, classFileLocator, TypePool.Default.ReaderMode.FAST, parentPool);
        typePool.clear();
        verify(cacheProvider).clear();
        verifyNoMoreInteractions(cacheProvider);
        verify(parentPool).clear();
        verifyNoMoreInteractions(parentPool);
    }

    private static class Foo {
        /* empty */
    }
}
