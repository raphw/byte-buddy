package net.bytebuddy.instrumentation;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LoadedTypeInitializerNoOp {

    @Test
    public void testIsNotAlive() throws Exception {
        assertThat(LoadedTypeInitializer.NoOp.INSTANCE.isAlive(), is(false));
    }
}
