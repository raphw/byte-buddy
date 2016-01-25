package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import java.lang.annotation.RetentionPolicy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeWriterFieldPoolRecordTest {

    private static final int MODIFIER = 42;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldAttributeAppender fieldAttributeAppender;

    @Mock
    private AnnotationValueFilter valueFilter;

    @Mock
    private AnnotationValueFilter.Factory annotationValueFilterFactory;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private FieldVisitor fieldVisitor;

    @Mock
    private AnnotationVisitor annotationVisitor;

    @Mock
    private FieldDescription fieldDescription;

    @Mock
    private AnnotationDescription annotationDescription;

    @Mock
    private TypeDescription annotationType;

    @Mock
    private Object defaultValue;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(fieldDescription.getActualModifiers()).thenReturn(MODIFIER);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(fieldDescription.getDescriptor()).thenReturn(BAR);
        when(fieldDescription.getGenericSignature()).thenReturn(QUX);
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(annotationDescription));
        when(fieldDescription.getType()).thenReturn(TypeDescription.Generic.OBJECT);
        when(classVisitor.visitField(MODIFIER, FOO, BAR, QUX, defaultValue)).thenReturn(fieldVisitor);
        when(classVisitor.visitField(MODIFIER, FOO, BAR, QUX, FieldDescription.NO_DEFAULT_VALUE)).thenReturn(fieldVisitor);
        when(annotationValueFilterFactory.on(fieldDescription)).thenReturn(valueFilter);
        when(fieldVisitor.visitAnnotation(any(String.class), anyBoolean())).thenReturn(annotationVisitor);
        when(annotationDescription.getAnnotationType()).thenReturn(annotationType);
        when(annotationType.getDescriptor()).thenReturn(BAZ);
        when(annotationType.getDeclaredMethods()).thenReturn(new MethodList.Empty<MethodDescription.InDefinedShape>());
        when(annotationDescription.getRetention()).thenReturn(RetentionPolicy.RUNTIME);
    }

    @Test
    public void testExplicitFieldEntryProperties() throws Exception {
        TypeWriter.FieldPool.Record record = new TypeWriter.FieldPool.Record.ForExplicitField(fieldAttributeAppender, defaultValue, fieldDescription);
        assertThat(record.getFieldAppender(), is(fieldAttributeAppender));
        assertThat(record.resolveDefault(FieldDescription.NO_DEFAULT_VALUE), is(defaultValue));
        assertThat(record.isImplicit(), is(false));
    }

    @Test
    public void testExplicitFieldEntryWritesField() throws Exception {
        TypeWriter.FieldPool.Record record = new TypeWriter.FieldPool.Record.ForExplicitField(fieldAttributeAppender, defaultValue, fieldDescription);
        record.apply(classVisitor, annotationValueFilterFactory);
        verify(classVisitor).visitField(MODIFIER, FOO, BAR, QUX, defaultValue);
        verifyNoMoreInteractions(classVisitor);
        verify(fieldAttributeAppender).apply(fieldVisitor, fieldDescription, valueFilter);
        verifyNoMoreInteractions(fieldAttributeAppender);
        verify(fieldVisitor).visitEnd();
        verifyNoMoreInteractions(fieldVisitor);
    }

    @Test
    public void testExplicitFieldEntryWritesFieldPartialApplication() throws Exception {
        TypeWriter.FieldPool.Record record = new TypeWriter.FieldPool.Record.ForExplicitField(fieldAttributeAppender, defaultValue, fieldDescription);
        record.apply(fieldVisitor, annotationValueFilterFactory);
        verify(fieldAttributeAppender).apply(fieldVisitor, fieldDescription, valueFilter);
        verifyNoMoreInteractions(fieldAttributeAppender);
        verifyZeroInteractions(fieldVisitor);
    }

    @Test
    public void testImplicitFieldEntryProperties() throws Exception {
        TypeWriter.FieldPool.Record record = new TypeWriter.FieldPool.Record.ForImplicitField(fieldDescription);
        assertThat(record.isImplicit(), is(true));
    }

    @Test
    public void testImplicitFieldEntryWritesField() throws Exception {
        TypeWriter.FieldPool.Record record = new TypeWriter.FieldPool.Record.ForImplicitField(fieldDescription);
        record.apply(classVisitor, annotationValueFilterFactory);
        verify(classVisitor).visitField(MODIFIER, FOO, BAR, QUX, FieldDescription.NO_DEFAULT_VALUE);
        verifyNoMoreInteractions(classVisitor);
        verify(fieldVisitor).visitAnnotation(BAZ, true);
        verify(fieldVisitor).visitEnd();
        verifyNoMoreInteractions(fieldVisitor);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
    }

    @Test(expected = IllegalStateException.class)
    public void testImplicitFieldWritesFieldPartialApplication() throws Exception {
        new TypeWriter.FieldPool.Record.ForImplicitField(fieldDescription).apply(fieldVisitor, annotationValueFilterFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void testImplicitFieldEntryAppliedToField() throws Exception {
        new TypeWriter.FieldPool.Record.ForImplicitField(fieldDescription).apply(fieldVisitor, annotationValueFilterFactory);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.FieldPool.Record.ForImplicitField.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.FieldPool.Record.ForExplicitField.class).apply();
    }
}
