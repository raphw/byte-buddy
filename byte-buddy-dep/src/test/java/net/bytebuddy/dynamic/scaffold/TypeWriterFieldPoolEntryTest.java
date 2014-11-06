package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.MockitoRule;
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
    public void testSimpleEntry() throws Exception {
        TypeWriter.FieldPool.Entry entry = new TypeWriter.FieldPool.Entry.Simple(fieldAttributeAppender, DEFAULT_VALUE);
        assertThat(entry.getFieldAppender(), is(fieldAttributeAppender));
        assertThat(entry.getDefaultValue(), is(DEFAULT_VALUE));
    }

    @Test
    public void testSimpleEntryWritesField() throws Exception {
        new TypeWriter.FieldPool.Entry.Simple(fieldAttributeAppender, DEFAULT_VALUE)
                .apply(classVisitor, fieldDescription);
        verify(classVisitor).visitField(MODIFIER, FOO, BAR, QUX, DEFAULT_VALUE);
        verify(fieldAttributeAppender).apply(fieldVisitor, fieldDescription);
        verifyNoMoreInteractions(fieldAttributeAppender);
        verifyNoMoreInteractions(classVisitor);
        verify(fieldVisitor).visitEnd();
        verifyNoMoreInteractions(fieldVisitor);
    }

    @Test
    public void testSimpleEntryEntryHashCodeEquals() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.FieldPool.Entry.Simple.class).apply();
    }

    @Test
    public void testNoOpEntryWritesField() throws Exception {
        TypeWriter.FieldPool.Entry.NoOp.INSTANCE.apply(classVisitor, fieldDescription);
        verify(classVisitor).visitField(MODIFIER, FOO, BAR, QUX, null);
        verifyNoMoreInteractions(classVisitor);
        verify(fieldVisitor).visitEnd();
        verifyNoMoreInteractions(fieldVisitor);
    }

    @Test
    public void testNoOpEntry() throws Exception {
        assertThat(TypeWriter.FieldPool.Entry.NoOp.INSTANCE.getDefaultValue(), nullValue());
        assertThat(TypeWriter.FieldPool.Entry.NoOp.INSTANCE.getFieldAppender(),
                is((FieldAttributeAppender) FieldAttributeAppender.NoOp.INSTANCE));
    }
}
