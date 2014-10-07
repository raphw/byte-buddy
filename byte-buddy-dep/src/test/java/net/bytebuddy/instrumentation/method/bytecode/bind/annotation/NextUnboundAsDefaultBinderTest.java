package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class NextUnboundAsDefaultBinderTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation.Target instrumentationTarget;
    @Mock
    private MethodDescription source, target;
    @Mock
    private TypeList typeList;

    @Before
    public void setUp() throws Exception {
        when(typeList.size()).thenReturn(2);
        when(typeList.iterator()).thenReturn(Arrays.asList(mock(TypeDescription.class), mock(TypeDescription.class)).iterator());
        when(source.getParameterTypes()).thenReturn(typeList);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationTarget);
    }

    @Test
    public void testFullyUnannotated() throws Exception {
        when(source.getParameterTypes()).thenReturn(typeList);
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.Empty.asList(0));
        Iterator<AnnotationDescription> iterator = Argument.NextUnboundAsDefaultsProvider.INSTANCE
                .makeIterator(instrumentationTarget, source, target);
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().prepare(Argument.class).load().value(), is(0));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().prepare(Argument.class).load().value(), is(1));
        assertThat(iterator.hasNext(), is(false));
        verify(source, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoval() throws Exception {
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.Empty.asList(0));
        Iterator<AnnotationDescription> iterator = Argument.NextUnboundAsDefaultsProvider.INSTANCE
                .makeIterator(instrumentationTarget, source, target);
        assertThat(iterator.hasNext(), is(true));
        iterator.remove();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().prepare(Argument.class).load().value(), is(1));
        assertThat(iterator.hasNext(), is(false));
        iterator.remove();
    }

    @Test
    public void testPartlyAnnotatedOrdered() throws Exception {
        Argument indexZeroArgument = mock(Argument.class);
        when(indexZeroArgument.value()).thenReturn(0);
        doReturn(Argument.class).when(indexZeroArgument).annotationType();
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.ForLoadedAnnotation.asList(new Annotation[][]{{indexZeroArgument}, {}}));
        Iterator<AnnotationDescription> iterator = Argument.NextUnboundAsDefaultsProvider.INSTANCE
                .makeIterator(instrumentationTarget, source, target);
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().prepare(Argument.class).load().value(), is(1));
        assertThat(iterator.hasNext(), is(false));
        verify(source, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
    }

    @Test
    public void testPartlyAnnotatedUnordered() throws Exception {
        Argument indexOneArgument = mock(Argument.class);
        when(indexOneArgument.value()).thenReturn(1);
        doReturn(Argument.class).when(indexOneArgument).annotationType();
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.ForLoadedAnnotation.asList(new Annotation[][]{{indexOneArgument}, {}}));
        Iterator<AnnotationDescription> iterator = Argument.NextUnboundAsDefaultsProvider.INSTANCE
                .makeIterator(instrumentationTarget, source, target);
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().prepare(Argument.class).load().value(), is(0));
        assertThat(iterator.hasNext(), is(false));
        verify(source, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
    }

    @Test
    public void testFullyAnnotatedUnordered() throws Exception {
        Argument indexZeroArgument = mock(Argument.class);
        when(indexZeroArgument.value()).thenReturn(0);
        doReturn(Argument.class).when(indexZeroArgument).annotationType();
        Argument indexOneArgument = mock(Argument.class);
        when(indexOneArgument.value()).thenReturn(1);
        doReturn(Argument.class).when(indexOneArgument).annotationType();
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.ForLoadedAnnotation
                .asList(new Annotation[][]{{indexOneArgument}, {indexZeroArgument}}));
        Iterator<AnnotationDescription> iterator = Argument.NextUnboundAsDefaultsProvider.INSTANCE
                .makeIterator(instrumentationTarget, source, target);
        assertThat(iterator.hasNext(), is(false));
        verify(source, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
    }
}
