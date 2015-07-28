package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeWriterFieldPoolEntryTest {

    private static final int MODIFIER = 42;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final Object DEFAULT_VALUE = new Object();

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldAttributeAppender fieldAttributeAppender;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private FieldVisitor fieldVisitor;

    @Mock
    private FieldDescription fieldDescription;

    @Before
    public void setUp() throws Exception {
        when(fieldDescription.getModifiers()).thenReturn(MODIFIER);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(fieldDescription.getDescriptor()).thenReturn(BAR);
        when(fieldDescription.getGenericSignature()).thenReturn(QUX);
        when(classVisitor.visitField(MODIFIER, FOO, BAR, QUX, DEFAULT_VALUE)).thenReturn(fieldVisitor);
        when(classVisitor.visitField(MODIFIER, FOO, BAR, QUX, null)).thenReturn(fieldVisitor);
    }

    @Test
    public void testRichFieldEntryProperties() throws Exception {
        TypeWriter.FieldPool.Entry entry = new TypeWriter.FieldPool.Entry.ForRichField(fieldAttributeAppender, DEFAULT_VALUE, fieldDescription);
        assertThat(entry.getFieldAppender(), is(fieldAttributeAppender));
        assertThat(entry.getDefaultValue(), is(DEFAULT_VALUE));
    }

    @Test
    public void testRichFieldEntryWritesField() throws Exception {
        TypeWriter.FieldPool.Entry entry = new TypeWriter.FieldPool.Entry.ForRichField(fieldAttributeAppender, DEFAULT_VALUE, fieldDescription);
        entry.apply(classVisitor);
        verify(classVisitor).visitField(MODIFIER, FOO, BAR, QUX, DEFAULT_VALUE);
        verify(fieldAttributeAppender).apply(fieldVisitor, fieldDescription);
        verifyNoMoreInteractions(fieldAttributeAppender);
        verifyNoMoreInteractions(classVisitor);
        verify(fieldVisitor).visitEnd();
        verifyNoMoreInteractions(fieldVisitor);
    }

    @Test
    public void testSimpleFieldEntryWritesField() throws Exception {
        TypeWriter.FieldPool.Entry entry = new TypeWriter.FieldPool.Entry.ForSimpleField(fieldDescription);
        entry.apply(classVisitor);
        verify(classVisitor).visitField(MODIFIER, FOO, BAR, QUX, null);
        verifyNoMoreInteractions(classVisitor);
        verify(fieldVisitor).visitEnd();
        verifyNoMoreInteractions(fieldVisitor);
    }

    @Test
    public void testSimpleFieldEntryProperties() throws Exception {
        TypeWriter.FieldPool.Entry entry = new TypeWriter.FieldPool.Entry.ForSimpleField(fieldDescription);
        assertThat(entry.getFieldAppender(), is((FieldAttributeAppender) FieldAttributeAppender.NoOp.INSTANCE));
        assertThat(entry.getDefaultValue(), is(FieldDescription.NO_DEFAULT_VALUE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.FieldPool.Entry.ForSimpleField.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.FieldPool.Entry.ForRichField.class).apply();
    }
}
