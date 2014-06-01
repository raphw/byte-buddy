package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class SubclassInstrumentationTargetTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodLookupEngine.Finding finding;
    @Mock
    private TypeDescription instrumentedType, superType, methodType, returnType;
    @Mock
    private BridgeMethodResolver.Factory bridgeMethodResolverFactory;
    @Mock
    private BridgeMethodResolver bridgeMethodResolver;
    @Mock
    private MethodDescription methodDescription, superMethodDescription;
    @Mock
    private Instrumentation.Target.MethodLookup methodLookup;

    private Instrumentation.Target instrumentationTarget;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(finding.getTypeDescription()).thenReturn(instrumentedType);
        when(instrumentedType.getSupertype()).thenReturn(superType);
        when(superType.getDeclaredMethods()).thenReturn(new MethodList.Explicit(Arrays.asList(superMethodDescription)));
        when(finding.getInvokableMethods()).thenReturn(new MethodList.Explicit(Arrays.asList(methodDescription)));
        when(bridgeMethodResolverFactory.make(any(MethodList.class))).thenReturn(bridgeMethodResolver);
        when(methodLookup.resolve(any(MethodDescription.class), any(Map.class), eq(bridgeMethodResolver)))
                .then(new Answer<MethodDescription>() {
                    @Override
                    public MethodDescription answer(InvocationOnMock invocation) throws Throwable {
                        return (MethodDescription) invocation.getArguments()[0];
                    }
                });
        when(methodDescription.getDeclaringType()).thenReturn(methodType);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(superType.getInternalName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(QUX);
        instrumentationTarget = new SubclassInstrumentationTarget(finding, bridgeMethodResolverFactory);
    }

    @Test
    public void testSpecializableMethodIsInvokable() throws Exception {
        when(methodDescription.isSpecializableFor(superType)).thenReturn(true);
        Instrumentation.SpecialMethodInvocation specialMethodInvocation = instrumentationTarget.invokeSuper(methodDescription, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is(methodDescription));
        assertThat(specialMethodInvocation.getTypeDescription(), is(superType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Instrumentation.Context instrumentationContext = mock(Instrumentation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, FOO, QUX, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testNonSpecializableMethodIsNotInvokable() throws Exception {
        assertThat(instrumentationTarget.invokeSuper(methodDescription, methodLookup).isValid(), is(false));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(instrumentationTarget.hashCode(), is(instrumentationTarget.hashCode()));
        assertThat(instrumentationTarget, is(instrumentationTarget));
        BridgeMethodResolver.Factory factory = mock(BridgeMethodResolver.Factory.class);
        when(factory.make(any(MethodList.class))).thenReturn(mock(BridgeMethodResolver.class));
        Instrumentation.Target other = new SubclassInstrumentationTarget(finding, factory);
        assertThat(instrumentationTarget.hashCode(), not(is(other.hashCode())));
        assertThat(instrumentationTarget, not(is(other)));
    }
}
