package net.bytebuddy.implementation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ImplementationContextExtractableViewInjectedCodeNoneTest {

    @Test
    public void testNoInjectedCode() throws Exception {
        assertThat(Implementation.Context.ExtractableView.InjectedCode.None.INSTANCE.isDefined(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testApplicationThrowsException() throws Exception {
        Implementation.Context.ExtractableView.InjectedCode.None.INSTANCE.getByteCodeAppender();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Implementation.Context.ExtractableView.InjectedCode.None.class).apply();
    }
}
