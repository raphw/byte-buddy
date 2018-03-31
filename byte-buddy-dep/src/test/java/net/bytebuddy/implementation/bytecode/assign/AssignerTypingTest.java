package net.bytebuddy.implementation.bytecode.assign;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AssignerTypingTest {

    @Test
    public void testStatic() throws Exception {
        assertThat(Assigner.Typing.of(false), is(Assigner.Typing.STATIC));
        assertThat(Assigner.Typing.STATIC.isDynamic(), is(false));
    }

    @Test
    public void testDynamic() throws Exception {
        assertThat(Assigner.Typing.of(true), is(Assigner.Typing.DYNAMIC));
        assertThat(Assigner.Typing.DYNAMIC.isDynamic(), is(true));
    }
}
