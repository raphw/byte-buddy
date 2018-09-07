package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassFileLocatorForUrlTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int VALUE = 42;

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private File file;

    @Before
    public void setUp() throws Exception {
        file = File.createTempFile(FOO, BAR);
    }

    @After
    public void tearDown() throws Exception {
        assertThat(file.delete(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(7) // Avoid leak since class loader cannot be closed
    public void testSuccessfulLocation() throws Exception {
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file));
        try {
            JarEntry jarEntry = new JarEntry(FOO + "/" + BAR + ".class");
            jarOutputStream.putNextEntry(jarEntry);
            jarOutputStream.write(VALUE);
            jarOutputStream.write(VALUE * 2);
            jarOutputStream.closeEntry();
        } finally {
            jarOutputStream.close();
        }
        URL url = file.toURI().toURL();
        ClassFileLocator classFileLocator = new ClassFileLocator.ForUrl(Collections.singleton(url));
        try {
            ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
            assertThat(resolution.isResolved(), is(true));
            assertThat(resolution.resolve(), is(new byte[]{VALUE, VALUE * 2}));
        } finally {
            classFileLocator.close();
        }
    }

    @Test
    @JavaVersionRule.Enforce(7) // Avoid leak since class loader cannot be closed
    public void testJarFileClosable() throws Exception {
        URL url = new URL("http://localhost:123");
        Closeable classFileLocator = new ClassFileLocator.ForUrl(url);
        classFileLocator.close();
    }

    @Test
    @JavaVersionRule.Enforce(7) // Avoid leak since class loader cannot be closed
    public void testNonSuccessfulLocation() throws Exception {
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file));
        try {
            JarEntry jarEntry = new JarEntry("noop.class");
            jarOutputStream.putNextEntry(jarEntry);
            jarOutputStream.write(VALUE);
            jarOutputStream.closeEntry();
        } finally {
            jarOutputStream.close();
        }
        URL url = file.toURI().toURL();
        ClassFileLocator classFileLocator = new ClassFileLocator.ForUrl(url);
        try {
            ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
            assertThat(resolution.isResolved(), is(false));
        } finally {
            classFileLocator.close();
        }
    }
}
