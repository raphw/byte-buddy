package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TypeSizeTest {

    @Test
    public void testSizes() throws Exception {
        assertThat(TypeSize.of(void.class).getSize(), is(0));
        assertThat(TypeSize.of(boolean.class).getSize(), is(1));
        assertThat(TypeSize.of(byte.class).getSize(), is(1));
        assertThat(TypeSize.of(short.class).getSize(), is(1));
        assertThat(TypeSize.of(int.class).getSize(), is(1));
        assertThat(TypeSize.of(char.class).getSize(), is(1));
        assertThat(TypeSize.of(float.class).getSize(), is(1));
        assertThat(TypeSize.of(Object.class).getSize(), is(1));
        assertThat(TypeSize.of(long.class).getSize(), is(2));
        assertThat(TypeSize.of(double.class).getSize(), is(2));
    }
}
