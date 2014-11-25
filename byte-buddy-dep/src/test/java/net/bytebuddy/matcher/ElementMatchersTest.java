package net.bytebuddy.matcher;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ElementMatchersTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testConstructor() throws Exception {
        assertThat(Modifier.isPrivate(ElementMatchers.class.getDeclaredConstructor().getModifiers()), is(true));
        Constructor<?> constructor = ElementMatchers.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException e) {
            throw (UnsupportedOperationException) e.getCause();
        }
    }
}
