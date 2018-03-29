package net.bytebuddy.utility;

import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StreamDrainerTest {

    @Test
    public void testDrainage() throws Exception {
        byte[] input = new byte[]{1, 2, 3, 4};
        assertThat(new StreamDrainer(1).drain(new ByteArrayInputStream(input)), is(input));
    }
}
