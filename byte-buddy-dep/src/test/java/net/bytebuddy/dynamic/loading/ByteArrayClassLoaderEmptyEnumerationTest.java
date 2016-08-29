package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteArrayClassLoaderEmptyEnumerationTest {

    @Test
    public void testNoFurtherElements() throws Exception {
        assertThat(ByteArrayClassLoader.EmptyEnumeration.INSTANCE.hasMoreElements(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testNextElementThrowsException() throws Exception {
        ByteArrayClassLoader.EmptyEnumeration.INSTANCE.nextElement();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteArrayClassLoader.EmptyEnumeration.class).apply();
    }
}
