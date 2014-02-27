package com.blogspot.mydailyjava.bytebuddy.dynamic;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TargetTypeTest {

    @Test
    public void testIsFinal() throws Exception {
        assertThat(Modifier.isFinal(TargetType.class.getModifiers()), is(true));
    }

    @Test
    public void testMemberInaccessibility() throws Exception {
        assertThat(TargetType.class.getDeclaredMethods().length, is(0));
        assertThat(TargetType.class.getDeclaredConstructors().length, is(1));
        assertThat(Modifier.isPrivate(TargetType.class.getDeclaredConstructor().getModifiers()), is(true));
    }
}
