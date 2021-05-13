package net.bytebuddy.utility;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class JavaConstantSimpleOtherTest {

    @Test
    public void testInteger() {
        assertThat(JavaConstant.Simple.ofDescription(42, (ClassLoader) null).asConstantDescription(), is((Object) 42));
    }
}
