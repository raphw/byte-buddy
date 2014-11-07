package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class MethodInvocationObjectPropertiesTest {

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
}
