package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassConstantReferenceTest {

    private static final String FOO = "Lfoo;";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription, instrumentedType;

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    public void setUp() throws Exception {
        when(implementationContext.getInstrumentedType()).thenReturn(instrumentedType);
        when(implementationContext.getClassFileVersion()).thenReturn(classFileVersion);
    }

    @Test
    public void testClassConstantModernVisible() throws Exception {
        when(typeDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        when(classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)).thenReturn(true);
        when(typeDescription.getDescriptor()).thenReturn(FOO);
        StackManipulation stackManipulation = ClassConstant.of(typeDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(typeDescription).getDescriptor();
        verify(typeDescription).isVisibleTo(instrumentedType);
        verify(typeDescription, times(9)).represents(any(Class.class));
        verifyNoMoreInteractions(typeDescription);
        verify(methodVisitor).visitLdcInsn(Type.getType(FOO));
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testClassConstantModernInvisible() throws Exception {
        when(typeDescription.isVisibleTo(instrumentedType)).thenReturn(false);
        when(classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)).thenReturn(true);
        when(typeDescription.getName()).thenReturn(FOO);
        StackManipulation stackManipulation = ClassConstant.of(typeDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(typeDescription).getName();
        verify(typeDescription).isVisibleTo(instrumentedType);
        verify(typeDescription, times(9)).represents(any(Class.class));
        verifyNoMoreInteractions(typeDescription);
        verify(methodVisitor).visitLdcInsn(FOO);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC,
                Type.getInternalName(Class.class),
                "forName",
                Type.getMethodDescriptor(Type.getType(Class.class), Type.getType(String.class)),
                false);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testClassConstantLegacy() throws Exception {
        when(typeDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        when(classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)).thenReturn(false);
        when(typeDescription.getName()).thenReturn(FOO);
        StackManipulation stackManipulation = ClassConstant.of(typeDescription);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(typeDescription).getName();
        verify(typeDescription, times(9)).represents(any(Class.class));
        verifyNoMoreInteractions(typeDescription);
        verify(methodVisitor).visitLdcInsn(FOO);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC,
                Type.getInternalName(Class.class),
                "forName",
                Type.getMethodDescriptor(Type.getType(Class.class), Type.getType(String.class)),
                false);
        verifyNoMoreInteractions(methodVisitor);
    }
}
