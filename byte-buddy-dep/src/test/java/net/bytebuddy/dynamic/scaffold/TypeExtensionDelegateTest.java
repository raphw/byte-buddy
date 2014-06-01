package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.Instrumentation;
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

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, returnType, parameterType, auxiliaryTypeDescription;
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
}
