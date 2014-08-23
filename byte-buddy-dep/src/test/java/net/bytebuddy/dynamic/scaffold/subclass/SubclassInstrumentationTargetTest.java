package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.instrumentation.AbstractInstrumentationTargetTest;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SubclassInstrumentationTargetTest extends AbstractInstrumentationTargetTest {

    private static final String BAR = "bar", BAZ = "baz", FOOBAR = "foobar";

    @Mock
    private MethodDescription superMethod, superMethodConstructor;
    @Mock
    private TypeDescription superType;
    @Mock
    private TypeList parameterTypes;

    @Override
    @Before
    public void setUp() throws Exception {
        when(instrumentedType.getSupertype()).thenReturn(superType);
        when(superType.getDeclaredMethods()).thenReturn(new MethodList.Explicit(Arrays.asList(superMethodConstructor)));
        when(superType.getInternalName()).thenReturn(BAR);
        when(superMethod.getDeclaringType()).thenReturn(superType);
        when(superType.getStackSize()).thenReturn(StackSize.ZERO);
        when(superMethod.getReturnType()).thenReturn(returnType);
        when(superMethod.getInternalName()).thenReturn(BAZ);
        when(superMethod.getDescriptor()).thenReturn(FOOBAR);
        when(superMethod.getParameterTypes()).thenReturn(parameterTypes);
        when(superMethodConstructor.isConstructor()).thenReturn(true);
        when(superMethodConstructor.getParameterTypes()).thenReturn(parameterTypes);
        when(superMethodConstructor.getReturnType()).thenReturn(returnType);
        when(superMethodConstructor.isSpecializableFor(superType)).thenReturn(true);
        when(superMethodConstructor.getInternalName()).thenReturn(QUXBAZ);
        when(superMethodConstructor.getDescriptor()).thenReturn(BAZBAR);
        super.setUp();
    }

    @Override
    protected Instrumentation.Target makeInstrumentationTarget() {
        return new SubclassInstrumentationTarget(finding, bridgeMethodResolverFactory);
    }

    @Test
    public void testSuperTypeMethodIsInvokable() throws Exception {
        when(superMethod.isSpecializableFor(superType)).thenReturn(true);
        Instrumentation.SpecialMethodInvocation specialMethodInvocation = instrumentationTarget.invokeSuper(superMethod, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is(superMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(superType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Instrumentation.Context instrumentationContext = mock(Instrumentation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, BAZ, FOOBAR, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testAbstractSuperTypeMethodIsNotInvokable() throws Exception {
        when(superMethod.isSpecializableFor(superType)).thenReturn(true);
        when(superMethod.isAbstract()).thenReturn(true);
        Instrumentation.SpecialMethodInvocation specialMethodInvocation = instrumentationTarget.invokeSuper(superMethod, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testSuperConstructorIsInvokable() throws Exception {
        when(superMethod.isConstructor()).thenReturn(true);
        Instrumentation.SpecialMethodInvocation specialMethodInvocation = instrumentationTarget.invokeSuper(superMethod, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is(superMethodConstructor));
        assertThat(specialMethodInvocation.getTypeDescription(), is(superType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Instrumentation.Context instrumentationContext = mock(Instrumentation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, instrumentationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, QUXBAZ, BAZBAR, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(instrumentationTarget.hashCode(), is(new SubclassInstrumentationTarget(finding, bridgeMethodResolverFactory).hashCode()));
        assertThat(instrumentationTarget, is((Instrumentation.Target) new SubclassInstrumentationTarget(finding, bridgeMethodResolverFactory)));
        BridgeMethodResolver.Factory factory = mock(BridgeMethodResolver.Factory.class);
        when(factory.make(any(MethodList.class))).thenReturn(mock(BridgeMethodResolver.class));
        Instrumentation.Target other = new SubclassInstrumentationTarget(finding, factory);
        assertThat(instrumentationTarget.hashCode(), not(is(other.hashCode())));
        assertThat(instrumentationTarget, not(is(other)));
    }
}
