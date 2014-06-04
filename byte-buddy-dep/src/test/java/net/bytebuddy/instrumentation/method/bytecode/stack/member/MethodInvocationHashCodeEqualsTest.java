package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class MethodInvocationHashCodeEqualsTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);
    @Mock
    private MethodDescription instanceMethod, otherInstanceMethod, staticMethod, interfaceMethod;
    @Mock
    private TypeDescription firstType, otherType, interfaceType, returnType;
    @Mock
    private TypeList parameterTypes;

    @Before
    public void setUp() throws Exception {
        when(staticMethod.isStatic()).thenReturn(true);
        when(interfaceMethod.isStatic()).thenReturn(true);
        when(instanceMethod.getDeclaringType()).thenReturn(firstType);
        when(otherInstanceMethod.getDeclaringType()).thenReturn(otherType);
        when(staticMethod.getDeclaringType()).thenReturn(firstType);
        when(interfaceMethod.getDeclaringType()).thenReturn(interfaceType);
        when(instanceMethod.getReturnType()).thenReturn(returnType);
        when(otherInstanceMethod.getReturnType()).thenReturn(returnType);
        when(staticMethod.getReturnType()).thenReturn(returnType);
        when(interfaceMethod.getReturnType()).thenReturn(returnType);
        when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(instanceMethod.getInternalName()).thenReturn(FOO);
        when(otherInstanceMethod.getInternalName()).thenReturn(FOO);
        when(staticMethod.getInternalName()).thenReturn(FOO);
        when(interfaceMethod.getInternalName()).thenReturn(FOO);
        when(instanceMethod.getParameterTypes()).thenReturn(parameterTypes);
        when(otherInstanceMethod.getParameterTypes()).thenReturn(parameterTypes);
        when(staticMethod.getParameterTypes()).thenReturn(parameterTypes);
        when(interfaceMethod.getParameterTypes()).thenReturn(parameterTypes);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(MethodInvocation.invoke(instanceMethod).hashCode(), is(MethodInvocation.invoke(instanceMethod).hashCode()));
        assertThat(MethodInvocation.invoke(instanceMethod), is(MethodInvocation.invoke(instanceMethod)));
        assertThat(MethodInvocation.invoke(instanceMethod).hashCode(), not(is(MethodInvocation.invoke(otherInstanceMethod).hashCode())));
        assertThat(MethodInvocation.invoke(instanceMethod), not(is(MethodInvocation.invoke(otherInstanceMethod))));
        assertThat(MethodInvocation.invoke(staticMethod).hashCode(), not(is(MethodInvocation.invoke(otherInstanceMethod).hashCode())));
        assertThat(MethodInvocation.invoke(staticMethod), not(is(MethodInvocation.invoke(otherInstanceMethod))));
        assertThat(MethodInvocation.invoke(interfaceMethod).hashCode(), not(is(MethodInvocation.invoke(otherInstanceMethod).hashCode())));
        assertThat(MethodInvocation.invoke(interfaceMethod), not(is(MethodInvocation.invoke(otherInstanceMethod))));
    }

    @Test
    public void testVirtualStaticHashCodeEquals() throws Exception {
        assertThat(MethodInvocation.invoke(instanceMethod).virtual(firstType).hashCode(),
                is(MethodInvocation.invoke(instanceMethod).virtual(firstType).hashCode()));
        assertThat(MethodInvocation.invoke(instanceMethod).virtual(firstType),
                is(MethodInvocation.invoke(instanceMethod).virtual(firstType)));
        assertThat(MethodInvocation.invoke(instanceMethod).virtual(firstType).hashCode(),
                not(is(MethodInvocation.invoke(instanceMethod).special(firstType).hashCode())));
        assertThat(MethodInvocation.invoke(instanceMethod).virtual(firstType),
                not(is(MethodInvocation.invoke(instanceMethod).special(firstType))));
    }
}
