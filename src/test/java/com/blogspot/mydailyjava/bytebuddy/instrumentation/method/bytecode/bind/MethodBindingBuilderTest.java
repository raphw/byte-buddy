package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodBindingBuilderTest {

    private static class Key {

        private final String identifier;

        private Key(String identifier) {
            this.identifier = identifier;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && identifier.equals(((Key) other).identifier);

        }

        @Override
        public int hashCode() {
            return identifier.hashCode();
        }
    }

    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String BAZ = "baz";

    private MethodDescription methodDescription;
    private MethodVisitor methodVisitor;
    private Assignment legalAssignment, illegalAssignment;

    @Before
    public void setUp() throws Exception {
        methodDescription = mock(MethodDescription.class);
        when(methodDescription.getParameterTypes()).thenReturn(new TypeList.ForLoadedType(new Class<?>[0]));
        when(methodDescription.isStatic()).thenReturn(false);
        when(methodDescription.isDeclaredInInterface()).thenReturn(false);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        when(methodDescription.getParameterSize()).thenReturn(0);
        TypeDescription returnTpeDescription = mock(TypeDescription.class);
        when(methodDescription.getReturnType()).thenReturn(returnTpeDescription);
        when(returnTpeDescription.getStackSize()).thenReturn(TypeSize.ZERO);
        methodVisitor = mock(MethodVisitor.class);
        legalAssignment = mock(Assignment.class, RETURNS_MOCKS);
        when(legalAssignment.isValid()).thenReturn(true);
        illegalAssignment = mock(Assignment.class, RETURNS_MOCKS);
        when(illegalAssignment.isValid()).thenReturn(false);
    }

    @Test
    public void testIllegalReturnTypeBinding() throws Exception {
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodDescription);
        MethodDelegationBinder.Binding binding = builder.build(illegalAssignment);
        assertThat(binding.isValid(), is(false));
        assertThat(binding.getTarget(), is(methodDescription));
    }

    @Test
    public void testLegalReturnTypeBinding() throws Exception {
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodDescription);
        MethodDelegationBinder.Binding binding = builder.build(legalAssignment);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(methodDescription));
        binding.apply(methodVisitor);
        verify(legalAssignment).apply(methodVisitor);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testIllegalParameterTypeBinding() throws Exception {
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodDescription);
        assertThat(builder.append(legalAssignment, 0, new Object()), is(true));
        assertThat(builder.append(illegalAssignment, 1, new Object()), is(true));
        MethodDelegationBinder.Binding binding = builder.build(legalAssignment);
        assertThat(binding.isValid(), is(false));
        assertThat(binding.getTarget(), is(methodDescription));
    }

    @Test
    public void testLegalParameterTypeBinding() throws Exception {
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodDescription);
        assertThat(builder.append(legalAssignment, 0, new Object()), is(true));
        assertThat(builder.append(legalAssignment, 1, new Object()), is(true));
        MethodDelegationBinder.Binding binding = builder.build(legalAssignment);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(methodDescription));
        binding.apply(methodVisitor);
        verify(legalAssignment, times(3)).apply(methodVisitor);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL, FOO, BAR, BAZ);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testUniqueIdentification() throws Exception {
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodDescription);
        assertThat(builder.append(legalAssignment, 42, new Key(FOO)), is(true));
        assertThat(builder.append(legalAssignment, 12, new Key(BAR)), is(true));
        MethodDelegationBinder.Binding binding = builder.build(legalAssignment);
        assertThat(binding.getTargetParameterIndex(new Key(FOO)), is(42));
        assertThat(binding.getTargetParameterIndex(new Key(BAR)), is(12));
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(methodDescription));
    }

    @Test
    public void testNonUniqueIdentification() throws Exception {
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodDescription);
        assertThat(builder.append(legalAssignment, 42, new Key(FOO)), is(true));
        assertThat(builder.append(legalAssignment, 12, new Key(FOO)), is(false));
    }
}
