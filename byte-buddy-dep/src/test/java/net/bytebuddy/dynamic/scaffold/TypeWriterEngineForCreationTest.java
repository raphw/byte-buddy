package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TypeWriterEngineForCreationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final int TYPE_MODIFIER = 42, CLASS_VERSION = 50;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, superType;

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private ClassVisitorWrapper classVisitorWrapper;

    @Mock
    private TypeAttributeAppender typeAttributeAppender;

    @Mock
    private TypeWriter.FieldPool fieldPool;

    @Mock
    private TypeWriter.FieldPool.Entry firstFieldEntry, secondFieldEntry;

    @Mock
    private TypeWriter.MethodPool methodPool, otherMethodPool;

    @Mock
    private TypeWriter.MethodPool.Entry firstMethodEntry, secondMethodEntry;

    @Mock
    private MethodDescription firstMethod, secondMethod;

    @Mock
    private FieldDescription firstField, secondField;

    @Mock
    private Instrumentation.Context.ExtractableView instrumentationContext;

    @Mock
    private ClassVisitor classVisitor;

    private List<MethodDescription> invokableMethods;

    @Before
    public void setUp() throws Exception {
        invokableMethods = Arrays.asList(firstMethod, secondMethod);
        FieldList declaredFields = new FieldList.Explicit(Arrays.asList(firstField, secondField));
        when(instrumentedType.getSupertype()).thenReturn(superType);
        when(instrumentedType.getDeclaredFields()).thenReturn(declaredFields);
        when(instrumentedType.getInterfaces()).thenReturn(new TypeList.Empty());
        when(classFileVersion.getVersionNumber()).thenReturn(Opcodes.V1_6);
        when(classVisitorWrapper.wrap(any(ClassVisitor.class))).thenReturn(classVisitor);
        when(fieldPool.target(firstField)).thenReturn(firstFieldEntry);
        when(fieldPool.target(secondField)).thenReturn(secondFieldEntry);
        when(methodPool.target(firstMethod)).thenReturn(firstMethodEntry);
        when(methodPool.target(secondMethod)).thenReturn(secondMethodEntry);
        when(instrumentedType.getInternalName()).thenReturn(FOO);
        when(instrumentedType.getGenericSignature()).thenReturn(QUX);
        when(instrumentedType.getActualModifiers(any(boolean.class))).thenReturn(TYPE_MODIFIER);
        TypeList interfaceTypes = mock(TypeList.class);
        when(interfaceTypes.toInternalNames()).thenReturn(new String[]{BAZ});
        when(instrumentedType.getInterfaces()).thenReturn(interfaceTypes);
        when(classFileVersion.getVersionNumber()).thenReturn(CLASS_VERSION);
        when(superType.getInternalName()).thenReturn(BAR);
    }

    @Test
    public void testTypeWriting() throws Exception {
        assertThat(new TypeWriter.Engine.ForCreation(instrumentedType,
                classFileVersion,
                invokableMethods,
                classVisitorWrapper,
                typeAttributeAppender,
                fieldPool,
                methodPool).create(instrumentationContext), notNullValue());
        verify(classVisitor).visit(CLASS_VERSION, TYPE_MODIFIER, FOO, QUX, BAR, new String[]{BAZ});
        verify(classVisitor).visitEnd();
        verifyNoMoreInteractions(classVisitor);
        verify(fieldPool).target(firstField);
        verify(firstFieldEntry).apply(classVisitor, firstField);
        verify(fieldPool).target(secondField);
        verify(secondFieldEntry).apply(classVisitor, secondField);
        verifyNoMoreInteractions(fieldPool);
        verifyNoMoreInteractions(firstFieldEntry);
        verifyNoMoreInteractions(secondFieldEntry);
        verify(methodPool).target(firstMethod);
        verify(firstMethodEntry).apply(eq(classVisitor), any(Instrumentation.Context.ExtractableView.class), eq(firstMethod));
        verify(methodPool).target(secondMethod);
        verify(secondMethodEntry).apply(eq(classVisitor), any(Instrumentation.Context.ExtractableView.class), eq(secondMethod));
        verifyNoMoreInteractions(methodPool);
        verifyNoMoreInteractions(firstMethodEntry);
        verifyNoMoreInteractions(secondMethodEntry);
        verify(typeAttributeAppender).apply(classVisitor, instrumentedType);
        verifyNoMoreInteractions(typeAttributeAppender);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.Engine.ForCreation.class).apply();
    }
}
