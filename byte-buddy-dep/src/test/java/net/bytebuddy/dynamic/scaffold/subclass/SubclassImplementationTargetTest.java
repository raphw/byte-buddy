package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.AbstractImplementationTargetTest;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SubclassImplementationTargetTest extends AbstractImplementationTargetTest {

    private static final String BAR = "bar", BAZ = "baz";

    @Mock
    private TypeDescription superType;

    @Mock
    private MethodDescription.InDefinedShape superTypeConstructor;

    @Mock
    private MethodDescription.Token superConstructorToken;

    @Override
    @Before
    public void setUp() throws Exception {
        when(instrumentedType.getSuperType()).thenReturn(superType);
        when(superType.asRawType()).thenReturn(superType);
        when(superType.getInternalName()).thenReturn(BAR);
        when(superType.getDeclaredMethods())
                .thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(Collections.singletonList(superTypeConstructor)));
        when(superTypeConstructor.asToken()).thenReturn(superConstructorToken);
        when(superTypeConstructor.getInternalName()).thenReturn(QUX);
        when(superTypeConstructor.getDescriptor()).thenReturn(BAZ);
        when(superTypeConstructor.asDefined()).thenReturn(superTypeConstructor);
        when(superTypeConstructor.isConstructor()).thenReturn(true);
        when(superTypeConstructor.getDeclaringType()).thenReturn(superType);
        when(superTypeConstructor.getReturnType()).thenReturn(TypeDescription.VOID);
        when(superTypeConstructor.getParameters()).thenReturn(new ParameterList.Empty());
        super.setUp();
    }

    @Override
    protected Implementation.Target makeImplementationTarget() {
        return new SubclassImplementationTarget(instrumentedType, methodGraph, SubclassImplementationTarget.OriginTypeIdentifier.SUPER_TYPE);
    }

    @Test
    public void testSuperTypeMethodIsInvokable() throws Exception {
        when(invokableMethod.isSpecializableFor(superType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) invokableMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(superType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, FOO, QUX, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testNonSpecializableSuperTypeMethodIsNotInvokable() throws Exception {
        when(invokableMethod.isSpecializableFor(superType)).thenReturn(false);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testSuperConstructorIsInvokable() throws Exception {
        when(invokableMethod.isConstructor()).thenReturn(true);
        when(superTypeConstructor.isSpecializableFor(superType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(superConstructorToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) superTypeConstructor));
        assertThat(specialMethodInvocation.getTypeDescription(), is(superType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, QUX, BAZ, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testSuperTypeOrigin() throws Exception {
        assertThat(new SubclassImplementationTarget(instrumentedType,
                methodGraph,
                SubclassImplementationTarget.OriginTypeIdentifier.SUPER_TYPE).getOriginType(), is(superType));
    }

    @Test
    public void testLevelTypeOrigin() throws Exception {
        assertThat(new SubclassImplementationTarget(instrumentedType,
                        methodGraph,
                        SubclassImplementationTarget.OriginTypeIdentifier.LEVEL_TYPE).getOriginType(),
                is(instrumentedType));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassImplementationTarget.class).apply();
        ObjectPropertyAssertion.of(SubclassImplementationTarget.OriginTypeIdentifier.class).apply();
    }
}
