package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InstrumentationSpecialMethodInvocationSimpleTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;
    @Mock
    private MethodDescription methodDescription;
    @Mock
    private TypeDescription typeDescription, returnType;
    @Mock
    private TypeList parameterTypes;

    private Instrumentation.SpecialMethodInvocation specialMethodInvocation;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(methodDescription.isSpecializableFor(typeDescription)).thenReturn(true);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getParameterTypes()).thenReturn(parameterTypes);
        specialMethodInvocation = Instrumentation.SpecialMethodInvocation.Simple.of(methodDescription, typeDescription);
    }

    @Test
    public void testIsValid() throws Exception {
        assertThat(specialMethodInvocation.isValid(), is(true));
    }

    @Test
    public void testMethodDescriptionIllegal() throws Exception {
        assertThat(specialMethodInvocation.getMethodDescription(), is(methodDescription));
    }

    @Test
    public void testTypeDescriptionIllegal() throws Exception {
        assertThat(specialMethodInvocation.getTypeDescription(), is(typeDescription));
    }

    @Test
    public void testApplicationIllegal() throws Exception {
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(specialMethodInvocation.hashCode(), is(specialMethodInvocation.hashCode()));
        assertThat(specialMethodInvocation, is(specialMethodInvocation));
        MethodDescription otherMethodDescription = mock(MethodDescription.class);
        when(otherMethodDescription.getInternalName()).thenReturn(FOO);
        when(otherMethodDescription.getParameterTypes()).thenReturn(parameterTypes);
        TypeDescription otherType = mock(TypeDescription.class);
        when(otherMethodDescription.getDeclaringType()).thenReturn(otherType);
        when(otherMethodDescription.getReturnType()).thenReturn(returnType);
        when(otherMethodDescription.isSpecializableFor(typeDescription)).thenReturn(true);
        Instrumentation.SpecialMethodInvocation other = Instrumentation.SpecialMethodInvocation.Simple.of(otherMethodDescription, typeDescription);
        assertThat(specialMethodInvocation.hashCode(), is(other.hashCode()));
        assertThat(specialMethodInvocation, is(other));
        assertThat(specialMethodInvocation.hashCode(), not(is(Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE.hashCode())));
        assertThat(specialMethodInvocation, not(is((Instrumentation.SpecialMethodInvocation) Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE)));
        MethodDescription otherUnequalMethodDescription = mock(MethodDescription.class);
        when(otherUnequalMethodDescription.getInternalName()).thenReturn(BAR);
        when(otherUnequalMethodDescription.getDeclaringType()).thenReturn(typeDescription);
        when(otherUnequalMethodDescription.getReturnType()).thenReturn(returnType);
        Instrumentation.SpecialMethodInvocation otherUnequal = Instrumentation.SpecialMethodInvocation.Simple.of(otherUnequalMethodDescription, typeDescription);
        assertThat(specialMethodInvocation.hashCode(), not(is(otherUnequal.hashCode())));
        assertThat(specialMethodInvocation, not(is(otherUnequal)));

    }
}
