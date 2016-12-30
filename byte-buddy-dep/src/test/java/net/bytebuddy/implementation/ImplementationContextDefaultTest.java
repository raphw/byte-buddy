package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
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
                {false, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL, Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE},
                {true, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC, Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC},
        });
    }

    private final boolean interfaceType;

    private final int accessorMethodModifiers;

    private final int cacheFieldModifiers;

    public ImplementationContextDefaultTest(boolean interfaceType, int accessorMethodModifiers, int cacheFieldModifiers) {
        this.interfaceType = interfaceType;
        this.accessorMethodModifiers = accessorMethodModifiers;
        this.cacheFieldModifiers = cacheFieldModifiers;
    }

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, firstDescription, secondDescription;

    @Mock
    private TypeInitializer typeInitializer, otherTypeInitializer, thirdTypeInitializer;

    @Mock
    private ClassFileVersion classFileVersion, auxiliaryClassFileVersion;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private FieldVisitor fieldVisitor;

    @Mock
    private AuxiliaryType auxiliaryType, otherAuxiliaryType;

    @Mock
    private DynamicType firstDynamicType, secondDynamicType;

    @Mock
    private TypeDescription.Generic firstFieldType, secondFieldType;

    @Mock
    private TypeDescription firstRawFieldType, secondRawFieldType;

    @Mock
    private StackManipulation firstFieldValue, secondFieldValue;

    @Mock
    private TypeDescription.Generic firstSpecialReturnType, secondSpecialReturnType;

    @Mock
    private TypeDescription firstRawSpecialReturnType, secondRawSpecialReturnType;

    @Mock
    private TypeDescription.Generic firstSpecialParameterType, secondSpecialParameterType;

    @Mock
    private TypeDescription firstRawSpecialParameterType, secondRawSpecialParameterType;

    @Mock
    private TypeDescription.Generic firstSpecialExceptionType, secondSpecialExceptionType;

    @Mock
    private TypeDescription firstRawSpecialExceptionType, secondRawSpecialExceptionType;

    @Mock
    private ByteCodeAppender injectedCodeAppender, terminationAppender;

    @Mock
    private Implementation.SpecialMethodInvocation firstSpecialInvocation, secondSpecialInvocation;

    @Mock
    private MethodDescription.InDefinedShape firstSpecialMethod, secondSpecialMethod;

    @Mock
    private AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

    @Mock
    private TypeDescription firstSpecialType, secondSpecialType;

    @Mock
    private FieldDescription.InDefinedShape firstField, secondField;

    @Mock
    private TypeDescription firstFieldDeclaringType, secondFieldDeclaringType;

    private TypeList.Generic firstSpecialExceptionTypes, secondSpecialExceptionTypes;

    @Mock
    private AnnotationValueFilter.Factory annotationValueFilterFactory;

    @Mock
    private TypeInitializer.Drain drain;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        firstSpecialExceptionTypes = new TypeList.Generic.Explicit(firstSpecialExceptionType);
        secondSpecialExceptionTypes = new TypeList.Generic.Explicit(secondSpecialExceptionType);
        when(instrumentedType.getInternalName()).thenReturn(BAZ);
        when(instrumentedType.asErasure()).thenReturn(instrumentedType);
        when(instrumentedType.isInterface()).thenReturn(interfaceType);
        when(auxiliaryType.make(any(String.class), any(ClassFileVersion.class), any(MethodAccessorFactory.class)))
                .thenReturn(firstDynamicType);
        when(firstDynamicType.getTypeDescription()).thenReturn(firstDescription);
        when(otherAuxiliaryType.make(any(String.class), any(ClassFileVersion.class), any(MethodAccessorFactory.class)))
                .thenReturn(secondDynamicType);
        when(secondDynamicType.getTypeDescription()).thenReturn(secondDescription);
        when(classVisitor.visitMethod(any(int.class), any(String.class), any(String.class), any(String.class), any(String[].class)))
                .thenReturn(methodVisitor);
        when(classVisitor.visitField(any(int.class), any(String.class), any(String.class), any(String.class), any(Object.class)))
                .thenReturn(fieldVisitor);
        when(firstFieldValue.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(secondFieldValue.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(firstFieldType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstFieldType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(firstFieldType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(firstFieldType);
        when(firstFieldType.asErasure()).thenReturn(firstRawFieldType);
        when(firstFieldType.asRawType()).thenReturn(firstFieldType);
        when(firstFieldType.asGenericType()).thenReturn(firstFieldType);
        when(firstFieldType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(firstFieldType);
        when(firstRawFieldType.asGenericType()).thenReturn(firstFieldType);
        when(firstRawFieldType.getDescriptor()).thenReturn(BAR);
        when(secondFieldType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondFieldType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(secondFieldType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(secondFieldType);
        when(secondFieldType.asErasure()).thenReturn(secondRawFieldType);
        when(secondFieldType.asRawType()).thenReturn(secondFieldType);
        when(secondFieldType.asGenericType()).thenReturn(secondFieldType);
        when(secondFieldType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(secondFieldType);
        when(secondRawFieldType.asGenericType()).thenReturn(secondFieldType);
        when(secondRawFieldType.getDescriptor()).thenReturn(QUX);
        when(injectedCodeAppender.apply(any(MethodVisitor.class), any(Implementation.Context.class), any(MethodDescription.class)))
                .thenReturn(new ByteCodeAppender.Size(0, 0));
        when(terminationAppender.apply(any(MethodVisitor.class), any(Implementation.Context.class), any(MethodDescription.class)))
                .thenReturn(new ByteCodeAppender.Size(0, 0));
        when(firstSpecialInvocation.getMethodDescription()).thenReturn(firstSpecialMethod);
        when(firstSpecialInvocation.getTypeDescription()).thenReturn(firstSpecialType);
        when(firstSpecialMethod.getReturnType()).thenReturn(firstSpecialReturnType);
        when(firstSpecialMethod.getInternalName()).thenReturn(FOO);
        when(firstSpecialMethod.getExceptionTypes()).thenReturn(firstSpecialExceptionTypes);
        when(firstRawSpecialParameterType.getDescriptor()).thenReturn(BAZ);
        when(firstSpecialParameterType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(firstRawSpecialReturnType.getDescriptor()).thenReturn(QUX);
        when(firstSpecialReturnType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(firstSpecialReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstSpecialReturnType.asRawType()).thenReturn(firstSpecialReturnType);
        when(firstRawSpecialExceptionType.getInternalName()).thenReturn(FOO);
        when(firstSpecialExceptionType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(firstSpecialExceptionType.asGenericType()).thenReturn(firstSpecialExceptionType);
        when(firstSpecialExceptionType.asRawType()).thenReturn(firstSpecialExceptionType);
        when(firstSpecialParameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(firstSpecialParameterType.asGenericType()).thenReturn(firstSpecialParameterType);
        when(firstSpecialParameterType.asRawType()).thenReturn(firstSpecialParameterType);
        when(firstSpecialInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(firstSpecialMethod.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(firstSpecialMethod, firstSpecialParameterType));
        when(secondSpecialInvocation.getMethodDescription()).thenReturn(secondSpecialMethod);
        when(secondSpecialInvocation.getTypeDescription()).thenReturn(secondSpecialType);
        when(secondSpecialMethod.getInternalName()).thenReturn(BAR);
        when(secondSpecialMethod.getReturnType()).thenReturn(secondSpecialReturnType);
        when(secondSpecialMethod.getExceptionTypes()).thenReturn(secondSpecialExceptionTypes);
        when(secondRawSpecialParameterType.getDescriptor()).thenReturn(BAR);
        when(secondRawSpecialReturnType.getDescriptor()).thenReturn(FOO);
        when(secondRawSpecialExceptionType.getInternalName()).thenReturn(BAZ);
        when(secondSpecialExceptionType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(secondSpecialExceptionType.asGenericType()).thenReturn(secondSpecialExceptionType);
        when(secondSpecialExceptionType.asRawType()).thenReturn(secondSpecialExceptionType);
        when(secondSpecialParameterType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondSpecialParameterType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(secondSpecialParameterType.asGenericType()).thenReturn(secondSpecialParameterType);
        when(secondSpecialParameterType.asRawType()).thenReturn(secondSpecialParameterType);
        when(secondSpecialReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(secondSpecialReturnType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(secondSpecialReturnType.asRawType()).thenReturn(secondSpecialReturnType);
        when(secondSpecialInvocation.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(new StackManipulation.Size(0, 0));
        when(secondSpecialMethod.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(secondSpecialMethod, secondSpecialParameterType));
        when(firstField.getType()).thenReturn(firstFieldType);
        when(firstField.getName()).thenReturn(FOO);
        when(firstField.getInternalName()).thenReturn(FOO);
        when(firstField.getDescriptor()).thenReturn(BAR);
        when(firstField.getDeclaringType()).thenReturn(firstFieldDeclaringType);
        when(firstField.asDefined()).thenReturn(firstField);
        when(firstFieldDeclaringType.getInternalName()).thenReturn(QUX);
        when(secondField.getType()).thenReturn(secondFieldType);
        when(secondField.getName()).thenReturn(BAR);
        when(secondField.getInternalName()).thenReturn(BAR);
        when(secondField.getDescriptor()).thenReturn(FOO);
        when(secondField.getDeclaringType()).thenReturn(secondFieldDeclaringType);
        when(secondField.asDefined()).thenReturn(secondField);
        when(secondFieldDeclaringType.getInternalName()).thenReturn(BAZ);
        when(firstSpecialReturnType.asErasure()).thenReturn(firstRawSpecialReturnType);
        when(secondSpecialReturnType.asErasure()).thenReturn(secondRawSpecialReturnType);
        when(firstSpecialExceptionType.asErasure()).thenReturn(firstRawSpecialExceptionType);
        when(secondSpecialExceptionType.asErasure()).thenReturn(secondRawSpecialExceptionType);
        when(firstSpecialParameterType.asErasure()).thenReturn(firstRawSpecialParameterType);
        when(secondSpecialParameterType.asErasure()).thenReturn(secondRawSpecialParameterType);
        when(firstSpecialParameterType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(firstSpecialParameterType);
        when(secondSpecialParameterType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(secondSpecialParameterType);
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
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        assertThat(implementationContext.getAuxiliaryTypes().size(), is(0));
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
        verifyZeroInteractions(classVisitor);
        verify(drain).apply(classVisitor, typeInitializer, implementationContext);
        verifyNoMoreInteractions(drain);
    }

    @Test
    public void testAuxiliaryTypeRegistration() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        assertThat(implementationContext.getAuxiliaryTypes().size(), is(0));
        assertThat(implementationContext.register(auxiliaryType), is(firstDescription));
        assertThat(implementationContext.getAuxiliaryTypes().size(), is(1));
        assertThat(implementationContext.getAuxiliaryTypes().contains(firstDynamicType), is(true));
        assertThat(implementationContext.register(otherAuxiliaryType), is(secondDescription));
        assertThat(implementationContext.getAuxiliaryTypes().size(), is(2));
        assertThat(implementationContext.getAuxiliaryTypes().contains(firstDynamicType), is(true));
        assertThat(implementationContext.getAuxiliaryTypes().contains(secondDynamicType), is(true));
        assertThat(implementationContext.register(auxiliaryType), is(firstDescription));
        assertThat(implementationContext.getAuxiliaryTypes().size(), is(2));
        assertThat(implementationContext.getAuxiliaryTypes().contains(firstDynamicType), is(true));
        assertThat(implementationContext.getAuxiliaryTypes().contains(secondDynamicType), is(true));
    }

    @Test
    public void testDrainEmpty() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
        verifyZeroInteractions(classVisitor);
        verify(drain).apply(classVisitor, typeInitializer, implementationContext);
        verifyNoMoreInteractions(drain);
    }

    @Test
    public void testDrainNoUserCodeNoInjectedCodeNoTypeInitializer() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(typeInitializer);
        verify(drain).apply(classVisitor, typeInitializer, implementationContext);
        verifyNoMoreInteractions(drain);
    }

    @Test
    public void testDrainUserCodeNoInjectedCodeNoTypeInitializer() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(typeInitializer);
        verify(drain).apply(classVisitor, typeInitializer, implementationContext);
        verifyNoMoreInteractions(drain);
    }

    @Test
    public void testDrainFieldCacheEntries() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        FieldDescription firstField = implementationContext.cache(firstFieldValue, firstRawFieldType);
        assertThat(implementationContext.cache(firstFieldValue, firstRawFieldType), is(firstField));
        FieldDescription secondField = implementationContext.cache(secondFieldValue, secondRawFieldType);
        assertThat(implementationContext.cache(secondFieldValue, secondRawFieldType), is(secondField));
        assertThat(firstField.getName(), not(secondField.getName()));
        when(typeInitializer.expandWith(any(ByteCodeAppender.class))).thenReturn(otherTypeInitializer);
        when(otherTypeInitializer.expandWith(any(ByteCodeAppender.class))).thenReturn(thirdTypeInitializer);
        when(thirdTypeInitializer.isDefined()).thenReturn(true);
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
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
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotRegisterFieldAfterDraining() throws Exception {
        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
        verifyZeroInteractions(classVisitor);
        verify(drain).apply(classVisitor, typeInitializer, implementationContext);
        verifyNoMoreInteractions(drain);
        implementationContext.cache(firstFieldValue, firstRawFieldType);
    }

    @Test
    public void testAccessorMethodRegistration() throws Exception {
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        MethodDescription.InDefinedShape firstMethodDescription = implementationContext.registerAccessorFor(firstSpecialInvocation, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(firstMethodDescription.getParameters(), is((ParameterList) new ParameterList.Explicit.ForTypes(firstMethodDescription, firstSpecialParameterType)));
        assertThat(firstMethodDescription.getReturnType(), is(firstSpecialReturnType));
        assertThat(firstMethodDescription.getInternalName(), startsWith(FOO));
        assertThat(firstMethodDescription.getModifiers(), is(accessorMethodModifiers));
        assertThat(firstMethodDescription.getExceptionTypes(), is(firstSpecialExceptionTypes));
        assertThat(implementationContext.registerAccessorFor(firstSpecialInvocation, MethodAccessorFactory.AccessType.DEFAULT), is(firstMethodDescription));
        when(secondSpecialMethod.isStatic()).thenReturn(true);
        MethodDescription.InDefinedShape secondMethodDescription = implementationContext.registerAccessorFor(secondSpecialInvocation, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(secondMethodDescription.getParameters(), is((ParameterList) new ParameterList.Explicit.ForTypes(secondMethodDescription, secondSpecialParameterType)));
        assertThat(secondMethodDescription.getReturnType(), is(secondSpecialReturnType));
        assertThat(secondMethodDescription.getInternalName(), startsWith(BAR));
        assertThat(secondMethodDescription.getModifiers(), is(accessorMethodModifiers | Opcodes.ACC_STATIC));
        assertThat(secondMethodDescription.getExceptionTypes(), is(secondSpecialExceptionTypes));
        assertThat(implementationContext.registerAccessorFor(firstSpecialInvocation, MethodAccessorFactory.AccessType.DEFAULT), is(firstMethodDescription));
        assertThat(implementationContext.registerAccessorFor(secondSpecialInvocation, MethodAccessorFactory.AccessType.DEFAULT), is(secondMethodDescription));
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers), Matchers.startsWith(FOO),
                eq("(" + BAZ + ")" + QUX), isNull(String.class), aryEq(new String[]{FOO}));
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + BAR + ")" + FOO), isNull(String.class), aryEq(new String[]{BAZ}));
    }

    @Test
    public void testAccessorMethodRegistrationWritesFirst() throws Exception {
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        MethodDescription firstMethodDescription = implementationContext.registerAccessorFor(firstSpecialInvocation, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(implementationContext.registerAccessorFor(firstSpecialInvocation, MethodAccessorFactory.AccessType.DEFAULT), is(firstMethodDescription));
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
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
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        MethodDescription secondMethodDescription = implementationContext.registerAccessorFor(secondSpecialInvocation, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(implementationContext.registerAccessorFor(secondSpecialInvocation, MethodAccessorFactory.AccessType.DEFAULT), is(secondMethodDescription));
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
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
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        MethodDescription firstFieldGetter = implementationContext.registerGetterFor(firstField, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(firstFieldGetter.getParameters(), is((ParameterList) new ParameterList.Empty<ParameterDescription>()));
        assertThat(firstFieldGetter.getReturnType(), is(firstFieldType));
        assertThat(firstFieldGetter.getInternalName(), startsWith(FOO));
        assertThat(firstFieldGetter.getModifiers(), is(accessorMethodModifiers));
        assertThat(firstFieldGetter.getExceptionTypes(), is((TypeList.Generic) new TypeList.Generic.Empty()));
        assertThat(implementationContext.registerGetterFor(firstField, MethodAccessorFactory.AccessType.DEFAULT), is(firstFieldGetter));
        when(secondField.isStatic()).thenReturn(true);
        MethodDescription secondFieldGetter = implementationContext.registerGetterFor(secondField, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(secondFieldGetter.getParameters(), is((ParameterList) new ParameterList.Empty<ParameterDescription>()));
        assertThat(secondFieldGetter.getReturnType(), is(secondFieldType));
        assertThat(secondFieldGetter.getInternalName(), startsWith(BAR));
        assertThat(secondFieldGetter.getModifiers(), is(accessorMethodModifiers | Opcodes.ACC_STATIC));
        assertThat(secondFieldGetter.getExceptionTypes(), is((TypeList.Generic) new TypeList.Generic.Empty()));
        assertThat(implementationContext.registerGetterFor(firstField, MethodAccessorFactory.AccessType.DEFAULT), is(firstFieldGetter));
        assertThat(implementationContext.registerGetterFor(secondField, MethodAccessorFactory.AccessType.DEFAULT), is(secondFieldGetter));
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers), Matchers.startsWith(FOO),
                eq("()" + BAR), isNull(String.class), isNull(String[].class));
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("()" + QUX), isNull(String.class), isNull(String[].class));
    }

    @Test
    public void testFieldGetterRegistrationWritesFirst() throws Exception {
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        MethodDescription firstMethodDescription = implementationContext.registerGetterFor(firstField, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(implementationContext.registerGetterFor(firstField, MethodAccessorFactory.AccessType.DEFAULT), is(firstMethodDescription));
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
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
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        MethodDescription secondMethodDescription = implementationContext.registerGetterFor(secondField, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(implementationContext.registerGetterFor(secondField, MethodAccessorFactory.AccessType.DEFAULT), is(secondMethodDescription));
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
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
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        MethodDescription.InDefinedShape firstFieldSetter = implementationContext.registerSetterFor(firstField, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(firstFieldSetter.getParameters(), is((ParameterList) new ParameterList.Explicit.ForTypes(firstFieldSetter, firstFieldType)));
        assertThat(firstFieldSetter.getReturnType(), is(TypeDescription.Generic.VOID));
        assertThat(firstFieldSetter.getInternalName(), startsWith(FOO));
        assertThat(firstFieldSetter.getModifiers(), is(accessorMethodModifiers));
        assertThat(firstFieldSetter.getExceptionTypes(), is((TypeList.Generic) new TypeList.Generic.Empty()));
        assertThat(implementationContext.registerSetterFor(firstField, MethodAccessorFactory.AccessType.DEFAULT), is(firstFieldSetter));
        when(secondField.isStatic()).thenReturn(true);
        MethodDescription.InDefinedShape secondFieldSetter = implementationContext.registerSetterFor(secondField, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(secondFieldSetter.getParameters(), is((ParameterList) new ParameterList.Explicit.ForTypes(secondFieldSetter, secondFieldType)));
        assertThat(secondFieldSetter.getReturnType(), is(TypeDescription.Generic.VOID));
        assertThat(secondFieldSetter.getInternalName(), startsWith(BAR));
        assertThat(secondFieldSetter.getModifiers(), is(accessorMethodModifiers | Opcodes.ACC_STATIC));
        assertThat(secondFieldSetter.getExceptionTypes(), is((TypeList.Generic) new TypeList.Generic.Empty()));
        assertThat(implementationContext.registerSetterFor(firstField, MethodAccessorFactory.AccessType.DEFAULT), is(firstFieldSetter));
        assertThat(implementationContext.registerSetterFor(secondField, MethodAccessorFactory.AccessType.DEFAULT), is(secondFieldSetter));
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers), Matchers.startsWith(FOO),
                eq("(" + BAR + ")V"), isNull(String.class), isNull(String[].class));
        verify(classVisitor).visitMethod(eq(accessorMethodModifiers | Opcodes.ACC_STATIC), Matchers.startsWith(BAR),
                eq("(" + QUX + ")V"), isNull(String.class), isNull(String[].class));
    }

    @Test
    public void testFieldSetterRegistrationWritesFirst() throws Exception {
        Implementation.Context.Default implementationContext = new Implementation.Context.Default(instrumentedType,
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        MethodDescription firstMethodDescription = implementationContext.registerSetterFor(firstField, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(implementationContext.registerSetterFor(firstField, MethodAccessorFactory.AccessType.DEFAULT), is(firstMethodDescription));
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
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
                classFileVersion,
                auxiliaryTypeNamingStrategy,
                typeInitializer,
                auxiliaryClassFileVersion);
        MethodDescription secondMethodDescription = implementationContext.registerSetterFor(secondField, MethodAccessorFactory.AccessType.DEFAULT);
        assertThat(implementationContext.registerSetterFor(secondField, MethodAccessorFactory.AccessType.DEFAULT), is(secondMethodDescription));
        implementationContext.drain(drain, classVisitor, annotationValueFilterFactory);
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
