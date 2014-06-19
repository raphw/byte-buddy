package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.MoreOpcodes;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeExtensionDelegateTest {

    public static final String CACHED_VALUE = "cachedValue";
    private static final String FOO = "foo", BAR = "bar", QUX = "qux";
    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, returnType, parameterType, auxiliaryTypeDescription, typeDescription;
    @Mock
    private TypeList parameterTypes;
    @Mock
    private MethodDescription methodDescription;
    @Mock
    private ClassFileVersion classFileVersion;
    @Mock
    private Instrumentation.SpecialMethodInvocation specialMethodInvocation;
    @Mock
    private AuxiliaryType auxiliaryType;
    @Mock
    private DynamicType dynamicType;
    @Mock
    private StackManipulation stackManipulation;
    @Mock
    private TypeWriter.MethodPool methodPool;
    @Mock
    private TypeWriter.MethodPool.Entry methodPoolEntry;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;
    @Mock
    private MethodAttributeAppender methodAttributeAppender;
    @Mock
    private ByteCodeAppender byteCodeAppender;

    private TypeExtensionDelegate typeExtensionDelegate;

    @Before
    public void setUp() throws Exception {
        when(specialMethodInvocation.getMethodDescription()).thenReturn(methodDescription);
        when(parameterTypes.size()).thenReturn(1);
        when(parameterTypes.get(0)).thenReturn(parameterType);
        final List<TypeDescription> list = new ArrayList<TypeDescription>();
        list.add(parameterType);
        when(parameterTypes.listIterator()).then(new Answer<Iterator<TypeDescription>>() {
            @Override
            public ListIterator<TypeDescription> answer(InvocationOnMock invocation) throws Throwable {
                return list.listIterator();
            }
        });
        when(parameterTypes.iterator()).then(new Answer<Iterator<TypeDescription>>() {
            @Override
            public Iterator<TypeDescription> answer(InvocationOnMock invocation) throws Throwable {
                return list.iterator();
            }
        });
        when(parameterType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(methodDescription.getDeclaringType()).thenReturn(instrumentedType);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getParameterTypes()).thenReturn(parameterTypes);
        when(instrumentedType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(specialMethodInvocation.apply(Matchers.any(MethodVisitor.class), Matchers.any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        typeExtensionDelegate = new TypeExtensionDelegate(instrumentedType, classFileVersion);
        when(auxiliaryType.make(Matchers.any(String.class),
                Matchers.any(ClassFileVersion.class),
                Matchers.any(AuxiliaryType.MethodAccessorFactory.class)))
                .thenReturn(dynamicType);
        when(dynamicType.getDescription()).thenReturn(auxiliaryTypeDescription);
        when(methodPool.target(Matchers.any(MethodDescription.class))).thenReturn(methodPoolEntry);
        when(stackManipulation.apply(Matchers.any(MethodVisitor.class), Matchers.any(Instrumentation.Context.class)))
                .thenReturn(new StackManipulation.Size(0, 0));
        when(typeDescription.getStackSize()).thenReturn(StackSize.ZERO);
        when(instrumentedType.getInternalName()).thenReturn(BAR);
        when(typeDescription.getDescriptor()).thenReturn(QUX);
    }

    @Test
    public void testAccessorMethodResemblesAccessedMethod() throws Exception {
        MethodDescription methodDescription = typeExtensionDelegate.registerAccessorFor(specialMethodInvocation);
        assertThat(methodDescription.getDeclaringType(), is(instrumentedType));
        assertThat(methodDescription.getInternalName(), startsWith(FOO));
        assertThat(methodDescription.getReturnType(), is(returnType));
        assertThat(methodDescription.getParameterTypes(), is(parameterTypes));
        assertThat(methodDescription.getModifiers(), is(Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL));
        assertThat(typeExtensionDelegate.target(methodDescription), not(nullValue()));
        Iterator<MethodDescription> iterator = typeExtensionDelegate.getRegisteredAccessors().iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(methodDescription));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testAccessorMethodIsOnlyRegisteredOnce() throws Exception {
        MethodDescription methodDescription = typeExtensionDelegate.registerAccessorFor(specialMethodInvocation);
        MethodDescription other = typeExtensionDelegate.registerAccessorFor(specialMethodInvocation);
        assertThat(methodDescription, is(other));
        assertThat(typeExtensionDelegate.target(methodDescription), not(nullValue()));
        Iterator<MethodDescription> iterator = typeExtensionDelegate.getRegisteredAccessors().iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(methodDescription));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testAccessorMethodImplementation() throws Exception {
        MethodDescription methodDescription = typeExtensionDelegate.registerAccessorFor(specialMethodInvocation);
        TypeWriter.MethodPool.Entry entry = typeExtensionDelegate.target(methodDescription);
        assertThat(entry.isDefineMethod(), is(true));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        entry.getAttributeAppender().apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        assertThat(entry.getByteCodeAppender().appendsCode(), is(true));
        Instrumentation.Context instrumentationContext = mock(Instrumentation.Context.class);
        ByteCodeAppender.Size size = entry.getByteCodeAppender().apply(methodVisitor, instrumentationContext, this.methodDescription);
        assertThat(size.getLocalVariableSize(), is(0));
        assertThat(size.getOperandStackSize(), is(2));
        verify(specialMethodInvocation).apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_0);
        verify(methodVisitor).visitInsn(MoreOpcodes.ALOAD_1);
        verify(methodVisitor).visitInsn(Opcodes.ARETURN);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testAuxiliaryTypeRegistration() throws Exception {
        assertThat(typeExtensionDelegate.register(this.auxiliaryType), is(auxiliaryTypeDescription));
        assertThat(typeExtensionDelegate.getRegisteredAuxiliaryTypes().size(), is(1));
        assertThat(typeExtensionDelegate.getRegisteredAuxiliaryTypes(), hasItem(dynamicType));
    }

    @Test
    public void testAuxiliaryTypeRegistrationDoesNotRegisterDuplicates() throws Exception {
        assertThat(typeExtensionDelegate.register(this.auxiliaryType), is(auxiliaryTypeDescription));
        assertThat(typeExtensionDelegate.register(this.auxiliaryType), is(auxiliaryTypeDescription));
        assertThat(typeExtensionDelegate.getRegisteredAuxiliaryTypes().size(), is(1));
        assertThat(typeExtensionDelegate.getRegisteredAuxiliaryTypes(), hasItem(dynamicType));
    }

    @Test
    public void testCacheFields() throws Exception {
        typeExtensionDelegate.cache(stackManipulation, typeDescription);
        assertThat(typeExtensionDelegate.getRegisteredFieldCaches().size(), is(1));
        assertThat(typeExtensionDelegate.getRegisteredFieldCaches().get(0).getFieldType(), is(typeDescription));
        assertThat(typeExtensionDelegate.getRegisteredFieldCaches().get(0).getDeclaringType(), is(instrumentedType));
        assertThat(typeExtensionDelegate.getRegisteredFieldCaches().get(0).getModifiers(),
                is(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC));
    }

    @Test
    public void testDoNotCacheFieldsOfDifferentType() throws Exception {
        typeExtensionDelegate.cache(stackManipulation, typeDescription);
        typeExtensionDelegate.cache(stackManipulation, returnType);
        assertThat(typeExtensionDelegate.getRegisteredFieldCaches().size(), is(2));
    }

    @Test
    public void testCacheFieldsMergesDuplicates() throws Exception {
        typeExtensionDelegate.cache(stackManipulation, typeDescription);
        typeExtensionDelegate.cache(stackManipulation, typeDescription);
        assertThat(typeExtensionDelegate.getRegisteredFieldCaches().size(), is(1));
        assertThat(typeExtensionDelegate.getRegisteredFieldCaches().get(0).getFieldType(), is(typeDescription));
        assertThat(typeExtensionDelegate.getRegisteredFieldCaches().get(0).getDeclaringType(), is(instrumentedType));
    }

    @Test
    public void testTypeInitializerWrapperOnNoOpWrappedEntry() throws Exception {
        MethodDescription methodDescription = MethodDescription.Latent.typeInitializerOf(instrumentedType);
        typeExtensionDelegate.cache(stackManipulation, typeDescription);
        TypeWriter.MethodPool methodPool = typeExtensionDelegate.wrapForTypeInitializerInterception(this.methodPool);
        TypeWriter.MethodPool.Entry entry = methodPool.target(methodDescription);
        assertThat(entry.isDefineMethod(), is(true));
        assertThat(entry.getByteCodeAppender().appendsCode(), is(true));
        ByteCodeAppender.Size size = entry.getByteCodeAppender().apply(methodVisitor, instrumentationContext, methodDescription);
        assertThat(size.getOperandStackSize(), is(0));
        assertThat(size.getLocalVariableSize(), is(0));
        verify(methodVisitor).visitFieldInsn(eq(Opcodes.PUTSTATIC), eq(BAR), Matchers.startsWith(CACHED_VALUE), eq(QUX));
        verify(methodVisitor).visitInsn(Opcodes.RETURN);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testTypeInitializerWrapperOnOperativeWrappedEntry() throws Exception {
        when(methodPoolEntry.isDefineMethod()).thenReturn(true);
        when(methodPoolEntry.getAttributeAppender()).thenReturn(methodAttributeAppender);
        when(methodPoolEntry.getByteCodeAppender()).thenReturn(byteCodeAppender);
        when(byteCodeAppender.appendsCode()).thenReturn(true);
        when(byteCodeAppender.apply(Matchers.any(MethodVisitor.class), Matchers.any(Instrumentation.Context.class), Matchers.any(MethodDescription.class)))
                .thenReturn(new ByteCodeAppender.Size(0, 0));
        MethodDescription methodDescription = MethodDescription.Latent.typeInitializerOf(instrumentedType);
        typeExtensionDelegate.cache(stackManipulation, typeDescription);
        TypeWriter.MethodPool methodPool = typeExtensionDelegate.wrapForTypeInitializerInterception(this.methodPool);
        TypeWriter.MethodPool.Entry entry = methodPool.target(methodDescription);
        assertThat(entry.isDefineMethod(), is(true));
        assertThat(entry.getByteCodeAppender().appendsCode(), is(true));
        ByteCodeAppender.Size size = entry.getByteCodeAppender().apply(methodVisitor, instrumentationContext, methodDescription);
        assertThat(size.getOperandStackSize(), is(0));
        assertThat(size.getLocalVariableSize(), is(0));
        verify(methodVisitor).visitFieldInsn(eq(Opcodes.PUTSTATIC), eq(BAR), Matchers.startsWith(CACHED_VALUE), eq(QUX));
        verify(byteCodeAppender).apply(methodVisitor, instrumentationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testCacheFieldAfterWrapThrowsException() throws Exception {
        typeExtensionDelegate.wrapForTypeInitializerInterception(methodPool);
        typeExtensionDelegate.cache(stackManipulation, typeDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeInitializerWrapperOnNonTypeInitializerThrowsException() throws Exception {
        typeExtensionDelegate.cache(stackManipulation, typeDescription);
        TypeWriter.MethodPool methodPool = typeExtensionDelegate.wrapForTypeInitializerInterception(this.methodPool);
        methodPool.target(mock(MethodDescription.class));
    }
}
