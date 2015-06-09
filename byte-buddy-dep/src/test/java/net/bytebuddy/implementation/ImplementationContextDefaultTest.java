package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.MoreOpcodes;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;

public class ImplementationContextDefaultTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, firstDescription, secondDescription;

    @Mock
    private InstrumentedType.TypeInitializer typeInitializer, otherTypeInitializer, thirdTypeInitializer;

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
    private TypeWriter.MethodPool.Entry entry, otherEntry;

    @Mock
    private Implementation.Context.ExtractableView.InjectedCode injectedCode;

    @Mock
    private AuxiliaryType auxiliaryType, otherAuxiliaryType;

    @Mock
    private DynamicType firstDynamicType, secondDynamicType;

    @Mock
    private TypeDescription firstFieldType, secondFieldType;

    @Mock
    private StackManipulation firstFieldValue, secondFieldValue;

    @Mock
    private ByteCodeAppender injectedCodeAppender, terminationAppender;

    @Mock
    private Implementation.SpecialMethodInvocation firstSpecialInvocation, secondSpecialInvocation;

    @Mock
    private MethodDescription firstSpecialMethod, secondSpecialMethod;

    @Mock
    private AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

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
        when(firstFieldValue.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(secondFieldValue.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(firstFieldType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstFieldType.getDescriptor()).thenReturn(BAR);
        when(secondFieldType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondFieldType.getDescriptor()).thenReturn(QUX);
        when(injectedCode.getByteCodeAppender()).thenReturn(injectedCodeAppender);
        when(injectedCodeAppender.apply(any(MethodVisitor.class), any(Implementation.Context.class), any(MethodDescription.class)))
                .thenReturn(new ByteCodeAppender.Size(0, 0));
        when(terminationAppender.apply(any(MethodVisitor.class), any(Implementation.Context.class), any(MethodDescription.class)))
                .thenReturn(new ByteCodeAppender.Size(0, 0));
        when(firstSpecialInvocation.getMethodDescription()).thenReturn(firstSpecialMethod);
        when(firstSpecialInvocation.getTypeDescription()).thenReturn(firstSpecialType);
        when(firstSpecialMethod.getReturnType()).thenReturn(firstSpecialReturnType);
        when(firstSpecialMethod.getInternalName()).thenReturn(FOO);
        when(firstSpecialMethod.getExceptionTypes()).thenReturn(firstSpecialExceptionTypes);
        when(firstSpecialParameterType.getDescriptor()).thenReturn(BAZ);
        when(firstSpecialParameterType.getSort()).thenReturn(GenericTypeDescription.Sort.RAW);
        when(firstSpecialReturnType.getDescriptor()).thenReturn(QUX);
        when(firstSpecialReturnType.getSort()).thenReturn(GenericTypeDescription.Sort.RAW);
        when(firstSpecialExceptionType.getInternalName()).thenReturn(FOO);
        when(firstSpecialExceptionType.getSort()).thenReturn(GenericTypeDescription.Sort.RAW);
        when(firstSpecialParameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstSpecialReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstSpecialInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        ParameterList firstSpecialMethodParameters = ParameterList.Explicit.latent(firstSpecialMethod, Collections.singletonList(firstSpecialParameterType));
        when(firstSpecialMethod.getParameters()).thenReturn(firstSpecialMethodParameters);
        when(secondSpecialInvocation.getMethodDescription()).thenReturn(secondSpecialMethod);
        when(secondSpecialInvocation.getTypeDescription()).thenReturn(secondSpecialType);
        when(secondSpecialMethod.getInternalName()).thenReturn(BAR);
        when(secondSpecialMethod.getReturnType()).thenReturn(secondSpecialReturnType);
        when(secondSpecialMethod.getExceptionTypes()).thenReturn(secondSpecialExceptionTypes);
        when(secondSpecialParameterType.getDescriptor()).thenReturn(BAR);
        when(secondSpecialReturnType.getDescriptor()).thenReturn(FOO);
        when(secondSpecialExceptionType.getInternalName()).thenReturn(BAZ);
        when(secondSpecialExceptionType.getSort()).thenReturn(GenericTypeDescription.Sort.RAW);
        when(secondSpecialParameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondSpecialParameterType.getSort()).thenReturn(GenericTypeDescription.Sort.RAW);
        when(secondSpecialReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondSpecialReturnType.getSort()).thenReturn(GenericTypeDescription.Sort.RAW);
        when(secondSpecialInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        ParameterList secondSpecialMethodParameters = ParameterList.Explicit.latent(secondSpecialMethod, Collections.singletonList(secondSpecialParameterType));
        when(secondSpecialMethod.getParameters()).thenReturn(secondSpecialMethodParameters);
        when(firstFieldType.getSort()).thenReturn(GenericTypeDescription.Sort.RAW);
        when(firstFieldType.asRawType()).thenReturn(firstFieldType); // REFACTOR
        when(firstField.getType()).thenReturn(firstFieldType);
        when(firstField.getName()).thenReturn(FOO);
        when(firstField.getInternalName()).thenReturn(FOO);
        when(firstField.getDescriptor()).thenReturn(BAR);
        when(firstField.getDeclaringType()).thenReturn(firstFieldDeclaringType);
        when(firstFieldDeclaringType.getInternalName()).thenReturn(QUX);
        when(secondFieldType.getSort()).thenReturn(GenericTypeDescription.Sort.RAW);
        when(secondFieldType.asRawType()).thenReturn(secondFieldType); // REFACTOR
        when(secondField.getType()).thenReturn(secondFieldType);
        when(secondField.getName()).thenReturn(BAR);
        when(secondField.getInternalName()).thenReturn(BAR);
        when(secondField.getDescriptor()).thenReturn(FOO);
        when(secondField.getDeclaringType()).thenReturn(secondFieldDeclaringType);
        when(secondFieldDeclaringType.getInternalName()).thenReturn(BAZ);
        when(firstSpecialReturnType.asRawType()).thenReturn(firstSpecialReturnType); // REFACTOR
        when(secondSpecialReturnType.asRawType()).thenReturn(secondSpecialReturnType); // REFACTOR
        when(firstSpecialExceptionType.asRawType()).thenReturn(firstSpecialExceptionType); // REFACTOR
        when(secondSpecialExceptionType.asRawType()).thenReturn(secondSpecialExceptionType); // REFACTOR
        when(firstSpecialParameterType.asRawType()).thenReturn(firstSpecialParameterType); // REFACTOR
        when(secondSpecialParameterType.asRawType()).thenReturn(secondSpecialParameterType); // REFACTOR
    }

    @Test
    public void testInitialContextIsEmpty() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().size(), is(0));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verifyZeroInteractions(classVisitor);
        verify(methodPool).target(MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(methodPool);
        verify(injectedCode).isDefined();
        verifyNoMoreInteractions(injectedCode);
    }

    @Test
    public void testAuxiliaryTypeRegistration() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().size(), is(0));
        assertThat(implementationContext.register(auxiliaryType), is(firstDescription));
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().size(), is(1));
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().contains(firstDynamicType), is(true));
        assertThat(implementationContext.register(otherAuxiliaryType), is(secondDescription));
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().size(), is(2));
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().contains(firstDynamicType), is(true));
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().contains(secondDynamicType), is(true));
        assertThat(implementationContext.register(auxiliaryType), is(firstDescription));
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().size(), is(2));
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().contains(firstDynamicType), is(true));
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().contains(secondDynamicType), is(true));
    }

    @Test
    public void testDrainEmpty() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verifyZeroInteractions(classVisitor);
    }

    @Test
    public void testDrainNoUserCodeNoInjectedCodeNoTypeInitializer() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verifyZeroInteractions(classVisitor);
        verify(typeInitializer).isDefined();
        verifyNoMoreInteractions(typeInitializer);
        verify(injectedCode).isDefined();
        verifyNoMoreInteractions(injectedCode);
    }

    @Test
    public void testDrainUserCodeNoInjectedCodeNoTypeInitializer() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.IMPLEMENT);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(entry).getSort();
        verify(entry).apply(classVisitor, implementationContext, MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(entry);
        verifyZeroInteractions(classVisitor);
        verify(typeInitializer, atLeast(1)).isDefined();
        verifyNoMoreInteractions(typeInitializer);
        verify(injectedCode, atLeast(1)).isDefined();
        verifyNoMoreInteractions(injectedCode);
    }

    @Test
    public void testDrainNoUserCodeInjectedCodeNoTypeInitializer() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        when(injectedCode.isDefined()).thenReturn(true);
        when(otherTypeInitializer.isDefined()).thenReturn(true);
        when(typeInitializer.expandWith(injectedCodeAppender)).thenReturn(otherTypeInitializer);
        when(otherTypeInitializer.withReturn()).thenReturn(terminationAppender);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(entry).getSort();
        verify(typeInitializer).expandWith(injectedCodeAppender);
        verifyNoMoreInteractions(typeInitializer);
        verify(injectedCode, atLeast(1)).isDefined();
        verify(injectedCode).getByteCodeAppender();
        verifyNoMoreInteractions(injectedCode);
        verify(otherTypeInitializer, atLeast(1)).isDefined();
        verify(otherTypeInitializer).withReturn();
        verifyNoMoreInteractions(otherTypeInitializer);
        verify(terminationAppender).apply(methodVisitor, implementationContext, MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(terminationAppender);
    }

    @Test
    public void testDrainNoUserCodeNoInjectedCodeTypeInitializer() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        when(typeInitializer.isDefined()).thenReturn(true);
        when(typeInitializer.withReturn()).thenReturn(terminationAppender);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(entry).getSort();
        verifyNoMoreInteractions(entry);
        verify(typeInitializer, atLeast(1)).isDefined();
        verify(typeInitializer).withReturn();
        verifyNoMoreInteractions(typeInitializer);
        verify(injectedCode, atLeast(1)).isDefined();
        verifyNoMoreInteractions(injectedCode);
        verify(terminationAppender).apply(methodVisitor, implementationContext, MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(terminationAppender);
    }

    @Test
    public void testDrainUserCodeNoInjectedCodeTypeInitializer() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.IMPLEMENT);
        when(typeInitializer.isDefined()).thenReturn(true);
        when(entry.prepend(typeInitializer)).thenReturn(otherEntry);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(entry).getSort();
        verify(entry).prepend(typeInitializer);
        verifyNoMoreInteractions(entry);
        verify(otherEntry).apply(classVisitor, implementationContext, MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verify(typeInitializer, atLeast(1)).isDefined();
        verifyNoMoreInteractions(typeInitializer);
        verify(injectedCode, atLeast(1)).isDefined();
        verifyNoMoreInteractions(injectedCode);
    }

    @Test
    public void testDrainFieldCacheEntries() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        FieldDescription firstField = implementationContext.cache(firstFieldValue, firstFieldType);
        assertThat(implementationContext.cache(firstFieldValue, firstFieldType), is(firstField));
        FieldDescription secondField = implementationContext.cache(secondFieldValue, secondFieldType);
        assertThat(implementationContext.cache(secondFieldValue, secondFieldType), is(secondField));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        when(typeInitializer.expandWith(any(ByteCodeAppender.class))).thenReturn(otherTypeInitializer);
        when(otherTypeInitializer.expandWith(any(ByteCodeAppender.class))).thenReturn(thirdTypeInitializer);
        when(thirdTypeInitializer.withReturn()).thenReturn(terminationAppender);
        when(thirdTypeInitializer.isDefined()).thenReturn(true);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitField(eq(Implementation.Context.ExtractableView.FIELD_CACHE_MODIFIER),
                Mockito.startsWith(Implementation.Context.Default.FIELD_CACHE_PREFIX),
                eq(BAR),
                Mockito.isNull(String.class),
                Mockito.isNull(Object.class));
        verify(classVisitor).visitField(eq(Implementation.Context.ExtractableView.FIELD_CACHE_MODIFIER),
                Mockito.startsWith(Implementation.Context.Default.FIELD_CACHE_PREFIX),
                eq(QUX),
                Mockito.isNull(String.class),
                Mockito.isNull(Object.class));
        verify(typeInitializer).expandWith(any(ByteCodeAppender.class));
        verify(otherTypeInitializer).expandWith(any(ByteCodeAppender.class));
        verify(thirdTypeInitializer).withReturn();
        verify(thirdTypeInitializer).isDefined();
        verify(terminationAppender).apply(methodVisitor, implementationContext, MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(terminationAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotRegisterFieldAfterDraining() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verifyZeroInteractions(classVisitor);
        verify(methodPool).target(MethodDescription.Latent.typeInitializerOf(instrumentedType));
        verifyNoMoreInteractions(methodPool);
        verify(injectedCode).isDefined();
        verifyNoMoreInteractions(injectedCode);
        implementationContext.cache(firstFieldValue, firstFieldType);
    }

    @Test
    public void testAccessorMethodRegistration() throws Exception {
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        MethodDescription firstMethodDescription = implementationContext.registerAccessorFor(firstSpecialInvocation);
        assertThat(firstMethodDescription.getParameters(), is(ParameterList.Explicit.latent(firstMethodDescription, Collections.singletonList(firstSpecialParameterType))));
        assertThat(firstMethodDescription.getReturnType(), is(firstSpecialReturnType));
        assertThat(firstMethodDescription.getInternalName(), startsWith(FOO));
        assertThat(firstMethodDescription.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER));
        assertThat(firstMethodDescription.getExceptionTypes(), is(firstSpecialExceptionTypes));
        assertThat(implementationContext.registerAccessorFor(firstSpecialInvocation), is(firstMethodDescription));
        when(secondSpecialMethod.isStatic()).thenReturn(true);
        MethodDescription secondMethodDescription = implementationContext.registerAccessorFor(secondSpecialInvocation);
        assertThat(secondMethodDescription.getParameters(), is(ParameterList.Explicit.latent(secondMethodDescription, Collections.singletonList(secondSpecialParameterType))));
        assertThat(secondMethodDescription.getReturnType(), is(secondSpecialReturnType));
        assertThat(secondMethodDescription.getInternalName(), startsWith(BAR));
        assertThat(secondMethodDescription.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC));
        assertThat(secondMethodDescription.getExceptionTypes(), is(secondSpecialExceptionTypes));
        assertThat(implementationContext.registerAccessorFor(firstSpecialInvocation), is(firstMethodDescription));
        assertThat(implementationContext.registerAccessorFor(secondSpecialInvocation), is(secondMethodDescription));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER), Matchers.startsWith(FOO),
                eq("(" + BAZ + ")" + QUX), isNull(String.class), aryEq(new String[]{FOO}));
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + BAR + ")" + FOO), isNull(String.class), aryEq(new String[]{BAZ}));
    }

    @Test
    public void testAccessorMethodRegistrationWritesFirst() throws Exception {
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        MethodDescription firstMethodDescription = implementationContext.registerAccessorFor(firstSpecialInvocation);
        assertThat(implementationContext.registerAccessorFor(firstSpecialInvocation), is(firstMethodDescription));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER), Matchers.startsWith(FOO),
                eq("(" + BAZ + ")" + QUX), isNull(String.class), aryEq(new String[]{FOO}));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_1);
        verify(firstSpecialInvocation).apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitInsn(Opcodes.ARETURN);
        verify(methodVisitor).visitMaxs(2, 1);
        verify(methodVisitor).visitEnd();
    }

    @Test
    public void testAccessorMethodRegistrationWritesSecond() throws Exception {
        when(secondSpecialMethod.isStatic()).thenReturn(true);
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        MethodDescription secondMethodDescription = implementationContext.registerAccessorFor(secondSpecialInvocation);
        assertThat(implementationContext.registerAccessorFor(secondSpecialInvocation), is(secondMethodDescription));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + BAR + ")" + FOO), isNull(String.class), aryEq(new String[]{BAZ}));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(secondSpecialInvocation).apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitInsn(Opcodes.ARETURN);
        verify(methodVisitor).visitMaxs(1, 0);
        verify(methodVisitor).visitEnd();
    }

    @Test
    public void testFieldGetterRegistration() throws Exception {
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        MethodDescription firstFieldGetter = implementationContext.registerGetterFor(firstField);
        assertThat(firstFieldGetter.getParameters(), is((ParameterList) new ParameterList.Empty()));
        assertThat(firstFieldGetter.getReturnType(), is(firstFieldType));
        assertThat(firstFieldGetter.getInternalName(), startsWith(FOO));
        assertThat(firstFieldGetter.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER));
        assertThat(firstFieldGetter.getExceptionTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(implementationContext.registerGetterFor(firstField), is(firstFieldGetter));
        when(secondField.isStatic()).thenReturn(true);
        MethodDescription secondFieldGetter = implementationContext.registerGetterFor(secondField);
        assertThat(secondFieldGetter.getParameters(), is((ParameterList) new ParameterList.Empty()));
        assertThat(secondFieldGetter.getReturnType(), is(secondFieldType));
        assertThat(secondFieldGetter.getInternalName(), startsWith(BAR));
        assertThat(secondFieldGetter.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC));
        assertThat(secondFieldGetter.getExceptionTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(implementationContext.registerGetterFor(firstField), is(firstFieldGetter));
        assertThat(implementationContext.registerGetterFor(secondField), is(secondFieldGetter));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER), Matchers.startsWith(FOO),
                eq("()" + BAR), isNull(String.class), isNull(String[].class));
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("()" + QUX), isNull(String.class), isNull(String[].class));
    }

    @Test
    public void testFieldGetterRegistrationWritesFirst() throws Exception {
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        MethodDescription firstMethodDescription = implementationContext.registerGetterFor(firstField);
        assertThat(implementationContext.registerGetterFor(firstField), is(firstMethodDescription));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
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
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        MethodDescription secondMethodDescription = implementationContext.registerGetterFor(secondField);
        assertThat(implementationContext.registerGetterFor(secondField), is(secondMethodDescription));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
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
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        MethodDescription firstFieldSetter = implementationContext.registerSetterFor(firstField);
        assertThat(firstFieldSetter.getParameters(), is(ParameterList.Explicit.latent(firstFieldSetter, Collections.singletonList(firstFieldType))));
        assertThat(firstFieldSetter.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(firstFieldSetter.getInternalName(), startsWith(FOO));
        assertThat(firstFieldSetter.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER));
        assertThat(firstFieldSetter.getExceptionTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(implementationContext.registerSetterFor(firstField), is(firstFieldSetter));
        when(secondField.isStatic()).thenReturn(true);
        MethodDescription secondFieldSetter = implementationContext.registerSetterFor(secondField);
        assertThat(secondFieldSetter.getParameters(), is(ParameterList.Explicit.latent(secondFieldSetter, Collections.singletonList(secondFieldType))));
        assertThat(secondFieldSetter.getReturnType(), is((TypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(secondFieldSetter.getInternalName(), startsWith(BAR));
        assertThat(secondFieldSetter.getModifiers(), is(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC));
        assertThat(secondFieldSetter.getExceptionTypes(), is((TypeList) new TypeList.Empty()));
        assertThat(implementationContext.registerSetterFor(firstField), is(firstFieldSetter));
        assertThat(implementationContext.registerSetterFor(secondField), is(secondFieldSetter));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER), Matchers.startsWith(FOO),
                eq("(" + BAR + ")V"), isNull(String.class), isNull(String[].class));
        verify(classVisitor).visitMethod(eq(AuxiliaryType.MethodAccessorFactory.ACCESSOR_METHOD_MODIFIER | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + QUX + ")V"), isNull(String.class), isNull(String[].class));
    }

    @Test
    public void testFieldSetterRegistrationWritesFirst() throws Exception {
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        MethodDescription firstMethodDescription = implementationContext.registerSetterFor(firstField);
        assertThat(implementationContext.registerSetterFor(firstField), is(firstMethodDescription));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
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
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        MethodDescription secondMethodDescription = implementationContext.registerSetterFor(secondField);
        assertThat(implementationContext.registerSetterFor(secondField), is(secondMethodDescription));
        when(entry.getSort()).thenReturn(TypeWriter.MethodPool.Entry.Sort.SKIP);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
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
        ObjectPropertyAssertion.of(Implementation.Context.Default.class).applyMutable();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldCacheEntry.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.AccessorMethodDelegation.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldSetter.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldGetter.class).apply();
    }
}
