package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.AnnotationDrivenBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

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
    private TypeDescription typeDescription;
    private AnnotationDrivenBinder.MethodInvoker methodInvoker;
    private MethodVisitor methodVisitor;
    private Assignment legalAssignment, illegalAssignment;

    @Before
    public void setUp() throws Exception {
        methodDescription = mock(MethodDescription.class);
        typeDescription = mock(TypeDescription.class);
        TypeList typeList = mock(TypeList.class);
        when(methodDescription.getParameterTypes()).thenReturn(typeList);
        when(methodDescription.isStatic()).thenReturn(false);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(typeDescription.getInternalName()).thenReturn(FOO);
        when(typeDescription.isInterface()).thenReturn(false);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        when(methodDescription.getStackSize()).thenReturn(0);
        TypeDescription returnTpeDescription = mock(TypeDescription.class);
        when(methodDescription.getReturnType()).thenReturn(returnTpeDescription);
        when(returnTpeDescription.getStackSize()).thenReturn(TypeSize.ZERO);
        methodInvoker = mock(AnnotationDrivenBinder.MethodInvoker.class);
        methodVisitor = mock(MethodVisitor.class);
        legalAssignment = mock(Assignment.class, RETURNS_MOCKS);
        when(legalAssignment.isValid()).thenReturn(true);
        illegalAssignment = mock(Assignment.class, RETURNS_MOCKS);
        when(illegalAssignment.isValid()).thenReturn(false);
    }

    @Test
    public void testIllegalReturnTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalAssignment);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        MethodDelegationBinder.Binding binding = builder.build(illegalAssignment);
        assertThat(binding.isValid(), is(false));
        assertThat(binding.getTarget(), is(methodDescription));
    }

    @Test
    public void testLegalReturnTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalAssignment);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        MethodDelegationBinder.Binding binding = builder.build(legalAssignment);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(methodDescription));
        binding.apply(methodVisitor);
        verify(legalAssignment, times(2)).apply(methodVisitor);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testIllegalParameterTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalAssignment);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(legalAssignment, 0, new Object()), is(true));
        assertThat(builder.append(illegalAssignment, 1, new Object()), is(true));
        MethodDelegationBinder.Binding binding = builder.build(legalAssignment);
        assertThat(binding.isValid(), is(false));
        assertThat(binding.getTarget(), is(methodDescription));
    }

    @Test
    public void testLegalParameterTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalAssignment);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(legalAssignment, 0, new Object()), is(true));
        assertThat(builder.append(legalAssignment, 1, new Object()), is(true));
        MethodDelegationBinder.Binding binding = builder.build(legalAssignment);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(methodDescription));
        binding.apply(methodVisitor);
        verify(legalAssignment, times(4)).apply(methodVisitor);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testUniqueIdentification() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalAssignment);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
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
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalAssignment);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(legalAssignment, 42, new Key(FOO)), is(true));
        assertThat(builder.append(legalAssignment, 12, new Key(FOO)), is(false));
    }
}
