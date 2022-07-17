package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ImplementationSimpleTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testSimple() {
        ByteCodeAppender.Size size = new Implementation.Simple(StackManipulation.Trivial.INSTANCE)
                .appender(implementationTarget)
                .apply(methodVisitor, implementationContext, methodDescription);
        assertThat(size.getOperandStackSize(), is(0));
        assertThat(size.getLocalVariableSize(), is(0));
    }

    @Test
    public void testDispatcher() {
        ByteCodeAppender.Size size = Implementation.Simple.of(new Implementation.Simple.Dispatcher() {
            public StackManipulation apply(Implementation.Target implementationTarget, MethodDescription instrumentedMethod) {
                return StackManipulation.Trivial.INSTANCE;
            }
        }).appender(implementationTarget).apply(methodVisitor, implementationContext, methodDescription);
        assertThat(size.getOperandStackSize(), is(0));
        assertThat(size.getLocalVariableSize(), is(0));
    }

    @Test
    public void testDispatcherWithExtendedLocalVariableArray() {
        ByteCodeAppender.Size size = Implementation.Simple.of(new Implementation.Simple.Dispatcher() {
            public StackManipulation apply(Implementation.Target implementationTarget, MethodDescription instrumentedMethod) {
                return StackManipulation.Trivial.INSTANCE;
            }
        }, 42).appender(implementationTarget).apply(methodVisitor, implementationContext, methodDescription);
        assertThat(size.getOperandStackSize(), is(0));
        assertThat(size.getLocalVariableSize(), is(42));
    }
}
