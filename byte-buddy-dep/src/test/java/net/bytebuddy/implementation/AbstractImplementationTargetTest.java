package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public abstract class AbstractImplementationTargetTest {

    protected static final String FOO = "foo", QUX = "qux", BAZ = "baz", QUXBAZ = "quxbaz", FOOBAZ = "foobaz", BAZBAR = "bazbar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    protected MethodGraph.Linked methodGraph, superGraph, defaultGraph;

    @Mock
    protected TypeDescription instrumentedType, methodDeclaringType, returnType, defaultMethodDeclaringType;

    @Mock
    protected MethodDescription.InDefinedShape invokableMethod, defaultMethod;

    protected Implementation.Target implementationTarget;

    @Mock
    protected MethodDescription.Token invokableToken, defaultToken;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(instrumentedType.asErasure()).thenReturn(instrumentedType);
        when(instrumentedType.getInternalName()).thenReturn(BAZ);
        when(methodGraph.getSuperGraph()).thenReturn(superGraph);
        when(superGraph.locate(Mockito.any(MethodDescription.Token.class))).thenReturn(MethodGraph.Node.Unresolved.INSTANCE);
        when(superGraph.locate(invokableToken)).thenReturn(new MethodGraph.Node.Simple(invokableMethod));
        when(methodGraph.getInterfaceGraph(defaultMethodDeclaringType)).thenReturn(defaultGraph);
        when(defaultGraph.locate(Mockito.any(MethodDescription.Token.class))).thenReturn(MethodGraph.Node.Unresolved.INSTANCE);
        when(defaultGraph.locate(defaultToken)).thenReturn(new MethodGraph.Node.Simple(defaultMethod));
        when(methodDeclaringType.asErasure()).thenReturn(methodDeclaringType);
        when(invokableMethod.getDeclaringType()).thenReturn(methodDeclaringType);
        when(invokableMethod.getReturnType()).thenReturn(returnType);
        when(returnType.getStackSize()).thenReturn(StackSize.ZERO);
        when(returnType.asErasure()).thenReturn(returnType);
        when(invokableMethod.getInternalName()).thenReturn(FOO);
        when(invokableMethod.getDescriptor()).thenReturn(QUX);
        when(invokableMethod.asToken()).thenReturn(invokableToken);
        when(invokableMethod.asDefined()).thenReturn(invokableMethod);
        when(defaultMethod.getInternalName()).thenReturn(QUXBAZ);
        when(defaultMethod.getDescriptor()).thenReturn(FOOBAZ);
        when(defaultMethod.getDeclaringType()).thenReturn(defaultMethodDeclaringType);
        when(defaultMethod.getReturnType()).thenReturn(returnType);
        when(defaultMethod.asToken()).thenReturn(defaultToken);
        when(defaultMethod.asDefined()).thenReturn(defaultMethod);
        when(defaultMethod.isSpecializableFor(defaultMethodDeclaringType)).thenReturn(true);
        when(defaultMethodDeclaringType.isInterface()).thenReturn(true);
        when(defaultMethodDeclaringType.asErasure()).thenReturn(defaultMethodDeclaringType);
        when(defaultMethodDeclaringType.getInternalName()).thenReturn(BAZBAR);
        implementationTarget = makeImplementationTarget();
    }

    protected abstract Implementation.Target makeImplementationTarget();

    @Test
    public void testDefaultMethodInvocation() throws Exception {
        Implementation.SpecialMethodInvocation specialMethodInvocation = implementationTarget.invokeDefault(defaultMethodDeclaringType, defaultToken);
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
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testIllegalDefaultMethod() throws Exception {
        assertThat(implementationTarget.invokeDefault(defaultMethodDeclaringType, mock(MethodDescription.Token.class)).isValid(), is(false));
    }

    @Test
    public void testIllegalSuperMethod() throws Exception {
        MethodDescription.Token methodToken = mock(MethodDescription.Token.class);
        when(methodToken.getInternalName()).thenReturn(FOO);
        assertThat(implementationTarget.invokeSuper(methodToken).isValid(), is(false));
    }

    @Test
    public void testIllegalSuperConstructor() throws Exception {
        MethodDescription.Token methodToken = mock(MethodDescription.Token.class);
        when(methodToken.getInternalName()).thenReturn(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
        assertThat(implementationTarget.invokeSuper(methodToken).isValid(), is(false));
    }
}
