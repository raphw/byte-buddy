package net.bytebuddy.instrumentation.type;

import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class InstrumentedTypeTypeInitializerTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private StackManipulation stackManipulation;

    @Test
    public void testNoneExpansion() throws Exception {
        assertThat(InstrumentedType.TypeInitializer.None.INSTANCE.expandWith(stackManipulation),
                is((InstrumentedType.TypeInitializer) new InstrumentedType.TypeInitializer.Simple(stackManipulation)));
    }

    @Test
    public void testNoneDefined() throws Exception {
        assertThat(InstrumentedType.TypeInitializer.None.INSTANCE.isDefined(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoneThrowsException() throws Exception {
        InstrumentedType.TypeInitializer.None.INSTANCE.getStackManipulation();
    }

    @Test
    public void testSimpleExpansion() throws Exception {
        assertThat(new InstrumentedType.TypeInitializer.Simple(stackManipulation).expandWith(stackManipulation),
                is((InstrumentedType.TypeInitializer) new InstrumentedType.TypeInitializer
                        .Simple(new StackManipulation.Compound(stackManipulation, stackManipulation))));
    }

    @Test
    public void testSimpleDefined() throws Exception {
        assertThat(new InstrumentedType.TypeInitializer.Simple(stackManipulation).isDefined(), is(true));
    }

    @Test
    public void testSimple() throws Exception {
        assertThat(new InstrumentedType.TypeInitializer.Simple(stackManipulation).getStackManipulation(), is(stackManipulation));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InstrumentedType.TypeInitializer.Simple.class).apply();
    }
}
