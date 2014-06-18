package net.bytebuddy.instrumentation;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeInitializerNoOp {

    @Test
    public void testIsNotAlive() throws Exception {
        assertThat(LoadedTypeInitializer.NoOp.INSTANCE.isAlive(), is(false));
    }
}
