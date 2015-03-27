package net.bytebuddy.instrumentation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class InstrumentationContextExtractableViewInjectedCodeNoneTest {

    @Test
    public void testNoInjectedCode() throws Exception {
        assertThat(Instrumentation.Context.ExtractableView.InjectedCode.None.INSTANCE.isDefined(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testApplicationThrowsException() throws Exception {
        Instrumentation.Context.ExtractableView.InjectedCode.None.INSTANCE.getStackManipulation();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Instrumentation.Context.ExtractableView.InjectedCode.None.class).apply();
    }
}
