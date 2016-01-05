package net.bytebuddy.description.type;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeInitializerTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ByteCodeAppender byteCodeAppender;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testNoneExpansion() throws Exception {
        assertThat(TypeInitializer.None.INSTANCE.expandWith(byteCodeAppender),
                is((TypeInitializer) new TypeInitializer.Simple(byteCodeAppender)));
    }

    @Test
    public void testNoneDefined() throws Exception {
        assertThat(TypeInitializer.None.INSTANCE.isDefined(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoneThrowsExceptionOnApplication() throws Exception {
        TypeInitializer.None.INSTANCE.apply(methodVisitor, implementationContext, methodDescription);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoneThrowsExceptionOnTermination() throws Exception {
        TypeInitializer.None.INSTANCE.withReturn();
    }

    @Test
    public void testSimpleExpansion() throws Exception {
        assertThat(new TypeInitializer.Simple(byteCodeAppender).expandWith(byteCodeAppender),
                is((TypeInitializer) new TypeInitializer.Simple(new ByteCodeAppender.Compound(byteCodeAppender, byteCodeAppender))));
    }

    @Test
    public void testSimpleApplication() throws Exception {
        TypeInitializer typeInitializer = new TypeInitializer.Simple(byteCodeAppender);
        assertThat(typeInitializer.isDefined(), is(true));
        typeInitializer.apply(methodVisitor, implementationContext, methodDescription);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyZeroInteractions(byteCodeAppender);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testSimpleApplicationAfterTermination() throws Exception {
        when(byteCodeAppender.apply(methodVisitor, implementationContext, methodDescription)).thenReturn(new ByteCodeAppender.Size(0, 0));
        ByteCodeAppender terminated = new TypeInitializer.Simple(byteCodeAppender).withReturn();
        terminated.apply(methodVisitor, implementationContext, methodDescription);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verify(methodVisitor).visitInsn(Opcodes.RETURN);
        verifyNoMoreInteractions(byteCodeAppender);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeInitializer.Simple.class).apply();
        ObjectPropertyAssertion.of(TypeInitializer.None.class).apply();
    }
}
