package net.bytebuddy.dynamic;

import net.bytebuddy.utility.ByteBuddyCommons;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class TargetTypeTest {

    @Test
    public void testConstructorIsHidden() throws Exception {
        assertThat(TargetType.class.getDeclaredConstructors().length, is(1));
        Constructor<?> constructor = TargetType.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause().getClass(), CoreMatchers.<Class<?>>is(UnsupportedOperationException.class));
        }
    }

    @Test
    public void testTypeIsFinal() throws Exception {
        assertThat(Modifier.isFinal(ByteBuddyCommons.class.getModifiers()), is(true));
    }
}
