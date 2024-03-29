package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class StackManipulationTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testLegalIsValid() throws Exception {
        assertThat(StackManipulation.Trivial.INSTANCE.isValid(), is(true));
    }

    @Test
    public void testIllegalIsNotValid() throws Exception {
        assertThat(StackManipulation.Illegal.INSTANCE.isValid(), is(false));
    }

    @Test
    public void testLegalIsApplicable() throws Exception {
        StackManipulation.Size size = StackManipulation.Trivial.INSTANCE.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalIsNotApplicable() throws Exception {
        StackManipulation.Illegal.INSTANCE.apply(methodVisitor, implementationContext);
    }
}
