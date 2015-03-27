package net.bytebuddy.instrumentation.method.bytecode.stack;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

public class StackManipulationTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Instrumentation.Context instrumentationContext;

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testLegalIsValid() throws Exception {
        assertThat(StackManipulation.LegalTrivial.INSTANCE.isValid(), is(true));
    }

    @Test
    public void testIllegalIsNotValid() throws Exception {
        assertThat(StackManipulation.Illegal.INSTANCE.isValid(), is(false));
    }

    @Test
    public void testLegalIsApplicable() throws Exception {
        StackManipulation.Size size = StackManipulation.LegalTrivial.INSTANCE.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalIsNotApplicable() throws Exception {
        StackManipulation.Illegal.INSTANCE.apply(methodVisitor, instrumentationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(StackManipulation.LegalTrivial.class).apply();
        ObjectPropertyAssertion.of(StackManipulation.Illegal.class).apply();
    }
}
