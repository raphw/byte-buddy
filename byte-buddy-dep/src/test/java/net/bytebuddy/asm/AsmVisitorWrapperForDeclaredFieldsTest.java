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
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AsmVisitorWrapperForDeclaredFieldsTest {

    private static final int MODIFIERS = 42, IRRELEVANT = -1;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ElementMatcher<? super FieldDescription.InDefinedShape> matcher;

    @Mock
    private AsmVisitorWrapper.ForDeclaredFields.FieldVisitorWrapper fieldVisitorWrapper;

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private FieldDescription.InDefinedShape foo, bar;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private TypePool typePool;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private FieldVisitor fieldVisitor, wrappedVisitor;

    @Before
    public void setUp() throws Exception {
        when(foo.getInternalName()).thenReturn(FOO);
        when(foo.getDescriptor()).thenReturn(QUX);
        when(bar.getInternalName()).thenReturn(BAR);
        when(bar.getDescriptor()).thenReturn(QUX);
        when(classVisitor.visitField(eq(MODIFIERS), any(String.class), eq(QUX), eq(BAZ), eq(QUX + BAZ))).thenReturn(fieldVisitor);
        when(fieldVisitorWrapper.wrap(instrumentedType, foo, fieldVisitor)).thenReturn(wrappedVisitor);
        when(matcher.matches(foo)).thenReturn(true);
    }

    @Test
    public void testMatched() throws Exception {
        assertThat(new AsmVisitorWrapper.ForDeclaredFields()
                .field(matcher, fieldVisitorWrapper)
                .wrap(instrumentedType,
                        classVisitor,
                        implementationContext,
                        typePool,
                        new FieldList.Explicit<FieldDescription.InDefinedShape>(foo, bar),
                        new MethodList.Empty<MethodDescription>(),
                        IRRELEVANT,
                        IRRELEVANT)
                .visitField(MODIFIERS, FOO, QUX, BAZ, QUX + BAZ), is(wrappedVisitor));
        verify(matcher).matches(foo);
        verifyNoMoreInteractions(matcher);
        verify(fieldVisitorWrapper).wrap(instrumentedType, foo, fieldVisitor);
        verifyNoMoreInteractions(fieldVisitorWrapper);
    }

    @Test
    public void testNotMatched() throws Exception {
        assertThat(new AsmVisitorWrapper.ForDeclaredFields()
                .field(matcher, fieldVisitorWrapper)
                .wrap(instrumentedType,
                        classVisitor,
                        implementationContext,
                        typePool,
                        new FieldList.Explicit<FieldDescription.InDefinedShape>(foo, bar),
                        new MethodList.Empty<MethodDescription>(),
                        IRRELEVANT,
                        IRRELEVANT)
                .visitField(MODIFIERS, BAR, QUX, BAZ, QUX + BAZ), is(fieldVisitor));
        verify(matcher).matches(bar);
        verifyNoMoreInteractions(matcher);
        verifyZeroInteractions(fieldVisitorWrapper);
    }

    @Test
    public void testUnknown() throws Exception {
        assertThat(new AsmVisitorWrapper.ForDeclaredFields()
                .field(matcher, fieldVisitorWrapper)
                .wrap(instrumentedType,
                        classVisitor,
                        implementationContext,
                        typePool,
                        new FieldList.Explicit<FieldDescription.InDefinedShape>(foo, bar),
                        new MethodList.Empty<MethodDescription>(),
                        IRRELEVANT,
                        IRRELEVANT)
                .visitField(MODIFIERS, FOO + BAR, QUX, BAZ, QUX + BAZ), is(fieldVisitor));
        verifyZeroInteractions(matcher);
        verifyZeroInteractions(fieldVisitorWrapper);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AsmVisitorWrapper.ForDeclaredFields.class).apply();
        ObjectPropertyAssertion.of(AsmVisitorWrapper.ForDeclaredFields.Entry.class).apply();
        ObjectPropertyAssertion.of(AsmVisitorWrapper.ForDeclaredFields.DispatchingVisitor.class).create(new ObjectPropertyAssertion.Creator<FieldList<?>>() {
            @Override
            public FieldList<?> create() {
                return new FieldList.Explicit<FieldDescription.InDefinedShape>(mock(FieldDescription.InDefinedShape.class));
            }
        }).applyBasic();
    }
}
