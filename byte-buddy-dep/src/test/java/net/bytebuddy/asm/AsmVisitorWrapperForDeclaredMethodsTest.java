package net.bytebuddy.asm;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AsmVisitorWrapperForDeclaredMethodsTest {

    private static final int MODIFIERS = 42, FLAGS = 42;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ElementMatcher<? super MethodDescription> matcher;

    @Mock
    private AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper methodVisitorWrapper;

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private MethodDescription.InDefinedShape foo, bar;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private TypePool typePool;

    @Mock
    private MethodVisitor methodVisitor, wrappedVisitor;

    @Before
    public void setUp() throws Exception {
        when(foo.getInternalName()).thenReturn(FOO);
        when(foo.getDescriptor()).thenReturn(QUX);
        when(bar.getInternalName()).thenReturn(BAR);
        when(bar.getDescriptor()).thenReturn(BAZ);
        when(classVisitor.visitMethod(eq(MODIFIERS), any(String.class), any(String.class), eq(BAZ), eq(new String[]{QUX + BAZ}))).thenReturn(methodVisitor);
        when(methodVisitorWrapper.wrap(instrumentedType, foo, methodVisitor, implementationContext, typePool, FLAGS, FLAGS * 2)).thenReturn(wrappedVisitor);
        when(matcher.matches(foo)).thenReturn(true);
    }

    @Test
    public void testMatchedInvokable() throws Exception {
        assertThat(new AsmVisitorWrapper.ForDeclaredMethods()
                .invokable(matcher, methodVisitorWrapper)
                .wrap(instrumentedType,
                        classVisitor,
                        implementationContext,
                        typePool,
                        new FieldList.Empty<FieldDescription.InDefinedShape>(),
                        new MethodList.Explicit<MethodDescription>(foo, bar),
                        FLAGS,
                        FLAGS * 2)
                .visitMethod(MODIFIERS, FOO, QUX, BAZ, new String[]{QUX + BAZ}), is(wrappedVisitor));
        verify(matcher).matches(foo);
        verifyNoMoreInteractions(matcher);
        verify(methodVisitorWrapper).wrap(instrumentedType, foo, methodVisitor, implementationContext, typePool, FLAGS, FLAGS * 2);
        verifyNoMoreInteractions(methodVisitorWrapper);
        verifyZeroInteractions(typePool);
    }

    @Test
    public void testNonMatchedInvokable() throws Exception {
        assertThat(new AsmVisitorWrapper.ForDeclaredMethods()
                .invokable(matcher, methodVisitorWrapper)
                .wrap(instrumentedType,
                        classVisitor,
                        implementationContext,
                        typePool,
                        new FieldList.Empty<FieldDescription.InDefinedShape>(),
                        new MethodList.Explicit<MethodDescription>(foo, bar),
                        FLAGS,
                        FLAGS * 2)
                .visitMethod(MODIFIERS, BAR, BAZ, BAZ, new String[]{QUX + BAZ}), is(methodVisitor));
        verify(matcher).matches(bar);
        verifyNoMoreInteractions(matcher);
        verifyZeroInteractions(methodVisitorWrapper);
        verifyZeroInteractions(typePool);
    }

    @Test
    public void testUnknownInvokable() throws Exception {
        assertThat(new AsmVisitorWrapper.ForDeclaredMethods()
                .invokable(matcher, methodVisitorWrapper)
                .wrap(instrumentedType,
                        classVisitor,
                        implementationContext,
                        typePool,
                        new FieldList.Empty<FieldDescription.InDefinedShape>(),
                        new MethodList.Explicit<MethodDescription>(foo, bar),
                        FLAGS,
                        FLAGS * 2)
                .visitMethod(MODIFIERS, FOO + BAR, QUX, BAZ, new String[]{QUX + BAZ}), is(methodVisitor));
        verifyZeroInteractions(matcher);
        verifyZeroInteractions(methodVisitorWrapper);
        verifyZeroInteractions(typePool);
    }

    @Test
    public void testNonMatchedMethod() throws Exception {
        assertThat(new AsmVisitorWrapper.ForDeclaredMethods()
                .method(matcher, methodVisitorWrapper)
                .wrap(instrumentedType,
                        classVisitor,
                        implementationContext,
                        typePool,
                        new FieldList.Empty<FieldDescription.InDefinedShape>(),
                        new MethodList.Explicit<MethodDescription>(foo, bar),
                        FLAGS,
                        FLAGS * 2)
                .visitMethod(MODIFIERS, FOO, QUX, BAZ, new String[]{QUX + BAZ}), is(methodVisitor));
        verifyZeroInteractions(matcher);
    }

    @Test
    public void testNonMatchedConstructor() throws Exception {
        assertThat(new AsmVisitorWrapper.ForDeclaredMethods()
                .constructor(matcher, methodVisitorWrapper)
                .wrap(instrumentedType,
                        classVisitor,
                        implementationContext,
                        typePool,
                        new FieldList.Empty<FieldDescription.InDefinedShape>(),
                        new MethodList.Explicit<MethodDescription>(foo, bar),
                        FLAGS,
                        FLAGS * 2)
                .visitMethod(MODIFIERS, FOO, QUX, BAZ, new String[]{QUX + BAZ}), is(methodVisitor));
        verifyZeroInteractions(matcher);
    }

    @Test
    public void testWriterFlags() throws Exception {
        assertThat(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(FLAGS).mergeWriter(0), is(FLAGS));
    }

    @Test
    public void testReaderFlags() throws Exception {
        assertThat(new AsmVisitorWrapper.ForDeclaredMethods().readerFlags(FLAGS).mergeReader(0), is(FLAGS));
    }
}
