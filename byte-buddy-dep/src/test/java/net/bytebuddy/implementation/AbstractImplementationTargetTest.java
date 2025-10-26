package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public abstract class AbstractImplementationTargetTest {

    protected static final String FOO = "foo", QUX = "qux", BAZ = "baz", QUXBAZ = "quxbaz", FOOBAZ = "foobaz", BAZBAR = "bazbar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    protected MethodGraph.Linked methodGraph, superGraph, defaultGraph;

    @Mock
    protected TypeDescription instrumentedType, methodDeclaringType, returnType, defaultMethodDeclaringType;

    @Mock
    protected TypeDescription.Generic genericInstrumentedType, genericReturnType;

    @Mock
    protected MethodDescription.InDefinedShape invokableMethod, defaultMethod;

    @Mock
    protected MethodDescription.SignatureToken invokableToken, defaultToken;

    protected Implementation.Target.AbstractBase.DefaultMethodInvocation defaultMethodInvocation;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(instrumentedType.asErasure()).thenReturn(instrumentedType);
        when(instrumentedType.getInternalName()).thenReturn(BAZ);
        when(methodGraph.getSuperClassGraph()).thenReturn(superGraph);
        when(superGraph.locate(any(MethodDescription.SignatureToken.class))).thenReturn(MethodGraph.Node.Unresolved.INSTANCE);
        when(superGraph.locate(invokableToken)).thenReturn(new MethodGraph.Node.Simple(invokableMethod));
        when(methodGraph.getInterfaceGraph(defaultMethodDeclaringType)).thenReturn(defaultGraph);
        when(defaultGraph.locate(any(MethodDescription.SignatureToken.class))).thenReturn(MethodGraph.Node.Unresolved.INSTANCE);
        when(defaultGraph.locate(defaultToken)).thenReturn(new MethodGraph.Node.Simple(defaultMethod));
        when(methodDeclaringType.asErasure()).thenReturn(methodDeclaringType);
        when(invokableMethod.getDeclaringType()).thenReturn(methodDeclaringType);
        when(invokableMethod.getReturnType()).thenReturn(genericReturnType);
        when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(genericReturnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(returnType.asErasure()).thenReturn(returnType);
        when(invokableMethod.getInternalName()).thenReturn(FOO);
        when(invokableMethod.getDescriptor()).thenReturn(QUX);
        when(invokableMethod.asSignatureToken()).thenReturn(invokableToken);
        when(invokableMethod.asDefined()).thenReturn(invokableMethod);
        when(defaultMethod.getInternalName()).thenReturn(QUXBAZ);
        when(defaultMethod.getDescriptor()).thenReturn(FOOBAZ);
        when(defaultMethod.getDeclaringType()).thenReturn(defaultMethodDeclaringType);
        when(defaultMethod.getReturnType()).thenReturn(genericReturnType);
        when(defaultMethod.asSignatureToken()).thenReturn(defaultToken);
        when(defaultMethod.asDefined()).thenReturn(defaultMethod);
        when(defaultMethod.isSpecializableFor(defaultMethodDeclaringType)).thenReturn(true);
        when(defaultMethodDeclaringType.isInterface()).thenReturn(true);
        when(defaultMethodDeclaringType.asErasure()).thenReturn(defaultMethodDeclaringType);
        when(defaultMethodDeclaringType.getInternalName()).thenReturn(BAZBAR);
        when(genericReturnType.asErasure()).thenReturn(returnType);
        when(genericReturnType.asGenericType()).thenReturn(genericReturnType);
        when(returnType.asGenericType()).thenReturn(genericReturnType);
        when(genericInstrumentedType.asErasure()).thenReturn(instrumentedType);
        when(genericInstrumentedType.asGenericType()).thenReturn(genericInstrumentedType);
        when(instrumentedType.asGenericType()).thenReturn(genericInstrumentedType);
        defaultMethodInvocation = Implementation.Target.AbstractBase.DefaultMethodInvocation.ENABLED;
    }

    protected abstract Implementation.Target makeImplementationTarget();

    @Test
    public void testDefaultMethodInvocation() throws Exception {
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeDefault(defaultToken, defaultMethodDeclaringType);
        assertThat(specialMethodInvocation.isValid(), is(true));
        assertThat(specialMethodInvocation.getMethodDescription(), is((MethodDescription) defaultMethod));
        assertThat(specialMethodInvocation.getTypeDescription(), is(defaultMethodDeclaringType));
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = specialMethodInvocation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESPECIAL, BAZBAR, QUXBAZ, FOOBAZ, true);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testDefaultMethodInvocationNotSupported() throws Exception {
        defaultMethodInvocation = Implementation.Target.AbstractBase.DefaultMethodInvocation.DISABLED;
        Implementation.SpecialMethodInvocation specialMethodInvocation = makeImplementationTarget().invokeDefault(defaultToken, defaultMethodDeclaringType);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }

    @Test
    public void testIllegalDefaultMethod() throws Exception {
        assertThat(makeImplementationTarget().invokeDefault(mock(MethodDescription.SignatureToken.class), defaultMethodDeclaringType).isValid(), is(false));
    }

    @Test
    public void testIllegalSuperMethod() throws Exception {
        MethodDescription.SignatureToken token = mock(MethodDescription.SignatureToken.class);
        when(token.getName()).thenReturn(FOO);
        assertThat(makeImplementationTarget().invokeSuper(token).isValid(), is(false));
    }

    @Test
    public void testIllegalSuperConstructor() throws Exception {
        MethodDescription.SignatureToken token = mock(MethodDescription.SignatureToken.class);
        when(token.getName()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
        assertThat(makeImplementationTarget().invokeSuper(token).isValid(), is(false));
    }
}
