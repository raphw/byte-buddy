package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Answers;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodBindingBuilderTest {

    private static final String FOO = "foo";

    private static final String BAR = "bar";

    private static final String BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private ParameterList<?> methodParameterList;

    @Mock
    private TargetMethodAnnotationDrivenBinder.MethodInvoker methodInvoker;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private TypeDescription.Generic returnType;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private StackManipulation legalStackManipulation, illegalStackManipulation;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodDescription.getParameters()).thenReturn((ParameterList) methodParameterList);
        when(methodDescription.isStatic()).thenReturn(false);
        TypeDescription declaringType = mock(TypeDescription.class);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getInternalName()).thenReturn(FOO);
        when(declaringType.isInterface()).thenReturn(false);
        when(methodDescription.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(BAZ);
        when(methodDescription.getStackSize()).thenReturn(0);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(legalStackManipulation.isValid()).thenReturn(true);
        when(illegalStackManipulation.isValid()).thenReturn(false);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testIllegalReturnTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        MethodDelegationBinder.MethodBinding.Builder builder = new MethodDelegationBinder.MethodBinding.Builder(methodInvoker, methodDescription);
        MethodDelegationBinder.MethodBinding methodBinding = builder.build(illegalStackManipulation);
        assertThat(methodBinding.isValid(), is(false));
        assertThat(methodBinding.getTarget(), is(methodDescription));
    }

    @Test
    public void testLegalReturnTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        MethodDelegationBinder.MethodBinding.Builder builder = new MethodDelegationBinder.MethodBinding.Builder(methodInvoker, methodDescription);
        MethodDelegationBinder.MethodBinding methodBinding = builder.build(legalStackManipulation);
        assertThat(methodBinding.isValid(), is(true));
        assertThat(methodBinding.getTarget(), is(methodDescription));
        methodBinding.apply(methodVisitor, implementationContext);
        verify(legalStackManipulation, times(2)).apply(methodVisitor, implementationContext);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testIllegalParameterTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        when(methodParameterList.size()).thenReturn(2);
        MethodDelegationBinder.MethodBinding.Builder builder = new MethodDelegationBinder.MethodBinding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(MethodDelegationBinder.ParameterBinding.Unique.of(legalStackManipulation, new Object())), is(true));
        assertThat(builder.append(MethodDelegationBinder.ParameterBinding.Unique.of(illegalStackManipulation, new Object())), is(true));
        MethodDelegationBinder.MethodBinding methodBinding = builder.build(legalStackManipulation);
        assertThat(methodBinding.isValid(), is(false));
        assertThat(methodBinding.getTarget(), is(methodDescription));
    }

    @Test
    public void testLegalParameterTypeBinding() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        when(methodParameterList.size()).thenReturn(2);
        MethodDelegationBinder.MethodBinding.Builder builder = new MethodDelegationBinder.MethodBinding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(MethodDelegationBinder.ParameterBinding.Unique.of(legalStackManipulation, new Object())), is(true));
        assertThat(builder.append(MethodDelegationBinder.ParameterBinding.Unique.of(legalStackManipulation, new Object())), is(true));
        MethodDelegationBinder.MethodBinding methodBinding = builder.build(legalStackManipulation);
        assertThat(methodBinding.isValid(), is(true));
        assertThat(methodBinding.getTarget(), is(methodDescription));
        methodBinding.apply(methodVisitor, implementationContext);
        verify(legalStackManipulation, times(4)).apply(methodVisitor, implementationContext);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testUniqueIdentification() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        when(methodParameterList.size()).thenReturn(2);
        MethodDelegationBinder.MethodBinding.Builder builder = new MethodDelegationBinder.MethodBinding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(MethodDelegationBinder.ParameterBinding.Unique.of(legalStackManipulation, new Key(FOO))), is(true));
        assertThat(builder.append(MethodDelegationBinder.ParameterBinding.Unique.of(legalStackManipulation, new Key(BAR))), is(true));
        MethodDelegationBinder.MethodBinding methodBinding = builder.build(legalStackManipulation);
        assertThat(methodBinding.getTargetParameterIndex(new Key(FOO)), is(0));
        assertThat(methodBinding.getTargetParameterIndex(new Key(BAR)), is(1));
        assertThat(methodBinding.isValid(), is(true));
        assertThat(methodBinding.getTarget(), is(methodDescription));
    }

    @Test
    public void testNonUniqueIdentification() throws Exception {
        when(methodInvoker.invoke(any(MethodDescription.class))).thenReturn(legalStackManipulation);
        MethodDelegationBinder.MethodBinding.Builder builder = new MethodDelegationBinder.MethodBinding.Builder(methodInvoker, methodDescription);
        assertThat(builder.append(MethodDelegationBinder.ParameterBinding.Unique.of(legalStackManipulation, new Key(FOO))), is(true));
        assertThat(builder.append(MethodDelegationBinder.ParameterBinding.Unique.of(legalStackManipulation, new Key(FOO))), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterNumberInequality() throws Exception {
        when(methodParameterList.size()).thenReturn(1);
        new MethodDelegationBinder.MethodBinding.Builder(methodInvoker, methodDescription).build(legalStackManipulation);
    }

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
}
