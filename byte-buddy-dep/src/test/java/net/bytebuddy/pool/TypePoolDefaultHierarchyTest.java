package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class TypePoolDefaultHierarchyTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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
        verifyZeroInteractions(cacheProvider);
        verifyZeroInteractions(classFileLocator);
        verify(parentPool).describe(FOO);
        verifyNoMoreInteractions(parentPool);
        verify(resolution).isResolved();
        verifyNoMoreInteractions(resolution);
    }

    @Test
    public void testChildSecond() throws Exception {
        TypePool typePool = new TypePool.Default(cacheProvider, classFileLocator, TypePool.Default.ReaderMode.FAST, parentPool);
        when(parentPool.describe(FOO)).thenReturn(resolution);
        when(resolution.isResolved()).thenReturn(false);
        when(classFileLocator.locate(FOO)).thenReturn(new ClassFileLocator.Resolution.Explicit(ClassFileExtraction.extract(Foo.class)));
        when(cacheProvider.register(eq(FOO), any(TypePool.Resolution.class))).then(new Answer<TypePool.Resolution>() {
            @Override
            public TypePool.Resolution answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (TypePool.Resolution) invocationOnMock.getArguments()[1];
            }
        });
        TypePool.Resolution resolution = typePool.describe(FOO);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        verify(cacheProvider).find(FOO);
        verify(cacheProvider).register(FOO, resolution);
        verifyZeroInteractions(cacheProvider);
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
