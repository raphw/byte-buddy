package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.IntegrationRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteArrayClassLoaderSingletonEnumerationTest {

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    @Test
    @IntegrationRule.Enforce
    public void testIteration() throws Exception {
        URL url = new URL("file://foo");
        Enumeration<URL> enumeration = new ByteArrayClassLoader.SingletonEnumeration(url);
        assertThat(enumeration.hasMoreElements(), is(true));
        assertThat(enumeration.nextElement(), is(url));
        assertThat(enumeration.hasMoreElements(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testSecondElementThrowsException() throws Exception {
        Enumeration<URL> enumeration = new ByteArrayClassLoader.SingletonEnumeration(new URL("file://foo"));
        enumeration.nextElement();
        enumeration.nextElement();
    }
}
