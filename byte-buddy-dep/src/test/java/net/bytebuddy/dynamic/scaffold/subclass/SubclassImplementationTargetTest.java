package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.AbstractImplementationTargetTest;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SubclassImplementationTargetTest extends AbstractImplementationTargetTest {

    private static final String BAR = "bar", BAZ = "baz";

    @Mock
    private TypeDescription.Generic superClass;

    @Mock
    private TypeDescription rawSuperClass;

    @Mock
    private MethodDescription.InGenericShape superClassConstructor;

    @Mock
    private MethodDescription.InDefinedShape definedSuperClassConstructor;

    @Mock
    private MethodDescription.SignatureToken superConstructorToken;

    @Before
    public void setUp() throws Exception {
        when(superGraph.locate(Mockito.any(MethodDescription.SignatureToken.class))).thenReturn(MethodGraph.Node.Unresolved.INSTANCE);
        when(superGraph.locate(invokableToken)).thenReturn(new MethodGraph.Node.Simple(invokableMethod));
        when(instrumentedType.getSuperClass()).thenReturn(superClass);
        when(superClass.asErasure()).thenReturn(rawSuperClass);
        when(superClass.asGenericType()).thenReturn(superClass);
        when(rawSuperClass.asGenericType()).thenReturn(superClass);
        when(rawSuperClass.asErasure()).thenReturn(rawSuperClass);
        when(rawSuperClass.getInternalName()).thenReturn(BAR);
        when(superClass.getDeclaredMethods())
                .thenReturn(new MethodList.Explicit<MethodDescription.InGenericShape>(superClassConstructor));
        when(superClassConstructor.asDefined()).thenReturn(definedSuperClassConstructor);
        when(definedSuperClassConstructor.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(definedSuperClassConstructor.getDeclaringType()).thenReturn(rawSuperClass);
        when(definedSuperClassConstructor.isConstructor()).thenReturn(true);
        when(superClassConstructor.isVisibleTo(instrumentedType)).thenReturn(true);
        when(superClassConstructor.asSignatureToken()).thenReturn(superConstructorToken);
        when(definedSuperClassConstructor.getInternalName()).thenReturn(QUX);
        when(definedSuperClassConstructor.getDescriptor()).thenReturn(BAZ);
        when(superClassConstructor.isConstructor()).thenReturn(true);
        when(superClassConstructor.getDeclaringType()).thenReturn(superClass);
        when(superClassConstructor.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(superClassConstructor.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InGenericShape>());
        when(invokableToken.getName()).thenReturn(FOO);
        when(superConstructorToken.getName()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
        super.setUp();
    }

    protected Implementation.Target makeImplementationTarget() {
        return new SubclassImplementationTarget(instrumentedType, methodGraph, defaultMethodInvocation, SubclassImplementationTarget.OriginTypeResolver.SUPER_CLASS);
    }

    @Test
    public void testSuperTypeMethodIsInvokable() throws Exception {
        when(invokableMethod.isSpecializableFor(rawSuperClass)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) invokableMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(rawSuperClass));
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
    public void testNonSpecializableSuperClassMethodIsNotInvokable() throws Exception {
        when(invokableMethod.isSpecializableFor(rawSuperClass)).thenReturn(false);
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testSuperConstructorIsInvokable() throws Exception {
        when(invokableMethod.isConstructor()).thenReturn(true);
        when(definedSuperClassConstructor.isSpecializableFor(rawSuperClass)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeSuper(superConstructorToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) superClassConstructor));
        assertThat(specialMethodInvocation.getTypeDescription(), is(rawSuperClass));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, QUX, BAZ, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }
}
