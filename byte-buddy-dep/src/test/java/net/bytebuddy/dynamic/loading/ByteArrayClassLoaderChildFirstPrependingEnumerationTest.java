package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.net.URL;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ByteArrayClassLoaderChildFirstPrependingEnumerationTest {

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private URL first, second, third;

    @Before
    public void setUp() throws Exception {
        first = new URL("file://foo");
        second = new URL("file://bar");
        third = new URL("file://qux");
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

    @Test
    public void testObjectPropertyAssertion() throws Exception {
        final Iterator<URL> urls = Arrays.asList(new URL("file://foo"), new URL("file://bar")).iterator();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.ChildFirst.PrependingEnumeration.class).create(new ObjectPropertyAssertion.Creator<URL>() {
            @Override
            public URL create() {
                return urls.next();
            }
        }).applyBasic();
    }
}