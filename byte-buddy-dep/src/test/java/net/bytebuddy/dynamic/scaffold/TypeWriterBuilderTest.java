package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.TypeInitializer;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TypeWriterBuilderTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType;
    @Mock
    private TypeDescription superType;
    @Mock
    private TypeList emptyTypeList;
    @Mock
    private Instrumentation.Context.ExtractableView instrumentationContext;
    @Mock
    private ClassVisitorWrapper classVisitorWrapper;
    @Mock
    private FieldDescription firstField, secondField;
    @Mock
    private FieldRegistry.Compiled compiledFieldRegistry;
    @Mock
    private TypeWriter.FieldPool.Entry fieldPoolEntry;
    @Mock
    private FieldAttributeAppender.Factory fieldAttributeAppenderFactory;
    @Mock
    private FieldAttributeAppender fieldAttributeAppender;
    @Mock
    private MethodDescription simpleMethod, toAbstractMethod, fromAbstractMethod, skippedMethod;
    @Mock
    private MethodRegistry.Compiled compiledMethodRegistry;
    @Mock
    private MethodAttributeAppender methodAttributeAppender;
    @Mock
    private MethodRegistry.Compiled.Entry emptyImplementation, abstractImplementation, skipImplementation;
    @Mock
    private ByteCodeAppender emptyImplementationByteCodeAppender, abstractImplementationByteCodeAppender;
    @Mock
    private TypeInitializer typeInitializer;

    private TypeWriter.InGeneralPhase<?> typeWriter;

    @Before
    public void setUp() throws Exception {
        when(instrumentedType.getSupertype()).thenReturn(superType);
        when(superType.getInternalName()).thenReturn(Type.getInternalName(Object.class));
        when(instrumentedType.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        when(instrumentedType.getInternalName()).thenReturn(FOO);
        when(instrumentedType.getName()).thenReturn(FOO);
        when(instrumentedType.getInterfaces()).thenReturn(emptyTypeList);
        when(emptyTypeList.toInternalNames()).thenReturn(null);
        when(firstField.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        when(firstField.getInternalName()).thenReturn(BAR);
        when(firstField.getDescriptor()).thenReturn(Type.getDescriptor(Object.class));
        when(secondField.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        when(secondField.getInternalName()).thenReturn(QUX);
        when(secondField.getDescriptor()).thenReturn(Type.getDescriptor(long.class));
        when(compiledFieldRegistry.target(any(FieldDescription.class))).thenReturn(fieldPoolEntry);
        when(fieldPoolEntry.getFieldAppenderFactory()).thenReturn(fieldAttributeAppenderFactory);
        when(fieldAttributeAppenderFactory.make(any(TypeDescription.class))).thenReturn(fieldAttributeAppender);
        when(compiledMethodRegistry.target(simpleMethod)).thenReturn(emptyImplementation);
        when(compiledMethodRegistry.target(fromAbstractMethod)).thenReturn(emptyImplementation);
        when(emptyImplementation.isDefineMethod()).thenReturn(true);
        when(emptyImplementation.getAttributeAppender()).thenReturn(methodAttributeAppender);
        when(emptyImplementation.getByteCodeAppender()).thenReturn(emptyImplementationByteCodeAppender);
        when(emptyImplementationByteCodeAppender.appendsCode()).thenReturn(true);
        when(emptyImplementationByteCodeAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class)))
                .thenAnswer(new Answer<ByteCodeAppender.Size>() {
                    @Override
                    public ByteCodeAppender.Size answer(InvocationOnMock invocation) throws Throwable {
                        ((MethodVisitor) invocation.getArguments()[0]).visitInsn(Opcodes.RETURN);
                        return new ByteCodeAppender.Size(0, 1);
                    }
                });
        when(compiledMethodRegistry.target(toAbstractMethod)).thenReturn(abstractImplementation);
        when(abstractImplementation.isDefineMethod()).thenReturn(true);
        when(abstractImplementation.getAttributeAppender()).thenReturn(methodAttributeAppender);
        when(abstractImplementation.getByteCodeAppender()).thenReturn(abstractImplementationByteCodeAppender);
        when(compiledMethodRegistry.target(skippedMethod)).thenReturn(skipImplementation);
        when(skipImplementation.isDefineMethod()).thenReturn(false);
        when(simpleMethod.getExceptionTypes()).thenReturn(emptyTypeList);
        when(simpleMethod.getInternalName()).thenReturn(BAR);
        when(simpleMethod.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        when(simpleMethod.getDescriptor()).thenReturn(Type.getMethodDescriptor(Type.VOID_TYPE));
        when(fromAbstractMethod.getExceptionTypes()).thenReturn(emptyTypeList);
        when(fromAbstractMethod.getInternalName()).thenReturn(BAZ);
        when(fromAbstractMethod.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT);
        when(fromAbstractMethod.isAbstract()).thenReturn(true);
        when(fromAbstractMethod.getDescriptor()).thenReturn(Type.getMethodDescriptor(Type.VOID_TYPE));
        when(toAbstractMethod.getExceptionTypes()).thenReturn(emptyTypeList);
        when(toAbstractMethod.getInternalName()).thenReturn(QUX);
        when(toAbstractMethod.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        when(toAbstractMethod.getDescriptor()).thenReturn(Type.getMethodDescriptor(Type.VOID_TYPE));
        when(classVisitorWrapper.wrap(any(ClassVisitor.class))).thenAnswer(new Answer<ClassVisitor>() {
            @Override
            public ClassVisitor answer(InvocationOnMock invocation) throws Throwable {
                return (ClassVisitor) invocation.getArguments()[0];
            }
        });
        typeWriter = new TypeWriter.Builder<Object>(instrumentedType,
                typeInitializer,
                instrumentationContext,
                ClassFileVersion.forCurrentJavaVersion()).build(classVisitorWrapper);
        verify(classVisitorWrapper).wrap(any(ClassWriter.class));
    }

    @Test
    public void testGeneralPhase() throws Exception {
        TypeAttributeAppender typeAttributeAppender = mock(TypeAttributeAppender.class);
        assertDynamicType(typeWriter.attributeType(typeAttributeAppender).make(), false, false);
        verify(typeAttributeAppender).apply(any(ClassVisitor.class), eq(instrumentedType));
        verifyNoMoreInteractions(typeAttributeAppender);
    }

    @Test
    public void testFieldWriting() throws Exception {
        assertDynamicType(typeWriter.members().writeFields(Arrays.asList(firstField, secondField), compiledFieldRegistry).make(), true, false);
        verify(compiledFieldRegistry).target(firstField);
        verify(compiledFieldRegistry).target(secondField);
        verifyNoMoreInteractions(compiledFieldRegistry);
    }

    @Test
    public void testMethodWriting() throws Exception {
        assertDynamicType(typeWriter.members().writeMethods(Arrays.asList(simpleMethod, fromAbstractMethod, toAbstractMethod, skippedMethod),
                compiledMethodRegistry).make(), false, true);
        verify(compiledMethodRegistry).target(simpleMethod);
        verify(compiledMethodRegistry).target(fromAbstractMethod);
        verify(compiledMethodRegistry).target(toAbstractMethod);
        verify(compiledMethodRegistry).target(skippedMethod);
        verifyNoMoreInteractions(compiledMethodRegistry);
    }

    @Test
    public void testFieldAndMethodWriting() throws Exception {
        assertDynamicType(typeWriter.members().writeFields(Arrays.asList(firstField, secondField), compiledFieldRegistry)
                .writeMethods(Arrays.asList(simpleMethod, fromAbstractMethod, toAbstractMethod, skippedMethod),
                        compiledMethodRegistry).make(), true, true);
    }

    private void assertDynamicType(DynamicType.Unloaded<?> unloaded, boolean fields, boolean methods) throws Exception {
        assertThat(unloaded.getDescription().getName(), is(FOO));
        Class<?> loaded = unloaded.load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(loaded.getName(), is(FOO));
        assertEquals(Object.class, loaded.getSuperclass());
        assertThat(Modifier.isPublic(loaded.getModifiers()), is(true));
        if (fields) {
            assertThat(loaded.getDeclaredFields().length, is(2));
            assertEquals(Object.class, loaded.getDeclaredField(BAR).getType());
            assertThat(Modifier.isPublic(loaded.getDeclaredField(BAR).getModifiers()), is(true));
            assertEquals(long.class, loaded.getDeclaredField(QUX).getType());
            assertThat(Modifier.isPublic(loaded.getDeclaredField(QUX).getModifiers()), is(true));
        }
        if (methods) {
            assertThat(loaded.getDeclaredMethods().length, is(3));
            assertEquals(void.class, loaded.getDeclaredMethod(BAR).getReturnType());
            assertThat(Modifier.isPublic(loaded.getDeclaredMethod(BAR).getModifiers()), is(true));
            assertThat(loaded.getDeclaredMethod(BAR).getParameterTypes().length, is(0));
            assertThat(Modifier.isAbstract(loaded.getDeclaredMethod(BAR).getModifiers()), is(false));
            assertEquals(void.class, loaded.getDeclaredMethod(QUX).getReturnType());
            assertThat(loaded.getDeclaredMethod(QUX).getParameterTypes().length, is(0));
            assertThat(Modifier.isPublic(loaded.getDeclaredMethod(QUX).getModifiers()), is(true));
            assertThat(Modifier.isAbstract(loaded.getDeclaredMethod(QUX).getModifiers()), is(true));
            assertEquals(void.class, loaded.getDeclaredMethod(BAZ).getReturnType());
            assertThat(Modifier.isPublic(loaded.getDeclaredMethod(BAZ).getModifiers()), is(true));
            assertThat(loaded.getDeclaredMethod(BAZ).getParameterTypes().length, is(0));
            assertThat(Modifier.isAbstract(loaded.getDeclaredMethod(BAZ).getModifiers()), is(false));
        }
    }
}
