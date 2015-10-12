package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ImplementationContextDefaultTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {true, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE},
                {false, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC}
        });
    }

    private final boolean classType;

    private final int accessorMethodModifiers;

    private final int cacheFieldModifiers;

    public ImplementationContextDefaultTest(boolean classType, int accessorMethodModifiers, int cacheFieldModifiers) {
        this.classType = classType;
        this.accessorMethodModifiers = accessorMethodModifiers;
        this.cacheFieldModifiers = cacheFieldModifiers;
    }

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
    private TypeWriter.MethodPool.Record record, otherRecord;

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
    private MethodDescription.InDefinedShape firstSpecialMethod, secondSpecialMethod;

    @Mock
    private AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

    @Mock
    private TypeDescription firstSpecialType, secondSpecialType, firstSpecialReturnType, secondSpecialReturnType,
            firstSpecialParameterType, secondSpecialParameterType, firstSpecialExceptionType, secondSpecialExceptionType,
            firstDeclaringType, secondDeclaringType;

    @Mock
    private FieldDescription.InDefinedShape firstField, secondField;

    @Mock
    private TypeDescription firstFieldDeclaringType, secondFieldDeclaringType;

    private GenericTypeList firstSpecialExceptionTypes, secondSpecialExceptionTypes;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        firstSpecialExceptionTypes = new GenericTypeList.Explicit(Collections.singletonList(firstSpecialExceptionType));
        secondSpecialExceptionTypes = new GenericTypeList.Explicit(Collections.singletonList(secondSpecialExceptionType));
        when(instrumentedType.getInternalName()).thenReturn(BAZ);
        when(instrumentedType.asErasure()).thenReturn(instrumentedType);
        when(instrumentedType.isClassType()).thenReturn(classType);
        when(methodPool.target(any(MethodDescription.class))).thenReturn(record);
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
        when(firstSpecialParameterType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(firstSpecialReturnType.getDescriptor()).thenReturn(QUX);
        when(firstSpecialReturnType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(firstSpecialExceptionType.getInternalName()).thenReturn(FOO);
        when(firstSpecialExceptionType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(firstSpecialParameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstSpecialReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstSpecialInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(firstSpecialMethod.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(firstSpecialMethod,
                Collections.singletonList(firstSpecialParameterType)));
        when(secondSpecialInvocation.getMethodDescription()).thenReturn(secondSpecialMethod);
        when(secondSpecialInvocation.getTypeDescription()).thenReturn(secondSpecialType);
        when(secondSpecialMethod.getInternalName()).thenReturn(BAR);
        when(secondSpecialMethod.getReturnType()).thenReturn(secondSpecialReturnType);
        when(secondSpecialMethod.getExceptionTypes()).thenReturn(secondSpecialExceptionTypes);
        when(secondSpecialParameterType.getDescriptor()).thenReturn(BAR);
        when(secondSpecialReturnType.getDescriptor()).thenReturn(FOO);
        when(secondSpecialExceptionType.getInternalName()).thenReturn(BAZ);
        when(secondSpecialExceptionType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(secondSpecialParameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondSpecialParameterType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(secondSpecialReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondSpecialReturnType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(secondSpecialInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(secondSpecialMethod.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(secondSpecialMethod,
                Collections.singletonList(secondSpecialParameterType)));
        when(firstFieldType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(firstFieldType.asErasure()).thenReturn(firstFieldType);
        when(firstFieldType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(firstFieldType);
        when(firstField.getType()).thenReturn(firstFieldType);
        when(firstField.getName()).thenReturn(FOO);
        when(firstField.getInternalName()).thenReturn(FOO);
        when(firstField.getDescriptor()).thenReturn(BAR);
        when(firstField.getDeclaringType()).thenReturn(firstFieldDeclaringType);
        when(firstField.asDefined()).thenReturn(firstField);
        when(firstFieldDeclaringType.getInternalName()).thenReturn(QUX);
        when(secondFieldType.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(secondFieldType.asErasure()).thenReturn(secondFieldType);
        when(secondFieldType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(secondFieldType);
        when(secondField.getType()).thenReturn(secondFieldType);
        when(secondField.getName()).thenReturn(BAR);
        when(secondField.getInternalName()).thenReturn(BAR);
        when(secondField.getDescriptor()).thenReturn(FOO);
        when(secondField.getDeclaringType()).thenReturn(secondFieldDeclaringType);
        when(secondField.asDefined()).thenReturn(secondField);
        when(secondFieldDeclaringType.getInternalName()).thenReturn(BAZ);
        when(firstSpecialReturnType.asErasure()).thenReturn(firstSpecialReturnType);
        when(secondSpecialReturnType.asErasure()).thenReturn(secondSpecialReturnType);
        when(firstSpecialExceptionType.asErasure()).thenReturn(firstSpecialExceptionType);
        when(secondSpecialExceptionType.asErasure()).thenReturn(secondSpecialExceptionType);
        when(firstSpecialParameterType.asErasure()).thenReturn(firstSpecialParameterType);
        when(secondSpecialParameterType.asErasure()).thenReturn(secondSpecialParameterType);
        when(firstSpecialParameterType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(firstSpecialParameterType);
        when(secondSpecialParameterType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(secondSpecialParameterType);
        when(firstFieldDeclaringType.asErasure()).thenReturn(firstFieldDeclaringType);
        when(secondFieldDeclaringType.asErasure()).thenReturn(secondFieldDeclaringType);
        when(firstSpecialMethod.getDeclaringType()).thenReturn(firstSpecialType);
        when(firstSpecialType.asErasure()).thenReturn(firstSpecialType);
        when(secondSpecialMethod.getDeclaringType()).thenReturn(secondSpecialType);
        when(secondSpecialType.asErasure()).thenReturn(secondSpecialType);
    }

    @Test
    public void testInitialContextIsEmpty() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        assertThat(implementationContext.getRegisteredAuxiliaryTypes().size(), is(0));
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verifyZeroInteractions(classVisitor);
        verify(methodPool).target(new MethodDescription.Latent.TypeInitializer(instrumentedType));
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
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verifyZeroInteractions(classVisitor);
    }

    @Test
    public void testDrainNoUserCodeNoInjectedCodeNoTypeInitializer() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
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
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(record).getSort();
        verify(record).apply(classVisitor, implementationContext);
        verifyNoMoreInteractions(record);
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
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        when(injectedCode.isDefined()).thenReturn(true);
        when(otherTypeInitializer.isDefined()).thenReturn(true);
        when(typeInitializer.expandWith(injectedCodeAppender)).thenReturn(otherTypeInitializer);
        when(otherTypeInitializer.withReturn()).thenReturn(terminationAppender);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(record).getSort();
        verify(typeInitializer).expandWith(injectedCodeAppender);
        verifyNoMoreInteractions(typeInitializer);
        verify(injectedCode, atLeast(1)).isDefined();
        verify(injectedCode).getByteCodeAppender();
        verifyNoMoreInteractions(injectedCode);
        verify(otherTypeInitializer, atLeast(1)).isDefined();
        verify(otherTypeInitializer).withReturn();
        verifyNoMoreInteractions(otherTypeInitializer);
        verify(terminationAppender).apply(methodVisitor, implementationContext, new MethodDescription.Latent.TypeInitializer(instrumentedType));
        verifyNoMoreInteractions(terminationAppender);
    }

    @Test
    public void testDrainNoUserCodeNoInjectedCodeTypeInitializer() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        when(typeInitializer.isDefined()).thenReturn(true);
        when(typeInitializer.withReturn()).thenReturn(terminationAppender);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(record).getSort();
        verifyNoMoreInteractions(record);
        verify(typeInitializer, atLeast(1)).isDefined();
        verify(typeInitializer).withReturn();
        verifyNoMoreInteractions(typeInitializer);
        verify(injectedCode, atLeast(1)).isDefined();
        verifyNoMoreInteractions(injectedCode);
        verify(terminationAppender).apply(methodVisitor, implementationContext, new MethodDescription.Latent.TypeInitializer(instrumentedType));
        verifyNoMoreInteractions(terminationAppender);
    }

    @Test
    public void testDrainUserCodeNoInjectedCodeTypeInitializer() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED);
        when(typeInitializer.isDefined()).thenReturn(true);
        when(record.prepend(typeInitializer)).thenReturn(otherRecord);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(record).getSort();
        verify(record).prepend(typeInitializer);
        verifyNoMoreInteractions(record);
        verify(otherRecord).apply(classVisitor, implementationContext);
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
        assertThat(firstField.getName(), not(secondField.getName()));
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        when(typeInitializer.expandWith(any(ByteCodeAppender.class))).thenReturn(otherTypeInitializer);
        when(otherTypeInitializer.expandWith(any(ByteCodeAppender.class))).thenReturn(thirdTypeInitializer);
        when(thirdTypeInitializer.withReturn()).thenReturn(terminationAppender);
        when(thirdTypeInitializer.isDefined()).thenReturn(true);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitField(eq(cacheFieldModifiers),
                Mockito.startsWith(Implementation.Context.Default.FIELD_CACHE_PREFIX),
                eq(BAR),
                Mockito.isNull(String.class),
                Mockito.isNull(Object.class));
        verify(classVisitor).visitField(eq(cacheFieldModifiers),
                Mockito.startsWith(Implementation.Context.Default.FIELD_CACHE_PREFIX),
                eq(QUX),
                Mockito.isNull(String.class),
                Mockito.isNull(Object.class));
        verify(typeInitializer).expandWith(any(ByteCodeAppender.class));
        verify(otherTypeInitializer).expandWith(any(ByteCodeAppender.class));
        verify(thirdTypeInitializer).withReturn();
        verify(thirdTypeInitializer).isDefined();
        verify(terminationAppender).apply(methodVisitor, implementationContext, new MethodDescription.Latent.TypeInitializer(instrumentedType));
        verifyNoMoreInteractions(terminationAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotRegisterFieldAfterDraining() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                classFileVersion);
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verifyZeroInteractions(classVisitor);
        verify(methodPool).target(new MethodDescription.Latent.TypeInitializer(instrumentedType));
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
        MethodDescription.InDefinedShape firstMethodDescription = implementationContext.registerAccessorFor(firstSpecialInvocation);
        assertThat(firstMethodDescription.getParameters(), is((ParameterList) new ParameterList.Explicit.ForTypes(firstMethodDescription,
                Collections.singletonList(firstSpecialParameterType))));
        assertThat(firstMethodDescription.getReturnType(), is((GenericTypeDescription) firstSpecialReturnType));
        assertThat(firstMethodDescription.getInternalName(), startsWith(FOO));
        assertThat(firstMethodDescription.getModifiers(), is(accessorMethodModifiers));
        assertThat(firstMethodDescription.getExceptionTypes(), is(firstSpecialExceptionTypes));
        assertThat(implementationContext.registerAccessorFor(firstSpecialInvocation), is(firstMethodDescription));
        when(secondSpecialMethod.isStatic()).thenReturn(true);
        MethodDescription.InDefinedShape secondMethodDescription = implementationContext.registerAccessorFor(secondSpecialInvocation);
        assertThat(secondMethodDescription.getParameters(), is((ParameterList) new ParameterList.Explicit.ForTypes(secondMethodDescription,
                Collections.singletonList(secondSpecialParameterType))));
        assertThat(secondMethodDescription.getReturnType(), is((GenericTypeDescription) secondSpecialReturnType));
        assertThat(secondMethodDescription.getInternalName(), startsWith(BAR));
        assertThat(secondMethodDescription.getModifiers(), is(accessorMethodModifiers | Opcodes.ACC_STATIC));
        assertThat(secondMethodDescription.getExceptionTypes(), is(secondSpecialExceptionTypes));
        assertThat(implementationContext.registerAccessorFor(firstSpecialInvocation), is(firstMethodDescription));
        assertThat(implementationContext.registerAccessorFor(secondSpecialInvocation), is(secondMethodDescription));
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers), Matchers.startsWith(FOO),
                eq("(" + BAZ + ")" + QUX), isNull(String.class), aryEq(new String[]{FOO}));
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
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
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers), Matchers.startsWith(FOO),
                eq("(" + BAZ + ")" + QUX), isNull(String.class), aryEq(new String[]{FOO}));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 1);
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
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + BAR + ")" + FOO), isNull(String.class), aryEq(new String[]{BAZ}));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
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
        assertThat(firstFieldGetter.getReturnType(), is((GenericTypeDescription) firstFieldType));
        assertThat(firstFieldGetter.getInternalName(), startsWith(FOO));
        assertThat(firstFieldGetter.getModifiers(), is(accessorMethodModifiers));
        assertThat(firstFieldGetter.getExceptionTypes(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(implementationContext.registerGetterFor(firstField), is(firstFieldGetter));
        when(secondField.isStatic()).thenReturn(true);
        MethodDescription secondFieldGetter = implementationContext.registerGetterFor(secondField);
        assertThat(secondFieldGetter.getParameters(), is((ParameterList) new ParameterList.Empty()));
        assertThat(secondFieldGetter.getReturnType(), is((GenericTypeDescription) secondFieldType));
        assertThat(secondFieldGetter.getInternalName(), startsWith(BAR));
        assertThat(secondFieldGetter.getModifiers(), is(accessorMethodModifiers | Opcodes.ACC_STATIC));
        assertThat(secondFieldGetter.getExceptionTypes(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(implementationContext.registerGetterFor(firstField), is(firstFieldGetter));
        assertThat(implementationContext.registerGetterFor(secondField), is(secondFieldGetter));
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers), Matchers.startsWith(FOO),
                eq("()" + BAR), isNull(String.class), isNull(String[].class));
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
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
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers), Matchers.startsWith(FOO),
                eq("()" + BAR), isNull(String.class), isNull(String[].class));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
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
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
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
        MethodDescription.InDefinedShape firstFieldSetter = implementationContext.registerSetterFor(firstField);
        assertThat(firstFieldSetter.getParameters(), is((ParameterList) new ParameterList.Explicit.ForTypes(firstFieldSetter,
                Collections.singletonList(firstFieldType))));
        assertThat(firstFieldSetter.getReturnType(), is((GenericTypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(firstFieldSetter.getInternalName(), startsWith(FOO));
        assertThat(firstFieldSetter.getModifiers(), is(accessorMethodModifiers));
        assertThat(firstFieldSetter.getExceptionTypes(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(implementationContext.registerSetterFor(firstField), is(firstFieldSetter));
        when(secondField.isStatic()).thenReturn(true);
        MethodDescription.InDefinedShape secondFieldSetter = implementationContext.registerSetterFor(secondField);
        assertThat(secondFieldSetter.getParameters(), is((ParameterList) new ParameterList.Explicit.ForTypes(secondFieldSetter,
                Collections.singletonList(secondFieldType))));
        assertThat(secondFieldSetter.getReturnType(), is((GenericTypeDescription) new TypeDescription.ForLoadedType(void.class)));
        assertThat(secondFieldSetter.getInternalName(), startsWith(BAR));
        assertThat(secondFieldSetter.getModifiers(), is(accessorMethodModifiers | Opcodes.ACC_STATIC));
        assertThat(secondFieldSetter.getExceptionTypes(), is((GenericTypeList) new GenericTypeList.Empty()));
        assertThat(implementationContext.registerSetterFor(firstField), is(firstFieldSetter));
        assertThat(implementationContext.registerSetterFor(secondField), is(secondFieldSetter));
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers), Matchers.startsWith(FOO),
                eq("(" + BAR + ")V"), isNull(String.class), isNull(String[].class));
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
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
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers), Matchers.startsWith(FOO),
                eq("(" + BAR + ")V"), isNull(String.class), isNull(String[].class));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 1);
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
        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.SKIPPED);
        implementationContext.drain(classVisitor, methodPool, injectedCode);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + QUX + ")V"), isNull(String.class), isNull(String[].class));
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitVarInsn(Opcodes.ALOAD, 0);
        verify(methodVisitor).visitFieldInsn(Opcodes.PUTSTATIC, BAZ, BAR, FOO);
        verify(methodVisitor).visitInsn(Opcodes.RETURN);
        verify(methodVisitor).visitMaxs(1, 0);
        verify(methodVisitor).visitEnd();
    }
}
