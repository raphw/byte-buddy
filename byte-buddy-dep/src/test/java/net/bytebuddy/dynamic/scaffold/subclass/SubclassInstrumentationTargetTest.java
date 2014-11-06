package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.instrumentation.AbstractInstrumentationTargetTest;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
        return new SubclassInstrumentationTarget(finding,
                bridgeMethodResolverFactory,
                SubclassInstrumentationTarget.OriginTypeIdentifier.SUPER_TYPE);
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
    public void testSuperTypeOrigin() throws Exception {
        assertThat(new SubclassInstrumentationTarget(finding,
                        bridgeMethodResolverFactory,
                        SubclassInstrumentationTarget.OriginTypeIdentifier.SUPER_TYPE).getOriginType(),
                is(finding.getTypeDescription().getSupertype()));
    }

    @Test
    public void testLevelTypeOrigin() throws Exception {
        assertThat(new SubclassInstrumentationTarget(finding,
                        bridgeMethodResolverFactory,
                        SubclassInstrumentationTarget.OriginTypeIdentifier.LEVEL_TYPE).getOriginType(),
                is(finding.getTypeDescription()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassInstrumentationTarget.class).apply();
    }
}
