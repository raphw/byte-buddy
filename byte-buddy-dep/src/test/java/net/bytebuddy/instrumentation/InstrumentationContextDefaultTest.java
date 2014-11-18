package net.bytebuddy.instrumentation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.MoreOpcodes;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class InstrumentationContextDefaultTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, firstDescription, secondDescription;
    @Mock
    private ClassFileVersion classFileVersion;
    @Mock
    private ClassVisitor classVisitor;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private FieldVisitor fieldVisitor;
    @Mock
    private TypeWriter.MethodPool methodPool;
    @Mock
    private TypeWriter.MethodPool.Entry entry;
    @Mock
    private Instrumentation.Context.ExtractableView.InjectedCode injectedCode;
    @Mock
    private AuxiliaryType auxiliaryType, otherAuxiliaryType;
    @Mock
    private DynamicType firstDynamicType, secondDynamicType;
    @Mock
    private TypeDescription firstFieldType, secondFieldType;
    @Mock
    private StackManipulation firstFieldValue, secondFieldValue, injectedCodeAppender;
    @Mock
    private MethodAttributeAppender attributeAppender;
    @Mock
    private ByteCodeAppender byteCodeAppender;
    @Mock
    private Instrumentation.SpecialMethodInvocation firstSpecialInvocation, secondSpecialInvocation;
    @Mock
    private MethodDescription firstSpecialMethod, secondSpecialMethod;
    @Mock
    private TypeDescription firstSpecialType, secondSpecialType, firstSpecialReturnType, secondSpecialReturnType,
            firstSpecialParameterType, secondSpecialParameterType, firstSpecialExceptionType, secondSpecialExceptionType;
    @Mock
    private FieldDescription firstField, secondField;
    @Mock
    private TypeDescription firstFieldDeclaringType, secondFieldDeclaringType;

    private TypeList firstSpecialExceptionTypes, secondSpecialExceptionTypes;

    @Before
    public void setUp() throws Exception {
        firstSpecialExceptionTypes = new TypeList.Explicit(Collections.singletonList(firstSpecialExceptionType));
        secondSpecialExceptionTypes = new TypeList.Explicit(Collections.singletonList(secondSpecialExceptionType));
        when(instrumentedType.getInternalName()).thenReturn(BAZ);
        when(methodPool.target(any(MethodDescription.class))).thenReturn(entry);
        when(auxiliaryType.make(any(String.class), any(ClassFileVersion.class), any(AuxiliaryType.MethodAccessorFactory.class)))
                .thenReturn(firstDynamicType);
        when(firstDynamicType.getTypeDescription()).thenReturn(firstDescription);
        when(otherAuxiliaryType.make(any(String.class), any(ClassFileVersion.class), any(AuxiliaryType.MethodAccessorFactory.class)))
                .thenReturn(secondDynamicType);
        when(secondDynamicType.getTypeDescription()).thenReturn(secondDescription);
        when(classVisitor.visitMethod(any(int.class), any(String.class), any(String.class), any(String.class), any(String[].class)))
                .thenReturn(methodVisitor);
        when(classVisitor.visitField(any(int.class), any(String.class), any(String.class), any(String.class), any(Object.class)))
                .thenReturn(fieldVisitor);
        when(firstFieldValue.apply(any(MethodVisitor.class), any(Instrumentation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(secondFieldValue.apply(any(MethodVisitor.class), any(Instrumentation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(firstFieldType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstFieldType.getDescriptor()).thenReturn(BAR);
        when(secondFieldType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondFieldType.getDescriptor()).thenReturn(QUX);
        when(entry.getAttributeAppender()).thenReturn(attributeAppender);
        when(entry.getByteCodeAppender()).thenReturn(byteCodeAppender);
        when(byteCodeAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class), any(MethodDescription.class)))
                .thenReturn(new ByteCodeAppender.Size(0, 0));
        when(injectedCode.getInjectedCode()).thenReturn(injectedCodeAppender);
        when(injectedCodeAppender.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(firstSpecialInvocation.getMethodDescription()).thenReturn(firstSpecialMethod);
        when(firstSpecialInvocation.getTypeDescription()).thenReturn(firstSpecialType);
        when(firstSpecialMethod.getReturnType()).thenReturn(firstSpecialReturnType);
        when(firstSpecialMethod.getInternalName()).thenReturn(FOO);
        when(firstSpecialMethod.getParameterTypes()).thenReturn(new TypeList.Explicit(Arrays.asList(firstSpecialParameterType)));
        when(firstSpecialMethod.getExceptionTypes()).thenReturn(firstSpecialExceptionTypes);
        when(firstSpecialParameterType.getDescriptor()).thenReturn(BAZ);
        when(firstSpecialReturnType.getDescriptor()).thenReturn(QUX);
        when(firstSpecialExceptionType.getInternalName()).thenReturn(FOO);
        when(firstSpecialParameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstSpecialReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstSpecialInvocation.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(secondSpecialInvocation.getMethodDescription()).thenReturn(secondSpecialMethod);
        when(secondSpecialInvocation.getTypeDescription()).thenReturn(secondSpecialType);
        when(secondSpecialMethod.getInternalName()).thenReturn(BAR);
        when(secondSpecialMethod.getReturnType()).thenReturn(secondSpecialReturnType);
        when(secondSpecialMethod.getParameterTypes()).thenReturn(new TypeList.Explicit(Arrays.asList(secondSpecialParameterType)));
        when(secondSpecialMethod.getExceptionTypes()).thenReturn(secondSpecialExceptionTypes);
        when(secondSpecialParameterType.getDescriptor()).thenReturn(BAR);
        when(secondSpecialReturnType.getDescriptor()).thenReturn(FOO);
        when(secondSpecialExceptionType.getInternalName()).thenReturn(BAZ);
        when(secondSpecialParameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondSpecialReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondSpecialInvocation.apply(any(MethodVisitor.class), any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(firstField.getFieldType()).thenReturn(firstFieldType);
        when(firstField.getName()).thenReturn(FOO);
        when(firstField.getInternalName()).thenReturn(FOO);
        when(firstField.getDescriptor()).thenReturn(BAR);
        when(firstField.getDeclaringType()).thenReturn(firstFieldDeclaringType);
        when(firstFieldDeclaringType.getInternalName()).thenReturn(QUX);
        when(secondField.getFieldType()).thenReturn(secondFieldType);
        when(secondField.getName()).thenReturn(BAR);
        when(secondField.getInternalName()).thenReturn(BAR);
        when(secondField.getDescriptor()).thenReturn(FOO);
        when(secondField.getDeclaringType()).thenReturn(secondFieldDeclaringType);
        when(secondFieldDeclaringType.getInternalName()).thenReturn(BAZ);
    }

    @Test
    public void testInitialContextIsEmpty() throws Exception {
        Instrumentation.Context.ExtractableView instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        assertThat(instrumentationContext.getRegisteredAuxiliaryTypes().size(), is(0));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verifyZeroInteractions(classVisitor);
        verify(methodPool).target(MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(methodPool);
        verify(injectedCode).isInjected();
        verifyNoMoreInteractions(injectedCode);
    }

    @Test
    public void testAuxiliaryTypeRegistration() throws Exception {
        Instrumentation.Context.ExtractableView instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        assertThat(instrumentationContext.getRegisteredAuxiliaryTypes().size(), is(0));
        assertThat(instrumentationContext.register(auxiliaryType), is(firstDescription));
        assertThat(instrumentationContext.getRegisteredAuxiliaryTypes().size(), is(1));
        assertThat(instrumentationContext.getRegisteredAuxiliaryTypes().contains(firstDynamicType), is(true));
        assertThat(instrumentationContext.register(otherAuxiliaryType), is(secondDescription));
        assertThat(instrumentationContext.getRegisteredAuxiliaryTypes().size(), is(2));
        assertThat(instrumentationContext.getRegisteredAuxiliaryTypes().contains(firstDynamicType), is(true));
        assertThat(instrumentationContext.getRegisteredAuxiliaryTypes().contains(secondDynamicType), is(true));
        assertThat(instrumentationContext.register(auxiliaryType), is(firstDescription));
        assertThat(instrumentationContext.getRegisteredAuxiliaryTypes().size(), is(2));
        assertThat(instrumentationContext.getRegisteredAuxiliaryTypes().contains(firstDynamicType), is(true));
        assertThat(instrumentationContext.getRegisteredAuxiliaryTypes().contains(secondDynamicType), is(true));
    }

    @Test
    public void testFieldCachingWithoutUserCodeOrInjectedCode() throws Exception {
        Instrumentation.Context.ExtractableView instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        FieldDescription firstField = instrumentationContext.cache(firstFieldValue, firstFieldType);
        assertThat(instrumentationContext.cache(firstFieldValue, firstFieldType), is(firstField));
        FieldDescription secondField = instrumentationContext.cache(secondFieldValue, secondFieldType);
        assertThat(instrumentationContext.cache(firstFieldValue, firstFieldType), is(firstField));
        assertThat(instrumentationContext.cache(secondFieldValue, secondFieldType), is(secondField));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitField(eq(Instrumentation.Context.ExtractableView.FIELD_CACHE_MODIFIER),
                any(String.class), eq(BAR), isNull(String.class), isNull());
        verify(classVisitor).visitField(eq(Instrumentation.Context.ExtractableView.FIELD_CACHE_MODIFIER),
                any(String.class), eq(QUX), isNull(String.class), isNull());
        verify(classVisitor).visitMethod(eq(MethodDescription.TYPE_INITIALIZER_MODIFIER),
                eq(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME),
                eq("()V"), isNull(String.class), isNull(String[].class));
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(firstFieldValue).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitFieldInsn(eq(Opcodes.PUTSTATIC), eq(BAZ), any(String.class), eq(BAR));
        verify(secondFieldValue).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitFieldInsn(eq(Opcodes.PUTSTATIC), eq(BAZ), any(String.class), eq(QUX));
        verify(methodVisitor).visitInsn(Opcodes.RETURN);
        verify(methodVisitor).visitMaxs(0, 0);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(firstFieldValue);
        verifyNoMoreInteractions(secondFieldValue);
        verify(methodPool).target(MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(methodPool);
        verify(injectedCode).isInjected();
        verifyNoMoreInteractions(injectedCode);
        verifyZeroInteractions(injectedCodeAppender);
        verifyZeroInteractions(attributeAppender);
        verifyZeroInteractions(byteCodeAppender);
    }

    @Test
    public void testFieldCachingWithUserCodeAndWithoutInjectedCode() throws Exception {
        when(entry.isDefineMethod()).thenReturn(true);
        when(byteCodeAppender.appendsCode()).thenReturn(true);
        Instrumentation.Context.ExtractableView instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        FieldDescription firstField = instrumentationContext.cache(firstFieldValue, firstFieldType);
        assertThat(instrumentationContext.cache(firstFieldValue, firstFieldType), is(firstField));
        FieldDescription secondField = instrumentationContext.cache(secondFieldValue, secondFieldType);
        assertThat(instrumentationContext.cache(firstFieldValue, firstFieldType), is(firstField));
        assertThat(instrumentationContext.cache(secondFieldValue, secondFieldType), is(secondField));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitField(eq(Instrumentation.Context.ExtractableView.FIELD_CACHE_MODIFIER),
                any(String.class), eq(BAR), isNull(String.class), isNull());
        verify(classVisitor).visitField(eq(Instrumentation.Context.ExtractableView.FIELD_CACHE_MODIFIER),
                any(String.class), eq(QUX), isNull(String.class), isNull());
        verify(classVisitor).visitMethod(eq(MethodDescription.TYPE_INITIALIZER_MODIFIER),
                eq(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME),
                eq("()V"), isNull(String.class), isNull(String[].class));
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(firstFieldValue).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitFieldInsn(eq(Opcodes.PUTSTATIC), eq(BAZ), any(String.class), eq(BAR));
        verify(secondFieldValue).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitFieldInsn(eq(Opcodes.PUTSTATIC), eq(BAZ), any(String.class), eq(QUX));
        verify(attributeAppender).apply(methodVisitor, MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verify(byteCodeAppender).appendsCode();
        verify(byteCodeAppender).apply(methodVisitor, instrumentationContext, MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(byteCodeAppender);
        verify(methodVisitor).visitMaxs(0, 0);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(firstFieldValue);
        verifyNoMoreInteractions(secondFieldValue);
        verify(methodPool).target(MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(methodPool);
        verify(injectedCode).isInjected();
        verifyNoMoreInteractions(injectedCode);
        verifyZeroInteractions(injectedCodeAppender);
    }

    @Test
    public void testFieldCachingWithoutUserCodeAndWithInjectedCode() throws Exception {
        when(byteCodeAppender.appendsCode()).thenReturn(true);
        when(injectedCode.isInjected()).thenReturn(true);
        Instrumentation.Context.ExtractableView instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        FieldDescription firstField = instrumentationContext.cache(firstFieldValue, firstFieldType);
        assertThat(instrumentationContext.cache(firstFieldValue, firstFieldType), is(firstField));
        FieldDescription secondField = instrumentationContext.cache(secondFieldValue, secondFieldType);
        assertThat(instrumentationContext.cache(firstFieldValue, firstFieldType), is(firstField));
        assertThat(instrumentationContext.cache(secondFieldValue, secondFieldType), is(secondField));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitField(eq(Instrumentation.Context.ExtractableView.FIELD_CACHE_MODIFIER),
                any(String.class), eq(BAR), isNull(String.class), isNull());
        verify(classVisitor).visitField(eq(Instrumentation.Context.ExtractableView.FIELD_CACHE_MODIFIER),
                any(String.class), eq(QUX), isNull(String.class), isNull());
        verify(classVisitor).visitMethod(eq(MethodDescription.TYPE_INITIALIZER_MODIFIER),
                eq(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME),
                eq("()V"), isNull(String.class), isNull(String[].class));
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(firstFieldValue).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitFieldInsn(eq(Opcodes.PUTSTATIC), eq(BAZ), any(String.class), eq(BAR));
        verify(secondFieldValue).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitFieldInsn(eq(Opcodes.PUTSTATIC), eq(BAZ), any(String.class), eq(QUX));
        verify(injectedCode).isInjected();
        verify(injectedCode).getInjectedCode();
        verify(injectedCodeAppender).apply(methodVisitor, instrumentationContext);
        verifyNoMoreInteractions(injectedCode);
        verify(entry).isDefineMethod();
        verify(methodVisitor).visitInsn(Opcodes.RETURN);
        verify(methodVisitor).visitMaxs(0, 0);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(firstFieldValue);
        verifyNoMoreInteractions(secondFieldValue);
        verify(methodPool).target(MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(methodPool);
        verifyZeroInteractions(attributeAppender);
        verifyZeroInteractions(byteCodeAppender);
    }

    @Test
    public void testFieldCachingWithUserCodeAndWithInjectedCode() throws Exception {
        when(entry.isDefineMethod()).thenReturn(true);
        when(byteCodeAppender.appendsCode()).thenReturn(true);
        when(injectedCode.isInjected()).thenReturn(true);
        Instrumentation.Context.ExtractableView instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        FieldDescription firstField = instrumentationContext.cache(firstFieldValue, firstFieldType);
        assertThat(instrumentationContext.cache(firstFieldValue, firstFieldType), is(firstField));
        FieldDescription secondField = instrumentationContext.cache(secondFieldValue, secondFieldType);
        assertThat(instrumentationContext.cache(firstFieldValue, firstFieldType), is(firstField));
        assertThat(instrumentationContext.cache(secondFieldValue, secondFieldType), is(secondField));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitField(eq(Instrumentation.Context.ExtractableView.FIELD_CACHE_MODIFIER),
                any(String.class), eq(BAR), isNull(String.class), isNull());
        verify(classVisitor).visitField(eq(Instrumentation.Context.ExtractableView.FIELD_CACHE_MODIFIER),
                any(String.class), eq(QUX), isNull(String.class), isNull());
        verify(classVisitor).visitMethod(eq(MethodDescription.TYPE_INITIALIZER_MODIFIER),
                eq(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME),
                eq("()V"), isNull(String.class), isNull(String[].class));
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(firstFieldValue).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitFieldInsn(eq(Opcodes.PUTSTATIC), eq(BAZ), any(String.class), eq(BAR));
        verify(secondFieldValue).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitFieldInsn(eq(Opcodes.PUTSTATIC), eq(BAZ), any(String.class), eq(QUX));
        verify(injectedCode).isInjected();
        verify(injectedCode).getInjectedCode();
        verify(injectedCodeAppender).apply(methodVisitor, instrumentationContext);
        verifyNoMoreInteractions(injectedCode);
        verify(entry).isDefineMethod();
        verify(entry, atLeast(1)).getAttributeAppender();
        verify(attributeAppender).apply(methodVisitor, MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verify(entry, atLeast(1)).getByteCodeAppender();
        verify(byteCodeAppender).appendsCode();
        verify(byteCodeAppender).apply(methodVisitor, instrumentationContext, MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(byteCodeAppender);
        verify(methodVisitor).visitMaxs(0, 0);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(firstFieldValue);
        verifyNoMoreInteractions(secondFieldValue);
        verify(methodPool).target(MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(methodPool);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotRegisterFieldAfterDraining() throws Exception {
        Instrumentation.Context.ExtractableView instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verifyZeroInteractions(classVisitor);
        verify(methodPool).target(MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(methodPool);
        verify(injectedCode).isInjected();
        verifyNoMoreInteractions(injectedCode);
        instrumentationContext.cache(firstFieldValue, firstFieldType);
    }

    @Test
    public void testAccessorMethodRegistration() throws Exception {
        Instrumentation.Context.Default instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        MethodDescription firstMethodDescription = instrumentationContext.registerAccessorFor(firstSpecialInvocation);
        assertThat(firstMethodDescription.getParameterTypes(), is((TypeList) new TypeList.Explicit(Arrays.asList(firstSpecialParameterType))));
        assertThat(firstMethodDescription.getReturnType(), is(firstSpecialReturnType));
        assertThat(firstMethodDescription.getInternalName(), startsWith(FOO));
        assertThat(firstMethodDescription.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER));
        assertThat(firstMethodDescription.getExceptionTypes(), is(firstSpecialExceptionTypes));
        assertThat(instrumentationContext.registerAccessorFor(firstSpecialInvocation), is(firstMethodDescription));
        when(secondSpecialMethod.isStatic()).thenReturn(true);
        MethodDescription secondMethodDescription = instrumentationContext.registerAccessorFor(secondSpecialInvocation);
        assertThat(secondMethodDescription.getParameterTypes(), is((TypeList) new TypeList.Explicit(Arrays.asList(secondSpecialParameterType))));
        assertThat(secondMethodDescription.getReturnType(), is(secondSpecialReturnType));
        assertThat(secondMethodDescription.getInternalName(), startsWith(BAR));
        assertThat(secondMethodDescription.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC));
        assertThat(secondMethodDescription.getExceptionTypes(), is(secondSpecialExceptionTypes));
        assertThat(instrumentationContext.registerAccessorFor(firstSpecialInvocation), is(firstMethodDescription));
        assertThat(instrumentationContext.registerAccessorFor(secondSpecialInvocation), is(secondMethodDescription));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER), Matchers.startsWith(FOO),
                eq("(" + BAZ + ")" + QUX), isNull(String.class), aryEq(new String[]{FOO}));
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + BAR + ")" + FOO), isNull(String.class), aryEq(new String[]{BAZ}));
    }

    @Test
    public void testAccessorMethodRegistrationWritesFirst() throws Exception {
        Instrumentation.Context.Default instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        MethodDescription firstMethodDescription = instrumentationContext.registerAccessorFor(firstSpecialInvocation);
        assertThat(instrumentationContext.registerAccessorFor(firstSpecialInvocation), is(firstMethodDescription));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER), Matchers.startsWith(FOO),
                eq("(" + BAZ + ")" + QUX), isNull(String.class), aryEq(new String[]{FOO}));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_1);
        verify(firstSpecialInvocation).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitInsn(Opcodes.ARETURN);
        verify(methodVisitor).visitMaxs(2, 1);
        verify(methodVisitor).visitEnd();
    }

    @Test
    public void testAccessorMethodRegistrationWritesSecond() throws Exception {
        when(secondSpecialMethod.isStatic()).thenReturn(true);
        Instrumentation.Context.Default instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        MethodDescription secondMethodDescription = instrumentationContext.registerAccessorFor(secondSpecialInvocation);
        assertThat(instrumentationContext.registerAccessorFor(secondSpecialInvocation), is(secondMethodDescription));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + BAR + ")" + FOO), isNull(String.class), aryEq(new String[]{BAZ}));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(secondSpecialInvocation).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitInsn(Opcodes.ARETURN);
        verify(methodVisitor).visitMaxs(1, 0);
        verify(methodVisitor).visitEnd();
    }

    @Test
    public void testFieldGetterRegistration() throws Exception {
        Instrumentation.Context.Default instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        MethodDescription firstFieldGetter = instrumentationContext.registerGetterFor(firstField);
        assertThat(firstFieldGetter.getParameterTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(firstFieldGetter.getReturnType(), is(firstFieldType));
        assertThat(firstFieldGetter.getInternalName(), startsWith(FOO));
        assertThat(firstFieldGetter.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER));
        assertThat(firstFieldGetter.getExceptionTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(instrumentationContext.registerGetterFor(firstField), is(firstFieldGetter));
        when(secondField.isStatic()).thenReturn(true);
        MethodDescription secondFieldGetter = instrumentationContext.registerGetterFor(secondField);
        assertThat(secondFieldGetter.getParameterTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(secondFieldGetter.getReturnType(), is(secondFieldType));
        assertThat(secondFieldGetter.getInternalName(), startsWith(BAR));
        assertThat(secondFieldGetter.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC));
        assertThat(secondFieldGetter.getExceptionTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(instrumentationContext.registerGetterFor(firstField), is(firstFieldGetter));
        assertThat(instrumentationContext.registerGetterFor(secondField), is(secondFieldGetter));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER), Matchers.startsWith(FOO),
                eq("()" + BAR), isNull(String.class), isNull(String[].class));
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("()" + QUX), isNull(String.class), isNull(String[].class));
    }

    @Test
    public void testFieldGetterRegistrationWritesFirst() throws Exception {
        Instrumentation.Context.Default instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        MethodDescription firstMethodDescription = instrumentationContext.registerGetterFor(firstField);
        assertThat(instrumentationContext.registerGetterFor(firstField), is(firstMethodDescription));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER), Matchers.startsWith(FOO),
                eq("()" + BAR), isNull(String.class), isNull(String[].class));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(methodVisitor).visitFieldInsn(Opcodes.GETFIELD, QUX, FOO, BAR);
        verify(methodVisitor).visitInsn(Opcodes.ARETURN);
        verify(methodVisitor).visitMaxs(1, 1);
        verify(methodVisitor).visitEnd();
    }

    @Test
    public void testFieldGetterRegistrationWritesSecond() throws Exception {
        when(secondField.isStatic()).thenReturn(true);
        Instrumentation.Context.Default instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        MethodDescription secondMethodDescription = instrumentationContext.registerGetterFor(secondField);
        assertThat(instrumentationContext.registerGetterFor(secondField), is(secondMethodDescription));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("()" + QUX), isNull(String.class), isNull(String[].class));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, BAZ, BAR, FOO);
        verify(methodVisitor).visitInsn(Opcodes.ARETURN);
        verify(methodVisitor).visitMaxs(0, 0);
        verify(methodVisitor).visitEnd();
    }

    @Test
    public void testFieldSetterRegistration() throws Exception {
        Instrumentation.Context.Default instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        MethodDescription firstFieldSetter = instrumentationContext.registerSetterFor(firstField);
        assertThat(firstFieldSetter.getParameterTypes(), is((TypeList) new TypeList.Explicit(Arrays.asList(firstFieldType))));
        assertThat(firstFieldSetter.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(firstFieldSetter.getInternalName(), startsWith(FOO));
        assertThat(firstFieldSetter.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER));
        assertThat(firstFieldSetter.getExceptionTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(instrumentationContext.registerSetterFor(firstField), is(firstFieldSetter));
        when(secondField.isStatic()).thenReturn(true);
        MethodDescription secondFieldSetter = instrumentationContext.registerSetterFor(secondField);
        assertThat(secondFieldSetter.getParameterTypes(), is((TypeList) new TypeList.Explicit(Arrays.asList(secondFieldType))));
        assertThat(secondFieldSetter.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(secondFieldSetter.getInternalName(), startsWith(BAR));
        assertThat(secondFieldSetter.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC));
        assertThat(secondFieldSetter.getExceptionTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(instrumentationContext.registerSetterFor(firstField), is(firstFieldSetter));
        assertThat(instrumentationContext.registerSetterFor(secondField), is(secondFieldSetter));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER), Matchers.startsWith(FOO),
                eq("(" + BAR + ")V"), isNull(String.class), isNull(String[].class));
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + QUX + ")V"), isNull(String.class), isNull(String[].class));
    }

    @Test
    public void testFieldSetterRegistrationWritesFirst() throws Exception {
        Instrumentation.Context.Default instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        MethodDescription firstMethodDescription = instrumentationContext.registerSetterFor(firstField);
        assertThat(instrumentationContext.registerSetterFor(firstField), is(firstMethodDescription));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER), Matchers.startsWith(FOO),
                eq("(" + BAR + ")V"), isNull(String.class), isNull(String[].class));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_1);
        verify(methodVisitor).visitFieldInsn(Opcodes.PUTFIELD, QUX, FOO, BAR);
        verify(methodVisitor).visitInsn(Opcodes.RETURN);
        verify(methodVisitor).visitMaxs(2, 1);
        verify(methodVisitor).visitEnd();
    }

    @Test
    public void testFieldSetterRegistrationWritesSecond() throws Exception {
        when(secondField.isStatic()).thenReturn(true);
        Instrumentation.Context.Default instrumentationContext = new Instrumentation.Context.Default(instrumentedType, classFileVersion);
        MethodDescription secondMethodDescription = instrumentationContext.registerSetterFor(secondField);
        assertThat(instrumentationContext.registerSetterFor(secondField), is(secondMethodDescription));
        instrumentationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + QUX + ")V"), isNull(String.class), isNull(String[].class));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(methodVisitor).visitFieldInsn(Opcodes.PUTSTATIC, BAZ, BAR, FOO);
        verify(methodVisitor).visitInsn(Opcodes.RETURN);
        verify(methodVisitor).visitMaxs(1, 0);
        verify(methodVisitor).visitEnd();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Instrumentation.Context.Default.FieldCacheAppender.class);
        ObjectPropertyAssertion.of(Instrumentation.Context.Default.FieldCacheEntry.class);
        ObjectPropertyAssertion.of(Instrumentation.Context.Default.AccessorMethodDelegation.class);
        ObjectPropertyAssertion.of(Instrumentation.Context.Default.FieldSetter.class);
        ObjectPropertyAssertion.of(Instrumentation.Context.Default.FieldGetter.class);
    }
}
