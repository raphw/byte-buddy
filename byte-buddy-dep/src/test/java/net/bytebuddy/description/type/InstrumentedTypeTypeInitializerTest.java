package net.bytebuddy.description.type;

import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
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

public class InstrumentedTypeTypeInitializerTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testNoneExpansion() throws Exception {
        assertThat(InstrumentedType.TypeInitializer.None.INSTANCE.expandWith(stackManipulation),
                is((InstrumentedType.TypeInitializer) new InstrumentedType.TypeInitializer.Simple(stackManipulation)));
    }

    @Test
    public void testNoneDefined() throws Exception {
        assertThat(InstrumentedType.TypeInitializer.None.INSTANCE.isDefined(), is(false));
        assertThat(InstrumentedType.TypeInitializer.None.INSTANCE.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoneThrowsExceptionOnApplication() throws Exception {
        InstrumentedType.TypeInitializer.None.INSTANCE.apply(methodVisitor, implementationContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoneThrowsExceptionOnTermination() throws Exception {
        InstrumentedType.TypeInitializer.None.INSTANCE.terminate();
    }

    @Test
    public void testSimpleExpansion() throws Exception {
        assertThat(new InstrumentedType.TypeInitializer.Simple(stackManipulation).expandWith(stackManipulation),
                is((InstrumentedType.TypeInitializer) new InstrumentedType.TypeInitializer
                        .Simple(new StackManipulation.Compound(stackManipulation, stackManipulation))));
    }

    @Test
    public void testSimpleApplication() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
        InstrumentedType.TypeInitializer typeInitializer = new InstrumentedType.TypeInitializer.Simple(stackManipulation);
        assertThat(typeInitializer.isDefined(), is(true));
        assertThat(typeInitializer.isValid(), is(true));
        verify(stackManipulation).isValid();
        typeInitializer.apply(methodVisitor, implementationContext);
        verify(stackManipulation).apply(methodVisitor, implementationContext);
        verifyZeroInteractions(stackManipulation);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testSimpleApplicationAfterTermination() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
        when(stackManipulation.apply(methodVisitor, implementationContext)).thenReturn(new StackManipulation.Size(0, 0));
        StackManipulation terminated = new InstrumentedType.TypeInitializer.Simple(stackManipulation).terminate();
        assertThat(terminated.isValid(), is(true));
        verify(stackManipulation).isValid();
        terminated.apply(methodVisitor, implementationContext);
        verify(stackManipulation).apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitInsn(Opcodes.RETURN);
        verifyNoMoreInteractions(stackManipulation);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InstrumentedType.TypeInitializer.Simple.class).apply();
        ObjectPropertyAssertion.of(InstrumentedType.TypeInitializer.None.class).apply();
    }
}
