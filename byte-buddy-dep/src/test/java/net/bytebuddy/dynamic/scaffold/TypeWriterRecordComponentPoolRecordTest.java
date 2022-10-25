package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.RecordComponentDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.RecordComponentAttributeAppender;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.RecordComponentVisitor;

import java.lang.annotation.RetentionPolicy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeWriterRecordComponentPoolRecordTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private RecordComponentAttributeAppender recordComponentAttributeAppender;

    @Mock
    private AnnotationValueFilter valueFilter;

    @Mock
    private AnnotationValueFilter.Factory annotationValueFilterFactory;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private RecordComponentVisitor recordComponentVisitor;

    @Mock
    private AnnotationVisitor annotationVisitor;

    @Mock
    private RecordComponentDescription recordComponentDescription;

    @Mock
    private AnnotationDescription annotationDescription;

    @Mock
    private TypeDescription annotationType;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(recordComponentDescription.getActualName()).thenReturn(FOO);
        when(recordComponentDescription.getDescriptor()).thenReturn(BAR);
        when(recordComponentDescription.getGenericSignature()).thenReturn(QUX);
        when(recordComponentDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(annotationDescription));
        when(recordComponentDescription.getType()).thenReturn(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class));
        when(classVisitor.visitRecordComponent(FOO, BAR, QUX)).thenReturn(recordComponentVisitor);
        when(classVisitor.visitRecordComponent(FOO, BAR, QUX)).thenReturn(recordComponentVisitor);
        when(annotationValueFilterFactory.on(recordComponentDescription)).thenReturn(valueFilter);
        when(recordComponentVisitor.visitAnnotation(any(String.class), anyBoolean())).thenReturn(annotationVisitor);
        when(annotationDescription.getAnnotationType()).thenReturn(annotationType);
        when(annotationType.getDescriptor()).thenReturn(BAZ);
        when(annotationType.getDeclaredMethods()).thenReturn(new MethodList.Empty<MethodDescription.InDefinedShape>());
        when(annotationDescription.getRetention()).thenReturn(RetentionPolicy.RUNTIME);
    }

    @Test
    public void testExplicitRecordComponentEntryProperties() throws Exception {
        TypeWriter.RecordComponentPool.Record record = new TypeWriter.RecordComponentPool.Record.ForExplicitRecordComponent(recordComponentAttributeAppender, recordComponentDescription);
        assertThat(record.getRecordComponentAppender(), is(recordComponentAttributeAppender));
        assertThat(record.isImplicit(), is(false));
    }

    @Test
    public void testExplicitRecordComponentEntryWritesRecordComponent() throws Exception {
        TypeWriter.RecordComponentPool.Record record = new TypeWriter.RecordComponentPool.Record.ForExplicitRecordComponent(recordComponentAttributeAppender, recordComponentDescription);
        record.apply(classVisitor, annotationValueFilterFactory);
        verify(classVisitor).visitRecordComponent(FOO, BAR, QUX);
        verifyNoMoreInteractions(classVisitor);
        verify(recordComponentAttributeAppender).apply(recordComponentVisitor, recordComponentDescription, valueFilter);
        verifyNoMoreInteractions(recordComponentAttributeAppender);
        verify(recordComponentVisitor).visitEnd();
        verifyNoMoreInteractions(recordComponentVisitor);
    }

    @Test
    public void testExplicitRecordComponentEntryWritesRecordComponentPartialApplication() throws Exception {
        TypeWriter.RecordComponentPool.Record record = new TypeWriter.RecordComponentPool.Record.ForExplicitRecordComponent(recordComponentAttributeAppender, recordComponentDescription);
        record.apply(recordComponentVisitor, annotationValueFilterFactory);
        verify(recordComponentAttributeAppender).apply(recordComponentVisitor, recordComponentDescription, valueFilter);
        verifyNoMoreInteractions(recordComponentAttributeAppender);
        verifyNoMoreInteractions(recordComponentVisitor);
    }

    @Test
    public void testImplicitRecordComponentEntryProperties() throws Exception {
        TypeWriter.RecordComponentPool.Record record = new TypeWriter.RecordComponentPool.Record.ForImplicitRecordComponent(recordComponentDescription);
        assertThat(record.isImplicit(), is(true));
    }

    @Test
    public void testImplicitRecordComponentEntryWritesRecordComponent() throws Exception {
        TypeWriter.RecordComponentPool.Record record = new TypeWriter.RecordComponentPool.Record.ForImplicitRecordComponent(recordComponentDescription);
        record.apply(classVisitor, annotationValueFilterFactory);
        verify(classVisitor).visitRecordComponent(FOO, BAR, QUX);
        verifyNoMoreInteractions(classVisitor);
        verify(recordComponentVisitor).visitAnnotation(BAZ, true);
        verify(recordComponentVisitor).visitEnd();
        verifyNoMoreInteractions(recordComponentVisitor);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testImplicitRecordComponentWritesRecordComponentPartialApplication() throws Exception {
        new TypeWriter.RecordComponentPool.Record.ForImplicitRecordComponent(recordComponentDescription).apply(recordComponentVisitor, annotationValueFilterFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void testImplicitRecordComponentEntryAppliedToRecordComponent() throws Exception {
        new TypeWriter.RecordComponentPool.Record.ForImplicitRecordComponent(recordComponentDescription).apply(recordComponentVisitor, annotationValueFilterFactory);
    }
}
