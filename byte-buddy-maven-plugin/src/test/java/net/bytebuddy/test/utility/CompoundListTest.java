package net.bytebuddy.test.utility;

import net.bytebuddy.utility.CompoundList;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static junit.framework.TestCase.fail;

public class CompoundListTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testConstruction() throws Throwable {
        Constructor<?> constructor = CompoundList.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
