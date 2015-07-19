package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.MethodLookupEngine;
import net.bytebuddy.implementation.AbstractImplementationTargetTest;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SubclassImplementationTargetTest extends AbstractImplementationTargetTest {

    private static final String BAR = "bar", BAZ = "baz", FOOBAR = "foobar";

    @Mock
    private MethodDescription.InDeclaredForm superMethod, superMethodConstructor;

    @Mock
    private TypeDescription superType;

    @Mock
    private ParameterList<?> parameterList;

    @Mock
    private GenericTypeList parameterTypes;

    @Override
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(parameterList.asTypeList()).thenReturn(parameterTypes);
        when(instrumentedType.getSuperType()).thenReturn(superType);
        when(superType.getDeclaredMethods())
                .thenReturn((MethodList) new MethodList.Explicit<MethodDescription>(Collections.singletonList(superMethodConstructor)));
        when(superType.getInternalName()).thenReturn(BAR);
        when(superType.asRawType()).thenReturn(superType);
        when(superType.asRawType()).thenReturn(superType);
        when(superType.getStackSize()).thenReturn(StackSize.ZERO);
        when(superMethod.getReturnType()).thenReturn(returnType);
        when(superMethod.getInternalName()).thenReturn(BAZ);
        when(superMethod.getDescriptor()).thenReturn(FOOBAR);
        when(superMethod.getParameters()).thenReturn((ParameterList) parameterList);
        when(superMethod.getDeclaringType()).thenReturn(superType);
        when(superMethod.asDeclared()).thenReturn(superMethod);
        when(superMethodConstructor.isConstructor()).thenReturn(true);
        when(superMethodConstructor.getParameters()).thenReturn((ParameterList) parameterList);
        when(superMethodConstructor.getReturnType()).thenReturn(returnType);
        when(superMethodConstructor.isSpecializableFor(superType)).thenReturn(true);
        when(superMethodConstructor.getInternalName()).thenReturn(QUXBAZ);
        when(superMethodConstructor.getDescriptor()).thenReturn(BAZBAR);
        when(superMethodConstructor.getDeclaringType()).thenReturn(superType);
        when(superMethodConstructor.asDeclared()).thenReturn(superMethodConstructor);
        super.setUp();
    }

    @Override
    protected Implementation.Target makeImplementationTarget() {
        return new SubclassImplementationTarget(finding,
                bridgeMethodResolverFactory,
                SubclassImplementationTarget.OriginTypeIdentifier.SUPER_TYPE);
    }

    @Test
    public void testSuperTypeMethodIsInvokable() throws Exception {
        when(superMethod.isSpecializableFor(superType)).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(superMethod, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) superMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(superType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, BAZ, FOOBAR, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    public void testAbstractSuperTypeMethodIsNotInvokable() throws Exception {
        when(superMethod.isSpecializableFor(superType)).thenReturn(true);
        when(superMethod.isAbstract()).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(superMethod, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testSuperConstructorIsInvokable() throws Exception {
        when(superMethod.isConstructor()).thenReturn(true);
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeSuper(superMethod, methodLookup);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) superMethodConstructor));
        assertThat(specialMethodInvocation.getTypeDescription(), is(superType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAR, QUXBAZ, BAZBAR, false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnknownConstructor() throws Exception {
        MethodDescription constructor = mock(MethodDescription.class);
        when(constructor.isConstructor()).thenReturn(true);
        when(constructor.getParameters()).thenReturn((ParameterList) new ParameterList.Empty());
        TypeDescription declaringType = mock(TypeDescription.class);
        when(declaringType.asRawType()).thenReturn(declaringType);
        when(constructor.getDeclaringType()).thenReturn(declaringType);
        when(constructor.asToken()).thenReturn(mock(MethodDescription.Token.class));
        assertThat(implementationTarget.invokeSuper(constructor, Implementation.Target.MethodLookup.Default.EXACT).isValid(), is(false));
    }

    @Test
    public void testSuperTypeOrigin() throws Exception {
        assertThat(new SubclassImplementationTarget(finding,
                        bridgeMethodResolverFactory,
                        SubclassImplementationTarget.OriginTypeIdentifier.SUPER_TYPE).getOriginType(),
                is(finding.getTypeDescription().getSuperType()));
    }

    @Test
    public void testLevelTypeOrigin() throws Exception {
        assertThat(new SubclassImplementationTarget(finding,
                        bridgeMethodResolverFactory,
                        SubclassImplementationTarget.OriginTypeIdentifier.LEVEL_TYPE).getOriginType(),
                is(finding.getTypeDescription()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassImplementationTarget.class).refine(new ObjectPropertyAssertion.Refinement<MethodLookupEngine.Finding>() {
            @Override
            public void apply(MethodLookupEngine.Finding mock) {
                when(mock.getInvokableMethods()).thenReturn((MethodList) new MethodList.Empty());
                when(mock.getInvokableDefaultMethods()).thenReturn(Collections.<TypeDescription, Set<MethodDescription>>emptyMap());
                TypeDescription typeDescription = mock(TypeDescription.class);
                when(mock.getTypeDescription()).thenReturn(typeDescription);
                when(typeDescription.asRawType()).thenReturn(typeDescription);
                when(typeDescription.getSuperType()).thenReturn(typeDescription);
                when(typeDescription.getDeclaredMethods()).thenReturn(new MethodList.Empty());
            }
        }).refine(new ObjectPropertyAssertion.Refinement<BridgeMethodResolver.Factory>() {
            @Override
            public void apply(BridgeMethodResolver.Factory mock) {
                when(mock.make(any(MethodList.class))).thenReturn(mock(BridgeMethodResolver.class));
            }
        }).apply();
        ObjectPropertyAssertion.of(SubclassImplementationTarget.OriginTypeIdentifier.class).apply();
    }
}
