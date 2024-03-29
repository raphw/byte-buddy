package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.IntegrationRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteArrayClassLoaderChildFirstPrependingEnumerationTest {

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private URL first, second, third;

    @Before
    public void setUp() throws Exception {
        first = URI.create("file://foo").toURL();
        second = URI.create("file://bar").toURL();
        third = URI.create("file://qux").toURL();
    }

    @Test
    @IntegrationRule.Enforce
    public void testPrepending() throws Exception {
        Vector<URL> vector = new Vector<URL>();
        vector.add(second);
        vector.add(third);
        vector.add(first);
        Enumeration<URL> enumeration = new ByteArrayClassLoader.ChildFirst.PrependingEnumeration(first, vector.elements());
        assertThat(enumeration.hasMoreElements(), is(true));
        assertThat(enumeration.nextElement(), is(first));
        assertThat(enumeration.hasMoreElements(), is(true));
        assertThat(enumeration.nextElement(), is(second));
        assertThat(enumeration.hasMoreElements(), is(true));
        assertThat(enumeration.nextElement(), is(third));
        assertThat(enumeration.hasMoreElements(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testNextElementThrowsException() throws Exception {
        Vector<URL> vector = new Vector<URL>();
        vector.add(second);
        vector.add(third);
        vector.add(first);
        Enumeration<URL> enumeration = new ByteArrayClassLoader.ChildFirst.PrependingEnumeration(first, vector.elements());
        enumeration.nextElement();
        enumeration.nextElement();
        enumeration.nextElement();
        enumeration.nextElement();
    }
}
