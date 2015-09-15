package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

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

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<URL> urls = Arrays.asList(new URL("file://foo"), new URL("file://bar")).iterator();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.SingletonEnumeration.class).create(new ObjectPropertyAssertion.Creator<URL>() {
            @Override
            public URL create() {
                return urls.next();
            }
        }).applyBasic();
    }
}