package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodHandleConstantTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;
    @Mock
    private FieldDescription fieldDescription;
    @Mock
    private TypeDescription typeDescription;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(QUX);
        when(fieldDescription.getDeclaringType()).thenReturn(typeDescription);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(fieldDescription.getInternalName()).thenReturn(BAR);
        when(fieldDescription.getDescriptor()).thenReturn(QUX);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testMethodHandleForStaticMethod() throws Exception {
        when(methodDescription.isStatic()).thenReturn(true);
        testMethodHandle(MethodHandleConstant.of(methodDescription), Opcodes.H_INVOKESTATIC);
    }

    @Test
    public void testMethodHandleForVirtualMethod() throws Exception {
        testMethodHandle(MethodHandleConstant.of(methodDescription), Opcodes.H_INVOKEVIRTUAL);
    }

    @Test
    public void testMethodHandleForPrivateMethod() throws Exception {
        when(methodDescription.isPrivate()).thenReturn(true);
        testMethodHandle(MethodHandleConstant.of(methodDescription), Opcodes.H_INVOKESPECIAL);
    }

    @Test
    public void testMethodHandleForDefaultMethod() throws Exception {
        when(methodDescription.isDefaultMethod()).thenReturn(true);
        testMethodHandle(MethodHandleConstant.of(methodDescription), Opcodes.H_INVOKESPECIAL);
    }

    @Test
    public void testMethodHandleForInterfaceMethod() throws Exception {
        when(methodDescription.isInterface()).thenReturn(true);
        testMethodHandle(MethodHandleConstant.of(methodDescription), Opcodes.H_INVOKEINTERFACE);
    }

    @Test
    public void testMethodHandleForConstructorMethod() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        testMethodHandle(MethodHandleConstant.of(methodDescription), Opcodes.H_NEWINVOKESPECIAL);
    }

    @Test
    public void testMethodHandleForMemberFieldGetter() throws Exception {
        testMethodHandle(MethodHandleConstant.ofGetter(fieldDescription), Opcodes.H_GETFIELD);
    }

    @Test
    public void testMethodHandleForMemberFieldPutter() throws Exception {
        testMethodHandle(MethodHandleConstant.ofPutter(fieldDescription), Opcodes.H_PUTFIELD);
    }

    @Test
    public void testMethodHandleForStaticFieldGetter() throws Exception {
        when(fieldDescription.isStatic()).thenReturn(true);
        testMethodHandle(MethodHandleConstant.ofGetter(fieldDescription), Opcodes.H_GETSTATIC);
    }

    @Test
    public void testMethodHandleForStaticFieldPutter() throws Exception {
        when(fieldDescription.isStatic()).thenReturn(true);
        testMethodHandle(MethodHandleConstant.ofPutter(fieldDescription), Opcodes.H_PUTSTATIC);
    }

    private void testMethodHandle(StackManipulation stackManipulation, int handleCode) throws Exception {
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitLdcInsn(new Handle(handleCode, FOO, BAR, QUX));
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodHandleConstant.class).apply();
    }
}
