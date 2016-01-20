package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.TypeReference;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AnnotationAppenderForTypeAnnotationsTest {

    private static final String FOO = "foo";

    private static final int BAR = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AnnotationAppender annotationAppender, result;

    @Mock
    private AnnotationValueFilter annotationValueFilter;

    @Mock
    private AnnotationDescription annotationDescription;

    @Mock
    private TypeDescription.Generic typeDescription, second, third;

    @Mock
    private TypeDescription erasure;

    private TypeDescription.Generic.Visitor<AnnotationAppender> visitor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(annotationAppender.append(eq(annotationDescription), eq(annotationValueFilter), eq(BAR), any(String.class))).thenReturn(result);
        when(typeDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(annotationDescription));
        when(second.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(result);
        when(third.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(result);
        when(second.asGenericType()).thenReturn(second);
        when(third.asGenericType()).thenReturn(third);
        when(typeDescription.asErasure()).thenReturn(erasure);
        visitor = new AnnotationAppender.ForTypeAnnotations(annotationAppender, annotationValueFilter, BAR, FOO);
    }

    @After
    @SuppressWarnings("unchecked")
    public void tearDown() throws Exception {
        verifyZeroInteractions(annotationDescription);
        verifyZeroInteractions(annotationValueFilter);
    }

    @Test
    public void testGenericArray() throws Exception {
        when(typeDescription.getComponentType()).thenReturn(second);
        assertThat(visitor.onGenericArray(typeDescription), is(result));
        verify(annotationAppender).append(annotationDescription, annotationValueFilter, BAR, FOO);
        verifyNoMoreInteractions(annotationAppender);
        verify(second).accept(new AnnotationAppender.ForTypeAnnotations(result, annotationValueFilter, BAR, FOO + "["));
    }

    @Test
    public void testWildcardUpperBound() throws Exception {
        when(typeDescription.getLowerBounds()).thenReturn(new TypeList.Generic.Empty());
        when(typeDescription.getUpperBounds()).thenReturn(new TypeList.Generic.Explicit(second));
        assertThat(visitor.onWildcard(typeDescription), is(result));
        verify(annotationAppender).append(annotationDescription, annotationValueFilter, BAR, FOO);
        verifyNoMoreInteractions(annotationAppender);
        verify(second).accept(new AnnotationAppender.ForTypeAnnotations(result, annotationValueFilter, BAR, FOO + "*"));
    }

    @Test
    public void testWildcardLowerBound() throws Exception {
        when(typeDescription.getLowerBounds()).thenReturn(new TypeList.Generic.Explicit(second));
        when(typeDescription.getUpperBounds()).thenReturn(new TypeList.Generic.Explicit(TypeDescription.Generic.OBJECT));
        assertThat(visitor.onWildcard(typeDescription), is(result));
        verify(annotationAppender).append(annotationDescription, annotationValueFilter, BAR, FOO);
        verifyNoMoreInteractions(annotationAppender);
        verify(second).accept(new AnnotationAppender.ForTypeAnnotations(result, annotationValueFilter, BAR, FOO + "*"));
    }

    @Test
    public void testTypeVariable() throws Exception {
        assertThat(visitor.onTypeVariable(typeDescription), is(result));
        verify(annotationAppender).append(annotationDescription, annotationValueFilter, BAR, FOO);
        verifyNoMoreInteractions(annotationAppender);
    }

    @Test
    public void testNonGenericArray() throws Exception {
        when(typeDescription.isArray()).thenReturn(true);
        when(typeDescription.getComponentType()).thenReturn(second);
        assertThat(visitor.onNonGenericType(typeDescription), is(result));
        verify(annotationAppender).append(annotationDescription, annotationValueFilter, BAR, FOO);
        verifyNoMoreInteractions(annotationAppender);
        verify(second).accept(new AnnotationAppender.ForTypeAnnotations(result, annotationValueFilter, BAR, FOO + "["));
    }

    @Test
    public void testNonGeneric() throws Exception {
        assertThat(visitor.onNonGenericType(typeDescription), is(result));
        verify(annotationAppender).append(annotationDescription, annotationValueFilter, BAR, FOO);
        verifyNoMoreInteractions(annotationAppender);
    }

    @Test
    public void testParameterized() throws Exception {
        when(erasure.getSegmentCount()).thenReturn(1);
        when(typeDescription.getTypeArguments()).thenReturn(new TypeList.Generic.Explicit(second));
        when(typeDescription.getOwnerType()).thenReturn(third);
        assertThat(visitor.onParameterizedType(typeDescription), is(result));
        verify(annotationAppender).append(annotationDescription, annotationValueFilter, BAR, FOO + ".");
        verifyNoMoreInteractions(annotationAppender);
        verify(second).accept(new AnnotationAppender.ForTypeAnnotations(result, annotationValueFilter, BAR, FOO + ".0;"));
        verify(third).accept(new AnnotationAppender.ForTypeAnnotations(result, annotationValueFilter, BAR, FOO + ""));
    }

    @Test
    public void testParameterizedNoOwner() throws Exception {
        when(typeDescription.getTypeArguments()).thenReturn(new TypeList.Generic.Explicit(second));
        assertThat(visitor.onParameterizedType(typeDescription), is(result));
        verify(annotationAppender).append(annotationDescription, annotationValueFilter, BAR, FOO);
        verifyNoMoreInteractions(annotationAppender);
        verify(second).accept(new AnnotationAppender.ForTypeAnnotations(result, annotationValueFilter, BAR, FOO + "0;"));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationAppender.ForTypeAnnotations.class).refine(new ObjectPropertyAssertion.Refinement<TypeReference>() {
            @Override
            public void apply(TypeReference mock) {
                when(mock.getValue()).thenReturn(new Random().nextInt());
            }
        }).apply();
    }
}