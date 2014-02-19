package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.AnnotationDrivenBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodBindingBuilderTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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

    @Mock
    private MethodDescription methodDescription;
    @Mock
    private AnnotationDrivenBinder.MethodInvoker methodInvoker;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock(answer = Answers.RETURNS_MOCKS)
    private StackManipulation legalStackManipulation, illegalStackManipulation;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        TypeList methodParameterList = mock(TypeList.class);
        when(methodDescription.getParameterTypes()).thenReturn(methodParameterList);
        when(methodDescription.isStatic()).thenReturn(false);
        TypeDescription declaringType = mock(TypeDescription.class);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getInternalName()).thenReturn(FOO);
        when(declaringType.isInterface()).thenReturn(false);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        when(methodDescription.getStackSize()).thenReturn(0);
        TypeDescription returnTpeDescription = mock(TypeDescription.class);
        when(methodDescription.getReturnType()).thenReturn(returnTpeDescription);
        when(returnTpeDescription.getStackSize()).thenReturn(StackSize.ZERO);
        when(legalStackManipulation.isValid()).thenReturn(true);
        when(illegalStackManipulation.isValid()).thenReturn(false);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testIllegalReturnTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        MethodDelegationBinder.Binding binding = builder.build(illegalStackManipulation);
        assertThat(binding.isValid(), is(false));
        assertThat(binding.getTarget(), is(methodDescription));
    }

    @Test
    public void testLegalReturnTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        MethodDelegationBinder.Binding binding = builder.build(legalStackManipulation);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(methodDescription));
        binding.apply(methodVisitor, instrumentationContext);
        verify(legalStackManipulation, times(2)).apply(methodVisitor, instrumentationContext);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testIllegalParameterTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(legalStackManipulation, 0, new Object()), is(true));
        assertThat(builder.append(illegalStackManipulation, 1, new Object()), is(true));
        MethodDelegationBinder.Binding binding = builder.build(legalStackManipulation);
        assertThat(binding.isValid(), is(false));
        assertThat(binding.getTarget(), is(methodDescription));
    }

    @Test
    public void testLegalParameterTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(legalStackManipulation, 0, new Object()), is(true));
        assertThat(builder.append(legalStackManipulation, 1, new Object()), is(true));
        MethodDelegationBinder.Binding binding = builder.build(legalStackManipulation);
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(methodDescription));
        binding.apply(methodVisitor, instrumentationContext);
        verify(legalStackManipulation, times(4)).apply(methodVisitor, instrumentationContext);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testUniqueIdentification() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(legalStackManipulation, 42, new Key(FOO)), is(true));
        assertThat(builder.append(legalStackManipulation, 12, new Key(BAR)), is(true));
        MethodDelegationBinder.Binding binding = builder.build(legalStackManipulation);
        assertThat(binding.getTargetParameterIndex(new Key(FOO)), is(42));
        assertThat(binding.getTargetParameterIndex(new Key(BAR)), is(12));
        assertThat(binding.isValid(), is(true));
        assertThat(binding.getTarget(), is(methodDescription));
    }

    @Test
    public void testNonUniqueIdentification() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        MethodDelegationBinder.Binding.Builder builder = new MethodDelegationBinder.Binding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(legalStackManipulation, 42, new Key(FOO)), is(true));
        assertThat(builder.append(legalStackManipulation, 12, new Key(FOO)), is(false));
    }
}
