package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SuperMethodCallPreparationAndExceptionTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;
    @Mock
    private TypeDescription typeDescription, superType, returnType, declaringType;
    @Mock
    private Instrumentation.Target instrumentationTarget;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;
    @Mock
    private MethodDescription methodDescription;
    @Mock
    private MethodList superTypeMethods;
    @Mock
    private TypeList methodParameters;

    @Before
    public void setUp() throws Exception {
        when(instrumentationTarget.getTypeDescription()).thenReturn(typeDescription);
    }

    @Test
    public void testPreparation() throws Exception {
        assertThat(SuperMethodCall.INSTANCE.prepare(instrumentedType), is(instrumentedType));
        verifyZeroInteractions(instrumentedType);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testConstructor() throws Exception {
        when(typeDescription.getSupertype()).thenReturn(superType);
        when(methodDescription.isConstructor()).thenReturn(true);
        when(superType.getDeclaredMethods()).thenReturn(superTypeMethods);
        when(superTypeMethods.filter(any(ElementMatcher.class))).thenReturn(superTypeMethods);
        when(instrumentationTarget.invokeSuper(methodDescription, Instrumentation.Target.MethodLookup.Default.EXACT))
                .thenReturn(Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE);
        SuperMethodCall.INSTANCE.appender(instrumentationTarget).apply(methodVisitor, instrumentationContext, methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testStaticMethod() throws Exception {
        when(typeDescription.getSupertype()).thenReturn(superType);
        when(methodDescription.isStatic()).thenReturn(true);
        when(methodDescription.getParameterTypes()).thenReturn(methodParameters);
        when(methodParameters.iterator()).thenReturn(Arrays.<TypeDescription>asList().iterator());
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(returnType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(superType.getDeclaredMethods()).thenReturn(superTypeMethods);
        when(superTypeMethods.filter(any(ElementMatcher.class))).thenReturn(superTypeMethods);
        when(instrumentationTarget.invokeSuper(eq(methodDescription), any(Instrumentation.Target.MethodLookup.class)))
                .thenReturn(Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE);
        SuperMethodCall.INSTANCE.appender(instrumentationTarget).apply(methodVisitor, instrumentationContext, methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testNoSuper() throws Exception {
        when(typeDescription.getSupertype()).thenReturn(superType);
        when(methodDescription.getParameterTypes()).thenReturn(methodParameters);
        when(methodParameters.iterator()).thenReturn(Arrays.<TypeDescription>asList().iterator());
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(returnType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(superType.getDeclaredMethods()).thenReturn(superTypeMethods);
        when(superTypeMethods.filter(any(ElementMatcher.class))).thenReturn(superTypeMethods);
        when(instrumentationTarget.invokeSuper(eq(methodDescription), any(Instrumentation.Target.MethodLookup.class)))
                .thenReturn(Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE);
        SuperMethodCall.INSTANCE.appender(instrumentationTarget).apply(methodVisitor, instrumentationContext, methodDescription);
    }
}
