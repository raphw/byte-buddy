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
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
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
    private TypeDescription rawSuperType;

    @Mock
    private MethodDescription.InGenericShape superClassConstructor;

    @Mock
    private MethodDescription.InDefinedShape definedSuperTypeConstructor;

    @Mock
    private MethodDescription.SignatureToken superConstructorToken;

    @Override
    @Before
    public void setUp() throws Exception {
        when(superGraph.locate(Mockito.any(MethodDescription.SignatureToken.class))).thenReturn(MethodGraph.Node.Unresolved.INSTANCE);
        when(superGraph.locate(invokableToken)).thenReturn(new MethodGraph.Node.Simple(invokableMethod));
        when(instrumentedType.getSuperClass()).thenReturn(superClass);
        when(superClass.asErasure()).thenReturn(rawSuperType);
        when(superClass.asGenericType()).thenReturn(superClass);
        when(rawSuperType.asGenericType()).thenReturn(superClass);
        when(rawSuperType.asErasure()).thenReturn(rawSuperType);
        when(rawSuperType.getInternalName()).thenReturn(BAR);
        when(superClass.getDeclaredMethods())
                .thenReturn(new MethodList.Explicit<MethodDescription.InGenericShape>(superClassConstructor));
        when(superClassConstructor.asDefined()).thenReturn(definedSuperTypeConstructor);
        when(definedSuperTypeConstructor.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(definedSuperTypeConstructor.getDeclaringType()).thenReturn(rawSuperType);
        when(definedSuperTypeConstructor.isConstructor()).thenReturn(true);
        when(superClassConstructor.isVisibleTo(instrumentedType)).thenReturn(true);
        when(superClassConstructor.asSignatureToken()).thenReturn(superConstructorToken);
        when(definedSuperTypeConstructor.getInternalName()).thenReturn(QUX);
        when(definedSuperTypeConstructor.getDescriptor()).thenReturn(BAZ);
        when(superClassConstructor.isConstructor()).thenReturn(true);
        when(superClassConstructor.getDeclaringType()).thenReturn(superClass);
        when(superClassConstructor.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
        when(superClassConstructor.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InGenericShape>());
        when(invokableToken.getName()).thenReturn(FOO);
        when(superConstructorToken.getName()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
        super.setUp();
    }

    @Override
    protected Implementation.Target makeImplementationTarget() {
        return new SubclassImplementationTarget(instrumentedType, methodGraph, SubclassImplementationTarget.OriginTypeResolver.SUPER_TYPE);
    }

    @Test
    public void testSuperTypeMethodIsInvokable() throws Exception {
        when(invokableMethod.isSpecializableFor(rawSuperType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) invokableMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(rawSuperType));
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
        when(invokableMethod.isSpecializableFor(rawSuperType)).thenReturn(false);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(invokableToken);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testSuperConstructorIsInvokable() throws Exception {
        when(invokableMethod.isConstructor()).thenReturn(true);
        when(definedSuperTypeConstructor.isSpecializableFor(rawSuperType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(superConstructorToken);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) superClassConstructor));
        assertThat(specialMethodInvocation.getTypeDescription(), is(rawSuperType));
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
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassImplementationTarget.class).apply();
        ObjectPropertyAssertion.of(SubclassImplementationTarget.OriginTypeResolver.class).apply();
    }
}
