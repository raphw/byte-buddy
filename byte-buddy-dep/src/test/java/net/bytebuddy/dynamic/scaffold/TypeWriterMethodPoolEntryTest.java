package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.HashCodeEqualsTester;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TypeWriterMethodPoolEntryTest {

    private static final int MODIFIER = 42;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ByteCodeAppender first, second;

    @Mock
    private MethodAttributeAppender methodAttributeAppender;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Instrumentation.Context.ExtractableView instrumentationContext;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodDescription typeDescription;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getDescriptor()).thenReturn(BAR);
        when(methodDescription.getGenericSignature()).thenReturn(QUX);
        TypeList exceptionTypes = mock(TypeList.class);
        when(exceptionTypes.toInternalNames()).thenReturn(new String[]{BAZ});
        when(methodDescription.getExceptionTypes()).thenReturn(exceptionTypes);
        when(methodDescription.getAdjustedModifiers(any(Boolean.class))).thenReturn(MODIFIER);
        when(classVisitor.visitMethod(MODIFIER, FOO, BAR, QUX, new String[]{BAZ})).thenReturn(methodVisitor);
        when(first.apply(methodVisitor, instrumentationContext, methodDescription)).thenReturn(new ByteCodeAppender.Size(0, 0));
    }

    @Test
    public void testSimpleEntry() throws Exception {
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry.Simple(first, methodAttributeAppender);
        assertThat(entry.isDefineMethod(), is(true));
        assertThat(entry.getByteCodeAppender(), is(first));
        assertThat(entry.getAttributeAppender(), is(methodAttributeAppender));
    }

    @Test
    public void testSimpleEntryDoesAppendCode() throws Exception {
        when(first.appendsCode()).thenReturn(true);
        new TypeWriter.MethodPool.Entry.Simple(first, methodAttributeAppender)
                .apply(classVisitor, instrumentationContext, methodDescription);
        verify(classVisitor).visitMethod(MODIFIER, FOO, BAR, QUX, new String[]{BAZ});
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verify(first).appendsCode();
        verify(first).apply(methodVisitor, instrumentationContext, methodDescription);
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(methodAttributeAppender);
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(0, 0);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testSimpleEntryDoesNotAppendCode() throws Exception {
        new TypeWriter.MethodPool.Entry.Simple(first, methodAttributeAppender)
                .apply(classVisitor, instrumentationContext, methodDescription);
        verify(classVisitor).visitMethod(MODIFIER, FOO, BAR, QUX, new String[]{BAZ});
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verify(first).appendsCode();
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(methodAttributeAppender);
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testSimpleEntryHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(TypeWriter.MethodPool.Entry.Simple.class).apply();
    }

    @Test
    public void testSkipEntry() throws Exception {
        assertThat(TypeWriter.MethodPool.Entry.Skip.INSTANCE.isDefineMethod(), is(false));
    }

    @Test
    public void testSkipEntryDoesNotApply() throws Exception {
        TypeWriter.MethodPool.Entry.Skip.INSTANCE.apply(classVisitor, instrumentationContext, methodDescription);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(instrumentationContext);
        verifyZeroInteractions(methodDescription);
    }

    @Test(expected = IllegalStateException.class)
    public void testSkipEntryWithoutByteCodeAppender() throws Exception {
        TypeWriter.MethodPool.Entry.Skip.INSTANCE.getByteCodeAppender();
    }

    @Test(expected = IllegalStateException.class)
    public void testSkipEntryWithoutAttributeAppender() throws Exception {
        TypeWriter.MethodPool.Entry.Skip.INSTANCE.getAttributeAppender();
    }
}
