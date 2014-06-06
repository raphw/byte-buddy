package net.bytebuddy.dynamic;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TargetTypeTest {

    @Test
    public void testIsFinal() throws Exception {
        assertThat(Modifier.isFinal(TargetType.class.getModifiers()), is(true));
    }

    @Test
    public void testMemberInaccessibility() throws Exception {
        assertThat(TargetType.class.getDeclaredMethods().length, is(0));
    }

    @Test
    public void testConstructorIsHidden() throws Exception {
        MatcherAssert.assertThat(TargetType.class.getDeclaredConstructors().length, is(1));
        Constructor<?> constructor = TargetType.class.getDeclaredConstructor();
        MatcherAssert.assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException e) {
            assertEquals(UnsupportedOperationException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testDescription() throws Exception {
        assertThat(TargetType.DESCRIPTION.represents(TargetType.class), is(true));
    }
}
